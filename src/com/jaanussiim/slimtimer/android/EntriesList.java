package com.jaanussiim.slimtimer.android;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.format.DateUtils;
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
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;

import com.jaanussiim.slimtimer.android.components.Entry;
import com.jaanussiim.slimtimer.android.components.Tag;
import com.jaanussiim.slimtimer.android.db.SlimtimerDB;
import com.jaanussiim.slimtimer.android.net_old.LoginRequest;
import com.jaanussiim.slimtimer.android.net_old.NetworkRequestCaller;
import com.jaanussiim.slimtimer.android.net_old.NetworkRequestsChain;
import com.jaanussiim.slimtimer.android.net_old.PostTimeReportRequest;
import com.jaanussiim.slimtimer.android.net_old.UploadEntryRequest;

public class EntriesList extends ListActivity implements OnItemSelectedListener, OnClickListener, NetworkRequestCaller {
  private static final int UPLOADING_ENTRIES_DIALOG = 0;

  private static final int MENU_OPTION_UPLOAD_COMPLETED = Menu.FIRST;
  private static final int MENU_OPTION_BACK = MENU_OPTION_UPLOAD_COMPLETED + 1;

  private static final int CONTEXT_MENU_COMPLETE = MENU_OPTION_BACK + 1;
  private static final int CONTEXT_MENU_DELETE = CONTEXT_MENU_COMPLETE + 1;

  private static final DateFormat PLAIN_TIME_FORMAT = new SimpleDateFormat("HH:mm");
  private static final DateFormat DATE_TIME_FORMAT = new SimpleDateFormat("MMM dd, HH:mm");
  private static final String LOG_TAG = "EntriesList";
  private SlimtimerDB database;
  private List<Entry> entries;

  private View lastSelected;
  private int lastSelectedIndex = -1;
  private EntriesAdapter adapter;
  private Entry contextMenuOnEntry;

  @Override
  protected void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    requestWindowFeature(Window.FEATURE_NO_TITLE);

    getListView().setBackgroundResource(R.color.screen_background);

    database = new SlimtimerDB(this);
    database.open();

    entries = database.listEntries(TimerCore.getInstance().getUser());
    //    Log.d(LOG_TAG, "Loaded " + entries.size() + " entries");

    if (entries.size() == 0) {
      Toast.makeText(this, R.string.entries_list_no_entries_found, Toast.LENGTH_LONG).show();
    }

    adapter = new EntriesAdapter(this, entries);
    setListAdapter(adapter);

