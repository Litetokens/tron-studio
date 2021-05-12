package org.litetokens.core.db;

import org.litetokens.core.db.AbstractRevokingStore.Dialog;
import org.litetokens.core.db.AbstractRevokingStore.RevokingState;
import org.litetokens.core.db.AbstractRevokingStore.RevokingTuple;
import org.litetokens.core.db2.common.IRevokingDB;
import org.litetokens.core.db2.core.ISession;
import org.litetokens.core.exception.RevokingStoreIllegalStateException;

public interface RevokingDatabase {

  ISession buildSession();

  ISession buildSession(boolean forceEnable);

  void add(IRevokingDB revokingDB);

  void merge() throws RevokingStoreIllegalStateException;

  void revoke() throws RevokingStoreIllegalStateException;

  void commit() throws RevokingStoreIllegalStateException;

  void pop() throws RevokingStoreIllegalStateException;

  void enable();

  int size();

  void check();

  void setMaxSize(int maxSize);

  void disable();

  void shutdown();
}
