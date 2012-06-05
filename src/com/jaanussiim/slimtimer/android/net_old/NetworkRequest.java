package com.jaanussiim.slimtimer.android.net_old;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONArray;
import org.json.JSONObject;

import com.jaanussiim.slimtimer.android.Constants;

public abstract class NetworkRequest implements Runnable {
  public static final String METHOD_DELETE = "DELETE";
  public static final String METHOD_POST = "POST";
  public static final String METHOD_PUT = "PUT";

  private static final String LOG_TAG = "NetworkConnection";
  public static final String APP_KEY = "eab1666faeba68953d6029fda893db";
  public static final String ACCEPT_HEADER = "Accept";
  public static final String CONTENT_TYPE_ACCEPT = "application/json";
  public static final String CONTENT_TYPE_HEADER = "Content-Type";
  public static final String REQUEST_CONTENT_TYPE = "application/x-yaml";
  public static final String CONTENT_LENGTH_HEADER = "Content-Length";
  private boolean stopped;

  public void execute() {
    final Thread t = new Thread(this);
    t.start();
  }

  public void run() {
    HttpURLConnection connection = null;
    OutputStream dos = null;
    InputStream is = null;
    try {
      final URL connectionUrl = new URL(Constants.SERVER_URL + getPath());
      //      Log.d(LOG_TAG, "Connect to: " + connectionUrl.toExternalForm());
      final String request = getRequestYaml();
      final byte[] requestBytes = request.getBytes();
      //      Log.d(LOG_TAG, request);

      connection = (HttpURLConnection) connectionUrl.openConnection();
      connection.setDoInput(true);
      if (requestBytes.length > 0) {
        connection.setDoOutput(true);
        connection.setRequestProperty(CONTENT_TYPE_HEADER, REQUEST_CONTENT_TYPE);
      }
      connection.setInstanceFollowRedirects(true);
      connection.setRequestProperty(ACCEPT_HEADER, CONTENT_TYPE_ACCEPT);
      connection.setRequestProperty("User-Agent", "AndroidSlimTimer/JaanusSiim");
      connection.setRequestProperty(CONTENT_LENGTH_HEADER, Integer.toString(requestBytes.length));
      connection.setRequestMethod(getRequestMethod());

      if (requestBytes.length > 0) {
        dos = connection.getOutputStream();
        dos.write(requestBytes);
      }

      is = connection.getInputStream();

      if (stopped) {
        return;
      }

      if (HttpURLConnection.HTTP_OK == connection.getResponseCode()) {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final byte[] buffer = new byte[1024];
        int totalRead = 0;
        int bytesRead = 0;
        while ((bytesRead = is.read(buffer)) > 0) {
          baos.write(buffer, 0, bytesRead);
          totalRead += bytesRead;
        }

        final String result = new String(baos.toByteArray());
        //        Log.d(LOG_TAG, "Content-length: " + connection.getContentLength() + " bytes read: "
        //            + totalRead);
        //        Log.d(LOG_TAG, "Response: " + result);

        Object response = null;
        if (result.startsWith("{")) {
          response = new JSONObject(result);
        } else if (result.length() > 0) {
          response = new JSONArray(result);
        } else {
          response = null;
        }
        handleResponse(response);
      }
    } catch (final Exception e) {
      //      Log.e(LOG_TAG, "error", e);

      if (!stopped) {
        handleConnectionError();
      }
    } finally {
      if (dos != null) {
        try {
          dos.close();
        } catch (final IOException ignore) {
        }
      }
      if (connection != null) {
        connection.disconnect();
      }
    }
  }

  public void cancel() {
    stopped = true;
  }

  public abstract String getRequestYaml();

  public abstract void handleResponse(final Object responseObject);

  public abstract String getRequestMethod();

  public abstract void handleConnectionError();

  public abstract String getPath();
}
