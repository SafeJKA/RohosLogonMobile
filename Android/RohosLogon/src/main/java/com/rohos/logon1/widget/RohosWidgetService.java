package com.rohos.logon1.widget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

public class RohosWidgetService extends RemoteViewsService {
	
	@Override
	public RemoteViewsFactory onGetViewFactory(Intent intent){
		return new StackRemoteViewsFactory(this.getApplicationContext(), intent);
	}
	
	/**
	 * This is the factory that will provide data to the collection widget.
	 */
	class StackRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory{
		private Context mContext;
	    private int mAppWidgetId;

	    public StackRemoteViewsFactory(Context context, Intent intent) {
	        mContext = context;
	        mAppWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
	                AppWidgetManager.INVALID_APPWIDGET_ID);
	    }
		
		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public RemoteViews getLoadingView() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public RemoteViews getViewAt(int position) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public int getViewTypeCount() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public boolean hasStableIds() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public void onCreate() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onDataSetChanged() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onDestroy() {
			// TODO Auto-generated method stub
			
		}
		
	}
}
