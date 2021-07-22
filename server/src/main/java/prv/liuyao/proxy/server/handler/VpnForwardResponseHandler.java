package prv.liuyao.proxy.server.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.util.ReferenceCountUtil;

public class VpnForwardResponseHandler extends ChannelInboundHandlerAdapter {

    private boolean isHttp;
    private Channel clientChannel;

    public VpnForwardResponseHandler(Channel clientChannel, boolean isHttp) {
        this.clientChannel = clientChannel;
        this.isHttp = isHttp;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (this.isHttp) {
            //客户端channel已关闭则不转发了
            if (!clientChannel.isOpen()) {
                ReferenceCountUtil.release(msg);
                return;
            }
            if (msg instanceof HttpResponse) {
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
