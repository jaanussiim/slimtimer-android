package com.jaanussiim.slimtimer.android;

import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.AdapterView.OnItemSelectedListener;

import com.jaanussiim.slimtimer.android.activities.LoginActivity;
import com.jaanussiim.slimtimer.android.components.Entry;
import com.jaanussiim.slimtimer.android.components.Task;
import com.jaanussiim.slimtimer.android.db.SlimtimerDB;
import com.jaanussiim.slimtimer.android.net_old.NetworkRequest;
import com.jaanussiim.slimtimer.android.net_old.NetworkRequestCaller;
import com.jaanussiim.slimtimer.android.net_old.SyncWithServerTasksRequest;

public class SlimtimerTasksList extends ListActivity implements OnItemSelectedListener,
    OnClickListener, NetworkRequestCaller {
  private static final int DIALOG_RETRIEVING_TASKS = 0;
  private static final int DIALOG_MINI_GUIDE = 1;

  public static final int ACTIVITY_REQUEST_EDIT_ENTRY = 0;
  public static final int ACTIVITY_REQUEST_SHOW_ENTRIES = 1;
  public static final int ACTIVITY_REQUEST_SHOW_TASKS = 2;

  private static final int MENU_OPTION_SHOW_REPORTS = Menu.FIRST;
  private static final int MENU_OPTION_MANAGE_TASKS = MENU_OPTION_SHOW_REPORTS + 1;
  private static final int MENU_OPTION_LOGOUT = MENU_OPTION_MANAGE_TASKS + 1;

  private static final int CONTEXT_MENU_OPTION_COMPLETE = MENU_OPTION_LOGOUT + 1;
  private static final int CONTEXT_MENU_OPTION_START = CONTEXT_MENU_OPTION_COMPLETE + 1;
  private static final int CONTEXT_MENU_OPTION_CANCEL = CONTEXT_MENU_OPTION_START + 1;

  private static final String LOG_TAG = "SlimtimerTasksList";
  private SlimtimerDB databaseHandler;
  private TasksAdapter adapter;
  private boolean retrieveDialogShown = false;

  private View lastSelected;
  private int lastSelectedIndex = -1;

  private List<Task> tasks;

  private Task contextMenuOnTask;

  @Override
  protected void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    getListView().setBackgroundResource(R.color.screen_background);

    databaseHandler = new SlimtimerDB(this);
    databaseHandler.open();

    tasks = databaseHandler.listUncompletedTasks(TimerCore.getInstance().getUser());
    adapter = new TasksAdapter(this, tasks);

    setListAdapter(adapter);

    if (tasks.size() == 0 && TimerCore.getInstance().isNewUser()) {
      //      Log.d(LOG_TAG, "For new user");
      showDialog(DIALOG_RETRIEVING_TASKS);
      final NetworkRequest retrieveTasks = new SyncWithServerTasksRequest(this, true);
      retrieveTasks.execute();
    } else if (tasks.size() == 0) {
      //      Log.i(LOG_TAG, "For user without tasks");
    }

    getListView().setOnItemSelectedListener(this);
  }

  @Override
  protected void onRestart() {
    super.onRestart();
    //    Log.d("ZZZZZZZZZZZZZZZZZZ", "onRestart");
  }

  @Override
  protected void onStart() {
    super.onStart();
    //    Log.d("ZZZZZZZZZZZZZZZZZZ", "onStart");
  }

  @Override
  protected void onResume() {
    super.onResume();
    //    Log.d("ZZZZZZZZZZZZZZZZZZ", "onResume");
  }

  @Override
  protected void onPause() {
    super.onPause();
    //    Log.d("ZZZZZZZZZZZZZZZZZZ", "onPause");
  }

  @Override
  protected void onStop() {
    super.onStop();
    //    Log.d("ZZZZZZZZZZZZZZZZZZ", "onStop");
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    //    Log.d("ZZZZZZZZZZZZZZZZZZ", "onDestroy");
    databaseHandler.close();
  }

  @Override
  protected Dialog onCreateDialog(final int id) {
    switch (id) {
    case DIALOG_RETRIEVING_TASKS:
      final ProgressDialog dialog = new ProgressDialog(this);
      dialog.setMessage(getText(R.string.retrieving_tasks_message));
      dialog.setIndeterminate(true);
      dialog.setCancelable(true);
      retrieveDialogShown = true;
      return dialog;
    case DIALOG_MINI_GUIDE:
      return new AlertDialog.Builder(SlimtimerTasksList.this).setTitle(R.string.mini_guide_title)
          .setMessage(R.string.new_user_mini_guide).setPositiveButton(
              R.string.mini_guide_button_ok, new DialogInterface.OnClickListener() {
                public void onClick(final DialogInterface dialog, final int whichButton) {
                  dismissDialog(DIALOG_MINI_GUIDE);
                }
              }).create();
    }
    return null;
  }

  @Override
  protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
    switch (requestCode) {
    case ACTIVITY_REQUEST_EDIT_ENTRY:
      if (resultCode == RESULT_OK) {
        reloadTasks();
      }
      return;
    case ACTIVITY_REQUEST_SHOW_ENTRIES:
      //fall through
    case ACTIVITY_REQUEST_SHOW_TASKS:
      reloadTasks();
      return;
    }
  }

  @Override
  public boolean onPrepareOptionsMenu(final Menu menu) {
    super.onPrepareOptionsMenu(menu);
    final SharedPreferences preferences = getSharedPreferences(Constants.PREFERENCES_NAME,
        Context.MODE_PRIVATE);
    if (!"".equals(preferences.getString(Constants.PREFERENCES_EMAIL_KEY, ""))) {
      menu.findItem(MENU_OPTION_LOGOUT).setVisible(true);
    } else {
      menu.findItem(MENU_OPTION_LOGOUT).setVisible(false);
    }
    return true;
  }

  @Override
  public boolean onCreateOptionsMenu(final Menu menu) {
    super.onCreateOptionsMenu(menu);
    menu.add(0, MENU_OPTION_SHOW_REPORTS, 0, R.string.menu_show_entries);
    menu.add(0, MENU_OPTION_MANAGE_TASKS, 0, R.string.menu_manage_tasks);
    menu.add(0, MENU_OPTION_LOGOUT, 0, R.string.menu_logout);
    return true;
  }

  @Override
  public void onCreateContextMenu(final ContextMenu menu, final View v,
      final ContextMenuInfo menuInfo) {
    final TaskViewWrapper wrapper = (TaskViewWrapper) v.getTag();
    contextMenuOnTask = tasks.get(wrapper.getPosition());

    if (!contextMenuOnTask.isRunning()) {
      menu.add(0, CONTEXT_MENU_OPTION_START, 0, R.string.tasks_list_context_menu_start);
    } else {
      menu.add(0, CONTEXT_MENU_OPTION_COMPLETE, 0, R.string.tasks_list_context_menu_complete);
      menu.add(0, CONTEXT_MENU_OPTION_CANCEL, 0, R.string.tasks_list_context_menu_cancel);
    }
  }

  @Override
  public boolean onMenuItemSelected(final int featureId, final MenuItem item) {
    switch (item.getItemId()) {
    case MENU_OPTION_SHOW_REPORTS:
      final Intent entriesIntent = new Intent(SlimtimerTasksList.this, EntriesList.class);
      startActivityForResult(entriesIntent, ACTIVITY_REQUEST_SHOW_ENTRIES);
      return true;
    case MENU_OPTION_MANAGE_TASKS:
      final Intent tasksIntent = new Intent(SlimtimerTasksList.this, TasksList.class);
      startActivityForResult(tasksIntent, ACTIVITY_REQUEST_SHOW_TASKS);
      return true;
    case MENU_OPTION_LOGOUT:
      final SharedPreferences preferences = getSharedPreferences(Constants.PREFERENCES_NAME,
          Context.MODE_PRIVATE);
      final SharedPreferences.Editor editor = preferences.edit();
      editor.remove(Constants.PREFERENCES_EMAIL_KEY);
      editor.remove(Constants.PREFERENCES_PASSWORD_KEY);
      editor.commit();
      final Intent loginIntent = new Intent(SlimtimerTasksList.this, LoginActivity.class);
      startActivity(loginIntent);
      finish();
      return true;
    case CONTEXT_MENU_OPTION_CANCEL:
      databaseHandler.cancelTaskRun(contextMenuOnTask.getDatabaseId(), TimerCore.getInstance()
          .getUser().getDatabaseId());
      reloadTasks();
      return true;
    case CONTEXT_MENU_OPTION_COMPLETE:
      databaseHandler.markCompleted(contextMenuOnTask, TimerCore.getInstance().getUser());
      reloadTasks();
      return true;
    case CONTEXT_MENU_OPTION_START:
      databaseHandler.markRunning(contextMenuOnTask, TimerCore.getInstance().getUser());
      reloadTasks();
      return true;
    default:
      return super.onMenuItemSelected(featureId, item);
    }
  }

  public void reloadTasks() {
    //TODO jaanus : what happens with last selected view and index?
    tasks = databaseHandler.listUncompletedTasks(TimerCore.getInstance().getUser());
    adapter.setTasks(tasks);
  }

  //OnItemSelectedListener
  public void onItemSelected(final AdapterView<?> parentView, final View childView,
      final int position, final long id) {
    //    Log.d(LOG_TAG, "onItemSelected");
    unselectPreviousElement();

    final TaskViewWrapper currentWrapper = (TaskViewWrapper) childView.getTag();
    final Task currentTask = tasks.get(position);
    currentWrapper.makeSelected(currentTask.isRunning());

    lastSelected = childView;
    lastSelectedIndex = position;
  }

  //OnItemSelectedListener
  public void onNothingSelected(final AdapterView<?> parentView) {
    unselectPreviousElement();

    lastSelected = null;
    lastSelectedIndex = -1;
  }

  //OnClickListener
  public void onClick(final View view) {
    handleViewClick(view);
  }

  @Override
  protected void onListItemClick(final ListView l, final View v, final int position, final long id) {
    handleViewClick(v);
  }

  private void handleViewClick(final View view) {
    final TaskViewWrapper wrapper = (TaskViewWrapper) view.getTag();
    final int position = wrapper.getPosition();
    final Task task = tasks.get(position);

    if (task.isRunning()) {
      final long reportId = databaseHandler.getReportId(task, TimerCore.getInstance().getUser());
      final Intent startEdit = new Intent(this, EditEntry.class);
      startEdit.putExtra(Entry.EDIT_INTENT_ID, reportId);
      startActivityForResult(startEdit, ACTIVITY_REQUEST_EDIT_ENTRY);
    } else {
      task.startRunning();
      databaseHandler.markRunning(task, TimerCore.getInstance().getUser());
    }

    if (task.isRunning()) {
      wrapper.makeViewRunning(task.getRunTime());
    } else {
      wrapper.makeViewDefault();
    }
  }

  private void unselectPreviousElement() {
    if (lastSelected != null) {
      final TaskViewWrapper wrapper = (TaskViewWrapper) lastSelected.getTag();
      final Task lastTask = tasks.get(lastSelectedIndex);
      wrapper.makeUnselected(lastTask.isRunning());
    }
  }

  private class TasksAdapter extends BaseAdapter {
    private static final String LOG_TAG = "TasksAdapter";
    private final Activity context;
    private List<Task> tasks;

    public TasksAdapter(final Activity context, final List<Task> tasks) {
      this.context = context;
      this.tasks = tasks;
    }

    public int getCount() {
      return tasks.size();
    }

    public Object getItem(final int position) {
      return tasks.get(position);
    }

    public long getItemId(final int position) {
      return position;
    }

    public View getView(final int position, final View convertView, final ViewGroup parent) {
      View row = convertView;
      TaskViewWrapper wrapper = null;

      if (row == null) {
        row = context.getLayoutInflater().inflate(R.layout.timing_list_element, null);
        wrapper = new TaskViewWrapper(row);
        row.setTag(wrapper);
      } else {
        wrapper = (TaskViewWrapper) row.getTag();
      }

      wrapper.setPosition(position);
      final Task displayedTask = tasks.get(position);

      final String currentItem = displayedTask.getName();
      wrapper.setTaskName(currentItem);

      if (displayedTask.isRunning()) {
        wrapper.makeViewRunning(displayedTask.getRunTime());
      } else {
        wrapper.makeViewDefault();
      }

      row.setOnClickListener(SlimtimerTasksList.this);
      SlimtimerTasksList.this.registerForContextMenu(row);

      return row;
    }

    public void setTasks(final List<Task> tasks) {
      this.tasks = tasks;
      context.runOnUiThread(new Runnable() {
        public void run() {
          notifyDataSetChanged();
        }
      });
    }
  }

  public void responseError() {
    //    Log.e(LOG_TAG, "TODO jaanus : response error");
  }

  public void requestError(final int errorCode) {
    responseError();
  }

  public void requestSuccess(final int requestCode) {
    if (retrieveDialogShown) {
      dismissDialog(DIALOG_RETRIEVING_TASKS);
      retrieveDialogShown = false;
    }
    runOnUiThread(new Runnable() {
      public void run() {
        if (TimerCore.getInstance().isNewUser()) {
          showDialog(DIALOG_MINI_GUIDE);
          TimerCore.getInstance().setIsNewUser(false);
        }
      }
    });
    reloadTasks();
  }
}
