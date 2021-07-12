package prv.liuyao.proxy.server;


import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

public class ServerStarter {

    public static void main(String[] args) {
        NioEventLoopGroup worker = new NioEventLoopGroup();
        NioEventLoopGroup boss = new NioEventLoopGroup();

        ServerBootstrap sbs = new ServerBootstrap();
        sbs.group(boss, worker)
                .channel(NioServerSocketChannel.class)
                /**
                 * 桥
                 */
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel nioSocketChannel) throws Exception {

                    }
                });

        System.out.println("aaaa");
    }

}
