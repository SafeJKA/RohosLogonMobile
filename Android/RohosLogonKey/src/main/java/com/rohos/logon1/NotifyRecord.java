package com.rohos.logon1;

import com.rohos.logon1.utils.AppLog;

public class NotifyRecord {

    private final String UNDEFINED = "undefined";

    private String mUserName = null;
    private String mComputerName = null;
    private String mType = null;
    private String mTitle = null;
    private String mText = null;
    private String mAttr = null;
    private String mBody = null;

    private long mTimeSent = 0L;

    public NotifyRecord (String notifyBody){
        mBody = notifyBody;
        parseNotifyBody();
    }

    public String getType(){
        return mType;
    }

    public String getText(){
        return mText;
    }

    /*public void setUserName(String value){
        mUserName = value;
    }*/

    public String getUserName(){
        return mUserName;
    }

    /*public void setComputerName(String value){
        mComputerName = value;
    }*/

    public String getComputerName(){
        return mComputerName;
    }

    public void setTitle(String value){
        mTitle = value;
    }

    public String getTitle(){
        return mTitle;
    }

    public void setTimeSent(long value){
        mTimeSent = value;
    }

    public long getTimeSent(){
        return mTimeSent;
    }

    public void setAttr(String value){
        mAttr = value;
    }

    public String getAttr(){
        return mAttr;
    }

    private void parseNotifyBody(){
        if(mBody == null){
            AppLog.log("NotifyRecord; Notification body is null");
            return;
        }

        String[] data = mBody.split(",");
        int length = data.length;
        switch(length){
            case 1:
                mType = data[0];
                mUserName = UNDEFINED;
                mComputerName = UNDEFINED;
                mText = UNDEFINED;
                break;
            case 2:
                mType = data[0];
                mUserName = data[1];
                mComputerName = UNDEFINED;
                mText = UNDEFINED;
                break;
            case 3:
                mType = data[0];
                mUserName = data[1];
                mComputerName = data[2];
                mText = UNDEFINED;
                break;
            case 4:
            default:
                mType = data[0];
                mUserName = data[1];
                mComputerName = data[2];
                mText = data[3];
        }
    }
}
