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
//    System.out.println("posBeforeHead " + (this.posBeforeHead < 1));
    if (this.posBeforeHead < 1) {
      this.posBeforeHead++;
      HttpProxyIntercept intercept = intercepts.get(0);
      intercept.beforeRequest(clientChannel, this.httpRequest);
    }
    this.posBeforeHead = 0;
  }

  public void beforeRequest(Channel clientChannel, HttpContent httpContent) throws Exception {
//    System.out.println("posBeforeContent " + (this.posBeforeContent < 1));
    if (this.posBeforeContent < 1) {
      this.posBeforeContent++;
      HttpProxyIntercept intercept = intercepts.get(0);
      intercept.beforeRequest(clientChannel, httpContent);
    }
    this.posBeforeContent = 0;
  }

  public void afterResponse(Channel clientChannel, Channel proxyChannel, HttpResponse httpResponse)
      throws Exception {
    this.httpResponse = httpResponse;
//    System.out.println("posAfterHead " + (this.posAfterHead < 1));
    if (this.posAfterHead < 1) {
      this.posAfterHead++;
      HttpProxyIntercept intercept = intercepts.get(0);
      intercept.afterResponse(clientChannel, proxyChannel, this.httpResponse);
    }
    this.posAfterHead = 0;
  }

  public void afterResponse(Channel clientChannel, Channel proxyChannel, HttpContent httpContent)
      throws Exception {
//    System.out.println("posAfterContent " + (this.posAfterContent < 1));
    if (this.posAfterContent < 1) {
      this.posAfterContent++;
      HttpProxyIntercept intercept = intercepts.get(0);
      intercept.afterResponse(clientChannel, proxyChannel, httpContent);
    }
    this.posAfterContent = 0;
  }

}
