package prv.liuyao.proxy.utils;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;

public class ByteBufferCipherUtil {
    private static AESUtil aesUtil = new AESUtil(PropertiesLoader.getString("transport.aes.key"));

    public static ByteBuf encrypt(ByteBuf buf) {
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        byte[] encrypt = aesUtil.encrypt(bytes);
        ByteBuf byteBuf = PooledByteBufAllocator.DEFAULT.directBuffer(bytes.length);
        byteBuf.writeBytes(encrypt);
        return byteBuf;
    }

    public static ByteBuf decrypt(ByteBuf buf) {
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        byte[] decrypt = aesUtil.decrypt(bytes);
        ByteBuf byteBuf = PooledByteBufAllocator.DEFAULT.directBuffer(bytes.length);
        byteBuf.writeBytes(decrypt);
        return byteBuf;
    }

    public static void release(ByteBuf buf) {
        buf.release();
    }

}
