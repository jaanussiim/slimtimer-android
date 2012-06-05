package com.jaanussiim.slimtimer.android;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.MultiAutoCompleteTextView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;

import com.jaanussiim.slimtimer.android.components.Entry;
import com.jaanussiim.slimtimer.android.components.Task;
import com.jaanussiim.slimtimer.android.db.SlimtimerDB;

public class EditEntry extends Activity implements OnDateSetListener, OnTimeSetListener {
  private static final int MENU_OPTION_COMPLETE = Menu.FIRST;
  private static final int MENU_OPTION_SAVE = MENU_OPTION_COMPLETE + 1;

  private static final int DIALOG_DATE_ID = 0;
  private static final int DIALOG_START_TIME_ID = 1;
  private static final int DIALOG_END_TIME_ID = 2;
  private static final String LOG_TAG = "EditEntry";

  private SlimtimerDB database;
  private TextView actionLabel;
  private EditText comment;
  private TextView taskDate;
  private TextView taskStartTime;
  private TextView taskEndTime;
  private Entry edited;
  private boolean workingOnStartTime;
  private Spinner tasksSpinner;
  private List<Task> tasks;
  private MultiAutoCompleteTextView tagsView;

  @Override
  protected void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    getWindow().setFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND,
        WindowManager.LayoutParams.FLAG_BLUR_BEHIND);

    setContentView(R.layout.edit_entry);

    database = new SlimtimerDB(this);
    database.open();

    final Object lastEntry = getLastNonConfigurationInstance();
    if (lastEntry == null) {
      edited = loadEntryFromDatabase(getIntent().getExtras());
    } else {
      edited = (Entry) lastEntry;
    }

    if (edited.getEndTime() == null) {
      edited.setEndTime(new Date());
      edited.setIsRunning(true);
    }

    actionLabel = (TextView) findViewById(R.id.edit_entry_action_label);
    if (edited.isRunning()) {
      actionLabel.setText(R.string.edit_entry_action_complete);
    } else {
      actionLabel.setText(R.string.edit_entry_action_edit);
    }

    tasksSpinner = (Spinner) findViewById(R.id.edit_entry_tasks_spinner);
    final List<Task> tasks = database.listUncompletedTasks(TimerCore.getInstance().getUser());
    tasksSpinner.setAdapter(new TasksAdapter(this, tasks));
    tasksSpinner.setSelection(tasks.indexOf(edited.getTask()), true);
    tasksSpinner.setEnabled(!edited.isRunning());

    taskDate = (TextView) findViewById(R.id.edit_entry_task_run_date);
    setEntrykDateView(edited.getStartTime().getTime());
    taskDate.setOnClickListener(new OnClickListener() {
      public void onClick(final View v) {
        showDialog(DIALOG_DATE_ID);
      }
    });

    taskStartTime = (TextView) findViewById(R.id.edit_entry_task_start_time);
    setStartTimeView(edited.getStartTime().getTime());
    taskStartTime.setOnClickListener(new OnClickListener() {
      public void onClick(final View v) {
        showDialog(DIALOG_START_TIME_ID);
      }
    });

    taskEndTime = (TextView) findViewById(R.id.edit_entry_task_end_time);
    setEndTimeView(edited.getEndTime().getTime());
    taskEndTime.setOnClickListener(new OnClickListener() {
      public void onClick(final View v) {
        showDialog(DIALOG_END_TIME_ID);
      }
    });

    tagsView = (MultiAutoCompleteTextView) findViewById(R.id.edit_entry_tags);
    tagsView.setText(edited.getTagsAsJoinedStrings());

    final List<String> tags = database.listTagsAsStrings();

    final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
        android.R.layout.simple_dropdown_item_1line, tags);
    tagsView.setAdapter(adapter);
    tagsView.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());

    comment = (EditText) findViewById(R.id.edit_entry_comment);
    comment.setText(edited.getComment());
  }

  private Entry loadEntryFromDatabase(final Bundle extras) {
    final Long entryId = extras.getLong(Entry.EDIT_INTENT_ID);
    return database.loadEntry(entryId, TimerCore.getInstance().getUser());
  }

  @Override
  public Object onRetainNonConfigurationInstance() {
    return edited;
  }

  @Override
  public boolean onCreateOptionsMenu(final Menu menu) {
    super.onCreateOptionsMenu(menu);
    menu.add(0, MENU_OPTION_COMPLETE, 0, R.string.edit_entry_menu_complete);
    menu.add(0, MENU_OPTION_SAVE, 0, R.string.edit_entry_menu_save);
    return true;
  }

  @Override
  public boolean onPrepareOptionsMenu(final Menu menu) {
    super.onPrepareOptionsMenu(menu);

    menu.findItem(MENU_OPTION_COMPLETE).setVisible(edited.isRunning());

    return true;
  }

  @Override
  public boolean onMenuItemSelected(final int featureId, final MenuItem item) {
    switch (item.getItemId()) {
    case MENU_OPTION_SAVE:
    case MENU_OPTION_COMPLETE:
      if (item.getItemId() == MENU_OPTION_SAVE && edited.isRunning()) {
        edited.setEndTime(null);
      }
      edited.setComment(comment.getText().toString());
      edited.parseTagsFromString(tagsView.getText().toString());
      database.updateEntry(edited);
      setResult(RESULT_OK);
      finish();
      return true;
    default:
      return super.onMenuItemSelected(featureId, item);
    }
  }

  @Override
  protected Dialog onCreateDialog(final int id) {
    switch (id) {
    case DIALOG_DATE_ID:
      final Calendar c = Calendar.getInstance();
      c.setTime(edited.getStartTime());
      return new DatePickerDialog(this, this, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c
          .get(Calendar.DAY_OF_MONTH));
    case DIALOG_START_TIME_ID:
    case DIALOG_END_TIME_ID:
      //TODO jaanus : handle 24h
      workingOnStartTime = id == DIALOG_START_TIME_ID;
      return new TimePickerDialog(this, this, 0, 0, false);
    default:
      return super.onCreateDialog(id);
    }
  }

  @Override
  protected void onPrepareDialog(final int id, final Dialog dialog) {
    switch (id) {
    case DIALOG_DATE_ID:
      final Calendar c = Calendar.getInstance();
      c.setTime(edited.getStartTime());
      ((DatePickerDialog) dialog).updateDate(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c
          .get(Calendar.DAY_OF_MONTH));
      break;
    case DIALOG_START_TIME_ID:
    case DIALOG_END_TIME_ID:
      final Calendar time = Calendar.getInstance();
      time.setTime(workingOnStartTime ? edited.getStartTime() : edited.getEndTime());
      ((TimePickerDialog) dialog).updateTime(time.get(Calendar.HOUR_OF_DAY), time
          .get(Calendar.MINUTE));
      break;
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    database.close();
  }

  public void onDateSet(final DatePicker view, final int year, final int monthOfYear,
      final int dayOfMonth) {
    //    Log.d(LOG_TAG, "Set date " + year + " : " + monthOfYear + " : " + dayOfMonth);
    final Calendar c = Calendar.getInstance();
    c.setTime(edited.getStartTime());
    c.set(Calendar.YEAR, year);
    c.set(Calendar.MONTH, monthOfYear);
    c.set(Calendar.DAY_OF_MONTH, dayOfMonth);

    //TODO jaanus : something also with end time

    edited.setStartTime(c.getTime());
    setEntrykDateView(c.getTimeInMillis());
  }

  private void setEntrykDateView(final long startTime) {
    taskDate.setText(DateUtils.formatDateTime(this, startTime, DateUtils.FORMAT_SHOW_DATE) + " ");
  }

  public void onTimeSet(final TimePicker view, final int hourOfDay, final int minute) {
    //    Log.d(LOG_TAG, "Set time " + hourOfDay + " : " + minute + " : " + workingOnStartTime);
    final Calendar c = Calendar.getInstance();

    if (workingOnStartTime) {
      c.setTime(edited.getStartTime());
    } else {
      c.setTime(edited.getEndTime());
    }

    c.set(Calendar.HOUR_OF_DAY, hourOfDay);
    c.set(Calendar.MINUTE, minute);

    final Date changedTime = c.getTime();
    if (workingOnStartTime) {
      edited.setStartTime(changedTime);
      setStartTimeView(changedTime.getTime());
    } else {
      edited.setEndTime(changedTime);
      setEndTimeView(changedTime.getTime());
    }
  }

  private void setEndTimeView(final Long endTime) {
    taskEndTime.setText(DateUtils.formatDateTime(this, endTime, DateUtils.FORMAT_SHOW_TIME));
  }

  private void setStartTimeView(final long time) {
    taskStartTime.setText(DateUtils.formatDateTime(this, time, DateUtils.FORMAT_SHOW_TIME) + "-");
  }

  class TasksAdapter extends BaseAdapter {
    private final List<Task> tasks;
    private final Activity context;

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

      if (row == null) {
        row = context.getLayoutInflater().inflate(R.layout.edit_entry_tasks_spinner, null);
      }

      ((TextView) row.findViewById(R.id.spinner_task_name)).setText(tasks.get(position).getName());

      return row;
    }
  }
}
