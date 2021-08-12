package prv.liuyao.proxy.utils.netty.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.util.ReferenceCountUtil;

public class HttpResponseHandler extends ChannelInboundHandlerAdapter {

    private Channel clientChannel;

    public HttpResponseHandler(Channel clientChannel) {
        this.clientChannel = clientChannel;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
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
    }

    public static void main(String[] args) {

    }
}
