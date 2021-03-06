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
import java.util.function.Consumer;
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
    private Consumer disruptorConsumer;
    private SimpleDisruptor tcpDisruptor = new SimpleDisruptor()
            .registryConsumer(o -> disruptorConsumer.accept(o)) // MQ 需保证数据包顺序
            ;

    public HttpProxyServerHandle() { }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
//        System.out.println(msg);
        String[] split = msg.getClass().toString().split("\\.");
        System.out.print(this.toString().split("@")[1] + " " + split[split.length-1] + " -> ");



        if (msg instanceof HttpRequest) {
            HttpRequest request = (HttpRequest) msg;
            //第一次建立连接取host和端口号和处理代理握手
            if (status == 0) {
                boolean requestProto = getRequestProto(request);
                if (!requestProto) { //bad request
                    ctx.channel().close();
                    System.out.println();
                    return;
                }
                status = 1;
                System.out.print(request.method().name() + " -> " + request.uri());



                // todo 只有https协议会走
                if ("CONNECT".equalsIgnoreCase(request.method().name())) {//建立代理握手
//                    System.out.println("CONNECT----\n" + request);
                    status = 2;
                    HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, SUCCESS);
                    ctx.writeAndFlush(response);
                    ctx.channel().pipeline().remove("httpCodec");
                    //fix issue #42
                    ReferenceCountUtil.release(msg);
                    System.out.println();
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
            System.out.print("content status: " + status + " -> ");
            if (status == 2) {  // todo https connect 的下一个包不处理 直接释放内存
                System.out.println(msg);
                ReferenceCountUtil.release(msg);
                status = 1;
                return;
            } else {
                handleProxyData(ctx.channel(), msg, true);
            }
        } else { //ssl和websocket的握手处理
            handleProxyData(ctx.channel(), msg, false);
//            System.out.println("Not Http ---- \n" + msg);
        }
        tcpDisruptor.push(msg); // 按照数据包到来的顺序放到队列头部
        System.out.println();
        System.out.println("----------------------------------------------------------------------------------------");
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

            cf.addListener(future -> {
                if (future.isSuccess()) {
                    disruptorConsumer = o -> cf.channel().writeAndFlush(o);
                } else {
                    disruptorConsumer = o -> ReferenceCountUtil.release(o);
                }
//                disruptor.push(msg); // 注意 首次连接 需要放到队列头部，若在这里加 则有可能晚于第二个数据包
                tcpDisruptor.start(); // 启动MQ
            });

            // 这种 消费没问题
//            disruptor.registryConsumer(o -> this.cf.channel().writeAndFlush(o));
//            disruptor.start(); // 启动MQ
//            System.out.println("------------------------- start ");

            // 这种 消费者失效
//            cf.addListener(future -> {
//                if (future.isSuccess()) {
//                    tcpDisruptor.registryConsumer(o -> cf.channel().writeAndFlush(o)).start();
//                } else {
//                    tcpDisruptor.registryConsumer(o -> ReferenceCountUtil.release(o)).start();
//                }
//            });
        }
    }

    // 解析http请求
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
        this.port = port == -1 ? (isSsl ? 443 : 80) : port;
        return true;
    }

    private void println(Object str) {
        System.out.println(this.toString().split("@")[1] + " --> " + str);
    }
}
