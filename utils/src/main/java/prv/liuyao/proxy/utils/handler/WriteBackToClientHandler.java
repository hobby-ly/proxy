package prv.liuyao.proxy.utils.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

public class WriteBackToClientHandler extends ChannelInboundHandlerAdapter {

    private Channel clientChannel;

    public WriteBackToClientHandler(Channel channel) {
        this.clientChannel = channel;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx0, Object msg0) throws Exception {
        //客户端channel已关闭则不转发了
        if (!clientChannel.isOpen()) {
            ReferenceCountUtil.release(msg0);
            return;
        }
        // server 返回的数据写回客户端
        this.clientChannel.writeAndFlush(msg0);
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
        cause.printStackTrace();
    }

    private void close(ChannelHandlerContext ctx) {
        ctx.channel().close();
        this.clientChannel.close();
    }
}
