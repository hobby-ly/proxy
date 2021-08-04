package proxyee;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.proxy.ProxyHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.resolver.NoopAddressResolverGroup;
import io.netty.util.ReferenceCountUtil;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HttpProxyServerHandle extends ChannelInboundHandlerAdapter {

  //http代理隧道握手成功
  public final static HttpResponseStatus SUCCESS = new HttpResponseStatus(200,
          "Connection established");

  private ChannelFuture cf;
  private String host;
  private int port;
  private int status = 0;
  private HttpProxyInterceptPipeline interceptPipeline;
  private List requestList;
  private boolean isConnect;
  private EventLoopGroup proxyGroup = new NioEventLoopGroup(1);

  public HttpProxyInterceptPipeline getInterceptPipeline() {
    return interceptPipeline;
  }


  public HttpProxyServerHandle() { }

  @Override
  public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
    if (msg instanceof HttpRequest) {
      HttpRequest request = (HttpRequest) msg;
      //第一次建立连接取host和端口号和处理代理握手
      if (status == 0) {
        RequestProto requestProto = getRequestProto(request);
        if (requestProto == null) { //bad request
          ctx.channel().close();
          return;
        }
        status = 1;
        this.host = requestProto.getHost();
        this.port = requestProto.getPort();
        if ("CONNECT".equalsIgnoreCase(request.method().name())) {//建立代理握手
          status = 2;
          HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
              SUCCESS);
          ctx.writeAndFlush(response);
          ctx.channel().pipeline().remove("httpCodec");
          //fix issue #42
          ReferenceCountUtil.release(msg);
          return;
        }
      }
      interceptPipeline = buildPipeline();
      interceptPipeline.setRequestProto(new RequestProto(host, port, false));
      //fix issue #27
      if (request.uri().indexOf("/") != 0) {
        URL url = new URL(request.uri());
        request.setUri(url.getFile());
      }
      interceptPipeline.beforeRequest(ctx.channel(), request);
    } else if (msg instanceof HttpContent) {
      if (status != 2) {
        interceptPipeline.beforeRequest(ctx.channel(), (HttpContent) msg);
      } else {
        ReferenceCountUtil.release(msg);
        status = 1;
      }
    } else { //ssl和websocket的握手处理
      handleProxyData(ctx.channel(), msg, false);
    }
  }

  @Override
  public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
    if (cf != null) {
      cf.channel().close();
    }
    ctx.channel().close();
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    if (cf != null) {
      cf.channel().close();
    }
    ctx.channel().close();
    throw new Exception(cause);
  }

  private void handleProxyData(Channel channel, Object msg, boolean isHttp)
      throws Exception {
    if (cf == null) {
      //connection异常 还有HttpContent进来，不转发
      if (isHttp && !(msg instanceof HttpRequest)) {
        System.out.println("ishttp");
        return;
      }
      /*
        添加SSL client hello的Server Name Indication extension(SNI扩展)
        有些服务器对于client hello不带SNI扩展时会直接返回Received fatal alert: handshake_failure(握手错误)
        例如：https://cdn.mdn.mozilla.net/static/img/favicon32.7f3da72dcea1.png
       */
      ChannelInitializer channelInitializer =
          isHttp ? new HttpProxyInitializer(channel) : new TunnelProxyInitializer(channel);
      Bootstrap bootstrap = new Bootstrap();
      bootstrap.group(this.proxyGroup) // 注册线程池
          .channel(NioSocketChannel.class) // 使用NioSocketChannel来作为连接用的channel类
          .handler(channelInitializer);
      requestList = new LinkedList();
      cf = bootstrap.connect(host, port);
      cf.addListener((ChannelFutureListener) future -> {
        if (future.isSuccess()) {
          future.channel().writeAndFlush(msg);
          synchronized (requestList) {
            requestList.forEach(obj -> future.channel().writeAndFlush(obj));
            requestList.clear();
            isConnect = true;
          }
        } else {
          requestList.forEach(obj -> ReferenceCountUtil.release(obj));
          requestList.clear();
          future.channel().close();
          channel.close();
        }
      });
    } else {
      synchronized (requestList) {
        if (isConnect) {
          cf.channel().writeAndFlush(msg);
        } else {
          requestList.add(msg);
        }
      }
    }
  }

  private HttpProxyInterceptPipeline buildPipeline() {
    HttpProxyInterceptPipeline interceptPipeline = new HttpProxyInterceptPipeline(
        new HttpProxyIntercept() {
          @Override
          public void beforeRequest(Channel clientChannel, HttpRequest httpRequest,
              HttpProxyInterceptPipeline pipeline) throws Exception {
            handleProxyData(clientChannel, httpRequest, true);
          }

          @Override
          public void beforeRequest(Channel clientChannel, HttpContent httpContent,
              HttpProxyInterceptPipeline pipeline) throws Exception {
            handleProxyData(clientChannel, httpContent, true);
          }

          @Override
          public void afterResponse(Channel clientChannel, Channel proxyChannel,
              HttpResponse httpResponse, HttpProxyInterceptPipeline pipeline) throws Exception {
            clientChannel.writeAndFlush(httpResponse);
            if (HttpHeaderValues.WEBSOCKET.toString()
                .equals(httpResponse.headers().get(HttpHeaderNames.UPGRADE))) {
              //websocket转发原始报文
              proxyChannel.pipeline().remove("httpCodec");
              clientChannel.pipeline().remove("httpCodec");
            }
          }

          @Override
          public void afterResponse(Channel clientChannel, Channel proxyChannel,
              HttpContent httpContent, HttpProxyInterceptPipeline pipeline) throws Exception {
            clientChannel.writeAndFlush(httpContent);
          }
        });
    return interceptPipeline;
  }

  public static RequestProto getRequestProto(HttpRequest httpRequest) {
    RequestProto requestProto = new RequestProto();
    int port = -1;
    String hostStr = httpRequest.headers().get(HttpHeaderNames.HOST);
    if (hostStr == null) {
      Pattern pattern = Pattern.compile("^(?:https?://)?(?<host>[^/]*)/?.*$");
      Matcher matcher = pattern.matcher(httpRequest.uri());
      if (matcher.find()) {
        hostStr = matcher.group("host");
      } else {
        return null;
      }
    }
    String uriStr = httpRequest.uri();
    Pattern pattern = Pattern.compile("^(?:https?://)?(?<host>[^:]*)(?::(?<port>\\d+))?(/.*)?$");
    Matcher matcher = pattern.matcher(hostStr);
    //先从host上取端口号没取到再从uri上取端口号 issues#4
    String portTemp = null;
    if (matcher.find()) {
      requestProto.setHost(matcher.group("host"));
      portTemp = matcher.group("port");
      if (portTemp == null) {
        matcher = pattern.matcher(uriStr);
        if (matcher.find()) {
          portTemp = matcher.group("port");
        }
      }
    }
    if (portTemp != null) {
      port = Integer.parseInt(portTemp);
    }
    boolean isSsl = uriStr.indexOf("https") == 0 || hostStr.indexOf("https") == 0;
    if (port == -1) {
      if (isSsl) {
        port = 443;
      } else {
        port = 80;
      }
    }
    requestProto.setPort(port);
    requestProto.setSsl(isSsl);
    return requestProto;
  }

}