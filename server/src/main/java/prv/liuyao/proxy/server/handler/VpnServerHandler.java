package prv.liuyao.proxy.server.handler;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.proxy.HttpProxyHandler;
import io.netty.handler.proxy.ProxyHandler;
import io.netty.handler.proxy.Socks4ProxyHandler;
import io.netty.handler.proxy.Socks5ProxyHandler;
import io.netty.resolver.NoopAddressResolverGroup;
import io.netty.util.ReferenceCountUtil;
import prv.liuyao.proxy.server.ServerStarter;

import java.net.InetSocketAddress;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VpnServerHandler extends ChannelInboundHandlerAdapter {

    private HttpResponseStatus SUCCESS =
            new HttpResponseStatus(200, "Connection established");

    private int status = 0;
    private String host;
    private int port;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HttpRequest) {
            HttpRequest request = (HttpRequest) msg;
            getRequestProto(request);
            this.status = 1;

            if ("CONNECT".equalsIgnoreCase(request.method().name())) {//建立代理握手
                this.status = 2;
                HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, SUCCESS);
                ctx.writeAndFlush(response);
                ctx.channel().pipeline().remove(ServerStarter.HTTP_DECODEC_NAME);
                //fix issue #42
                ReferenceCountUtil.release(msg);
                return;
            }
            //fix issue #27
            if (request.uri().indexOf("/") != 0) {
                URL url = new URL(request.uri());
                request.setUri(url.getFile());
            }
            handleProxyData(ctx.channel(), request, true);
        } else if (msg instanceof HttpContent) {
            if (status != 2) {
                handleProxyData(ctx.channel(), (HttpContent) msg, true);
            } else {
                ReferenceCountUtil.release(msg);
                status = 1;
            }
        } else {
            handleProxyData(ctx.channel(), msg, false);
        }
    }

    private ChannelFuture cf;
    private List requestList;
    private boolean isConnect;

    private void handleProxyData(Channel clientChannel, Object msg, boolean isHttp)
            throws Exception {
        if (cf == null) {
            //connection异常 还有HttpContent进来，不转发
            if (isHttp && !(msg instanceof HttpRequest)) {
                return;
            }
      /*
        添加SSL client hello的Server Name Indication extension(SNI扩展)
        有些服务器对于client hello不带SNI扩展时会直接返回Received fatal alert: handshake_failure(握手错误)
        例如：https://cdn.mdn.mozilla.net/static/img/favicon32.7f3da72dcea1.png
       */
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(new NioEventLoopGroup(1)) // 注册线程池
                    .channel(NioSocketChannel.class) // 使用NioSocketChannel来作为连接用的channel类
                    .handler(new ChannelInitializer(){
                        @Override
                        protected void initChannel(Channel ch0) throws Exception {
                            if (isHttp) {
                                ch0.pipeline().addLast(ServerStarter.HTTP_DECODEC_NAME, new HttpClientCodec());
                            }
                            ch0.pipeline().addLast(new VpnForwardHandler(clientChannel, isHttp));
                        }
                    });

            requestList = new LinkedList();
            cf = bootstrap.connect(this.host, this.port);
            cf.addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    future.channel().writeAndFlush(msg);
                    synchronized (requestList) {
                        requestList.forEach(obj -> future.channel().writeAndFlush(obj));
                        requestList.clear();
                        isConnect = true;
                    }
                } else {
                    requestList.forEach(obj -> ReferenceCountUtil.release(obj));
                    requestList.clear();
                    future.channel().close();
                    clientChannel.close();
                }
            });
        } else {
            synchronized (requestList) {
                if (isConnect) {
                    cf.channel().writeAndFlush(msg);
                } else {
                    requestList.add(msg);
                }
            }
        }
    }

    public void getRequestProto(HttpRequest httpRequest) {
        int port = -1;
        String hostStr = httpRequest.headers().get(HttpHeaderNames.HOST);
        if (hostStr == null) {
            Pattern pattern = Pattern.compile("^(?:https?://)?(?<host>[^/]*)/?.*$");
            Matcher matcher = pattern.matcher(httpRequest.uri());
            if (matcher.find()) {
                hostStr = matcher.group("host");
            } else {
                throw new RuntimeException("host is null ...");
            }
        }
        String uriStr = httpRequest.uri();
        Pattern pattern = Pattern.compile("^(?:https?://)?(?<host>[^:]*)(?::(?<port>\\d+))?(/.*)?$");
        Matcher matcher = pattern.matcher(hostStr);
        //先从host上取端口号没取到再从uri上取端口号 issues#4
        String portTemp = null;
        if (matcher.find()) {
            this.host = (matcher.group("host"));
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
        boolean ssl = uriStr.indexOf("https") == 0 || hostStr.indexOf("https") == 0;
        this.port = port == -1 ? (ssl ? 443 : 80) : port;
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        ctx.channel().close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.channel().close();
    }

}
