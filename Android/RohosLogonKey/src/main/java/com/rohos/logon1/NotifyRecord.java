package com.rohos.logon1;

public class NotifyRecord {

    private String mUserName = null;
    private String mComputerName = null;
    private String mType = null;
    private String mTitle = null;
    private String mText = null;
    private String mAttr = null;

    private long mTimeSent = 0L;

    public NotifyRecord (String notifyBody){
        String[] data = notifyBody.split(",");
        mType = data[0];
        mUserName = data[1];
        mComputerName = data[2];
        mText = data[3];
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
}
