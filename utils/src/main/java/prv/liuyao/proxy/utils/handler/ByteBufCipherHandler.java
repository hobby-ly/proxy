package prv.liuyao.proxy.utils.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.ReferenceCountUtil;
import prv.liuyao.proxy.utils.AESUtil;
import prv.liuyao.proxy.utils.PropertiesLoader;

import java.util.List;

public class ByteBufCipherHandler {

    private static AESUtil aesUtil = new AESUtil(PropertiesLoader.getString("transport.aes.key"));
    public static final int PROTOCOL_HEAD_LENGTH = 12;
    public static final int PROTOCOL_HEAD_FLAG = 0x14141414;

    /**
     * 加密handler
     */
    public static class Encrypt extends ChannelInboundHandlerAdapter {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            Object msg1 = msg;
            if (msg instanceof ByteBuf) {
                ByteBuf buf = (ByteBuf) msg;
                byte[] bytes = new byte[buf.readableBytes()];
                buf.readBytes(bytes);
                // 加密
                byte[] encrypt = aesUtil.encrypt(bytes);
                // 封装协议
                ByteBuf byteBuf = PooledByteBufAllocator.DEFAULT.directBuffer(PROTOCOL_HEAD_LENGTH + bytes.length);
                byteBuf.writeInt(PROTOCOL_HEAD_FLAG);
                byteBuf.writeLong(encrypt.length);
                byteBuf.writeBytes(encrypt);
                msg1 = byteBuf;
                ReferenceCountUtil.release(msg);
            }
            super.channelRead(ctx, msg1);
        }
    }

    /**
     * 解密handler
     *
     * ByteToMessageDecoder
     *  解码器 ByteBuffer 环绕处理
     *  会将半包留存 等待下一次调用处理
     */
    public static class Decrypt extends ByteToMessageDecoder {
        @Override
        protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
            // 注意 getInt() 始终从第1个位置开始， 而不是 已经读到哪个位置开始
            //                                readIndex()
            //                                    ↓
            // ---------------------------------------------------- : buf
            // ↑
            // getXxx() 始终从此处开始
            while (in.readableBytes() >= PROTOCOL_HEAD_LENGTH) {
                long dataLength = in.getLong(in.readerIndex() + 4);
                if (in.readableBytes() < dataLength + PROTOCOL_HEAD_LENGTH) {
                    return;
                }
                if (in.getInt(in.readerIndex()) != PROTOCOL_HEAD_FLAG) {
                    throw new RuntimeException("协议头错误");
                }
                // 解析协议
                in.readInt();
                dataLength = in.readLong();
                byte[] bytes = new byte[(int) dataLength];
                in.readBytes(bytes);
                // 解密
                byte[] decrypt = aesUtil.decrypt(bytes);
                ByteBuf byteBuf = PooledByteBufAllocator.DEFAULT.directBuffer(decrypt.length);
                byteBuf.writeBytes(decrypt);
                // 返回
                out.add(byteBuf);
            }
        }
    }
}
