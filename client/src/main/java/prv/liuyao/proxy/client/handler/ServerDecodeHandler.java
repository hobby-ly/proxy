package prv.liuyao.proxy.client.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

/**
 * 解码器 ByteBuffer 环绕处理
 * 会将半包留存 等待下一次调用处理
 */
public class ServerDecodeHandler extends ByteToMessageDecoder {
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> list) throws Exception {


    }
}
