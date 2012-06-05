package com.jaanussiim.slimtimer.android.net_old;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;

import org.json.JSONException;
import org.json.JSONObject;

import com.jaanussiim.slimtimer.android.TimerCore;
import com.jaanussiim.slimtimer.android.components.Entry;
import com.jaanussiim.slimtimer.android.db.SlimtimerDB;

public class UploadEntryRequest extends NetworkRequest {
  private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

  private static final String LOG_TAG = "UploadEntriesRequest";
  private static final String PATH = "/users/{0}/time_entries?api_key={1}&access_token={2}";
  private static final String REQUEST = "user:\n  email: {0}\n  password: {1}\napi_key: {2}";

  private final Entry entry;

  private final SlimtimerDB database;

  public UploadEntryRequest(final Entry entry, final SlimtimerDB database) {
    this.entry = entry;
    this.database = database;
  }

  @Override
  public String getPath() {
    final TimerCore tCore = TimerCore.getInstance();
    return MessageFormat.format(PATH, tCore.getUserid().toString(), APP_KEY, TimerCore.getInstance().getAccessToken());
  }

  @Override
  public String getRequestMethod() {
    return "POST";
  }

  @Override
  public String getRequestYaml() {
    final StringBuffer rContent = new StringBuffer();

    rContent.append("time_entry:\n");
    rContent.append("  start_time: ").append(DATE_FORMAT.format(entry.getStartTime())).append("\n");
    rContent.append("  end_time: ").append(DATE_FORMAT.format(entry.getEndTime())).append("\n");
    rContent.append("  duration_in_seconds: ").append(entry.getRunTime() / 1000).append("\n");
    rContent.append("  tags: ").append(entry.getTagsAsJoinedStrings()).append("\n");
    rContent.append("  comments: ").append(entry.getComment()).append("\n");
    rContent.append("  in_progress: ").append(false).append("\n");
    rContent.append("  task_id: ").append(entry.getTask().getSlimId().toString()).append("\n");

    return rContent.toString();
  }

  @Override
  public void handleConnectionError() {
    //TODO jaanus : implement handleConnectionError
    //    Log.e(LOG_TAG, "TODO jaanus : implement handleConnectionError");
  }

  @Override
  public void handleResponse(final Object responseObject) {
    final JSONObject response = (JSONObject) responseObject;
    try {
      final long entryId = response.getLong("id");
      //      Log.d(LOG_TAG, "entry id: " + entryId);
      database.addTimeReport(entry.getRunTime() / 1000);
      database.deleteEntry(entry);
    } catch (final JSONException e) {
      //      Log.e(LOG_TAG, "entry upload response", e);
    }
  }
}
