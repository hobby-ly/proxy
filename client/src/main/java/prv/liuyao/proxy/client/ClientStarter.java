package prv.liuyao.proxy.client;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import prv.liuyao.proxy.client.handler.VpnTransportHandler;
import prv.liuyao.proxy.utils.PropertiesLoader;
import prv.liuyao.proxy.utils.handler.ByteBufCipherHandler;
import prv.liuyao.proxy.utils.handler.LastHandler;

public class ClientStarter {
    static int port = PropertiesLoader.getInteger("app.port");

    public static void main(String[] args) {

        NioEventLoopGroup worker = new NioEventLoopGroup(4);
        NioEventLoopGroup boss = new NioEventLoopGroup(4);

        try {
            ServerBootstrap sbs = new ServerBootstrap().group(boss, worker);
            ChannelFuture bind = sbs
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<NioSocketChannel>() {
                        @Override
                        protected void initChannel(NioSocketChannel ch) throws Exception {
                            ch.pipeline()
//                                    .addLast(new ByteBufCipherHandler.Encrypt())
                                    .addLast(new VpnTransportHandler())
                                    .addLast(new LastHandler());
                        }
                    }).bind(port);
            System.out.println("client start port: " + port);
            bind.sync().channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            worker.shutdownGracefully();
            boss.shutdownGracefully();
        }
        System.out.println("client stop ...");
    }

}
