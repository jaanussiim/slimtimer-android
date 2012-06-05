package com.jaanussiim.slimtimer.android.net_old;

public interface NetworkRequestCaller {
  int LOGIN_ERROR = 1;

  int LOGIN_REQUEST = 1;
  int REQUESTS_CHAIN = 2;
  int SYNC_SERVER_TASKS = 4;

  void requestSuccess(int requestCode);

  void requestError(int errorCode);
}
