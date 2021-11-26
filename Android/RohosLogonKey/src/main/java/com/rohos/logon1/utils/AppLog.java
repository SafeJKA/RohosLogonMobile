package com.rohos.logon1.utils;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by yura on 7/14/16.
 */
public class AppLog{

    private static AppLog mLog = null;
    private static ThreadPoolExecutor mExecutor;

    private final String TAG = "AppLog";
    private final String CR = "\n";
    private final long LOG_SIZE =  1024L * 150L; // 150 kB

    private String mLogFile = "rohoslogon.log";
    private String mPath = null;

    public AppLog(Context context){
        try{
            mPath = context.getExternalFilesDir(null).getPath();

            File dir = new File(mPath);
            if(!dir.exists()) {
                dir.mkdir();
                Log.d(TAG, "Create dir " + dir.getAbsolutePath());
            }

            mExecutor = new ThreadPoolExecutor(10, 100, 1, TimeUnit.SECONDS,
                    new ArrayBlockingQueue<Runnable>(2));
        }catch(Exception e){
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    public static void log(final String message){
        if(mLog == null) return;
        try{
            Runnable r = new Runnable(){
                @Override
                public void run() {
                    mLog.writeLog(message);
                }
            };

            mExecutor.execute(r);
            //new Thread(r).start();
        }catch(Exception e){
            Log.e("AppLog", Log.getStackTraceString(e));
        }
    }

    /*public static void getLog(){
        if(mLog == null) return;
        try{
            Runnable r = new Runnable(){
                @Override
                public void run() {
                    StringBuilder log = mLog.readLog();
                    if(log != null){
                        byte[] data = log.toString().getBytes();
                        TApplication app = TApplication.getInstance();
                        if(app == null) return;

                        app.setLogBuffer(data);

                        Context context = app.getApplicationContext();
                        Intent intent = new Intent(context, ShowSysLogs.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        context.startActivity(intent);
                    }
                }
            };

            new Thread(r).start();
        }catch(Exception e){
            log(Log.getStackTraceString(e));
        }
    }*/

    public static void initAppLog(Context context){
        mLog = new AppLog(context);
    }

    protected synchronized void writeLog (String message){
        StringBuilder sb = new StringBuilder();

        sb.append(message);
        sb.append(CR);
        sb.append(getCurDate());
        sb.append(CR + CR);

        //String fName = new String(fName);
        File f = new File(new String(mPath), new String(mLogFile));
        boolean appendFile = true;

        byte[] buff = sb.toString().getBytes();
        if(f.exists()){
            try{
                // Rewrite file error.txt if size of the file is larger then 50K
                if(f.length() > LOG_SIZE){
                    appendFile = false;
                    //File dir = new File(mPath);
                    //String[] list = dir.list();
                    //File newName = new File(f.getParent(), "kidlogger" + list.length + ".log");
                    //Log.d(TAG, newName.getAbsolutePath());
                    //f.renameTo(newName);
                    //f = new File(new String(mPath), new String(mLogFile));
                }

                FileOutputStream fos = new FileOutputStream(f, appendFile);
                fos.write(buff);
                fos.close();
            }catch(IOException e){
                Log.i(TAG, " writeLog " + e.toString());
            }
        }else{
            try{
                boolean created = f.createNewFile();
                if(created){
                    FileOutputStream fos = new FileOutputStream(f, false);
                    fos.write(buff);
                    fos.close();
                }
            }catch(IOException e){
                Log.e(TAG, Log.getStackTraceString(e));
            }
        }
    }

    protected synchronized StringBuilder readLog(){
        File file = new File(new String(mPath), new String(mLogFile));
        if(!file.exists())
            return null;

        StringBuilder sb = new StringBuilder();
        FileReader fr;
        try {
            fr = new FileReader(file);
            BufferedReader br = new BufferedReader(fr);
            String line;
            while((line = br.readLine()) != null){
                sb.append(line);
                sb.append(CR);
            }
            fr.close();
        } catch (IOException e) {
            Log.e("SysLog.readLog", "Error: " + e.toString());
            return null;
        }

        return sb;
    }

    protected File getLogFile(){
        return new File(new String(mPath), new String(mLogFile));
    }

    private String getCurDate(){
        long d = System.currentTimeMillis();
        return String.format("%tF %tT", d, d);
    }
}
