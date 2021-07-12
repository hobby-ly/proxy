package prv.liuyao.proxy.utils.test;

import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;

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
        // 监听端口的handler 只能得到客户端建立的连接accept 是读不到数据的
        // 是在listen 的pipeline 中 --不同的pipeline 得到的东西不一样
        SocketChannel client = (SocketChannel) msg; // accept 怎么没调呢? 框架帮着做了

        // 2. 响应式的 handler
        ChannelPipeline p = client.pipeline();
        p.addLast(handler); // a-1: client: pipeline[ChannelInit]

        // 1. 注册
        selector.register(client);

    }
}
