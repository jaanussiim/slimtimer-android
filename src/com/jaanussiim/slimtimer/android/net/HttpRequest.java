package com.jaanussiim.slimtimer.android.net;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;

import android.util.Log;

import com.jaanussiim.slimtimer.android.log.Logger;

public class HttpRequest implements Runnable {
  private static final String T = "HttpRequest";

  public static final String METHOD_POST = "POST";
  public static final String METHOD_GET = "GET";

  public static final String CONTENT_TYPE_YAML = "application/x-yaml";
  public static final String CONTENT_TYPE_JSON = "application/json";

  private static final HttpParams params = new BasicHttpParams();
  private String requestUri = "";
  private String method = METHOD_GET;
  private String contentType = "";
  private String body;
  private String accept;
  private HttpRequestListener listener;
  private boolean requestCancelled;

  static {
    HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
    HttpConnectionParams.setStaleCheckingEnabled(params, false);
    HttpConnectionParams.setConnectionTimeout(params, 20 * 1000);
    HttpConnectionParams.setSoTimeout(params, 20 * 1000);
    HttpConnectionParams.setSocketBufferSize(params, 8192);
  }

  public void execute() {
    final Thread t = new Thread(this);
    t.start();
  }

  @Override
  public void run() {
    if ("".equals(requestUri)) {
      throw new RuntimeException("missing uri");
    }
    final DefaultHttpClient client = new DefaultHttpClient(params);
    HttpRequestBase httpRequest = null;
    if (METHOD_POST.equals(method)) {
      Logger.d(T, "Request body: \n'" + body + "'");
      httpRequest = new HttpPost(requestUri);
      ByteArrayEntity baEntity;
      try {
        baEntity = new ByteArrayEntity(body.getBytes("utf-8"));
        baEntity.setContentType(contentType);
        ((HttpPost) httpRequest).setEntity(baEntity);
      } catch (final UnsupportedEncodingException e) {
        Log.e(T, "Error", e);
      }
    } else if (METHOD_GET.equals(method)) {
      httpRequest = new HttpGet(requestUri);
    }

    httpRequest.setHeader("Content-Type", contentType);
    httpRequest.setHeader("Accept", accept);

    try {
      final HttpResponse resp = client.execute(httpRequest);
      final int statusCode = resp.getStatusLine().getStatusCode();
      final ByteArrayOutputStream baos = new ByteArrayOutputStream();
      resp.getEntity().writeTo(baos);
      final byte[] data = baos.toByteArray();
      Logger.d(T, "Response code: '" + statusCode + "'");
      Logger.d(T, "Response: \n'" + new String(data, "utf-8") + "'");

      if (requestCancelled) {
        return;
      }

      if (requestComplete(statusCode, data)) {
        Log.d(T, "Byte data handled");
      } else {
        final String response = new String(data, "utf-8");
        if (requestComplete(statusCode, response)) {
          Log.d(T, "String data handled");
        }
      }
    } catch (final IOException e) {
      Logger.e(T, "Network error", e);

      if (requestCancelled) {
        return;
      }

      networkError();
    }
  }

  protected void networkError() {
    if (listener != null) {
      listener.requestComplete(HttpRequestListener.REQUEST_NETWORK_ERROR);
    }
  }

  protected boolean requestComplete(final int statusCode, final String response) {
    return false;
  }

  protected boolean requestComplete(final int statusCode, final byte[] data) {
    return false;
  }

  public void setRequestUri(final String uri) {
    this.requestUri = uri;
  }

  public void setRequestMethod(final String method) {
    this.method = method;
  }

  protected void setContentType(final String contentType) {
    this.contentType = contentType;
  }

  protected void setBody(final String body) {
    this.body = body;
  }

  protected void setAccept(final String accepts) {
    this.accept = accepts;
  }

  protected void setMethod(final String method) {
    this.method = method;
  }

  public void setListener(final HttpRequestListener listener) {
    this.listener = listener;
  }

  public HttpRequestListener getListener() {
    return listener;
  }

  public void cancel() {
    requestCancelled = true;
  }

  public String testGetUri() {
    return requestUri;
  }

  public String testGetContentType() {
    return contentType;
  }

  public String testGetBody() {
    return body;
  }

  public String testGetAccept() {
    return accept;
  }

  public String testGetMethod() {
    return method;
  }
}
