package prv.liuyao.proxy.client;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import prv.liuyao.proxy.client.handler.TransportHandler;
import prv.liuyao.proxy.utils.PropertiesLoader;

import java.net.InetSocketAddress;
import java.util.Properties;

public class ClientStarter {
    static int port = PropertiesLoader.getInteger("client.port");

    public static void main(String[] args) {

        NioEventLoopGroup worker = new NioEventLoopGroup();
        NioEventLoopGroup boss = new NioEventLoopGroup();

        ServerBootstrap sbs = new ServerBootstrap();
        ChannelFuture bind = sbs.group(boss, worker)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new TransportHandler());
                    }
                }).bind(new InetSocketAddress(port));

        System.out.println("client start port: " + port);
        try {
            bind.sync().channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("client stop ...");
    }

}
