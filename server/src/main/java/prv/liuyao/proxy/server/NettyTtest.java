package prv.liuyao.proxy.server;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.CharsetUtil;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

public class NettyTtest {

    @Test
    public void myBytebuf(){

//        ByteBuf buf = ByteBufAllocator.DEFAULT.buffer(8, 20);
        //pool 池化
//        ByteBuf buf = UnpooledByteBufAllocator.DEFAULT.heapBuffer(8, 20);
        ByteBuf buf = PooledByteBufAllocator.DEFAULT.heapBuffer(8, 20);
        print(buf);

        for (int i = 0; i < 6; i++) {
            buf.writeBytes(new byte[]{1, 2, 3, 4});
            print(buf);
        }
    }

    public static void print(ByteBuf buf){
        System.out.println("buf.isReadable()    :"+buf.isReadable());
        System.out.println("buf.readerIndex()   :"+buf.readerIndex());
        System.out.println("buf.readableBytes() "+buf.readableBytes());
        System.out.println("buf.isWritable()    :"+buf.isWritable());
        System.out.println("buf.writerIndex()   :"+buf.writerIndex());
        System.out.println("buf.writableBytes() :"+buf.writableBytes());
        System.out.println("buf.capacity()  :"+buf.capacity());
        System.out.println("buf.maxCapacity()   :"+buf.maxCapacity());
        System.out.println("buf.isDirect()  :"+buf.isDirect());
        System.out.println("--------------");
    }

    @Test
    public void testEventGroupLoop() throws IOException {
        NioEventLoopGroup selector = new NioEventLoopGroup(1);
        for (int i = 0; i < 2; i++) {
            int finalI = i;
            selector.execute(() -> {
                for (;;) {
                    System.out.println("testEventGroup " + finalI);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        System.in.read();
    }

    /**
     * server: nc -l 127.0.0.1 9090
     *
     * @exception java.lang.IllegalStateException: channel not registered to an event loop
     *              thread.register(client);
     */
    @Test
    public void clientMode() throws InterruptedException {
        NioEventLoopGroup thread = new NioEventLoopGroup(1);

        // 客户端模式
        NioSocketChannel client = new NioSocketChannel();

        thread.register(client); // epoll_ctl(5, ADD, 3)

        // 添加读事件处理
        ChannelPipeline p = client.pipeline();
        p.addLast(new MyInHandler());

        // reactor 异步特征
        ChannelFuture connect = client.connect(new InetSocketAddress("127.0.0.1", 9090));
        ChannelFuture sync = connect.sync();
        ByteBuf byteBuf = Unpooled.copiedBuffer("hello server".getBytes());
        ChannelFuture send = client.writeAndFlush(byteBuf); // 异步的
        send.sync();


        sync.channel().closeFuture().sync();
        System.out.println("client over ...");

    }

    /**
     * client connect: nc 127.0.0.1 9091
     * @throws IOException
     * @throws InterruptedException
     */
    @Test
    public void serverMode() throws IOException, InterruptedException {

        NioEventLoopGroup thread = new NioEventLoopGroup(1);
        NioServerSocketChannel server = new NioServerSocketChannel();

        thread.register(server);

        // 指不定什么时候来数据 响应式
        ChannelPipeline p = server.pipeline();
        p.addLast(new MyAcceptHandler(thread, new ChannelInit())); // accept接收客户端 并注册到selector
        ChannelFuture bind = server.bind(new InetSocketAddress("127.0.0.1", 9091));
        bind.sync().channel().closeFuture().sync();

        System.out.println("server close ...");

    }

    @Test
    public void nettyClient() throws InterruptedException {

        NioEventLoopGroup group = new NioEventLoopGroup(1);
        Bootstrap bs = new Bootstrap();
        ChannelFuture connect = bs.group(group)
                .channel(NioSocketChannel.class)
//                .handler(new ChannelInit())
                .handler(new ChannelInitializer<SocketChannel>() {

                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline p = ch.pipeline();
                        p.addLast(new MyInHandler());
                    }
                })
                .connect(new InetSocketAddress("127.0.0.1", 9090));

        Channel client = connect.sync().channel();
        ByteBuf byteBuf = Unpooled.copiedBuffer("hello server".getBytes());
        ChannelFuture send = client.writeAndFlush(byteBuf); // 异步的
        send.sync();

        client.closeFuture().sync();
    }

    @Test
    public void nettyServer() throws InterruptedException {
        NioEventLoopGroup group = new NioEventLoopGroup(1);
        ServerBootstrap bs = new ServerBootstrap();
        ChannelFuture bind = bs.group(group, group)
                .channel(NioServerSocketChannel.class)
//                .childHandler(new ChannelInit())
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ChannelPipeline p = ch.pipeline();
                        p.addLast(new MyInHandler());
                    }
                })
                .bind(new InetSocketAddress("127.0.0.1", 9090));

        bind.sync().channel().closeFuture().sync();
    }
}

class MyAcceptHandler extends ChannelInboundHandlerAdapter {

    private final EventLoopGroup selector;
    private final ChannelHandler handler;

    public MyAcceptHandler(EventLoopGroup thread, ChannelHandler myInHandler) {
        this.selector = thread;
        this.handler = myInHandler; // ChannelInit
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        System.out.println("handler server register ...");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // server: listen socket -> accept client
        // client: socket -> R/W
        SocketChannel client = (SocketChannel) msg; // accept 怎么没调呢? 框架帮着做了

        // 2. 响应式的 handler
        ChannelPipeline p = client.pipeline();
        p.addLast(handler); // a-1: client: pipeline[ChannelInit]

        // 1. 注册
        selector.register(client);

    }
}

// 桥 为客户端添加handler(业务, 避免客户端独有的资源被其他客户端修改) 新客户端注册用
@ChannelHandler.Sharable
class ChannelInit extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        Channel client = ctx.channel();
        ChannelPipeline p = client.pipeline();
        p.addLast(new MyInHandler()); // a-2: client: pipeline[ChannelInit, MyInhandler]
        ctx.pipeline().remove(this); // pipeline[MyInhandler]
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println("channelinit read ...");
    }
}


class MyInHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        System.out.println("handler client registered ...");
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("handler client active ...");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = (ByteBuf) msg;

//        CharSequence str = buf.readCharSequence(buf.readableBytes(), CharsetUtil.UTF_8);

        CharSequence str = buf.getCharSequence(0, buf.readableBytes(), CharsetUtil.UTF_8);
        // 心跳回复
        ctx.writeAndFlush(buf);

        System.out.println("read: " + str);

    }
}
