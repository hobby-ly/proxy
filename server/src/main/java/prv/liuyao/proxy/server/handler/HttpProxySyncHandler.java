package prv.liuyao.proxy.server.handler;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.util.ReferenceCountUtil;
import prv.liuyao.proxy.server.ServerStarter;
import prv.liuyao.proxy.utils.handler.CreatHandler;
import prv.liuyao.proxy.utils.handler.WriteBackToClientHandler;

import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 同步版
 */
public class HttpProxySyncHandler extends ChannelInboundHandlerAdapter implements CreatHandler {
    //http代理隧道握手成功
    public final static HttpResponseStatus SUCCESS = new HttpResponseStatus(200,
            "Connection established");

    private ChannelFuture cf;

    public HttpProxySyncHandler() {
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HttpRequest) {
            HttpRequest request = (HttpRequest) msg;
            //第一次建立连接取host和端口号和处理代理握手
            if (null == this.cf) {
                boolean isConnect = "CONNECT".equalsIgnoreCase(request.method().name());
                boolean initSuccess = channelInit(request, ctx.channel(), isConnect);
                if (!initSuccess) { //bad request
                    ctx.channel().close();
                    return;
                }

                // 只有https协议会走
                if (isConnect) {//建立代理握手
                    HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, SUCCESS);
                    ctx.writeAndFlush(response);
                    ctx.channel().pipeline().remove(ServerStarter.HTTP_DECODEC_NAME);
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
        }
        this.cf.channel().writeAndFlush(msg);
    }

    private boolean channelInit(HttpRequest httpRequest, Channel channel, boolean isSsl) {
        // 先获得host 和 port
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
        String portTemp;
        String host;
        if (matcher.find()) {
            host = matcher.group("host");
            portTemp = matcher.group("port");
            if (portTemp == null) {
                matcher = pattern.matcher(uriStr);
                if (matcher.find()) {
                    portTemp = matcher.group("port");
                }
            }
        } else {
            return false;
        }
        if (portTemp != null) {
            port = Integer.parseInt(portTemp);
        }
        port = port == -1 ? (isSsl ? 443 : 80) : port;
        // 处理连接
        this.cf = new Bootstrap()
                .group(new NioEventLoopGroup(1))
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        if (!isSsl) {
                            // 不加 http协议 浏览器不能识别，https 协议由于数据加密 所有不会被解包
                            ch.pipeline().addLast(ServerStarter.HTTP_DECODEC_NAME, new HttpClientCodec());
                        }

                        // todo 加密

                        ch.pipeline().addLast(new WriteBackToClientHandler(channel) );
                    }
                }).connect(host, port);
        try {
            this.cf.sync();
        } catch (InterruptedException e) {
            System.out.println("连接失败: " + host + ":" + port);
            throw new RuntimeException(e);
        }
        if (!this.cf.isSuccess()) {
            throw new RuntimeException("连接失败: " + host + ":" + port);
        }
        return true;
    }

    private void println(Object str) {
        System.out.println(this.toString().split("@")[1] + " --> " + str);
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        close(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        close(ctx);
        cause.printStackTrace();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        close(ctx);
    }

    private void close(ChannelHandlerContext ctx) {
        if (cf != null) {
            this.cf.channel().close();
        }
        ctx.channel().close();
    }

    @Override
    public ChannelHandler newEntity() {
        return new HttpProxySyncHandler();
    }
}
