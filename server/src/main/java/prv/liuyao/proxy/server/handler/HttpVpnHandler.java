package prv.liuyao.proxy.server.handler;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.util.ReferenceCountUtil;
import prv.liuyao.proxy.server.ServerStarter;
import prv.liuyao.proxy.utils.netty.handler.ByteBufCipherHandler;

import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HttpVpnHandler extends ChannelInboundHandlerAdapter {

    private HttpResponseStatus SUCCESS =
            new HttpResponseStatus(200, "Connection established");

    private int status = 0;
    private String host;
    private int port;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println("--------msg: " + msg);
        // 已经解密的http 协议
        if (msg instanceof HttpRequest) {
            HttpRequest request = (HttpRequest) msg;
            System.out.println("----request: " + request);
            getRequestProto(request);
        }
        handleProxyData(ctx.channel(), msg, true);
    }

    private ChannelFuture cf;
    private List requestList;
    private boolean isConnect;

    private void handleProxyData(Channel clientChannel, Object msg, boolean isHttp)
            throws Exception {
        if (this.cf == null) {
            this.cf = new Bootstrap().group(new NioEventLoopGroup(1)) // 注册线程池
                    .channel(NioSocketChannel.class) // 使用NioSocketChannel来作为连接用的channel类
                    .handler(new ChannelInitializer(){
                        @Override
                        protected void initChannel(Channel ch0) throws Exception {
                            ch0.pipeline()
                                    .addLast(new ByteBufCipherHandler.Encrypt())
                                    .addLast(new ChannelInboundHandlerAdapter(){
                                        @Override
                                        public void channelRead(ChannelHandlerContext ctx0, Object msg0) throws Exception {
                                            // server 返回的数据写回浏览器
                                            clientChannel.writeAndFlush(msg0);
                                        }
                                    });
                        }
                    }).connect(this.host, this.port);
        }
            cf.channel().writeAndFlush(msg);

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
