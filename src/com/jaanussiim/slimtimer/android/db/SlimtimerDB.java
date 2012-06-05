package com.jaanussiim.slimtimer.android.db;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.jaanussiim.slimtimer.android.components.Entry;
import com.jaanussiim.slimtimer.android.components.Tag;
import com.jaanussiim.slimtimer.android.components.Task;
import com.jaanussiim.slimtimer.android.components.User;
import com.jaanussiim.slimtimer.android.utils.Encrypter;

public class SlimtimerDB {
  public static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
  //NEVER CHANGE VALUE OF THIS T
  public static final String T = "SlimtimerDB";
  private static final String DATABASE_NAME = "m_timer_database";
  private static final int DATABASE_VERSION = 3;

  private static final String USERS_TABLE = "users";
  private static final String TASKS_TABLE = "tasks";
  private static final String ENTRIES_TABLE = "entries";
  private static final String TAGS_TABLE = "tags";
  private static final String ENTRIES_TAGS_TABLE = "entries_tags";
  private static final String TIME_REPORTS_TABLE = "time_reports";
  private static final String SETTINGS_TABLE = "settings";

  private static final String KEY_EMAIL = "email";
  private static final String KEY_PASSWORD = "password";
  private static final String KEY_ACCESS_TOKEN = "access_token";
  private static final String KEY_SLIM_USER_ID = "slim_user_id";
  private static final String KEY_TASK_ID = "task_id";
  private static final String KEY_NAME = "name";
  private static final String KEY_USER_ID = "user_id";
  private static final String KEY_ID = "id";
  private static final String KEY_START_TIME = "start_time";
  private static final String KEY_END_TIME = "end_time";
  private static final String KEY_COMMENT = "comment";
  private static final String KEY_ENTRY_ID = "entry_id";
  private static final String KEY_TAG_ID = "tag_id";
  private static final String KEY_COMPLETED = "completed";
  private static final String KEY_SYNCHRONIZED = "in_sync";
  private static final String KEY_DELETED = "deleted";

  public static final int SETTING_KEY_REPORTING_CODE = 1;

  private final Context ctx;
  private DatabaseHelper databaseHelper;
  private SQLiteDatabase database;

  private static class DatabaseHelper extends SQLiteOpenHelper {
    private final Context ctx;

    DatabaseHelper(final Context context) {
      super(context, DATABASE_NAME, null, DATABASE_VERSION);
      ctx = context;
    }

    @Override
    public void onCreate(final SQLiteDatabase db) {
      Log.d(T, "onCreate()");
      onUpgrade(db, 0, DATABASE_VERSION);
    }

    @Override
    public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
      for (int i = oldVersion + 1; i <= newVersion; i++) {
        executeScript(db, String.format("db/%03d.sql", i));
      }
    }

