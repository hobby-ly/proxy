package prv.liuyao.proxy.utils.test;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.CharsetUtil;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;


/**
 * selector: 多路复用器
 */
public class NettyTtest {

    @Test
    public void myBytebuf(){

//        ByteBuf buf = ByteBufAllocator.DEFAULT.buffer(8, 20);
        //pool 池化
//        ByteBuf buf = UnpooledByteBufAllocator.DEFAULT.heapBuffer(8, 20);
//        ByteBuf buf = PooledByteBufAllocator.DEFAULT.heapBuffer(8, 20);
        // 堆外 直接内存
        ByteBuf buf = PooledByteBufAllocator.DEFAULT.directBuffer(8, 20);
        print(buf);

        for (int i = 0; i < 6; i++) {
            buf.writeBytes(new byte[]{1, 2, 3, 4});
            print(buf);
        }

        ByteBuf byteBuf = Unpooled.copiedBuffer("hello server".getBytes());

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
        // 理解成线程池 1为只有一个线程
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
        // 1. 读
        // 2. 读出来数据后要做哪些事情 后续有几步 各种事情, 如过滤`编码`加密等,这些都是handler
        //    这一系列handler 放到pipeline 中
        ChannelPipeline p = client.pipeline();
        p.addLast(new MyInHandler());

        // reactor 异步特征
        ChannelFuture connect = client.connect(new InetSocketAddress("127.0.0.1", 9090));
        // 阻塞 等待连接成功
        ChannelFuture sync = connect.sync();
        ByteBuf byteBuf = Unpooled.copiedBuffer("hello server".getBytes());

        // 异步的 连接成功了 发送数据
        ChannelFuture send = client.writeAndFlush(byteBuf);
        // 阻塞 等待发送成功
        send.sync();

        // 阻塞 等待断开
        sync.channel().closeFuture().sync();
        // 服务端断开后继续执行
        System.out.println("client over ...");

    }

    /**
     * client connect: nc 127.0.0.1 9091
     */
    @Test
    public void serverMode() throws Exception {

        NioEventLoopGroup thread = new NioEventLoopGroup(1);
        NioServerSocketChannel server = new NioServerSocketChannel();

        thread.register(server);

        // 指不定什么时候来数据 响应式
        ChannelPipeline p = server.pipeline();
        // accept接收客户端 并注册到selector
//        p.addLast(new MyAcceptHandler(thread, new MyInHandler())); // 见MyInHandler注释
        p.addLast(new MyAcceptHandler(thread, new ChannelInit())); // 桥

        ChannelFuture bind = server.bind(new InetSocketAddress("127.0.0.1", 9090));
        // 阻塞 等待连接成功 等待关闭连接
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



