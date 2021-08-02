package prv.liuyao.proxy.utils.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class WriteToChannelHandler extends ChannelInboundHandlerAdapter {

    private Channel channel;

    public WriteToChannelHandler(Channel channel) {
        this.channel = channel;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx0, Object msg0) throws Exception {
        // server 返回的数据写回客户端
        this.channel.writeAndFlush(msg0);
    }
}
