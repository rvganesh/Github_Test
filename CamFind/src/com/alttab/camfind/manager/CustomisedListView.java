package com.alttab.camfind.manager;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ListView;

public class CustomisedListView extends ListView {

	private float x,y;
	private float x1,y1;
	private boolean rv;
	public CustomisedListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

	}

	public CustomisedListView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public CustomisedListView(Context context) {
		super(context);
	}


	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) 
	{
		if(ev.getAction()==MotionEvent.ACTION_DOWN)
		{
			x = ev.getX();
			y = ev.getY();
			rv = false;
		}
		if(ev.getAction()==MotionEvent.ACTION_MOVE)
		{ 
			x1 = Math.abs(x-ev.getX());
			y1 = Math.abs(y-ev.getY());
			if(x1<10&&y1>20)
			{
				rv = true;

			}
			else
			{
				rv = false;
			}

			if(y1>50)
				rv = true;
			return rv;
		}

		return super.onInterceptTouchEvent(ev);
	}
}