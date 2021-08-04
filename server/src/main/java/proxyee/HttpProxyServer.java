package proxyee;


import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

public class HttpProxyServer {

  //http代理隧道握手成功
  public final static HttpResponseStatus SUCCESS = new HttpResponseStatus(200,
      "Connection established");

  private HttpProxyCACertFactory caCertFactory;
  private HttpProxyServerConfig serverConfig;
  private HttpProxyInterceptInitializer proxyInterceptInitializer;
  private ProxyConfig proxyConfig;

  private EventLoopGroup bossGroup;
  private EventLoopGroup workerGroup;

  private void init() {
    if (serverConfig == null) {
      serverConfig = new HttpProxyServerConfig();
    }
    serverConfig.setProxyLoopGroup(new NioEventLoopGroup(serverConfig.getProxyGroupThreads()));

    if (serverConfig.isHandleSsl()) {
    }
    if (proxyInterceptInitializer == null) {
      proxyInterceptInitializer = new HttpProxyInterceptInitializer();
    }
  }

  public HttpProxyServer serverConfig(HttpProxyServerConfig serverConfig) {
    this.serverConfig = serverConfig;
    return this;
  }

  public HttpProxyServer proxyInterceptInitializer(
      HttpProxyInterceptInitializer proxyInterceptInitializer) {
    this.proxyInterceptInitializer = proxyInterceptInitializer;
    return this;
  }

  public HttpProxyServer proxyConfig(ProxyConfig proxyConfig) {
    this.proxyConfig = proxyConfig;
    return this;
  }

  public HttpProxyServer caCertFactory(HttpProxyCACertFactory caCertFactory) {
    this.caCertFactory = caCertFactory;
    return this;
  }

  public void start(int port) {
    init();
    bossGroup = new NioEventLoopGroup(serverConfig.getBossGroupThreads());
    workerGroup = new NioEventLoopGroup(serverConfig.getWorkerGroupThreads());
    try {
      ServerBootstrap b = new ServerBootstrap();
      b.group(bossGroup, workerGroup)
          .channel(NioServerSocketChannel.class)
//          .option(ChannelOption.SO_BACKLOG, 100)
          .handler(new LoggingHandler(LogLevel.DEBUG))
          .childHandler(new ChannelInitializer<Channel>() {

            @Override
            protected void initChannel(Channel ch) throws Exception {
              ch.pipeline().addLast("httpCodec", new HttpServerCodec());
              ch.pipeline().addLast("serverHandle",
                  new HttpProxyServerHandle());
            }
          });
      ChannelFuture f = b
          .bind(port)
          .sync();
      f.channel().closeFuture().sync();
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      bossGroup.shutdownGracefully();
      workerGroup.shutdownGracefully();
    }
  }

}
