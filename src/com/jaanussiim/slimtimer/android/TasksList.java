package com.jaanussiim.slimtimer.android;

import java.util.List;

import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
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
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;

import com.jaanussiim.slimtimer.android.components.Task;
import com.jaanussiim.slimtimer.android.db.SlimtimerDB;
import com.jaanussiim.slimtimer.android.net_old.LoginRequest;
import com.jaanussiim.slimtimer.android.net_old.NetworkRequestCaller;
import com.jaanussiim.slimtimer.android.net_old.NetworkRequestsChain;
import com.jaanussiim.slimtimer.android.net_old.SyncTaskRequest;
import com.jaanussiim.slimtimer.android.net_old.SyncWithServerTasksRequest;

public class TasksList extends ListActivity implements OnItemSelectedListener, OnClickListener,
    NetworkRequestCaller {
  private static final int SYNCHRONIZING_DIALOG = 0;

  private static final int MENU_OPTION_ADD = Menu.FIRST;
  private static final int MENU_OPTION_SYNC = MENU_OPTION_ADD + 1;

  private static final int CONTEXT_MENU_OPTION_COMPLETE = MENU_OPTION_SYNC + 1;
  private static final int CONTEXT_MENU_OPTION_INCOMPLETE = CONTEXT_MENU_OPTION_COMPLETE + 1;
  private static final int CONTEXT_MENU_OPTION_DELETE = CONTEXT_MENU_OPTION_INCOMPLETE + 1;
  private static final int CONTEXT_MENU_OPTION_UNDELETE = CONTEXT_MENU_OPTION_DELETE + 1;

  private static final int ACTIVITY_REQUEST_EDIT_TASK = 0;
  private SlimtimerDB databaseHandler;
  private List<Task> tasks;
  private TasksListAdapter adapter;
  private View lastSelected;
  private int lastSelectedIndex = -1;

  private Task contextMenuOn;

  @Override
  protected void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    getListView().setBackgroundResource(R.color.screen_background);

    databaseHandler = new SlimtimerDB(this);
    databaseHandler.open();

    tasks = databaseHandler.listAllTasks(TimerCore.getInstance().getUser());
    adapter = new TasksListAdapter(this, tasks);

    setListAdapter(adapter);
    getListView().setOnItemSelectedListener(this);
  }

  @Override
  public boolean onCreateOptionsMenu(final Menu menu) {
    super.onCreateOptionsMenu(menu);
    menu.add(0, MENU_OPTION_ADD, 0, R.string.manage_tasks_menu_add);
    menu.add(0, MENU_OPTION_SYNC, 0, R.string.manage_tasks_menu_sync);
    return true;
  }

  @Override
  public boolean onPrepareOptionsMenu(final Menu menu) {
    super.onPrepareOptionsMenu(menu);

    return true;
  }

  @Override
  public void onCreateContextMenu(final ContextMenu menu, final View v,
      final ContextMenuInfo menuInfo) {
    final TaskListElementWrapper wrapper = (TaskListElementWrapper) v.getTag();
    contextMenuOn = tasks.get(wrapper.getPosition());

    if (contextMenuOn.isDeleted()) {
      menu.add(0, CONTEXT_MENU_OPTION_UNDELETE, 0, R.string.manage_tasks_context_menu_undelete);
      return;
    } else {
      menu.add(0, CONTEXT_MENU_OPTION_DELETE, 0, R.string.manage_tasks_context_menu_delete);
    }

    if (contextMenuOn.isCompleted()) {
      menu.add(0, CONTEXT_MENU_OPTION_INCOMPLETE, 0, R.string.manage_tasks_context_menu_incomplete);
    } else {
      menu.add(0, CONTEXT_MENU_OPTION_COMPLETE, 0, R.string.manage_tasks_context_menu_complete);
    }
  }

  @Override
  public boolean onMenuItemSelected(final int featureId, final MenuItem item) {
    switch (item.getItemId()) {
    case MENU_OPTION_ADD:
      final Intent startEdit = new Intent(this, EditTask.class);
      startEdit.putExtra(Task.EDIT_TASK_ID, -1L);
      startActivityForResult(startEdit, ACTIVITY_REQUEST_EDIT_TASK);
      return true;
    case CONTEXT_MENU_OPTION_DELETE:
    case CONTEXT_MENU_OPTION_UNDELETE:
      contextMenuOn.deleteTask(item.getItemId() == CONTEXT_MENU_OPTION_DELETE);
      if (contextMenuOn.isDeleted() && contextMenuOn.getSlimId() <= 0) {
        databaseHandler.deleteTask(contextMenuOn);
      } else {
        databaseHandler.updateOrCreateTaskByDbId(contextMenuOn, TimerCore.getInstance().getUser());
      }
      reloadTasks();
      return true;
    case CONTEXT_MENU_OPTION_COMPLETE:
    case CONTEXT_MENU_OPTION_INCOMPLETE:
      contextMenuOn.setCompleted(item.getItemId() == CONTEXT_MENU_OPTION_COMPLETE);
      databaseHandler.updateOrCreateTaskByDbId(contextMenuOn, TimerCore.getInstance().getUser());
      reloadTasks();
      return true;
    case MENU_OPTION_SYNC:
      showDialog(SYNCHRONIZING_DIALOG);
      runOnUiThread(new Runnable() {
        public void run() {
          final NetworkRequestsChain networkRequests = new NetworkRequestsChain(TasksList.this);
          if (TimerCore.getInstance().getAccessToken() == null) {
            final SharedPreferences preferences = getSharedPreferences(Constants.PREFERENCES_NAME,
                Context.MODE_PRIVATE);
            final String username = preferences.getString(Constants.PREFERENCES_EMAIL_KEY, "");
            final String password = preferences.getString(Constants.PREFERENCES_PASSWORD_KEY, "");
            networkRequests.add(new LoginRequest(TasksList.this, username, password, false));
          }

          networkRequests.add(new SyncWithServerTasksRequest(TasksList.this, false));

          for (final Task task : tasks) {
            if (task.isInSyncWithServer()) {
              continue;
            }

            if (task.isDeleted() && task.getSlimId().equals(Task.ID_UNKNOWN)) {
              databaseHandler.deleteTask(task);
              continue;
            }

            networkRequests.add(new SyncTaskRequest(task, databaseHandler));
          }

          networkRequests.execute();
        }
      });
      return true;
    default:
      return super.onOptionsItemSelected(item);
    }
  }

  @Override
  protected Dialog onCreateDialog(final int id) {
    switch (id) {
    case SYNCHRONIZING_DIALOG:
      final ProgressDialog dialog = new ProgressDialog(this);
      dialog.setMessage(getText(R.string.manage_tasks_synchronizing_message));
      dialog.setIndeterminate(true);
      dialog.setCancelable(true);
      return dialog;
    default:
      return super.onCreateDialog(id);
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    databaseHandler.close();
  }

  @Override
  protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
    if (resultCode != RESULT_CANCELED) {
      reloadTasks();
    }
  }

  public void reloadTasks() {
    tasks = databaseHandler.listAllTasks(TimerCore.getInstance().getUser());
    adapter.setTasks(tasks);
  }

  class TasksListAdapter extends BaseAdapter {
    private List<Task> tasks;
    private final TasksList ctx;

    public TasksListAdapter(final TasksList tasksList, final List<Task> tasks) {
      ctx = tasksList;
      this.tasks = tasks;
    }

    public void setTasks(final List<Task> tasks) {
      this.tasks = tasks;
      ctx.runOnUiThread(new Runnable() {
        public void run() {
          notifyDataSetChanged();
        }
      });
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
      TaskListElementWrapper wrapper = null;

      if (row == null) {
        row = ctx.getLayoutInflater().inflate(R.layout.task_manage_list_item, null);
        wrapper = new TaskListElementWrapper(row);
        row.setTag(wrapper);
      } else {
        wrapper = (TaskListElementWrapper) row.getTag();
      }

      final Task displayedTask = tasks.get(position);
      wrapper.setPosition(position);
      wrapper.setName(displayedTask.getName());
      wrapper.makeUnselected(displayedTask.isCompleted(), displayedTask.isDeleted());

      row.setOnClickListener(TasksList.this);
      TasksList.this.registerForContextMenu(row);

      return row;
    }
  }

  class TaskListElementWrapper {
    private final View wrapped;
    private TextView nameView;
    private int position;

    public TaskListElementWrapper(final View wrapped) {
      this.wrapped = wrapped;
    }

    public void setPosition(final int position) {
      this.position = position;
    }

    public int getPosition() {
      return position;
    }

    public void setStyle(final boolean completed) {
      if (completed) {
        wrapped.setBackgroundResource(R.color.manage_tasks_complete_background);
      } else {
        wrapped.setBackgroundResource(R.color.manage_tasks_default_background);
      }
    }

    public void setName(final String name) {
      getNameVew().setText(name);
    }

    private TextView getNameVew() {
      if (nameView == null) {
        nameView = (TextView) wrapped.findViewById(R.id.manage_task_name);
      }

      return nameView;
    }

    public void makeSelected(final boolean complete) {
      if (complete) {
        wrapped.setBackgroundResource(R.color.manage_tasks_complete_selected_background);
      } else {
        wrapped.setBackgroundResource(R.color.manage_tasks_default_selected_background);
      }
    }

    public void makeUnselected(final boolean complete, final boolean deleted) {
      if (deleted) {
        wrapped.setBackgroundResource(R.color.manage_tasks_deleted_background);
        return;
      }

      if (complete) {
        wrapped.setBackgroundResource(R.color.manage_tasks_complete_background);
      } else {
        wrapped.setBackgroundResource(R.color.manage_tasks_default_background);
      }
    }
  }

  @Override
  protected void onListItemClick(final ListView l, final View v, final int position, final long id) {
    handleViewClick(v);
  }

  public void onItemSelected(final AdapterView<?> parentView, final View childView,
      final int position, final long id) {
    unselectPreviousElement();

    final TaskListElementWrapper currentWrapper = (TaskListElementWrapper) childView.getTag();
    final Task currentEntry = tasks.get(position);
    currentWrapper.makeSelected(currentEntry.isCompleted());

    lastSelected = childView;
    lastSelectedIndex = position;
  }

  //OnItemSelectedListener
  public void onNothingSelected(final AdapterView<?> parentView) {
    unselectPreviousElement();

    lastSelected = null;
    lastSelectedIndex = -1;
  }

  private void unselectPreviousElement() {
    if (lastSelected != null) {
      final TaskListElementWrapper wrapper = (TaskListElementWrapper) lastSelected.getTag();
      final Task lastEntry = tasks.get(lastSelectedIndex);
      wrapper.makeUnselected(lastEntry.isCompleted(), lastEntry.isDeleted());
    }
  }

  public void onClick(final View v) {
    handleViewClick(v);
  }

  private void handleViewClick(final View view) {
    final TaskListElementWrapper wrapper = (TaskListElementWrapper) view.getTag();
    final int position = wrapper.getPosition();
    final Task task = tasks.get(position);

    final Intent startEdit = new Intent(this, EditTask.class);
    startEdit.putExtra(Task.EDIT_TASK_ID, task.getDatabaseId());
    startActivityForResult(startEdit, ACTIVITY_REQUEST_EDIT_TASK);
  }

  public void requestError(final int errorCode) {
    removeDialog(SYNCHRONIZING_DIALOG);
    runOnUiThread(new Runnable() {
      public void run() {
        Toast.makeText(TasksList.this, R.string.manage_tasks_sync_error, Toast.LENGTH_LONG).show();
        reloadTasks();
      }
    });
  }

  public void requestSuccess(final int code) {
    if (code != NetworkRequestCaller.REQUESTS_CHAIN) {
      return;
    }
    removeDialog(SYNCHRONIZING_DIALOG);
    runOnUiThread(new Runnable() {
      public void run() {
        Toast.makeText(TasksList.this, R.string.manage_tasks_sync_complete, Toast.LENGTH_SHORT)
            .show();
        reloadTasks();
      }
    });
  }
}
