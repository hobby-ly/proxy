package prv.liuyao.proxy.utils.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

public class WriteBackToClientHandler extends ChannelInboundHandlerAdapter {

    protected Channel clientChannel;

    public WriteBackToClientHandler(Channel channel) {
        if (null == channel) {
            throw new RuntimeException("channel is null");
        }
        this.clientChannel = channel;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        //客户端channel已关闭则不转发了
        if (!clientChannel.isOpen()) {
            ReferenceCountUtil.release(msg);
            return;
        }
        // server 返回的数据写回客户端
        this.clientChannel.writeAndFlush(msg);
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        close(ctx);
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        close(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        close(ctx);
//        cause.printStackTrace();
    }

    private void close(ChannelHandlerContext ctx) {
        ctx.channel().close();
        this.clientChannel.close();
    }
}
