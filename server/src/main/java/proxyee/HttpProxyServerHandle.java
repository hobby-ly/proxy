package proxyee;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.util.ReferenceCountUtil;
import prv.liuyao.proxy.utils.handler.WriteBackToClientHandler;
import prv.liuyao.proxy.utils.queue.SimpleDisruptor;

import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HttpProxyServerHandle extends ChannelInboundHandlerAdapter {

    //http代理隧道握手成功
    public final static HttpResponseStatus SUCCESS = new HttpResponseStatus(200,
            "Connection established");

    private ChannelFuture cf;
    private String host;
    private int port;
    private int status = 0;
    private SimpleDisruptor disruptor = new SimpleDisruptor()
            .registryConsumer(o -> {

            });
    private List requestList;
    private Boolean isConnect;

    public HttpProxyServerHandle() { }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
        if (msg instanceof HttpRequest) {
            HttpRequest request = (HttpRequest) msg;
            //第一次建立连接取host和端口号和处理代理握手
            if (status == 0) {
                boolean requestProto = getRequestProto(request);
                if (!requestProto) { //bad request
                    ctx.channel().close();
                    return;
                }
                status = 1;
                if ("CONNECT".equalsIgnoreCase(request.method().name())) {//建立代理握手
//                    System.out.println("CONNECT----\n" + request);
                    status = 2;
                    HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, SUCCESS);
                    ctx.writeAndFlush(response);
                    ctx.channel().pipeline().remove("httpCodec");
                    //fix issue #42
                    ReferenceCountUtil.release(msg);
                    return;
                }
            }
            //fix issue #27
            if (request.uri().indexOf("/") != 0) {
                URL url = new URL(request.uri());
                request.setUri(url.getFile());
            }
            // todo 只有这里不会进行channel 初始化
            handleProxyData(ctx.channel(), request, true);
//            System.out.println("HttpRequest----\n" + msg);
        } else if (msg instanceof HttpContent) {
//            System.out.println("HttpContent " + status + "----\n" + msg);
            if (status == 2) {
                ReferenceCountUtil.release(msg);
                status = 1;
            } else {
                handleProxyData(ctx.channel(), msg, true);
            }
        } else { //ssl和websocket的握手处理
            handleProxyData(ctx.channel(), msg, false);
//            System.out.println("Not Http ---- \n" + msg);
        }
//        System.out.println(" =============================== ");
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        if (cf != null) {
            cf.channel().close();
        }
        ctx.channel().close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (cf != null) {
            cf.channel().close();
        }
        ctx.channel().close();
        cause.printStackTrace();
        throw new Exception(cause);
    }

    private void handleProxyData(Channel channel, Object msg, boolean isHttp) throws Exception {
        if (cf == null) {
            //connection异常 还有HttpContent进来，不转发
            if (isHttp && !(msg instanceof HttpRequest)) {
                System.out.println("ishttp");
                return;
            }
            this.cf = new Bootstrap()
                    .group(new NioEventLoopGroup(1))
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer(){
                        @Override
                        protected void initChannel(Channel ch) throws Exception {
                            if (isHttp) {
                                ch.pipeline().addLast("httpCodec", new HttpClientCodec());
                            }
                            ch.pipeline().addLast(new WriteBackToClientHandler(channel) {
                                @Override
                                public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                    super.channelRead(ctx, msg);
                                    if (isHttp && msg instanceof HttpResponse) {
                                        String upgrade  = ((HttpResponse) msg).headers().get(HttpHeaderNames.UPGRADE);
                                        if (HttpHeaderValues.WEBSOCKET.toString().equals(upgrade)) {
                                            System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> remove http codec");
                                            //websocket转发原始报文
                                            ctx.channel().pipeline().remove("httpCodec");
                                            this.clientChannel.pipeline().remove("httpCodec");
                                        }
                                    }
                                }
                            });
                        }
                    }).connect(host, port); // todo 不要使用sync  会阻塞其他连接

            requestList = new LinkedList();
            cf.addListener((ChannelFutureListener) future -> {
                isConnect = future.isSuccess();
                handlerData(msg);
//                System.out.println(this.cf.isSuccess() + " -- " + future.isSuccess());
//                if (future.isSuccess()) {
//                    future.channel().writeAndFlush(msg);
//                    synchronized (requestList) {
//                        requestList.forEach(obj -> future.channel().writeAndFlush(obj));
//                        requestList.clear();
//                        isConnect = true;
//                    }
//                } else {
//                    requestList.forEach(obj -> ReferenceCountUtil.release(obj));
//                    requestList.clear();
//                    future.channel().close();
//                }
            });
        }
        else {
            handlerData(msg);
//            synchronized (requestList) {
//                if (isConnect) {
//                    cf.channel().writeAndFlush(msg);
//                } else {
//                    requestList.add(msg);
//                }
//            }
        }
    }

    // 消息队列实现 单机最快Dispatch
    private void handlerData(Object msg) {
        System.out.println(isConnect + " 消息积压： " + requestList.size());
        if (null == isConnect) {
            requestList.add(msg);
        } else if (isConnect) {
            this.cf.channel().writeAndFlush(msg);
            synchronized (requestList) {
                requestList.forEach(obj -> this.cf.channel().writeAndFlush(obj));
                requestList.clear();
            }
        } else {
            requestList.forEach(obj -> ReferenceCountUtil.release(obj));
            requestList.clear();
        }
    }

    public boolean getRequestProto(HttpRequest httpRequest) {
        int port = -1;
        String hostStr = httpRequest.headers().get(HttpHeaderNames.HOST);
        if (hostStr == null) {
            Pattern pattern = Pattern.compile("^(?:https?://)?(?<host>[^/]*)/?.*$");
            Matcher matcher = pattern.matcher(httpRequest.uri());
            if (matcher.find()) {
                hostStr = matcher.group("host");
            } else {
                return false;
            }
        }
        String uriStr = httpRequest.uri();
        Pattern pattern = Pattern.compile("^(?:https?://)?(?<host>[^:]*)(?::(?<port>\\d+))?(/.*)?$");
        Matcher matcher = pattern.matcher(hostStr);
        //先从host上取端口号没取到再从uri上取端口号 issues#4
        String portTemp = null;
        if (matcher.find()) {
            this.host = matcher.group("host");
            portTemp = matcher.group("port");
            if (portTemp == null) {
                matcher = pattern.matcher(uriStr);
                if (matcher.find()) {
                    portTemp = matcher.group("port");
                }
            }
        }
        if (portTemp != null) {
            port = Integer.parseInt(portTemp);
        }
        boolean isSsl = uriStr.indexOf("https") == 0 || hostStr.indexOf("https") == 0;
        if (port == -1) {
            if (isSsl) {
                port = 443;
            } else {
                port = 80;
            }
        }
        this.port = port;
        return true;
    }

}