    private void executeScript(final SQLiteDatabase db, final String filename) {
      Log.d(T, "Executing SQL script " + filename);
      try {
        final InputStream is = ctx.getAssets().open(filename);
        final ByteArrayOutputStream os = new ByteArrayOutputStream(512);

        int chr;
        while ((chr = is.read()) != -1) {
          if (((char) chr) == ';') {
            db.execSQL(os.toString("utf-8"));
            os.reset();
          } else {
            os.write(chr);
          }
        }
      } catch (final IOException e) {
        Log.d(T, "SQL execute failed with IOE: " + e);
      }
    }
  }

  public SlimtimerDB(final Context ctx) {
    this.ctx = ctx;
  }

  //TODO jaanus : check this open/close handling
  public void open() {
    databaseHelper = new DatabaseHelper(ctx);
    database = databaseHelper.getWritableDatabase();
  }

  public void close() {
    databaseHelper.close();
  }

  protected void insertRawUser(final Long remoteId, final String encryptedUsername, final String encryptedPassword) {
    final ContentValues values = new ContentValues();
    values.put(KEY_SLIM_USER_ID, remoteId);
    values.put(KEY_EMAIL, encryptedUsername);
    values.put(KEY_PASSWORD, encryptedPassword);
    database.insert(USERS_TABLE, null, values);
  }

  public User getLoggedInUser() {
    //TODO jaanus: cache logged in user
    return findUserWithCondition("password != ''", null);
  }

  private User mapToUser(final Cursor c) {
    if (c == null) {
      return null;
    }

    if (!c.moveToFirst()) {
      return null;
    }

    final Long id = getLong(c, KEY_ID);
    final String email = getDecryptedString(c, KEY_EMAIL);
    final Long slimId = getLong(c, KEY_SLIM_USER_ID);
    final String password = getDecryptedString(c, KEY_PASSWORD);
    final String accessToken = getDecryptedString(c, KEY_ACCESS_TOKEN);
    c.close();

    return new User(id, email, slimId, password, accessToken, false);
  }

  private String getDecryptedString(final Cursor c, final String columnName) {
    final int index = c.getColumnIndex(columnName);

    if (index == -1) {
      return null;
    }

    return Encrypter.decrypt(c.getString(index));
  }

  private Long getLong(final Cursor c, final String columnName) {
    final int index = c.getColumnIndex(columnName);

    if (index == -1) {
      return null;
    }

    return c.getLong(index);
  }

  public SQLiteDatabase testGetDatabase() {
    return database;
  }

  protected void insertUser(final String email, final Long slimId, final String password, final String accessToken) {
    final User u = insertUser(slimId, email);
    final HashMap<String, Object> values = new HashMap<String, Object>();
    values.put(KEY_EMAIL, email);
    values.put(KEY_PASSWORD, password);
    values.put(KEY_ACCESS_TOKEN, accessToken);
    updateUserWithValues(u, values);
  }

  private User insertUser(final Long remoteid, final String username) {
    final ContentValues values = new ContentValues();
    values.put(KEY_SLIM_USER_ID, remoteid);
    values.put(KEY_EMAIL, Encrypter.encrypt(username));

    database.insert(USERS_TABLE, null, values);

    return findUserByRemoteId(remoteid);
  }

  public void saveReportingCode(final String code) {
    saveSetting(SETTING_KEY_REPORTING_CODE, code);
  }

  public String getReportingCode() {
    return valueForKey(SETTING_KEY_REPORTING_CODE);
  }

  private String valueForKey(final int settingKeyReportingCode) {
    final String[] userSelectKeys = new String[] { "value" };
    final Cursor c = database.query(SETTINGS_TABLE, userSelectKeys, "key = ?", new String[] { Integer.toString(settingKeyReportingCode) },
        null, null, null);
    String value = "";

    if (c.moveToFirst()) {
      value = c.getString(0);
    }

    c.close();

    return Encrypter.decrypt(value);
  }

  private void saveSetting(final int code, final String value) {
    final ContentValues values = new ContentValues();
    values.put("key", new Integer(code));
    values.put("value", Encrypter.encrypt(value));
    database.insert(SETTINGS_TABLE, null, values);
  }

  public User findOrCreateUser(final Long userId, final String username) {
    User u = findUserByRemoteId(userId);
    if (u == null) {
      insertUser(userId, username);
      u = findUserByRemoteId(userId);
    }

    //TODO jaanus: improve this
    if (!u.getEmail().equals(userId)) {
      updateUserEmail(u, username);
      u = findUserByRemoteId(userId);
    }

    return u;
  }

  private void updateUserEmail(final User u, final String username) {
    final HashMap<String, Object> values = new HashMap<String, Object>();
    values.put(KEY_EMAIL, username);
    updateUserWithValues(u, values);
  }

  public void setUserLoggedIn(final User u, final String password, final String accessToken) {
    final HashMap<String, Object> values = new HashMap<String, Object>();
    values.put(KEY_PASSWORD, password);
    values.put(KEY_ACCESS_TOKEN, accessToken);
    updateUserWithValues(u, values);
  }

  private void updateUserWithValues(final User u, final HashMap<String, Object> values) {
    final Iterator<String> iterator = values.keySet().iterator();
    final ContentValues cValues = new ContentValues();
    while (iterator.hasNext()) {
      final String key = iterator.next();
      Object value = values.get(key);
      if (KEY_EMAIL.equals(key) || KEY_PASSWORD.equals(key) || KEY_ACCESS_TOKEN.equals(key)) {
        value = Encrypter.encrypt((String) value);
      }

      if (value instanceof String) {
        cValues.put(key, (String) value);
      } else {
        throw new RuntimeException("add cast for " + key);
      }
    }

    database.update(USERS_TABLE, cValues, "slim_user_id = ?", new String[] { Long.toString(u.getSlimId()) });
  }

  private User findUserByRemoteId(final Long userId) {
    return findUserWithCondition("slim_user_id = ?", new String[] { Long.toString(userId) });
  }

  private User findUserWithCondition(final String condition, final String[] params) {
    final String[] userSelectKeys = new String[] { KEY_ID, KEY_EMAIL, KEY_PASSWORD, KEY_SLIM_USER_ID, KEY_ACCESS_TOKEN };
    final Cursor c = database.query(USERS_TABLE, userSelectKeys, condition, params, null, null, null);
    final User user = mapToUser(c);
    c.close();

    return user;
  }

  //################### old methods to be tested
  public List<Task> listUncompletedTasks(final User user) {
    return listTasksQuery(KEY_USER_ID + " = ? AND completed = 0", new String[] { Long.toString(user.getDatabaseId()) });
  }

  public List<Task> listAllTasks(final User user) {
    return listTasksQuery(KEY_USER_ID + " = ?", new String[] { Long.toString(user.getDatabaseId()) });
  }

  protected List<Task> listAllTasks() {
    return listTasksQuery(null, null);
  }

  public List<Task> listTasksRequiringUpdate(final User user) {
    return listTasksQuery("user_id = ? AND in_sync = 0", new String[] { Long.toString(user.getDatabaseId()) });
  }

  private List<Task> listTasksQuery(final String selection, final String[] selectionArgs) {
    final List<Task> tasks = new ArrayList<Task>();

    final String[] queryKeys = new String[] { KEY_ID, KEY_TASK_ID, KEY_NAME, KEY_COMPLETED, KEY_SYNCHRONIZED, KEY_DELETED };
    final Cursor c = database.query(TASKS_TABLE, queryKeys, selection, selectionArgs, null, null, null);

    while (c.moveToNext()) {
      final long id = c.getLong(0);
      final Long taskId = c.getLong(1);
      final String name = c.getString(2);
      final boolean completed = c.getInt(3) == 1;
      final boolean inSync = c.getInt(4) == 1;
      final boolean deleted = c.getInt(5) == 1;
      final long startTime = getStartTime(id);
      final Task loaded = new Task(id, taskId, name, completed, inSync, deleted, startTime > 1000, startTime);
      tasks.add(loaded);
    }
    c.close();

    Collections.sort(tasks);
    return tasks;
  }

  private long getStartTime(final long id) {
    final Cursor c = database.query(ENTRIES_TABLE, new String[] { KEY_START_TIME }, "task_id = ? AND (end_time IS NULL OR end_time = '')",
        new String[] { Long.toString(id) }, null, null, null);
    long startTime = 0L;

    if (c.moveToFirst()) {
      try {
        final Date d = DATE_FORMAT.parse(c.getString(0));
        startTime = d.getTime();
      } catch (final ParseException ignore) {
        //        Log.e(LOG_TAG, "error parsing start time", ignore);
      }
    }

    c.close();

    return startTime;
  }

  public void updateTasks(final List<Task> tasks, final User user) {
    final List<Task> previousTasks = listAllTasks(user);
    for (final Task task : tasks) {
      updateOrCreateBySlimId(task, user);
      previousTasks.remove(task);
    }

    for (final Task deleted : previousTasks) {
      //      Log.d(LOG_TAG, "Deleting " + deleted);
      deleteTask(deleted);
    }
  }

  private void updateOrCreateBySlimId(final Task task, final User user) {
    final Cursor c = database.query(TASKS_TABLE, new String[] { KEY_ID }, "task_id = ? AND user_id = ?", new String[] {
        Long.toString(task.getSlimId()), Long.toString(user.getDatabaseId()) }, null, null, null);
    if (c.moveToFirst()) {
      updateTask(task, user);
    } else {
      insertTask(task, user);
    }
    c.close();
  }

  public void updateOrCreateTaskByDbId(final Task saved, final User user) {
    final Cursor c = database.query(TASKS_TABLE, new String[] { KEY_ID }, "id = ?", new String[] { Long.toString(saved.getDatabaseId()) },
        null, null, null);
    //    Log.d(LOG_TAG, "updateOrCreateTask " + saved.toString());
    if (c.moveToFirst()) {
      //      Log.d(LOG_TAG, "update previous");
      updateTask(saved, user);
    } else {
      //      Log.d(LOG_TAG, "create new");
      insertTask(saved, user);
    }
    c.close();
  }

  private void updateTask(final Task saved, final User user) {
    final ContentValues values = new ContentValues();
    values.put(KEY_NAME, saved.getName());
    values.put(KEY_TASK_ID, saved.getSlimId());
    values.put(KEY_COMPLETED, saved.isCompleted() ? 1 : 0);
    values.put(KEY_SYNCHRONIZED, saved.isInSyncWithServer() ? 1 : 0);
    values.put(KEY_DELETED, saved.isDeleted() ? 1 : 0);
    database.update(TASKS_TABLE, values, "id = ? AND user_id = ?", new String[] { Long.toString(saved.getDatabaseId()),
        Long.toString(user.getDatabaseId()) });
  }

  private void insertTask(final Task saved, final User user) {
    final ContentValues values = new ContentValues();
    values.put(KEY_TASK_ID, saved.getSlimId());
    values.put(KEY_NAME, saved.getName());
    values.put(KEY_USER_ID, user.getDatabaseId());
    values.put(KEY_COMPLETED, saved.isCompleted() ? 1 : 0);
    values.put(KEY_SYNCHRONIZED, saved.isInSyncWithServer() ? 1 : 0);
    values.put(KEY_DELETED, saved.isDeleted() ? 1 : 0);
    database.insert(TASKS_TABLE, null, values);
  }

  public User findOrCreateUser(final String email, final Long userId) {
    //    debugDumpAllUsers();
    final User result = findUserByEmailAndSlimId(email, userId);

    if (result != null) {
      return result;
    }

    final long id = insertUser(email, userId);
    if (id > 0) {
      return new User(id, userId, email, true);
    }

    return null;
  }

  private long insertUser(final String email, final Long userId) {
    final ContentValues values = new ContentValues();
    values.put(KEY_EMAIL, email);
    values.put(KEY_SLIM_USER_ID, userId);
    return database.insert(USERS_TABLE, null, values);
  }

  private User findUserByEmailAndSlimId(final String email, final Long userId) {
    final String[] userSelectKeys = new String[] { KEY_ID, KEY_EMAIL, KEY_SLIM_USER_ID };
    final Cursor c = database.query(USERS_TABLE, userSelectKeys, "email = ? and slim_user_id = ?",
        new String[] { email, userId.toString() }, null, null, null);
    final User user = mapToUserOld(c);
    c.close();

    return user;
  }

  private User mapToUserOld(final Cursor c) {
    if (c == null) {
      return null;
    }

    if (!c.moveToFirst()) {
      return null;
    }

    final long id = c.getLong(0);
    final String email = c.getString(1);
    final Long slimId = c.getLong(2);

    return new User(id, slimId, email, false);
  }

  public void markRunning(final Task task, final User user) {
    markRunning(task, user, new Date());
  }

  protected void markRunning(final Task task, final User user, final Date startTime) {
    final ContentValues values = new ContentValues();
    values.put(KEY_TASK_ID, Long.toString(task.getDatabaseId()));
    values.put(KEY_USER_ID, Long.toString(user.getDatabaseId()));
    values.put(KEY_START_TIME, DATE_FORMAT.format(startTime));
    database.insert(ENTRIES_TABLE, null, values);
  }

  public Entry markCompletedAndReturnEntry(final Task task, final User user) {
    markCompleted(task, user);
    return lastReportForTask(task, user);
  }

  public void markCompleted(final Task task, final User user) {
    final Date endTime = new Date();
    final ContentValues values = new ContentValues();
    values.put(KEY_END_TIME, DATE_FORMAT.format(endTime));
    final int affectedRows = database.update(ENTRIES_TABLE, values, "task_id = ? AND user_id = ? AND (end_time IS NULL OR end_time = '')",
        new String[] { Long.toString(task.getDatabaseId()), Long.toString(user.getDatabaseId()) });
    //    Log.d(LOG_TAG, "completed " + affectedRows + " entries");
  }

  public long getReportId(final Task task, final User user) {
    final Cursor c = database.query(ENTRIES_TABLE, new String[] { KEY_ID },
        "task_id = ? AND user_id = ? AND (end_time IS NULL OR end_time = '')", new String[] { Long.toString(task.getDatabaseId()),
            Long.toString(user.getDatabaseId()) }, null, null, null);

    long result = -1;

    if (c.moveToFirst()) {
      result = c.getLong(0);
    }

    c.close();

    return result;
  }

  private Entry lastReportForTask(final Task task, final User user) {
    final Cursor c = database.query(ENTRIES_TABLE, new String[] { KEY_ID, KEY_COMMENT },
        "user_id = ? AND task_id = ? AND end_time IS NOT NULL", new String[] { Long.toString(user.getDatabaseId()),
            Long.toString(task.getDatabaseId()) }, null, null, "end_time DESC");

    Entry entry = null;
    if (c.moveToFirst()) {
      final long id = c.getLong(0);
      final String comment = c.getString(1);
      entry = new Entry(id, user, task, comment, new Tag[0]);
    }
    c.close();

    return entry;
  }

  public Task loadTask(final long databaseId) {
    //    Log.d("LLLLLLLLL", "id = " + databaseId);
    return listTasksQuery("id = ?", new String[] { Long.toString(databaseId) }).get(0);
  }

  public List<Entry> listEntries(final User user) {
    //    debugDumpAllEntries();
    //    Log.d(LOG_TAG, "Select where user_id=" + Long.toString(user.getDatabaseId()));
    final List<Entry> result = new ArrayList<Entry>();
    final Cursor c = database.query(ENTRIES_TABLE, new String[] { KEY_ID, KEY_TASK_ID, KEY_COMMENT, KEY_START_TIME, KEY_END_TIME },
        "user_id = ?", new String[] { Long.toString(user.getDatabaseId()) }, null, null, "start_time ASC");
    while (c.moveToNext()) {
      final Entry entry = mapEntry(c, user);
      if (entry != null) {
        result.add(entry);
      }
    }
    c.close();
    return result;
  }

  public Entry loadEntry(final Long entryId, final User user) {
    //    Log.d(LOG_TAG, "Load entry " + entryId);
    final Cursor c = database.query(ENTRIES_TABLE, new String[] { KEY_ID, KEY_TASK_ID, KEY_COMMENT, KEY_START_TIME, KEY_END_TIME },
        "id = ?", new String[] { entryId.toString() }, null, null, null);
    Entry result = null;

    if (c.moveToFirst()) {
      result = mapEntry(c, user);
    }
    c.close();

    return result;
  }

  private Entry mapEntry(final Cursor c, final User user) {
    try {
      final long id = c.getLong(0);
      final Task task = loadTask(c.getLong(1));
      final String comment = c.getString(2);
      final Date startTime = DATE_FORMAT.parse(c.getString(3));
      Date endTime = null;
      final String endString = c.getString(4);
      if (endString != null && !"".equals(endString)) {
        endTime = DATE_FORMAT.parse(endString);
      }

      return new Entry(id, user, task, comment, loadTagsForEntry(id), startTime, endTime);
    } catch (final ParseException e) {
      //      Log.e(LOG_TAG, "error loading entries", e);
      return null;
    }
  }

  private Tag[] loadTagsForEntry(final long entryId) {
    final Cursor c = database.query(ENTRIES_TAGS_TABLE, new String[] { KEY_TAG_ID }, KEY_ENTRY_ID + " = ?", new String[] { Long
        .toString(entryId) }, null, null, null);

    final List<Tag> tags = new ArrayList<Tag>();

    while (c.moveToNext()) {
      tags.add(loadTagForId(c.getLong(0)));
    }

    c.close();
    return tags.toArray(new Tag[tags.size()]);
  }

  private Tag loadTagForId(final long tagId) {
    final Cursor c = database.query(TAGS_TABLE, new String[] { KEY_NAME }, KEY_ID + " = ?", new String[] { Long.toString(tagId) }, null,
        null, null);
    Tag result = null;
    if (c.moveToNext()) {
      final String tagName = c.getString(0);
      result = new Tag(tagId, tagName);
    }
    c.close();
    return result;
  }

  public User loadUser(final String username) {
    final Cursor c = database.query(USERS_TABLE, new String[] { KEY_ID, KEY_SLIM_USER_ID, KEY_EMAIL }, "email = ?",
        new String[] { username }, null, null, null);
    User user = null;
    if (c.moveToFirst()) {
      final long id = c.getLong(0);
      final Long slimId = c.getLong(1);
      user = new User(id, slimId, username, false);
    } else {
      //      Log.e(LOG_TAG, "error loading " + username);
    }
    c.close();
    return user;
  }

  //  private void debugDumpAllEntries() {
  //    final Cursor c = database.query(ENTRIES_TABLE, new String[] { KEY_ID, KEY_USER_ID, KEY_TASK_ID,
  //        KEY_COMMENT, KEY_START_TIME, KEY_END_TIME }, null, null, null, null, null);
  //    while (c.moveToNext()) {
  //      final long id = c.getLong(0);
  //      final long userId = c.getLong(1);
  //      final long taskId = c.getLong(2);
  //      final String comment = c.getString(3);
  //      final String startTime = c.getString(4);
  //      final String endTime = c.getString(5);
  //      //      Log.d(LOG_TAG, MessageFormat.format("Entry {0} - {1} - {2} - {3} - {4} - {5}", id, userId,
  //      //          taskId, comment, startTime, endTime));
  //    }
  //    c.close();
  //  }

  //  private void debugDumpAllUsers() {
  //    final Cursor c = database.query(USERS_TABLE,
  //        new String[] { KEY_ID, KEY_EMAIL, KEY_SLIM_USER_ID }, null, null, null, null, null);
  //    while (c.moveToNext()) {
  //      final long id = c.getLong(0);
  //      final String email = c.getString(1);
  //      final Long slimId = c.getLong(2);
  //      //      Log.d(LOG_TAG, MessageFormat.format("User {0} - {1} - {2}", id, email, slimId));
  //    }
  //    c.close();
  //  }

  public List<String> listTagsAsStrings() {
    final Cursor c = database.query(TAGS_TABLE, new String[] { KEY_NAME }, null, null, null, null, null);
    final List<String> result = new ArrayList<String>();
    while (c.moveToNext()) {
      result.add(c.getString(0));
    }
    c.close();
    Collections.sort(result);
    return result;
  }

  public void updateEntry(final Entry edited) {
    if (edited.getDatabaseId() == -1) {
      //      Log.e(LOG_TAG, "Can't update entry! " + edited);
      return;
    }

    final ContentValues values = new ContentValues();
    values.put(KEY_COMMENT, edited.getComment());
    values.put(KEY_START_TIME, DATE_FORMAT.format(edited.getStartTime()));
    if (edited.getEndTime() != null) {
      values.put(KEY_END_TIME, DATE_FORMAT.format(edited.getEndTime()));
    } else {
      values.put(KEY_END_TIME, "");
    }

    final int affectedRows = database
        .update(ENTRIES_TABLE, values, KEY_ID + " = ?", new String[] { Long.toString(edited.getDatabaseId()) });
    //    Log.d(LOG_TAG, "Updated " + affectedRows + " entry");

    updateEntryTags(edited.getDatabaseId(), edited.getTags());
  }

  private void updateEntryTags(final Long entryId, final Tag[] tags) {
    List<Tag> databseTags = listTags();
    final List<Tag> missingTags = filterMissingTags(databseTags, tags);

    if (missingTags.size() > 0) {
      insertTags(missingTags);
      databseTags = listTags();
    }

    final List<Tag> taskTagsFromDatabase = mapToDatabaseTags(databseTags, tags);

    database.beginTransaction();
    database.delete(ENTRIES_TAGS_TABLE, "entry_id = ?", new String[] { Long.toString(entryId) });
    database.setTransactionSuccessful();
    database.endTransaction();

    for (final Tag tag : taskTagsFromDatabase) {
      final ContentValues values = new ContentValues();
      values.put(KEY_ENTRY_ID, entryId);
      values.put(KEY_TAG_ID, tag.getDatabaseId());
      database.insert(ENTRIES_TAGS_TABLE, null, values);
    }
  }

  private List<Tag> mapToDatabaseTags(final List<Tag> databaseTags, final Tag[] tags) {
    final List<Tag> result = new ArrayList<Tag>(tags.length);

    for (final Tag tag : tags) {
      final Tag dbTag = databaseTags.get(databaseTags.indexOf(tag));
      result.add(dbTag);
    }

    return result;
  }

  private void insertTags(final List<Tag> missingTags) {
    for (final Tag tag : missingTags) {
      insertTag(tag);
    }
  }

  private void insertTag(final Tag tag) {
    final ContentValues values = new ContentValues();
    values.put(KEY_NAME, tag.getName());
    database.insert(TAGS_TABLE, null, values);
  }

  private List<Tag> filterMissingTags(final List<Tag> databseTags, final Tag[] tags) {
    final List<Tag> missing = new ArrayList<Tag>();

    for (final Tag tag : tags) {
      if (!databseTags.contains(tag)) {
        missing.add(tag);
      }
    }

    return missing;
  }

  protected List<Tag> listTags() {
    final Cursor c = database.query(TAGS_TABLE, new String[] { KEY_ID, KEY_NAME }, null, null, null, null, null);
    final List<Tag> result = new ArrayList<Tag>();

    while (c.moveToNext()) {
      final Long id = c.getLong(0);
      final String name = c.getString(1);
      result.add(new Tag(id, name));
    }
    c.close();

    return result;
  }

  public void cancelTaskRun(final long taskId, final Long userId) {
    //    Log.d(LOG_TAG, "delete entry for " + taskId + " user : " + userId);
    database.beginTransaction();
    final int deletedRows = database.delete(ENTRIES_TABLE, "user_id = ? AND task_id = ? AND (end_time IS NULL OR end_time = '')",
        new String[] { Long.toString(userId), Long.toString(taskId) });
    database.setTransactionSuccessful();
    database.endTransaction();
    //    Log.d(LOG_TAG, "deleted " + deletedRows + " entries");
  }

  public void deleteEntry(final Entry deletedEntry) {
    database.beginTransaction();
    final int affectedRows = database.delete(ENTRIES_TABLE, "id = ?", new String[] { Long.toString(deletedEntry.getDatabaseId()) });
    database.setTransactionSuccessful();
    database.endTransaction();
    //    Log.d(LOG_TAG, "deleted " + affectedRows + " entries");
  }

  public void insertOrUpdateEntry(final Entry entry, final User user) {
    if (entry.getDatabaseId() != -1) {
      updateEntry(entry);
    } else {
      insertEntry(entry, user);
    }
  }

  private void insertEntry(final Entry entry, final User user) {
    final ContentValues values = new ContentValues();
    values.put(KEY_USER_ID, Long.toString(user.getDatabaseId()));
    values.put(KEY_TASK_ID, Long.toString(entry.getTask().getDatabaseId()));
    values.put(KEY_COMMENT, entry.getComment());
    values.put(KEY_START_TIME, DATE_FORMAT.format(entry.getStartTime()));
    values.put(KEY_END_TIME, entry.getEndTime() == null ? "" : DATE_FORMAT.format(entry.getEndTime()));
    final long entryId = database.insert(ENTRIES_TABLE, null, values);
    updateEntryTags(entryId, entry.getTags());
  }

  public void addTimeReport(final long seconds) {
    final ContentValues values = new ContentValues();
    values.put("seconds", new Long(seconds));
    database.insert(TIME_REPORTS_TABLE, null, values);
  }

  public int timeReportsSum() {
    final String sql = "SELECT sum(seconds) FROM time_reports";
    final Cursor c = database.rawQuery(sql, null);
    // move to the first row of the cursor
    c.moveToFirst();
    final int total = c.getInt(0);
    c.close();

    return total;
  }

  public void deleteTimeReports() {
    final int affectedRows = database.delete(TIME_REPORTS_TABLE, null, null);
    Log.d(T, "Deleted " + affectedRows + "time reports");
  }

  public void deleteTask(final Task task) {
    database.beginTransaction();
    database.delete(ENTRIES_TABLE, "task_id = ?", new String[] { Long.toString(task.getDatabaseId()) });
    database.delete(TASKS_TABLE, "id = ?", new String[] { Long.toString(task.getDatabaseId()) });
    database.setTransactionSuccessful();
    database.endTransaction();
  }
}
