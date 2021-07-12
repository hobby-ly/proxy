package prv.liuyao.proxy.utils.test;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;

/**
 * 此处若使用MyInHandler 需要加@ChannelHandler.Sharable注解才能实现多个客户端连接
 * 但此处有一个问题, 业务handler用户自己实现, 不应该做成单例(也就是共享,如果有属性呢,如下有参构造)
 * 需要每来一个客户端,就需要new一个
 * 每连接 -> new MyInHandler()
 *
 * so: 需要一个没有业务功能的handler{@link prv.liuyao.proxy.utils.test.ChannelInit}
 */
//@ChannelHandler.Sharable
class MyInHandler extends ChannelInboundHandlerAdapter {

    private String clientId;

    public MyInHandler() {
    }

    public MyInHandler(String clientId) {
        this.clientId = clientId;
    }

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
        // 此处可做心跳回复
        ctx.writeAndFlush(buf);

        System.out.println("read: " + str);

    }
}
