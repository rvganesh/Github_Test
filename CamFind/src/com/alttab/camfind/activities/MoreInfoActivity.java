package com.alttab.camfind.activities;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;

import com.alttab.camfind.R;

public class MoreInfoActivity extends Activity{


	private ImageView ivPrevious;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.moreinfo_screen);
		init();
		setupDefaults();
		setupEvents();

	}

	private void init()
	{
		ivPrevious=(ImageView)findViewById(R.id.ivPrevious);
	}

	private void setupDefaults(){
	}

	private void setupEvents(){
		ivPrevious.setOnClickListener(myClickListener);
	}

	OnClickListener myClickListener =new OnClickListener()
	{
		@Override
		public void onClick(View v) 
		{
			finish();
		}
	};
}