    getListView().setOnItemSelectedListener(this);
  }

  @Override
  public boolean onPrepareOptionsMenu(final Menu menu) {
    super.onPrepareOptionsMenu(menu);

    final boolean hasCompletedEntries = hasCompleted(entries);
    menu.findItem(MENU_OPTION_UPLOAD_COMPLETED).setVisible(hasCompletedEntries);
    menu.findItem(MENU_OPTION_BACK).setVisible(!hasCompletedEntries);

    return true;
  }

  @Override
  public boolean onCreateOptionsMenu(final Menu menu) {
    super.onCreateOptionsMenu(menu);
    menu.add(0, MENU_OPTION_UPLOAD_COMPLETED, 0, R.string.entries_menu_upload_completed);
    menu.add(0, MENU_OPTION_BACK, 0, R.string.entries_menu_back);
    return true;
  }

  @Override
  public void onCreateContextMenu(final ContextMenu menu, final View v, final ContextMenuInfo menuInfo) {
    final EntryViewWrapper wrapper = (EntryViewWrapper) v.getTag();
    contextMenuOnEntry = entries.get(wrapper.getPosition());

    if (contextMenuOnEntry.isRunning()) {
      menu.add(0, CONTEXT_MENU_COMPLETE, 0, R.string.entries_context_menu_complete);
    } else {
      menu.add(0, CONTEXT_MENU_DELETE, 0, R.string.entries_context_menu_delete);
    }
  }

  @Override
  public boolean onMenuItemSelected(final int featureId, final MenuItem item) {
    switch (item.getItemId()) {
    case MENU_OPTION_BACK:
      finish();
      return true;
    case MENU_OPTION_UPLOAD_COMPLETED:
      showDialog(UPLOADING_ENTRIES_DIALOG);
      runOnUiThread(new Runnable() {
        public void run() {
          final NetworkRequestsChain networkRequests = new NetworkRequestsChain(EntriesList.this);
          if (TimerCore.getInstance().getAccessToken() == null) {
            final SharedPreferences preferences = getSharedPreferences(Constants.PREFERENCES_NAME, Context.MODE_PRIVATE);
            final String username = preferences.getString(Constants.PREFERENCES_EMAIL_KEY, "");
            final String password = preferences.getString(Constants.PREFERENCES_PASSWORD_KEY, "");
            networkRequests.add(new LoginRequest(EntriesList.this, username, password, false));
          }

          for (final Entry entry : entries) {
            if (entry.isRunning()) {
              continue;
            }

            networkRequests.add(new UploadEntryRequest(entry, database));
          }

          networkRequests.add(new PostTimeReportRequest(getApplicationContext(), database, getSharedPreferences(Constants.PREFERENCES_NAME,
              Context.MODE_PRIVATE)));

          networkRequests.execute();
        }
      });
      return true;
    case CONTEXT_MENU_COMPLETE:
      database.markCompleted(contextMenuOnEntry.getTask(), TimerCore.getInstance().getUser());
      reloadEntries();
      return true;
    case CONTEXT_MENU_DELETE:
      database.deleteEntry(contextMenuOnEntry);
      reloadEntries();
      return true;
    default:
      return super.onMenuItemSelected(featureId, item);
    }
  }

  @Override
  protected Dialog onCreateDialog(final int id) {
    switch (id) {
    case UPLOADING_ENTRIES_DIALOG:
      final ProgressDialog dialog = new ProgressDialog(this);
      dialog.setMessage(getText(R.string.uploading_entries_message));
      dialog.setIndeterminate(true);
      dialog.setCancelable(true);
      return dialog;
    default:
      return super.onCreateDialog(id);
    }
  }

  //OnItemSelectedListener
  public void onItemSelected(final AdapterView<?> parentView, final View childView, final int position, final long id) {
    unselectPreviousElement();

    final EntryViewWrapper currentWrapper = (EntryViewWrapper) childView.getTag();
    final Entry currentEntry = entries.get(position);
    currentWrapper.makeSelected();

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
      final EntryViewWrapper wrapper = (EntryViewWrapper) lastSelected.getTag();
      final Entry lastEntry = entries.get(lastSelectedIndex);
      wrapper.makeUnselected(lastEntry.isRunning());
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    database.close();
  }

  private boolean hasCompleted(final List<Entry> entries) {
    for (final Entry e : entries) {
      if (!e.isRunning()) {
        return true;
      }
    }

    return false;
  }

  private void reloadEntries() {
    entries = database.listEntries(TimerCore.getInstance().getUser());
    adapter.setEntries(entries);
  }

  private class EntriesAdapter extends BaseAdapter {
    private final Activity context;
    private List<Entry> entries;

    public EntriesAdapter(final Activity context, final List<Entry> entries) {
      this.context = context;
      this.entries = entries;
    }

    public void setEntries(final List<Entry> entries) {
      this.entries = entries;
      context.runOnUiThread(new Runnable() {
        public void run() {
          notifyDataSetChanged();
        }
      });
    }

    public int getCount() {
      return entries.size();
    }

    public Object getItem(final int position) {
      return entries.get(position);
    }

    public long getItemId(final int position) {
      return position;
    }

    public View getView(final int position, final View convertView, final ViewGroup parent) {
      View row = convertView;
      EntryViewWrapper wrapper = null;

      if (row == null) {
        row = context.getLayoutInflater().inflate(R.layout.entry_list_element, null);
        wrapper = new EntryViewWrapper(row, EntriesList.this);
        row.setTag(wrapper);
      } else {
        wrapper = (EntryViewWrapper) row.getTag();
      }

      wrapper.setPosition(position);
      final Entry entry = entries.get(position);

      wrapper.setTaskName(entry.getTaskName());

      //TODO jaanus : replace with DateUtils.formatDateRange
      String startTimeString = null;
      final Date startTime = entry.getStartTime();
      if (dateIsToday(startTime)) {
        startTimeString = PLAIN_TIME_FORMAT.format(startTime);
      } else {
        startTimeString = DATE_TIME_FORMAT.format(startTime);
      }
      //spaces for HH:mm
      String endTimeString = "     ";
      if (!entry.isRunning()) {
        endTimeString = PLAIN_TIME_FORMAT.format(entry.getEndTime());
      }

      final String runningTime = DateUtils.formatElapsedTime(entry.getRunTime() / 1000);

      final String runTimeDisplayString = MessageFormat.format("{0}-{1} {2}", startTimeString, endTimeString, runningTime);

      wrapper.setEntryTimeText(runTimeDisplayString);
      wrapper.setComment(entry.getComment());

      final String tagsString = createTagsString(entry.getTags());

      wrapper.setTagsString(tagsString);

      if (entry.isRunning()) {
        wrapper.setBackgroundResource(R.color.entry_list_unfinished_background);
      } else {
        wrapper.setBackgroundResource(R.color.entry_list_default_background);
      }

      row.setOnClickListener(EntriesList.this);
      EntriesList.this.registerForContextMenu(row);

      return row;
    }

    private String createTagsString(final Tag[] tags) {
      final StringBuffer buff = new StringBuffer();

      for (int i = 0; i < tags.length; i++) {
        buff.append(tags[i].getName());
        if (i != tags.length - 1) {
          buff.append(",");
        }
      }

      return buff.toString();
    }

    private boolean dateIsToday(final Date startTime) {
      final Calendar c = Calendar.getInstance();
      c.set(Calendar.HOUR_OF_DAY, 0);
      c.set(Calendar.MINUTE, 0);
      c.set(Calendar.SECOND, 0);

      final Date midnight = c.getTime();

      return startTime.compareTo(midnight) > 0;
    }
  }

  public void onClick(final View v) {
    handleViewClick(v);
  }

  @Override
  protected void onListItemClick(final ListView l, final View v, final int position, final long id) {
    handleViewClick(v);
  }

  @Override
  protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
    if (requestCode == SlimtimerTasksList.ACTIVITY_REQUEST_EDIT_ENTRY && resultCode == RESULT_OK) {
      reloadEntries();
    }
  }

  private void handleViewClick(final View view) {
    final EntryViewWrapper wrapper = (EntryViewWrapper) view.getTag();
    final int position = wrapper.getPosition();
    final Entry entry = entries.get(position);

    final Intent startEdit = new Intent(this, EditEntry.class);
    startEdit.putExtra(Entry.EDIT_INTENT_ID, entry.getDatabaseId());
    startActivityForResult(startEdit, SlimtimerTasksList.ACTIVITY_REQUEST_EDIT_ENTRY);
  }

  public void executionComplete() {
    removeDialog(UPLOADING_ENTRIES_DIALOG);
    runOnUiThread(new Runnable() {
      public void run() {
        Toast.makeText(EntriesList.this, R.string.entries_list_upload_complete, Toast.LENGTH_SHORT).show();
        reloadEntries();
      }
    });
  }

  public void requestError(final int errorCode) {
    removeDialog(UPLOADING_ENTRIES_DIALOG);
    runOnUiThread(new Runnable() {
      public void run() {
        Toast.makeText(EntriesList.this, R.string.entries_upload_error, Toast.LENGTH_LONG).show();
        reloadEntries();
      }
    });
  }

  public void requestSuccess(final int code) {
    if (code != NetworkRequestCaller.LOGIN_REQUEST) {
      executionComplete();
    }
  }
}
