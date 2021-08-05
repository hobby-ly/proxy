package prv.liuyao.proxy.server;


import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;
import prv.liuyao.proxy.server.handler.VpnServerHandler;
import prv.liuyao.proxy.utils.PropertiesLoader;
import prv.liuyao.proxy.utils.handler.ByteBufCipherHandler;

public class ServerStarter {

    public static final String HTTP_DECODEC_NAME = "httpCodec";
    static int port = PropertiesLoader.getInteger("app.port");

    public static void main(String[] args) {

        NioEventLoopGroup worker = new NioEventLoopGroup(1);
        NioEventLoopGroup boss = new NioEventLoopGroup(1);

        ServerBootstrap sbs = new ServerBootstrap();
        ChannelFuture bind = sbs.group(boss, worker)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ch.pipeline()
                                .addLast(new ByteBufCipherHandler.Decrypt())
                                .addLast(HTTP_DECODEC_NAME, new HttpServerCodec())
                                .addLast(new VpnServerHandler());
                    }
                }).bind(port);
        System.out.println("server start port: " + port);
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
