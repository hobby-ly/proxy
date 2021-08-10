package prv.liuyao.proxy.server.handler;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.util.ReferenceCountUtil;
import prv.liuyao.proxy.server.ServerStarter;
import prv.liuyao.proxy.utils.handler.WriteBackToClientHandler;
import prv.liuyao.proxy.utils.queue.SimpleDisruptor;

import java.net.URL;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 纯异步版
 * https: 协议
 *  1. client CONNECT httpRequest ok? -> server
 *  2. ok client-ok? <- server
 *  3. client httpContent ok -> server 握手成功
 *  4. client data -> server
 *  5. client <- server data
 *  so. https 数据加密，所以没法用HttpClientCodec解包，浏览器会根据解密算法解包
 *
 * http 协议
 *  1. client GET httpRequest data -> server
 *  so. 协议不加密，需要用HttpClientCodec解包，不然浏览器没法识别字节数组
 */
public class HttpProxyAsyncHandler extends ChannelInboundHandlerAdapter {
    //http代理隧道握手成功
    public final static HttpResponseStatus SUCCESS = new HttpResponseStatus(200,
            "Connection established");

    private ChannelFuture cf;
    private Consumer disruptorConsumer;
    private SimpleDisruptor tcpDisruptor = new SimpleDisruptor()
            .registryConsumer(o -> disruptorConsumer.accept(o)) // MQ 需保证数据包顺序
            ;
    private int status = 0;

    public HttpProxyAsyncHandler() {
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HttpRequest) {
            HttpRequest request = (HttpRequest) msg;
            //第一次建立连接取host和端口号和处理代理握手
            if (status == 0) {
                boolean isConnect = "CONNECT".equalsIgnoreCase(request.method().name());
                boolean initSuccess = channelInit(request, ctx.channel(), isConnect);
                if (!initSuccess) { //bad request
                    ctx.channel().close();
                    return;
                }
                status = 1;

                // todo 只有https协议会走
                if (isConnect) {//建立代理握手
                    status = 2;
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
        } else if (msg instanceof HttpContent) {
            if (status == 2) {  // todo https connect 的下一个包不处理 直接释放内存
                ReferenceCountUtil.release(msg);
                status = 1;
                return;
            }
        }
        tcpDisruptor.push(msg); // 按照数据包到来的顺序放到队列头部
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

                        ch.pipeline().addLast(new WriteBackToClientHandler(channel) {
                            @Override
                            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                super.channelRead(ctx, msg);
                                if (msg instanceof HttpResponse) {
                                    String upgrade = ((HttpResponse) msg).headers().get(HttpHeaderNames.UPGRADE);
                                    if (HttpHeaderValues.WEBSOCKET.toString().equals(upgrade)) {
                                        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> remove http codec");
                                        //websocket转发原始报文
                                        ctx.channel().pipeline().remove(ServerStarter.HTTP_DECODEC_NAME);
                                        this.clientChannel.pipeline().remove(ServerStarter.HTTP_DECODEC_NAME);
                                    }
                                }
                            }
                        });
                    }
                }).connect(host, port);

        this.cf.addListener(future -> {
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

    private void close(ChannelHandlerContext ctx) {
        if (null != this.tcpDisruptor) {
            disruptorConsumer = o -> ReferenceCountUtil.release(o);
            this.tcpDisruptor.shutdown();
        }
        if (cf != null) {
            this.cf.channel().close();
        }
        ctx.channel().close();
    }

}
