package com.alttab.camfind.activities;



import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.alttab.camfind.R;

public class WaitScreenActivity extends Activity {


	private Button btnContact,btnInfo;
	private TextView txtPeopleBehindValue,txtPeopleAheadValue;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.waitscreen);

		init();
		setupDefaults();
		setupEvents();
	}


	private void init() {
		btnContact			= (Button)findViewById(R.id.btnContact);
		btnInfo				= (Button)findViewById(R.id.btnMoreInfo);
		txtPeopleBehindValue= (TextView)findViewById(R.id.txtPeopleBehindValue);
		txtPeopleAheadValue	= (TextView)findViewById(R.id.txtPeopleAheadValue);
	}

	private void setupDefaults() {
		txtPeopleAheadValue	.setText("");
		txtPeopleBehindValue.setText("");
	}

	private void setupEvents() {

		btnContact	.setOnClickListener(myClickListener);
		btnInfo		.setOnClickListener(myClickListener);

	}


	OnClickListener myClickListener =new OnClickListener()
	{
		@Override
		public void onClick(View v) {

			if(v.getId()==R.id.btnContact)
			{
				Intent intent = new Intent(Intent.ACTION_SEND);
				intent.putExtra(Intent.EXTRA_EMAIL  , "");
				intent.putExtra(Intent.EXTRA_SUBJECT, "");
				intent.putExtra(Intent.EXTRA_TEXT   , "");
				intent.setType("message/rfc822");
				try 
				{
					startActivity(Intent.createChooser(intent, "Send mail..."));
				} 
				catch (android.content.ActivityNotFoundException ex) {
					Toast.makeText(WaitScreenActivity.this, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
				} 
			}
			else if(v.getId()==R.id.btnMoreInfo)
			{
				Intent i=new Intent(WaitScreenActivity.this,MoreInfoActivity.class);
				startActivity(i);
			}
		}
	};
}
