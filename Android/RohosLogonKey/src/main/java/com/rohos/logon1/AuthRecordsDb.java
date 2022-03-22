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
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Process;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.rohos.logon1.utils.AppLog;

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

    // Fields of users table
    private static final String ID_ = "_id";
    private static final String USER_NAME = "user_name";
    private static final String SECRET_KEY = "secret_key";
    private static final String DATA = "data";
    private static final String HOST_ID = "host_id";
    private static final String SETTINGS = "settings";
    private static final String PARAM1 = "param1"; //lets reserve for future...     <----this will be the topic for each registered profile
    private static final String PARAM2 = "param2"; //str
    private static final String PARAM3 = "param3"; //int
    private static final String PARAM4 = "param4"; //int

    // Fields of hosts table
    private static final String HOST_NAME = "host_name";
    private static final String HOST_IP = "host_ip";
    private static final String HOST_PORT = "host_port";
    private static final String SEND_TOKEN_SESSION = "send_token_session"; //int

    // Fields of notify table
    private static final String PN_USER = "user";
    private static final String PN_PCN = "pc_name";
    private static final String PN_TYPE = "type";
    private static final String PN_TITLE = "title";
    private static final String PN_TEXT = "text";
    private static final String PN_TS = "time_sent";
    private static final String PN_ATTR = "attr";

    // @VisibleForTesting
    static final String TABLE_USERS = "users";
    static final String TABLE_HOSTS = "hosts";
    static final String TABLE_PN = "notify";
    // @VisibleForTesting
    static final String PATH = "databases";

    // @VisibleForTesting
    SQLiteDatabase mDatabase;

    private final String TAG = "RecordsDb";
    private final int DB_VERSION = 1;



    public AuthRecordsDb(Context context) {
        mDatabase = openDatabase(context);

        // Create tables if they don't exist
        checkTables(context);
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
        mDatabase.delete(AuthRecordsDb.TABLE_USERS, null, null);
        return true;
    }

    /*
     * Commented, not being used
    public boolean nameExists(String email) {
        Cursor cursor = getAccount(email);
        try {
            return !cursorIsEmpty(cursor);
        } finally {
            tryCloseCursor(cursor);
        }
    }
     */

    public ArrayList<String[]> getNotifications(){
        ArrayList<String[]> notifyList = new ArrayList<>();
        try{
            String query = "SELECT user, pc_name, text FROM notify ORDER BY time_sent DESC";
            Cursor cr = mDatabase.rawQuery(query, null, null);
            if(cr == null || !cr.moveToFirst()){
                AppLog.log(TAG + "; couldn't get notifications");
                return notifyList;
            }

            int userName = cr.getColumnIndex("user");
            int pcName = cr.getColumnIndex("pc_name");
            int nText = cr.getColumnIndex("text");

            do{
                notifyList.add(new String[]{cr.getString(userName), cr.getString(pcName), cr.getString(nText)});
            }while(cr.moveToNext());
        }catch(Exception e){
            AppLog.log(Log.getStackTraceString(e));
        }
        return notifyList;
    }

    public void deleteAllNotifications(){
        try{
            mDatabase.delete(TABLE_PN, null, null);
        }catch(Exception e){
            AppLog.log(Log.getStackTraceString(e));
        }
    }

    public boolean hostExists(String host) {
        Cursor cursor = null;
        try {
            cursor = mDatabase.query(TABLE_USERS, null, HOST_ID + "= ?",
                    new String[]{host}, null, null, null);

            return !cursorIsEmpty(cursor);
        } catch (Exception e) {
            AppLog.log(Log.getStackTraceString(e));
           // Log.e(TAG, "ERROR " + e.toString());
        } finally {
            if (cursor != null)
                tryCloseCursor(cursor);
        }

        return false;
    }

    public ArrayList<String[]> getHostList() {
        ArrayList<String[]> arrayList = null;
        try {
            String query = "SELECT user_name, secret_key, host_name FROM users LEFT JOIN hosts ON users.host_id=hosts._id GROUP BY host_name";
            Cursor cursor = mDatabase.rawQuery(query, null, null);
            if (cursor == null || !cursor.moveToFirst()) {
                AppLog.log(TAG + "; couldn't get host list");
                return null;
            }

            int userName = cursor.getColumnIndex(USER_NAME);
            int secret = cursor.getColumnIndex(SECRET_KEY);
            int hostName = cursor.getColumnIndex(HOST_NAME);

            arrayList = new ArrayList<>();
            arrayList.add(new String[]{cursor.getString(userName), cursor.getString(secret), cursor.getString(hostName)});

            while (cursor.moveToNext()) {
                arrayList.add(new String[]{cursor.getString(userName), cursor.getString(secret), cursor.getString(hostName) });
            }
        } catch (Exception e) {
            AppLog.log(Log.getStackTraceString(e));
        }

        return arrayList;
    }

    public void getHosts() {
        Cursor cursor = null;
        try {
            cursor = getNames();
            if (!cursorIsEmpty(cursor)) {
               // Log.d(TAG, "Cursor size " + cursor.getCount());
                while (cursor.moveToNext()) {
                    int index = cursor.getColumnIndex(HOST_ID);
                    String host = cursor.getString(index);
                //    Log.d(TAG, "host: " + host + ", length " + host.length());
                }
            } else {
              //  Log.d(TAG, "Cursor id empty");
            }
        } finally {
            tryCloseCursor(cursor);
        }
    }

    public int getHostId(String name){
        String query = "SELECT _id FROM hosts WHERE host_name=?";
        String[] selectArgs = new String[]{name};
        Cursor cursor = mDatabase.rawQuery(query, selectArgs, null);
        if(cursor == null || !cursor.moveToFirst()){
            return -1;
        }
        int columnIndex = cursor.getColumnIndex(ID_);

        return cursor.getInt(columnIndex);
    }

    public AuthRecord getAuthRecord(String name, String host) {
        Cursor cursor = getAccount(name, host);
        AuthRecord ai = new AuthRecord();
        try {
            if (!cursorIsEmpty(cursor)) {
                cursor.moveToFirst();

                int index = cursor.getColumnIndex(USER_NAME);
                if(index >= 0)
                    ai.qr_user = cursor.getString(index);
                index = cursor.getColumnIndex(DATA);
                if(index >= 0)
                    ai.qr_data = cursor.getString(index);
                index = cursor.getColumnIndex(SECRET_KEY);
                if(index >= 0)
                    ai.qr_secret_key = cursor.getString(index);
                index = cursor.getColumnIndex(HOST_NAME);
                if(index >= 0)
                    ai.qr_host_name = cursor.getString(index);
                index = cursor.getColumnIndex(HOST_IP);
                if(index >= 0)
                    ai.qr_host_ip = cursor.getString(index);
                index = cursor.getColumnIndex(HOST_PORT);
                if(index >= 0)
                    ai.qr_host_port = cursor.getInt(index);

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

                int index = cursor.getColumnIndex(USER_NAME);
                if(index >= 0)
                    ai.qr_user = cursor.getString(index);
                index = cursor.getColumnIndex(DATA);
                if(index >= 0)
                    ai.qr_data = cursor.getString(index);
                index = cursor.getColumnIndex(SECRET_KEY);
                if(index >= 0)
                    ai.qr_secret_key = cursor.getString(index);
                index = cursor.getColumnIndex(HOST_ID);
                if(index >= 0)
                    ai.qr_host_name = cursor.getString(index);
                index = cursor.getColumnIndex(HOST_IP);
                if(index >= 0)
                    ai.qr_host_ip = cursor.getString(index);
                index = cursor.getColumnIndex(HOST_PORT);
                if(index >= 0)
                    ai.qr_host_port = cursor.getInt(index);

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
        return USER_NAME + " = " + DatabaseUtils.sqlEscapeString(name);
    }

    public void insertNotify(NotifyRecord nr){
        ContentValues cv = new ContentValues();
        cv.put(PN_USER, nr.getUserName());
        cv.put(PN_PCN, nr.getComputerName());
        cv.put(PN_TYPE, nr.getType());
        cv.put(PN_TITLE, nr.getTitle());
        cv.put(PN_TEXT, nr.getText());
        cv.put(PN_TS, nr.getTimeSent());

        mDatabase.insert(TABLE_PN, null, cv);
    }

    public void delete(String computer_name, String record_name) {
        try{
            int hostID = getHostId(computer_name);
            mDatabase.delete(TABLE_USERS, USER_NAME + "='" + record_name + "' AND " +
                    HOST_ID + "=" + hostID, null);
        }catch(Exception e){
            AppLog.log(Log.getStackTraceString(e));
        }
    }


    /**
     * Save Authentication record to database, creating a new  entry if necessary.
     *
     * @param ai Authentication record, user name, host, secret, data
     */
    public void update(AuthRecord ai) {
        ContentValues val = new ContentValues();
        val.put(HOST_NAME, ai.qr_host_name);
        val.put(HOST_IP, ai.qr_host_ip);
        val.put(HOST_PORT, ai.qr_host_port);
        val.put(SEND_TOKEN_SESSION, 0);

        int hostID = getHostId(ai.qr_host_name);
        AppLog.log(TAG + "; host id:" + hostID);
        if(hostID < 0){
            long id = mDatabase.insert(TABLE_HOSTS, null, val);
            hostID = (int)id;
            AppLog.log("Inserted host id:" + hostID);
        }else{
            mDatabase.update(TABLE_HOSTS, val, HOST_NAME + "= ?", new String[]{ai.qr_host_name});
        }

        ContentValues values = new ContentValues();
        values.put(USER_NAME, ai.qr_user);
        values.put(SECRET_KEY, ai.qr_secret_key);
        values.put(DATA, ai.qr_data);
        values.put(HOST_ID, hostID);
        values.put(SETTINGS, ".");

        int updated = mDatabase.update(TABLE_USERS, values, USER_NAME + "= ? AND " + HOST_ID + "= ?",
                new String[]{ai.qr_user, String.valueOf(hostID)});
        if (updated == 0) {
            mDatabase.insert(TABLE_USERS, null, values);
        }
    }

    private Cursor getNames() {
        String query = "SELECT user_name, host_name FROM users LEFT JOIN hosts ON users.host_id = hosts._id";
        Cursor cursor = mDatabase.rawQuery(query, null, null);
        return cursor;


        //return mDatabase.query(TABLE_USERS, null, null, null, null, null, null, null);
    }

    private Cursor getAccount(String name, String host) {
        String query = "SELECT user_name, data, secret_key, host_name, host_ip, host_port FROM users " +
                "LEFT JOIN hosts ON users.host_id = hosts._id WHERE user_name=? AND host_name=?";
        String[] selectArgs = new String[]{name, host};

        Cursor cursor = mDatabase.rawQuery(query, selectArgs, null);
        return cursor;
        //return mDatabase.query(TABLE_USERS, null, USER_NAME + "= ? AND " + HOST_ID + "= ?",
        //        new String[]{name, host}, null, null, null);
    }

    private Cursor getAccountByHostName(String host) {
        return mDatabase.query(TABLE_USERS, null, HOST_ID + "= ?",
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
            if (cursorIsEmpty(cursor)) {
                AppLog.log(TAG + "; Couldn't get Names and Hosts from DB");
                return 0;
            }

            int nameCount = cursor.getCount();
            int indexName = cursor.getColumnIndex(AuthRecordsDb.USER_NAME);
            int indexHost = cursor.getColumnIndex(AuthRecordsDb.HOST_NAME);

            for (int i = 0; i < nameCount; ++i) {
                cursor.moveToPosition(i);
                String name = cursor.getString(indexName);
                String host = cursor.getString(indexHost);
                result.add(name + "|" + host);
            }

            return nameCount;
        }catch(Exception e){
            AppLog.log(Log.getStackTraceString(e));
        }finally {
            tryCloseCursor(cursor);
        }
        return 0;
    }

    private static class RecordsDbOpenException extends RuntimeException {
        public RecordsDbOpenException(String message, Exception e) {
            super(message, e);
        }
    }

    private void checkTables(Context context){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        try {
            int dbVersion = sp.getInt("db_version", 0);

            if (dbVersion == DB_VERSION) {
                return;
            }

            // Create table users
            StringBuilder sb = new StringBuilder();
            sb.append("CREATE TABLE IF NOT EXISTS ".concat(TABLE_USERS));
            sb.append(" (");
            sb.append(ID_.concat(" INTEGER PRIMARY KEY, "));
            sb.append(USER_NAME.concat(" TEXT NOT NULL, "));
            sb.append(SECRET_KEY.concat(" TEXT NOT NULL, "));
            sb.append(DATA.concat(" TEXT NOT NULL, "));
            sb.append(HOST_ID.concat(" INTEGER DEFAULT 0, "));
            sb.append(SETTINGS.concat(" TEXT NOT NULL, "));
            sb.append(PARAM1.concat(" TEXT, "));
            sb.append(PARAM2.concat(" TEXT, "));
            sb.append(PARAM3.concat(" INTEGER DEFAULT 0, "));
            sb.append(PARAM4.concat(" INTEGER DEFAULT 0)"));

            mDatabase.execSQL(sb.toString());

            // Crate table hosts
            sb.delete(0, sb.length());
            sb.append("CREATE TABLE IF NOT EXISTS ".concat(TABLE_HOSTS));
            sb.append(" (");
            sb.append(ID_.concat(" INTEGER PRIMARY KEY, "));
            sb.append(HOST_NAME.concat(" TEXT NOT NULL UNIQUE, "));
            sb.append(HOST_IP.concat(" TEXT NOT NULL, "));
            sb.append(HOST_PORT.concat(" INTEGER DEFAULT 0, "));
            sb.append(SEND_TOKEN_SESSION.concat(" INTEGER DEFAULT 0)"));

            mDatabase.execSQL(sb.toString());

            // Create table notify
            sb.delete(0, sb.length());
            sb.append("CREATE TABLE IF NOT EXISTS ".concat(TABLE_PN));
            sb.append(" (");
            sb.append(PN_USER.concat(" TEXT NOT NULL, "));
            sb.append(PN_PCN.concat(" TEXT NOT NULL, ")); // computer name
            sb.append(PN_TYPE.concat(" TEXT, "));
            sb.append(PN_TITLE.concat(" TEXT, "));
            sb.append(PN_TEXT.concat(" TEXT, "));
            sb.append(PN_TS.concat(" INTEGER DEFAULT 0, ")); // time sent
            sb.append(PN_ATTR.concat(" TEXT)"));

            mDatabase.execSQL(sb.toString());
        }catch(SQLiteException se){
            AppLog.log("Error: " + se.getMessage());
        }catch(Exception e){
            AppLog.log(Log.getStackTraceString(e));
        }

        try{
            // Copy data from table accounts if the one exists
            Cursor cursor = mDatabase.query("accounts", null, null, null, null, null, null);
            if (!cursor.moveToFirst()) {
                return;
            }

            // Insert data to table hosts
            int hostName = cursor.getColumnIndex("hostName");
            int hostIP = cursor.getColumnIndex("hostIP");
            int hostPort = cursor.getColumnIndex("hostPort");
            do {
                ContentValues hosts = new ContentValues();
                hosts.put(HOST_NAME, cursor.getString(hostName));
                hosts.put(HOST_IP, cursor.getString(hostIP));
                hosts.put(HOST_PORT, cursor.getInt(hostPort));
                hosts.put(SEND_TOKEN_SESSION, 0);

                mDatabase.insert(TABLE_HOSTS, null, hosts);
            } while (cursor.moveToNext());

            // Insert data to table users
            int userName = cursor.getColumnIndex("email");
            int secretKey = cursor.getColumnIndex("secret");
            int data = cursor.getColumnIndex("data");
            int settings = cursor.getColumnIndex("settings");
            cursor.moveToFirst();
            do {
                int id_ = getHostId(cursor.getString(hostName));

                ContentValues users = new ContentValues();
                users.put(USER_NAME, cursor.getString(userName));
                users.put(SECRET_KEY, cursor.getString(secretKey));
                users.put(DATA, cursor.getString(data));
                users.put(HOST_ID, id_);
                users.put(SETTINGS, cursor.getString(settings));

                mDatabase.insert(TABLE_USERS, null, users);
            } while (cursor.moveToNext());

            // Drop table accounts
            mDatabase.execSQL("DROP TABLE IF EXISTS accounts");

            SharedPreferences.Editor e = sp.edit();
            e.putInt("db_version", DB_VERSION);
            e.commit();
        }catch(Exception e){}
    }
}
