package prv.liuyao.proxy.utils.netty.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import prv.liuyao.proxy.utils.ByteBufferCipherUtil;

public class ByteBufDecrypt extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Object msg1 = msg;
        if (msg instanceof ByteBuf) {
            msg1 = ByteBufferCipherUtil.decrypt((ByteBuf) msg);
        }
        super.channelRead(ctx, msg1);
    }
}
