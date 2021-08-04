package proxyee;

import io.netty.channel.EventLoopGroup;
import io.netty.handler.ssl.SslContext;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Date;

public class HttpProxyServerConfig {
  private SslContext clientSslCtx;
  private String issuer;
  private Date caNotBefore;
  private Date caNotAfter;
  private PrivateKey caPriKey;
  private PrivateKey serverPriKey;
  private PublicKey serverPubKey;
  private EventLoopGroup proxyLoopGroup;
  private int bossGroupThreads;
  private int workerGroupThreads;
  private int proxyGroupThreads;
  private boolean handleSsl;

  public SslContext getClientSslCtx() {
    return clientSslCtx;
  }

  public EventLoopGroup getProxyLoopGroup() {
    return proxyLoopGroup;
  }

  public void setProxyLoopGroup(EventLoopGroup proxyLoopGroup) {
    this.proxyLoopGroup = proxyLoopGroup;
  }

  public boolean isHandleSsl() {
    return handleSsl;
  }

}
