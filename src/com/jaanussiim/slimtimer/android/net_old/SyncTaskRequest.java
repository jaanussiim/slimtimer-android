package com.jaanussiim.slimtimer.android.net_old;

import java.text.MessageFormat;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import com.jaanussiim.slimtimer.android.TimerCore;
import com.jaanussiim.slimtimer.android.components.Task;
import com.jaanussiim.slimtimer.android.db.SlimtimerDB;

public class SyncTaskRequest extends NetworkRequest {
  private static final int REQUEST_CREATE = 0;
  private static final int REQUEST_UPDATE = 1;
  private static final int REQUEST_DELETE = 2;

  private static final String ID_KEY = "id";

  private int requestType;

  private final Task task;
  private final SlimtimerDB databaseHandler;

  private static final String CREATE_PATH = "/users/{0}/tasks?api_key={1}&access_token={2}";
  private static final String UPDATE_DELETE_PATH = "/users/{0}/tasks/{1}?api_key={2}&access_token={3}";
  private static final String CREATE_CONTENT = "task:\n  name: {0}";
  private static final String LOG_TAG = "SyncTask";

  public SyncTaskRequest(final Task task, final SlimtimerDB databaseHandler) {
    this.task = task;
    this.databaseHandler = databaseHandler;

    if (task.getSlimId() <= 0) {
      requestType = REQUEST_CREATE;
    } else if (task.isDeleted()) {
      requestType = REQUEST_DELETE;
    } else {
      requestType = REQUEST_UPDATE;
    }

    //    Log.d(LOG_TAG, "RType: " + requestType);
  }

  @Override
  public String getPath() {
    final TimerCore tCore = TimerCore.getInstance();
    if (requestType == REQUEST_CREATE) {
      return MessageFormat.format(CREATE_PATH, tCore.getUserid().toString(), APP_KEY, TimerCore
          .getInstance().getAccessToken());
    } else if (requestType == REQUEST_DELETE || requestType == REQUEST_UPDATE) {
      return MessageFormat.format(UPDATE_DELETE_PATH, tCore.getUserid().toString(), task
          .getSlimId().toString(), APP_KEY, TimerCore.getInstance().getAccessToken());
    } else {
      return null;
    }
  }

  @Override
  public String getRequestMethod() {
    switch (requestType) {
    case REQUEST_CREATE:
      return NetworkRequest.METHOD_POST;
    case REQUEST_DELETE:
      return NetworkRequest.METHOD_DELETE;
    case REQUEST_UPDATE:
      return NetworkRequest.METHOD_PUT;
    default:
      return null;
    }
  }

  @Override
  public String getRequestYaml() {
    switch (requestType) {
    case REQUEST_CREATE:
      return MessageFormat.format(CREATE_CONTENT, task.getName());
    case REQUEST_DELETE:
      return "";
    case REQUEST_UPDATE:
      final StringBuffer result = new StringBuffer();
      result.append("task:\n");
      result.append("  name: ").append(task.getName()).append("\n");
      if (task.isCompleted()) {
        result.append("  completed_on: ").append(SlimtimerDB.DATE_FORMAT.format(new Date()));
      }
      return result.toString();
    default:
      return null;
    }
  }

  @Override
  public void handleConnectionError() {
    //    Log.e(LOG_TAG, "Could not sync " + task.getName());
  }

  @Override
  public void handleResponse(final Object responseObject) {
    switch (requestType) {
    case REQUEST_CREATE:
      final JSONObject response = (JSONObject) responseObject;
      try {
        final long slimId = response.getLong(ID_KEY);
        task.setSlimId(slimId);
        task.setIsInSync(true);
        databaseHandler.updateOrCreateTaskByDbId(task, TimerCore.getInstance().getUser());
      } catch (final JSONException e) {
        //        Log.e(LOG_TAG, "nadling " + requestType + " error", e);
      }
      break;
    case REQUEST_DELETE:
      databaseHandler.deleteTask(task);
      break;
    }
  }
}
