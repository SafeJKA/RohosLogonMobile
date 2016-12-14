package com.rohos.logon1.widget;

import com.rohos.logon1.R;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;

public class RohosWidgetProvider extends AppWidgetProvider {
	
	private final String TAG = "RohosWidgetProvider";
	
	@Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager,
            int appWidgetId, Bundle newOptions){
		Log.d(TAG, "onAppWidgetOptionsChanged");
	}
	
	/**
     * Update all widgets in the list
     */
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds){
        super.onUpdate(context, appWidgetManager, appWidgetIds);

        for (int i = 0; i < appWidgetIds.length; ++i){
            updateWidget(context, appWidgetIds[i]);
            Log.d(TAG, "id " + i);
        }
        
        Log.d(TAG, "onUpdate");
    }
    
    @Override
    public void onDeleted(Context context, int[] appWidgetIds){
    	super.onDeleted(context, appWidgetIds);
    	Log.d(TAG, "onDelete");
    }
    
    @Override
    public void onReceive(Context context, Intent intent){
    	String action = intent.getAction();
    	
    	super.onReceive(context, intent);
    	Log.d(TAG, intent.getAction());
    }
    
    /**
     * Update the widget appWidgetId
     */
    private static void updateWidget(Context context, int appWidgetId){
    	RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget);
        PendingIntent clickIntent;
    	
        final Intent intent = new Intent(context, UnlockPcService.class);
        clickIntent = PendingIntent.getService(context, 0, intent, 0);
        remoteViews.setOnClickPendingIntent(R.id.unlock_pc, clickIntent);
        
    	AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, remoteViews);
    }
}
