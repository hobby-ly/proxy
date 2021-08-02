package prv.liuyao.proxy.client.handler;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import prv.liuyao.proxy.utils.ByteBufferCipherUtil;
import prv.liuyao.proxy.utils.PropertiesLoader;
import prv.liuyao.proxy.utils.netty.handler.ByteBufCipherHandler;
import prv.liuyao.proxy.utils.netty.handler.ByteBufDecryptHandler;

public class VpnTransportHandler extends ChannelInboundHandlerAdapter {

    private static int serverPort = PropertiesLoader.getInteger("server.port");
    private static String serverHost = PropertiesLoader.getString("server.host");

    private Channel sendChannel;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // 浏览器发来数据 已加密
        System.out.println("client request: " + msg);
        if (null == sendChannel) {
            System.out.println("this: " + this);
            this.sendChannel = new Bootstrap().group(new NioEventLoopGroup(1))
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline()
                                    // server端返回数据
                                    .addLast(new ByteBufCipherHandler.Decrypt())
                                    .addLast(new ChannelInboundHandlerAdapter(){
                                @Override
                                public void channelRead(ChannelHandlerContext ctx0, Object msg0) throws Exception {
                                    // server 返回的数据写回浏览器
                                    ctx.channel().writeAndFlush(msg0).sync();
                                }
                            });
                        }
                    }).connect(this.serverHost, this.serverPort).sync().channel();
        }
        // 向vpn server端 发送
        this.sendChannel.writeAndFlush(msg).sync();
    }
}
