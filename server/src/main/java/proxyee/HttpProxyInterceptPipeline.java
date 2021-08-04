package proxyee;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import java.util.LinkedList;
import java.util.List;

public class HttpProxyInterceptPipeline {

  private List<HttpProxyIntercept> intercepts = new LinkedList<>();

  private int posBeforeHead = 0;
  private int posBeforeContent = 0;
  private int posAfterHead = 0;
  private int posAfterContent = 0;

  private HttpRequest httpRequest;
  private HttpResponse httpResponse;

  public HttpProxyInterceptPipeline(HttpProxyIntercept defaultIntercept) {
    this.intercepts.add(defaultIntercept);
  }

  public void beforeRequest(Channel clientChannel, HttpRequest httpRequest) throws Exception {
    this.httpRequest = httpRequest;
    if (this.posBeforeHead < intercepts.size()) {
      HttpProxyIntercept intercept = intercepts.get(this.posBeforeHead++);
      intercept.beforeRequest(clientChannel, this.httpRequest, this);
    }
    this.posBeforeHead = 0;
  }

  public void beforeRequest(Channel clientChannel, HttpContent httpContent) throws Exception {
    if (this.posBeforeContent < intercepts.size()) {
      HttpProxyIntercept intercept = intercepts.get(this.posBeforeContent++);
      intercept.beforeRequest(clientChannel, httpContent, this);
    }
    this.posBeforeContent = 0;
  }

  public void afterResponse(Channel clientChannel, Channel proxyChannel, HttpResponse httpResponse)
      throws Exception {
    this.httpResponse = httpResponse;
    if (this.posAfterHead < intercepts.size()) {
      HttpProxyIntercept intercept = intercepts.get(this.posAfterHead++);
      intercept.afterResponse(clientChannel, proxyChannel, this.httpResponse, this);
    }
    this.posAfterHead = 0;
  }

  public void afterResponse(Channel clientChannel, Channel proxyChannel, HttpContent httpContent)
      throws Exception {
    if (this.posAfterContent < intercepts.size()) {
      HttpProxyIntercept intercept = intercepts.get(this.posAfterContent++);
      intercept.afterResponse(clientChannel, proxyChannel, httpContent, this);
    }
    this.posAfterContent = 0;
  }

}
