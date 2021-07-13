package prv.liuyao.proxy.client.handler;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import prv.liuyao.proxy.utils.PropertiesLoader;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;

public class TransportHandler extends ChannelInboundHandlerAdapter {

    private int port = PropertiesLoader.getInteger("server.port");
    private String host = PropertiesLoader.getString("server.host");


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println("client request: " + msg);

        CompletableFuture res = new CompletableFuture<>();

        // todo: channel 连接池
        NioEventLoopGroup worker = new NioEventLoopGroup(1);
        Bootstrap bs = new Bootstrap();
        ChannelFuture connect = bs.group(worker)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline p = ch.pipeline();
                        p.addLast(new ChannelInboundHandlerAdapter(){
                            @Override
                            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                res.complete(msg);
                            }
                        });
                    }
                }).connect(new InetSocketAddress(host, port));
        NioSocketChannel client = (NioSocketChannel) connect.sync().channel();
        client.writeAndFlush(msg);

        Object resBody = res.get();

        System.out.println("client response: " + resBody);

        ctx.writeAndFlush(resBody);
    }
}
