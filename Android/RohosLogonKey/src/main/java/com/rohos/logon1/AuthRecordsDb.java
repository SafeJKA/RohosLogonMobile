/*
 * Copyright 2010 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.rohos.logon1;

import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Process;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;

/**
 * A database of authentication records. Each records is get created by scanning QR-Code from Rohos Logon Key desktop app (Win,Mac)
 * <p>
 * Inspired by : Google Authenticator.AccountDb.java class by sweis@google.com (Steve Weis)
 *
 * @author AlexShilon
 */
public class AuthRecordsDb {

    private static final String ID_COLUMN = "_id";
    private static final String USER_NAME_COLUMN = "email";
    private static final String SECRET_COLUMN = "secret";
    private static final String DATA_COLUMN = "data";
    private static final String HOST_NAME_COLUMN = "hostName";
    private static final String HOST_IP_COLUMN = "hostIP";
    private static final String HOST_PORT_COLUMN = "hostPort";
    private static final String SETT_COLUMN = "settings";
    private static final String P1_COLUMN = "param1"; //lets reserve for future...     <----this will be the topic for each registered profile
    private static final String P2_COLUMN = "param2"; //str
    private static final String P3_COLUMN = "param3"; //int
    private static final String P4_COLUMN = "param4"; //int


    // @VisibleForTesting
    static final String TABLE_NAME = "accounts";
    // @VisibleForTesting
    static final String PATH = "databases";

    // @VisibleForTesting
    SQLiteDatabase mDatabase;

    private final String TAG = "RohosLogon.RecordsDb";


    public AuthRecordsDb(Context context) {
        mDatabase = openDatabase(context);

        // Create the table if it doesn't exist
        mDatabase.execSQL(String.format(
                "CREATE TABLE IF NOT EXISTS %s" +
                        " (%s INTEGER PRIMARY KEY, %s TEXT NOT NULL, %s TEXT NOT NULL, %s TEXT NOT NULL," +
                        " %s TEXT NOT NULL, %s TEXT NOT NULL, %s INTEGER, %s TEXT NOT NULL," +
                        " %s TEXT, %s TEXT, %s INTEGER DEFAULT 0, %s INTEGER DEFAULT 0)",
                TABLE_NAME,
                ID_COLUMN, USER_NAME_COLUMN, SECRET_COLUMN, DATA_COLUMN,
                HOST_NAME_COLUMN, HOST_IP_COLUMN, HOST_PORT_COLUMN, SETT_COLUMN,
                P1_COLUMN, P2_COLUMN, P3_COLUMN, P4_COLUMN));

    }

    /*
     * Tries three times to open database before throwing AccountDbOpenException.
     */
    private SQLiteDatabase openDatabase(Context context) {
        for (int count = 0; true; count++) {
            try {
                return context.openOrCreateDatabase(PATH, Context.MODE_PRIVATE, null);
            } catch (SQLiteException e) {
                if (count < 2) {
                    continue;
                } else {
                    throw new RecordsDbOpenException("Failed to open AccountDb database in three tries.\n"
                            + getAccountDbOpenFailedErrorString(context), e);
                }
            }
        }
    }

    private String getAccountDbOpenFailedErrorString(Context context) {
        String dataPackageDir = context.getApplicationInfo().dataDir;
        String databaseDirPathname = context.getDatabasePath(PATH).getParent();
        String databasePathname = context.getDatabasePath(PATH).getAbsolutePath();
        String[] dirsToStat = new String[]{dataPackageDir, databaseDirPathname, databasePathname};
        StringBuilder error = new StringBuilder();
        int myUid = Process.myUid();
        for (String directory : dirsToStat) {
            try {
                FileUtilities.StatStruct stat = FileUtilities.getStat(directory);
                String ownerUidName = null;
                try {
                    if (stat.uid == 0) {
                        ownerUidName = "root";
                    } else {
                        PackageManager packageManager = context.getPackageManager();
                        ownerUidName = (packageManager != null) ? packageManager.getNameForUid(stat.uid) : null;
                    }
                } catch (Exception e) {
                    ownerUidName = e.toString();
                }
                error.append(directory + " directory stat (my UID: " + myUid);
                if (ownerUidName == null) {
                    error.append("): ");
                } else {
                    error.append(", dir owner UID name: " + ownerUidName + "): ");
                }
                error.append(stat.toString() + "\n");
            } catch (IOException e) {
                error.append(directory + " directory stat threw an exception: " + e + "\n");
            }
        }
        return error.toString();
    }

    /**
     * Closes this database and releases any system resources held.
     */
    public void close() {
        mDatabase.close();
    }


    /*
     * deleteAllData() will remove all rows. Useful for testing.
     */
    public boolean deleteAllData() {
        mDatabase.delete(AuthRecordsDb.TABLE_NAME, null, null);
        return true;
    }

    public boolean nameExists(String email) {
        Cursor cursor = getAccount(email);
        try {
            return !cursorIsEmpty(cursor);
        } finally {
            tryCloseCursor(cursor);
        }
    }

    public boolean hostExists(String host) {
        Cursor cursor = null;
        try {
            cursor = mDatabase.query(TABLE_NAME, null, HOST_NAME_COLUMN + "= ?",
                    new String[]{host}, null, null, null);

            return !cursorIsEmpty(cursor);
        } catch (Exception e) {
           // Log.e(TAG, "ERROR " + e.toString());
        } finally {
            if (cursor != null)
                tryCloseCursor(cursor);
        }

        return false;
    }

