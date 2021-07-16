package prv.liuyao.proxy.server;


import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;
import prv.liuyao.proxy.server.handler.HttpServerHandler;
import prv.liuyao.proxy.utils.PropertiesLoader;

import java.net.InetSocketAddress;
import java.util.Properties;

public class ServerStarter {

    static int port = PropertiesLoader.getInteger("server.port");

    public static void main(String[] args) {

        NioEventLoopGroup worker = new NioEventLoopGroup();
        NioEventLoopGroup boss = new NioEventLoopGroup();

        ServerBootstrap sbs = new ServerBootstrap();
        ChannelFuture bind = sbs.group(boss, worker)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ch.pipeline()
                                .addLast("httpCodec", new HttpServerCodec())
                                .addLast(new HttpServerHandler());
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new ChannelInboundHandlerAdapter(){
                            @Override
                            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                ctx.writeAndFlush(msg);
                            }
                        });

                    }
                }).bind(new InetSocketAddress(port));
        System.out.println("server start port: " + port);
        try {
            bind.sync().channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("server stop ...");
    }

}
