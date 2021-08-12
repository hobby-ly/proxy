package prv.liuyao.proxy.utils.netty.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import prv.liuyao.proxy.utils.ByteBufferCipherUtil;

/**
 * 解密处理
 */
public class ByteBufDecryptHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Object msg1 = msg;
        if (msg instanceof ByteBuf) {
            msg1 = ByteBufferCipherUtil.decrypt((ByteBuf) msg);
        }
        ReferenceCountUtil.release(msg);
        super.channelRead(ctx, msg1);
    }
}