    public void getHosts() {
        Cursor cursor = null;
        try {
            cursor = getNames();
            if (!cursorIsEmpty(cursor)) {
               // Log.d(TAG, "Cursor size " + cursor.getCount());
                while (cursor.moveToNext()) {
                    String host = cursor.getString(cursor.getColumnIndex(HOST_NAME_COLUMN));
                //    Log.d(TAG, "host: " + host + ", length " + host.length());
                }
            } else {
              //  Log.d(TAG, "Cursor id empty");
            }
        } finally {
            tryCloseCursor(cursor);
        }
    }

    public AuthRecord getAuthRecord(String name) {
        Cursor cursor = getAccount(name);
        AuthRecord ai = new AuthRecord();
        try {
            if (!cursorIsEmpty(cursor)) {
                cursor.moveToFirst();


                ai.qr_user = cursor.getString(cursor.getColumnIndex(USER_NAME_COLUMN));
                ai.qr_data = cursor.getString(cursor.getColumnIndex(DATA_COLUMN));
                ai.qr_secret_key = cursor.getString(cursor.getColumnIndex(SECRET_COLUMN));
                ai.qr_host_name = cursor.getString(cursor.getColumnIndex(HOST_NAME_COLUMN));
                ai.qr_host_ip = cursor.getString(cursor.getColumnIndex(HOST_IP_COLUMN));
                ai.qr_host_port = cursor.getInt(cursor.getColumnIndex(HOST_PORT_COLUMN));


                return ai;
            }
        } finally {
            tryCloseCursor(cursor);
        }
        return ai;
    }

    public AuthRecord getAuthRecordByHostName(String host) {
        Cursor cursor = getAccountByHostName(host);
        AuthRecord ai = new AuthRecord();
        try {
            if (!cursorIsEmpty(cursor)) {
                cursor.moveToFirst();


                ai.qr_user = cursor.getString(cursor.getColumnIndex(USER_NAME_COLUMN));
                ai.qr_data = cursor.getString(cursor.getColumnIndex(DATA_COLUMN));
                ai.qr_secret_key = cursor.getString(cursor.getColumnIndex(SECRET_COLUMN));
                ai.qr_host_name = cursor.getString(cursor.getColumnIndex(HOST_NAME_COLUMN));
                ai.qr_host_ip = cursor.getString(cursor.getColumnIndex(HOST_IP_COLUMN));
                ai.qr_host_port = cursor.getInt(cursor.getColumnIndex(HOST_PORT_COLUMN));


                return ai;
            }
        } catch (Exception e) {
          //  Log.e("DB", e.toString());
        } finally {
            tryCloseCursor(cursor);
        }
        return ai;
    }

    private static String whereClause(String name) {
        return USER_NAME_COLUMN + " = " + DatabaseUtils.sqlEscapeString(name);
    }

    public void delete(String computer_name, String record_name) {
        mDatabase.delete(TABLE_NAME, USER_NAME_COLUMN + " = " + DatabaseUtils.sqlEscapeString(record_name) + " AND " +
                HOST_NAME_COLUMN + " = " + DatabaseUtils.sqlEscapeString(computer_name), null);
    }


    /**
     * Save Authentication record to database, creating a new  entry if necessary.
     *
     * @param ai Authentication record, user name, host, secret, data
     */
    public void update(AuthRecord ai) {
        ContentValues values = new ContentValues();
        values.put(USER_NAME_COLUMN, ai.qr_user);
        values.put(SECRET_COLUMN, ai.qr_secret_key);
        values.put(DATA_COLUMN, ai.qr_data);
        values.put(HOST_NAME_COLUMN, ai.qr_host_name);
        values.put(HOST_IP_COLUMN, ai.qr_host_ip);
        values.put(HOST_PORT_COLUMN, ai.qr_host_port);
        values.put(SETT_COLUMN, ".");

        int updated = mDatabase.update(TABLE_NAME, values,
                whereClause(ai.qr_user), null);
        if (updated == 0) {
            mDatabase.insert(TABLE_NAME, null, values);
        }
    }

    private Cursor getNames() {
        return mDatabase.query(TABLE_NAME, null, null, null, null, null, null, null);
    }

    private Cursor getAccount(String name) {
        return mDatabase.query(TABLE_NAME, null, USER_NAME_COLUMN + "= ?",
                new String[]{name}, null, null, null);
    }

    private Cursor getAccountByHostName(String host) {
        return mDatabase.query(TABLE_NAME, null, HOST_NAME_COLUMN + "= ?",
                new String[]{host}, null, null, null);
    }

    /**
     * Returns true if the cursor is null, or contains no rows.
     */
    private static boolean cursorIsEmpty(Cursor c) {
        return c == null || c.getCount() == 0;
    }

    /**
     * Closes the cursor if it is not null and not closed.
     */
    private static void tryCloseCursor(Cursor c) {
        if (c != null && !c.isClosed()) {
            c.close();
        }
    }

    /**
     * Get list of all account names.
     *
     * @param result Collection of strings-- account names are appended, without
     *               clearing this collection on entry.
     * @return Number of accounts added to the output parameter.
     */
    public int getNames(Collection<String> result) {
        Cursor cursor = getNames();

        try {
            if (cursorIsEmpty(cursor))
                return 0;

            int nameCount = cursor.getCount();
            int index = cursor.getColumnIndex(AuthRecordsDb.USER_NAME_COLUMN);

            for (int i = 0; i < nameCount; ++i) {
                cursor.moveToPosition(i);
                String username = cursor.getString(index);
                result.add(username);
            }

            return nameCount;
        } finally {
            tryCloseCursor(cursor);
        }
    }

    private static class RecordsDbOpenException extends RuntimeException {
        public RecordsDbOpenException(String message, Exception e) {
            super(message, e);
        }
    }

}
