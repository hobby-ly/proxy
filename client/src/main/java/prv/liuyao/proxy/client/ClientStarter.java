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
import prv.liuyao.proxy.utils.netty.handler.ByteBufCipherHandler;
import prv.liuyao.proxy.utils.netty.handler.ByteBufEncryptHandler;

public class ClientStarter {
    static int port = PropertiesLoader.getInteger("app.port");

    public static void main(String[] args) {

        NioEventLoopGroup worker = new NioEventLoopGroup(1);
        NioEventLoopGroup boss = new NioEventLoopGroup(10);

        try {
            ServerBootstrap sbs = new ServerBootstrap().group(boss, worker);
            ChannelFuture bind = sbs
                    .channel(NioServerSocketChannel.class)
//                    .handler(new LoggingHandler(LogLevel.DEBUG))
                    .childHandler(new ChannelInitializer<NioSocketChannel>() {
                        @Override
                        protected void initChannel(NioSocketChannel ch) throws Exception {
//                            ch.pipeline().addLast("httpServerCodec", new HttpServerCodec());
                            ch.pipeline()
                                    .addLast(new ByteBufCipherHandler.Encrypt())
                                    .addLast(new VpnTransportHandler());
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
