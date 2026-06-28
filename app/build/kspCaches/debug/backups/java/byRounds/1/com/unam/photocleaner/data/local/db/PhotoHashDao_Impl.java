package com.unam.photocleaner.data.local.db;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.room.util.StringUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
import java.lang.SuppressWarnings;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class PhotoHashDao_Impl implements PhotoHashDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<PhotoHashEntity> __insertionAdapterOfPhotoHashEntity;

  public PhotoHashDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfPhotoHashEntity = new EntityInsertionAdapter<PhotoHashEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `photo_hashes` (`photoId`,`dHash`,`sharpness`) VALUES (?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final PhotoHashEntity entity) {
        statement.bindLong(1, entity.getPhotoId());
        statement.bindLong(2, entity.getDHash());
        statement.bindDouble(3, entity.getSharpness());
      }
    };
  }

  @Override
  public Object insert(final PhotoHashEntity entity, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfPhotoHashEntity.insert(entity);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object get(final long photoId, final Continuation<? super PhotoHashEntity> $completion) {
    final String _sql = "SELECT * FROM photo_hashes WHERE photoId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, photoId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<PhotoHashEntity>() {
      @Override
      @Nullable
      public PhotoHashEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfPhotoId = CursorUtil.getColumnIndexOrThrow(_cursor, "photoId");
          final int _cursorIndexOfDHash = CursorUtil.getColumnIndexOrThrow(_cursor, "dHash");
          final int _cursorIndexOfSharpness = CursorUtil.getColumnIndexOrThrow(_cursor, "sharpness");
          final PhotoHashEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpPhotoId;
            _tmpPhotoId = _cursor.getLong(_cursorIndexOfPhotoId);
            final long _tmpDHash;
            _tmpDHash = _cursor.getLong(_cursorIndexOfDHash);
            final double _tmpSharpness;
            _tmpSharpness = _cursor.getDouble(_cursorIndexOfSharpness);
            _result = new PhotoHashEntity(_tmpPhotoId,_tmpDHash,_tmpSharpness);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteStale(final List<Long> validIds,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final StringBuilder _stringBuilder = StringUtil.newStringBuilder();
        _stringBuilder.append("DELETE FROM photo_hashes WHERE photoId NOT IN (");
        final int _inputSize = validIds.size();
        StringUtil.appendPlaceholders(_stringBuilder, _inputSize);
        _stringBuilder.append(")");
        final String _sql = _stringBuilder.toString();
        final SupportSQLiteStatement _stmt = __db.compileStatement(_sql);
        int _argIndex = 1;
        for (long _item : validIds) {
          _stmt.bindLong(_argIndex, _item);
          _argIndex++;
        }
        __db.beginTransaction();
        try {
          _stmt.executeUpdateDelete();
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
