package prv.liuyao.proxy.client.handler;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import prv.liuyao.proxy.utils.AESUtil;
import prv.liuyao.proxy.utils.PropertiesLoader;

public class VpnTransportHandler extends ChannelInboundHandlerAdapter {

    private static int serverPort = PropertiesLoader.getInteger("server.port");
    private static String serverHost = PropertiesLoader.getString("server.host");
    private static AESUtil aesUtil = new AESUtil(PropertiesLoader.getString("transport.aes.key"));

    private Channel sendChannel;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println("client request: " + msg);
        if (null == sendChannel) {
            NioEventLoopGroup worker = new NioEventLoopGroup(1);
            this.sendChannel = new Bootstrap().group(worker)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new ChannelInboundHandlerAdapter(){
                                @Override
                                public void channelRead(ChannelHandlerContext ctx0, Object msg0) throws Exception {
                                    // server 返回的数据写回客户端
                                    ctx.channel().writeAndFlush(msg0);
                                }
                            });
                        }
                    }).connect(this.serverHost, this.serverPort).sync().channel();
        }
        // 向server发送
        this.sendChannel.writeAndFlush(msg).sync();
    }
}
