package org.litetokens.core.db2.core;

import com.google.common.collect.Maps;
import com.google.common.collect.Streams;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.ArrayUtils;
import org.litetokens.common.utils.ByteUtil;
import org.litetokens.core.config.args.Args;
import org.litetokens.core.db.common.WrappedByteArray;
import org.litetokens.core.db2.common.IRevokingDB;
import org.litetokens.core.db2.common.LevelDB;
import org.litetokens.core.db2.common.Value;
import org.litetokens.core.exception.ItemNotFoundException;

public class RevokingDBWithCachingNewValue implements IRevokingDB {
  @Setter
  @Getter
  private Snapshot head;
  @Getter
  private String dbName;

  public RevokingDBWithCachingNewValue(String dbName) {
    this.dbName = dbName;
    head = new SnapshotRoot(Args.getInstance().getOutputDirectoryByDbName(dbName), dbName);
  }

  /**
   * close the database.
   */
  @Override
  public void close() {
    head.close();
  }

  @Override
  public void reset() {
    head.reset();
    head.close();
    head = new SnapshotRoot(Args.getInstance().getOutputDirectoryByDbName(dbName), dbName);
  }

  @Override
  public void put(byte[] key, byte[] value) {
    head.put(key, value);
  }

  @Override
  public void delete(byte[] key) {
    head.remove(key);
  }

  @Override
  public byte[] get(byte[] key) throws ItemNotFoundException {
    byte[] value = head.get(key);
    if (ArrayUtils.isEmpty(value)) {
      throw new ItemNotFoundException();
    }
    return value;
  }

  @Override
  public byte[] getUnchecked(byte[] key) {
    try {
      return get(key);
    } catch (ItemNotFoundException e) {
      return null;
    }
  }

  @Override
  public boolean has(byte[] key) {
    return head.get(key) != null;
  }

  @Override
  public Iterator<Map.Entry<byte[], byte[]>> iterator() {
    return head.iterator();
  }

  //for blockstore
  @Override
  public Set<byte[]> getlatestValues(long limit) {
    if (limit <= 0) {
      return Collections.emptySet();
    }

    Set<byte[]> result = new HashSet<>();
    Snapshot snapshot = head;
    long tmp = limit;
    for (; tmp > 0 && snapshot.getPrevious() != null; --tmp, snapshot = snapshot.getPrevious()) {
      Streams.stream(((SnapshotImpl) snapshot).db)
          .map(Map.Entry::getValue)
          .map(Value::getBytes)
          .forEach(result::add);
    }

    if (snapshot.getPrevious() == null && tmp != 0) {
      result.addAll(((LevelDB) ((SnapshotRoot) snapshot).db).getDb().getlatestValues(tmp));
    }

    return result;
  }

  //for blockstore
  @Override
  public Set<byte[]> getValuesNext(byte[] key, long limit) {
    if (limit <= 0) {
      return Collections.emptySet();
    }

    Map<WrappedByteArray, WrappedByteArray> collection = new HashMap<>();
    if (head.getPrevious() != null) {
      ((SnapshotImpl) head).collect(collection);
    }

    Map<WrappedByteArray, WrappedByteArray> levelDBMap = new HashMap<>();
    ((LevelDB) ((SnapshotRoot) head.getRoot()).db).getDb().getNext(key, limit).entrySet().stream()
        .map(e -> Maps.immutableEntry(WrappedByteArray.of(e.getKey()), WrappedByteArray.of(e.getValue())))
        .forEach(e -> levelDBMap.put(e.getKey(), e.getValue()));

    levelDBMap.putAll(collection);

    return levelDBMap.entrySet().stream()
        .filter(e -> ByteUtil.greaterOrEquals(e.getKey().getBytes(), key))
        .limit(limit)
        .map(Map.Entry::getValue)
        .map(WrappedByteArray::getBytes)
        .collect(Collectors.toSet());

  }

}
