package com.jaanussiim.slimtimer.android;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import com.jaanussiim.slimtimer.android.components.Task;
import com.jaanussiim.slimtimer.android.db.SlimtimerDB;

public class EditTask extends Activity {
  private SlimtimerDB database;
  private Task edited;
  private EditText name;
  private CheckBox completed;

  @Override
  protected void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    getWindow().setFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND,
        WindowManager.LayoutParams.FLAG_BLUR_BEHIND);

    setContentView(R.layout.edit_task);

    database = new SlimtimerDB(this);
    database.open();

    final Object lastEntry = getLastNonConfigurationInstance();
    if (lastEntry == null) {
      final Long entryId = getIntent().getExtras().getLong(Task.EDIT_TASK_ID);
      edited = entryId.equals(-1L) ? new Task(-1L, "", false) : database.loadTask(entryId);
    } else {
      edited = (Task) lastEntry;
    }

    name = (EditText) findViewById(R.id.edit_task_name);
    name.setText(edited.getName());

    completed = (CheckBox) findViewById(R.id.edit_task_completed_checkbox);
    completed.setChecked(edited.isCompleted());

    final Button cancel = (Button) findViewById(R.id.edit_task_button_cancel);
    cancel.setOnClickListener(new OnClickListener() {
      public void onClick(final View v) {
        setResult(RESULT_CANCELED);
        finish();
      }
    });

    final Button save = (Button) findViewById(R.id.edit_task_button_save);
    save.setOnClickListener(new OnClickListener() {
      public void onClick(final View v) {
        final String taskName = name.getText().toString();
        if ("".equals(taskName)) {
          return;
        }

        if (taskName.equals(edited.getName()) && completed.isChecked() == edited.isCompleted()) {
          return;
        }

        edited.setName(taskName);
        edited.setCompleted(completed.isChecked());

        database.updateOrCreateTaskByDbId(edited, TimerCore.getInstance().getUser());
        setResult(RESULT_OK);
        finish();
      }
    });
  }

  @Override
  public Object onRetainNonConfigurationInstance() {
    return edited;
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    database.close();
  }
}
