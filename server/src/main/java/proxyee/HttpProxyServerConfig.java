package proxyee;

import io.netty.channel.EventLoopGroup;
import io.netty.handler.ssl.SslContext;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Date;

public class HttpProxyServerConfig {
  private SslContext clientSslCtx;

  public SslContext getClientSslCtx() {
    return clientSslCtx;
  }

}
