package prv.liuyao.proxy.server;


import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;
import prv.liuyao.proxy.server.handler.VpnServerAsyncHandler;
import prv.liuyao.proxy.utils.PropertiesLoader;

import java.lang.reflect.InvocationTargetException;


public class ServerTestStarter {

    public static final String HTTP_DECODEC_NAME = "httpCodec";

    public static void main(String[] args) throws IllegalAccessException, InvocationTargetException, InstantiationException {

        int port = PropertiesLoader.getInteger("app.port");

        NioEventLoopGroup worker = new NioEventLoopGroup(1);
        NioEventLoopGroup boss = new NioEventLoopGroup(1);

        ServerBootstrap sbs = new ServerBootstrap();
        ChannelFuture bind = sbs.group(boss, worker)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ch.pipeline()
//                                .addLast(new ByteBufCipherHandler.Decrypt())
                                .addLast(HTTP_DECODEC_NAME, new HttpServerCodec())
                                .addLast(new VpnServerAsyncHandler());
                    }
                }).bind(port);
        System.out.println("server start\n\tport: " + port + "\n\tkey"
                + PropertiesLoader.getString("transport.aes.key"));
        try {
            bind.sync().channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            worker.shutdownGracefully();
            boss.shutdownGracefully();
        }
        System.out.println("server stop ...");
    }

}
