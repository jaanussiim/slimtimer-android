package com.jaanussiim.slimtimer.android.net_old;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import com.jaanussiim.slimtimer.android.Constants;
import com.jaanussiim.slimtimer.android.db.SlimtimerDB;
import com.jaanussiim.slimtimer.android.utils.ManufacturerExractor;

public class PostTimeReportRequest extends NetworkRequest {
  private static final String TIME_REPORT_URI = "http://apps.jaanussiim.com/apps/mtimer/add";

  private static final String T = PostTimeReportRequest.class.getSimpleName();

  private static final HttpParams params = new BasicHttpParams();

  static {
    HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
    HttpConnectionParams.setStaleCheckingEnabled(params, false);
    HttpConnectionParams.setConnectionTimeout(params, 20 * 1000);
    HttpConnectionParams.setSoTimeout(params, 20 * 1000);
    HttpConnectionParams.setSocketBufferSize(params, 8192);
  }

  private final SlimtimerDB database;

  private final SharedPreferences sharedPreferences;

  private final Context context;

  public PostTimeReportRequest(Context context, SlimtimerDB database, SharedPreferences sharedPreferences) {
    this.context = context;
    this.database = database;
    this.sharedPreferences = sharedPreferences;
  }

  @Override
  public void run() {
    DefaultHttpClient client = new DefaultHttpClient(params);
    HttpPost post = new HttpPost(TIME_REPORT_URI);
    post.setHeader("Content-Type", "application/x-www-form-urlencoded");
    StringBuffer content = new StringBuffer();

    content.append("seconds=").append(database.timeReportsSum());
    String reportCode = sharedPreferences.getString(Constants.PREFERENCES_REPORTING_CODE, "");
    if (!"".equals(reportCode)) {
      Log.d(T, "reporting code present");
      content.append("&reporting_code=").append(reportCode);
    }
    content.append("&platform=android");
    content.append("&version=").append(Build.VERSION.RELEASE);
    content.append("&model=").append(urlencode(Build.MODEL));
    content.append("&manufacturer=").append(urlencode(ManufacturerExractor.getManufacturer()));
    WindowManager wmanager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    Display display = wmanager.getDefaultDisplay();
    content.append("&s_width=").append(display.getWidth());
    content.append("&s_height=").append(display.getHeight());
    DisplayMetrics outMetrics = new DisplayMetrics();
    display.getMetrics(outMetrics);
    content.append("&density=").append(outMetrics.density);

    try {
      ByteArrayEntity baEntity = new ByteArrayEntity(content.toString().getBytes("utf-8"));
      baEntity.setContentType("application/x-www-form-urlencoded");
      post.setEntity(baEntity);
    } catch (UnsupportedEncodingException e) {
      Log.e("PTRR", "", e);
      return;
    }

    try {
      HttpResponse resp = client.execute(post);
      if (resp.getStatusLine().getStatusCode() == 200) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        resp.getEntity().writeTo(baos);
        String reportingCode = new String(baos.toByteArray(), "utf-8");
        Log.d(T, "code:" + reportingCode);
        final SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(Constants.PREFERENCES_REPORTING_CODE, reportingCode);
        editor.commit();
        database.deleteTimeReports();
      }
    } catch (IOException e) {
      Log.e("PTRR", "", e);
    }
  }

  private String urlencode(String value) {
    try {
      return URLEncoder.encode(value, "utf-8");
    } catch (UnsupportedEncodingException e) {
      Log.e("PTRR", "", e);
    }
    return "notencoded";
  }

  @Override
  public String getPath() {
    return null;
  }

  @Override
  public String getRequestMethod() {
    return null;
  }

  @Override
  public String getRequestYaml() {
    return null;
  }

  @Override
  public void handleConnectionError() {
  }

  @Override
  public void handleResponse(Object responseObject) {
  }
}
