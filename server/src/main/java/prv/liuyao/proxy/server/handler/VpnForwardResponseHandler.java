package prv.liuyao.proxy.server.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.util.ReferenceCountUtil;
import prv.liuyao.proxy.utils.ByteBufferCipherUtil;

public class VpnForwardResponseHandler extends ChannelInboundHandlerAdapter {

    private boolean isHttp;
    private Channel clientChannel;

    public VpnForwardResponseHandler(Channel clientChannel, boolean isHttp) {
        this.clientChannel = clientChannel;
        this.isHttp = isHttp;
    }

    // 发出请求后的返回处理
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ByteBuf) {
            ByteBuf encrypt = ByteBufferCipherUtil.encrypt((ByteBuf) msg);
            clientChannel.writeAndFlush(encrypt).sync();
            ReferenceCountUtil.release(msg);
            return;
        }
        System.out.println("server tcp response: " + msg);
        if (this.isHttp) {
            //客户端channel已关闭则不转发了
            if (!clientChannel.isOpen()) {
                ReferenceCountUtil.release(msg);
                return;
            }
            if (msg instanceof HttpResponse) {
                // todo 如何转换为 ByteBuf 加密返回 or msg直接加密？？？
                // 这里应该直接返回给客户端 由vpn客户端进行http response解析 并返回B/C端
                HttpResponse httpResponse = (HttpResponse) msg;
                clientChannel.writeAndFlush(httpResponse);
                if (HttpHeaderValues.WEBSOCKET.toString()
                        .equals(httpResponse.headers().get(HttpHeaderNames.UPGRADE))) {
                    //websocket转发原始报文
                    ctx.channel().pipeline().remove("httpCodec");
                    clientChannel.pipeline().remove("httpCodec");
                }
            } else if (msg instanceof HttpContent) {
                clientChannel.writeAndFlush((HttpContent) msg);
            } else {
                clientChannel.writeAndFlush(msg);
            }
        } else {
            clientChannel.writeAndFlush(msg);
        }
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        ctx.channel().close();
        clientChannel.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.channel().close();
        clientChannel.close();
        cause.printStackTrace();
    }
}
