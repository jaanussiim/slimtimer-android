package com.jaanussiim.slimtimer.android.net_old;

import java.util.ArrayList;
import java.util.Iterator;

public class NetworkRequestsChain extends ArrayList<NetworkRequest> implements Runnable {
  private static final long serialVersionUID = -7891965993629119531L;
  private final NetworkRequestCaller entriesList;

  public NetworkRequestsChain(final NetworkRequestCaller entriesList) {
    this.entriesList = entriesList;
  }

  public void execute() {
    final Thread t = new Thread(this);
    t.start();
  }

  public void run() {
    final Iterator<NetworkRequest> i = listIterator();
    while (i.hasNext()) {
      final NetworkRequest request = i.next();
      request.run();
    }

    entriesList.requestSuccess(NetworkRequestCaller.REQUESTS_CHAIN);
  }
}
