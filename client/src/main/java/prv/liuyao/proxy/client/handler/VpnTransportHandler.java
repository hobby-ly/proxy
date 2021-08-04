package prv.liuyao.proxy.client.handler;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import prv.liuyao.proxy.utils.PropertiesLoader;
import prv.liuyao.proxy.utils.handler.ByteBufCipherHandler;
import prv.liuyao.proxy.utils.handler.WriteBackToClientHandler;

public class VpnTransportHandler extends ChannelInboundHandlerAdapter {

    private int port = PropertiesLoader.getInteger("server.port");
    private String host = PropertiesLoader.getString("server.host");

    private Channel sendChannel;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (null == sendChannel) {
            NioEventLoopGroup worker = new NioEventLoopGroup(1);
            this.sendChannel = new Bootstrap().group(worker)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline()
//                                    .addLast(new ByteBufCipherHandler.Decrypt())
                                    .addLast(new ChannelInboundHandlerAdapter(){
                                        @Override
                                        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                            System.out.println(msg);
                                            super.channelRead(ctx, msg);
                                        }
                                    })
                                    .addLast(new WriteBackToClientHandler(ctx.channel()));
                        }
                    }).connect(this.host, this.port).sync().channel();
        }
        // 向server发送
        this.sendChannel.writeAndFlush(msg).sync();
    }
}
