package com.jaanussiim.slimtimer.android.net_old;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

import com.jaanussiim.slimtimer.android.TimerCore;
import com.jaanussiim.slimtimer.android.components.Task;
import com.jaanussiim.slimtimer.android.db.SlimtimerDB;

public class SyncWithServerTasksRequest extends NetworkRequest {
  private static final String LOG_TAG = "RetrieveTasksRequest";
  private static final String PATH = "/users/{0}/tasks?api_key={1}&access_token={2}";
  //  private static final String PATH = "/users/{0}/tasks?api_key={1}&access_token={2}&show_completed=no";
  private static final String TASK_NAME_KEY = "name";
  private static final String TASK_ID_KEY = "id";
  private static final String COMPLETED_ON_KEY = "completed_on";
  private final Context slimtimerTasksList;
  private final boolean newUser;

  public SyncWithServerTasksRequest(final Context ctx, final boolean newUser) {
    this.slimtimerTasksList = ctx;
    this.newUser = newUser;
  }

  @Override
  public String getPath() {
    final TimerCore timerCore = TimerCore.getInstance();
    return MessageFormat.format(PATH, timerCore.getUserid().toString(), APP_KEY, timerCore
        .getAccessToken());
  }

  @Override
  public String getRequestMethod() {
    //TODO jaanus : fix this
    return "GET";
  }

  @Override
  public String getRequestYaml() {
    return "";
  }

  @Override
  public void handleConnectionError() {
    //    Log.e(LOG_TAG, "TODO jaanus : implement error handling");
  }

  @Override
  public void handleResponse(final Object responseObject) {
    //    Log.d(LOG_TAG, "TODO jaanus : handle response");
    final JSONArray response = (JSONArray) responseObject;
    final List<Task> tasks = new ArrayList<Task>();

    try {
      for (int i = 0; i < response.length(); i++) {
        final JSONObject jsonTask = response.getJSONObject(i);
        final Long id = jsonTask.getLong(TASK_ID_KEY);
        final String name = jsonTask.getString(TASK_NAME_KEY);
        final boolean completed = !jsonTask.isNull(COMPLETED_ON_KEY);
        System.out.println(name + " : " + jsonTask.getString(COMPLETED_ON_KEY));
        final Task saved = new Task(id, name, completed);
        //        Log.d(LOG_TAG, saved.toString());
        tasks.add(saved);
      }

      final SlimtimerDB db = new SlimtimerDB(slimtimerTasksList);
      db.open();
      if (tasks.size() == 0 && newUser) {
        tasks.add(new Task(-1L, Task.ID_UNKNOWN, "Daily scrum", false, false, false, false, 0));
        tasks.add(new Task(-1L, Task.ID_UNKNOWN, "AjaxScaffold", false, false, false, false, 0));
        tasks.add(new Task(-1L, Task.ID_UNKNOWN, "Blog posts", false, false, false, false, 0));
        tasks.add(new Task(-1L, Task.ID_UNKNOWN, "Jogging", false, false, false, false, 0));
        tasks
            .add(new Task(-1L, Task.ID_UNKNOWN, "Counter-strike ;)", false, false, false, false, 0));
        for (final Task task : tasks) {
          db.updateOrCreateTaskByDbId(task, TimerCore.getInstance().getUser());
        }
      } else {
        db.updateTasks(tasks, TimerCore.getInstance().getUser());
      }
      db.close();

      ((NetworkRequestCaller) slimtimerTasksList)
          .requestSuccess(NetworkRequestCaller.SYNC_SERVER_TASKS);
    } catch (final JSONException e) {
      //      Log.e(LOG_TAG, "parsing response", e);
      ((NetworkRequestCaller) slimtimerTasksList)
          .requestError(NetworkRequestCaller.SYNC_SERVER_TASKS);
    } catch (final Exception e) {
      //      Log.e(LOG_TAG, "parsing other exception", e);
    }
  }
}
