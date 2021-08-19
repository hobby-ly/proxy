package prv.liuyao.proxy.server;


import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;
import prv.liuyao.proxy.server.handler.HttpProxySyncHandler;
import prv.liuyao.proxy.utils.PropertiesLoader;
import prv.liuyao.proxy.utils.handler.ByteBufCipherHandler;
import prv.liuyao.proxy.utils.handler.CreatHandler;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;


public class ServerStarter {

    public static final String HTTP_DECODEC_NAME = "httpCodec";

    public static void main(String[] args) throws IllegalAccessException, InvocationTargetException, InstantiationException {
        int port = PropertiesLoader.getInteger("app.port");
        String handlerClass = PropertiesLoader.getString("netty.handler");

        NioEventLoopGroup worker = new NioEventLoopGroup(1);
        NioEventLoopGroup boss = new NioEventLoopGroup(1);

        // 反射方式创建handler
        CreatHandler usedHandler;
        Constructor<ChannelHandler> handlerConstructor;
        try {
            Class aClass = Class.forName(handlerClass);
            Constructor constructor = aClass.getConstructor();
            Object handler = constructor.newInstance();
            if (!(handler instanceof ChannelHandler) || !(handler instanceof CreatHandler)) {
                System.out.println("handler class error: " + handler.getClass());
                return;
            }
            usedHandler = (CreatHandler) handler;
            System.out.println("use handler " + aClass.getName());
            handlerConstructor = constructor;
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        long l;
        Object o;

        l = System.currentTimeMillis();
        for (int i = 0; i < 100_0000; i++) {
            o = new HttpProxySyncHandler();
        }
        System.out.println(System.currentTimeMillis() - l);

        l = System.currentTimeMillis();
        for (int i = 0; i < 100_0000; i++) {
            o = usedHandler.newEntity();
        }
        System.out.println(System.currentTimeMillis() - l);

        if (true) {
            return;
        }

        ServerBootstrap sbs = new ServerBootstrap();
        ChannelFuture bind = sbs.group(boss, worker)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ch.pipeline()
                                .addLast(new ByteBufCipherHandler.Decrypt())
                                .addLast(HTTP_DECODEC_NAME, new HttpServerCodec())
                                .addLast(handlerConstructor.newInstance());
                    }
                }).bind(port);
        System.out.println("server start port: " + port + ", "
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
