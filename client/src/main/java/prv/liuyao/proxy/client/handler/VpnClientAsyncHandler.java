package prv.liuyao.proxy.client.handler;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.ReferenceCountUtil;
import prv.liuyao.proxy.utils.PropertiesLoader;
import prv.liuyao.proxy.utils.handler.WriteBackToClientHandler;
import prv.liuyao.proxy.utils.queue.SimpleDisruptor;

import java.util.function.Consumer;

public class VpnClientAsyncHandler extends ChannelInboundHandlerAdapter {

    private int port = PropertiesLoader.getInteger("server.port");
    private String host = PropertiesLoader.getString("server.host");

    private ChannelFuture sendConnect;
    private Consumer disruptorConsumer;
    private SimpleDisruptor tcpDisruptor = new SimpleDisruptor()
            .registryConsumer(o -> disruptorConsumer.accept(o)) // MQ 需保证数据包顺序
            ;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (null == sendConnect) {
            NioEventLoopGroup worker = new NioEventLoopGroup(1);
            this.sendConnect = new Bootstrap().group(worker)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new WriteBackToClientHandler(ctx.channel()));
                        }
                    }).connect(this.host, this.port);
            this.sendConnect.addListener(future -> {
                if (future.isSuccess()) {
                    disruptorConsumer = o -> sendConnect.channel().writeAndFlush(o);
                } else {
                    disruptorConsumer = o -> ReferenceCountUtil.release(o);
                }
                tcpDisruptor.start();
            });
        }

        // 向server发送 加入队列 等待发送
        this.tcpDisruptor.push(msg);
    }
}
