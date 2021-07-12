package prv.liuyao.proxy.utils.test;

import io.netty.channel.*;

/**
 * 桥
 *  为客户端添加handler(业务, 避免客户端独有的资源被其他客户端修改)
 *  不处理业务
 *  新客户端注册用
 *
 * 此handler 功能同{@link io.netty.channel.ChannelInitializer}一样
 *  client: ***.handler(new ChannelInitializer<SocketChannel>() {})
 *  server: ***.childHandler(new ChannelInitializer<NioSocketChannel>() {})
 */
@ChannelHandler.Sharable
class ChannelInit extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        Channel client = ctx.channel();
        ChannelPipeline p = client.pipeline();
        /**
         * 这里可以抽象 由coder添加handler
         *  -> {@link io.netty.channel.ChannelInitializer}.initChannel()
         */
        p.addLast(new MyInHandler()); // -> a-2: client: pipeline[ChannelInit, MyInhandler]
        ctx.pipeline().remove(this); // -> pipeline[MyInhandler]
    }

//    @Override
//    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
//        System.out.println("channelinit read ...");
//        super.channelRead(ctx, msg);
//    }
}
