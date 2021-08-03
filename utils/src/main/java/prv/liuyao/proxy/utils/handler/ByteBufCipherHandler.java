package prv.liuyao.proxy.utils.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import prv.liuyao.proxy.utils.AESUtil;
import prv.liuyao.proxy.utils.PropertiesLoader;

public class ByteBufCipherHandler {

    private static AESUtil aesUtil = new AESUtil(PropertiesLoader.getString("transport.aes.key"));

    /**
     * 加密handler
     */
    public static class Encrypt extends Crypt {

        @Override
        byte[] action(byte[] data) {
            System.out.println("加密");
            return aesUtil.encrypt(data);
        }
    }
    /**
     * 解密handler
     */
    public static class Decrypt extends Crypt {
        @Override
        byte[] action(byte[] data) {
            System.out.println("解密");
            return aesUtil.decrypt(data);
        }
    }

    private abstract static class Crypt extends ChannelInboundHandlerAdapter{

        abstract byte[] action(byte[] data);

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            Object msg1 = msg;
            if (msg instanceof ByteBuf) {
                ByteBuf buf = (ByteBuf) msg;
                byte[] bytes = new byte[buf.readableBytes()];
                buf.readBytes(bytes);
                byte[] encrypt = action(bytes);
                ByteBuf byteBuf = PooledByteBufAllocator.DEFAULT.directBuffer(bytes.length);
                byteBuf.writeBytes(encrypt);
                msg1 = byteBuf;
                ReferenceCountUtil.release(msg);
            }
            super.channelRead(ctx, msg1);
        }
    }
}
