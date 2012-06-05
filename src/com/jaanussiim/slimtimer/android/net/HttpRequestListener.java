package com.jaanussiim.slimtimer.android.net;

public interface HttpRequestListener {
  int REQUEST_OK = 1;
  int REQUEST_AUTHENTICATION_ERROR = 2;
  int REQUEST_NETWORK_ERROR = 3;

  void requestComplete(int status);
}
