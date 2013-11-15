package com.alttab.camfind.activities;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import com.alttab.camfind.R;
import com.alttab.camfind.constants.Constants;
import com.alttab.camfind.oauth.OAuthAccessor;
import com.alttab.camfind.oauth.OAuthConsumer;
import com.alttab.camfind.oauth.OAuthException;
import com.alttab.camfind.oauth.OAuthMessage;
import com.alttab.camfind.persistance.SharedPersistance;
import com.common.ConnectionDetector;
import com.common.DeviceUtil;
import com.common.PrintLog;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

public class SplashActivity extends Activity 
{

	private String TAG ="SplashActivity";
	private static HashMap<String, String> map;
	private static Set<Entry<String, String>> entryset;

	private String authorizationHeader;

	private OAuthConsumer consumer;
	private OAuthAccessor accessor;
	private OAuthMessage message;

	private Handler handler;
	private SharedPersistance persistance;
	private ConnectionDetector connectionDetector;
	private boolean isNetworkFailure;


	@Override
	protected void onDestroy() {
		super.onDestroy();
		if(handler!=null){
			handler.removeCallbacks(startActivityRunnable);
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.splashscreen);
		init();
		setupDefaults();
		setupEvents();
	}

	private void init() {
		map 				= new HashMap<String, String>();
		handler 			= new Handler();
		persistance 		= new SharedPersistance(getApplicationContext());
		connectionDetector 	= new ConnectionDetector(getApplicationContext());
	}	


	private void setupDefaults() 
	{
		if(persistance.getRegister())
		{
			handler.postDelayed(startActivityRunnable, 2000);
		}
		else
		{
			if(connectionDetector.isConnectingToInternet())
			{
				registerOnCamfind();
			}
			else
			{
				isNetworkFailure = true;
				handler.postDelayed(startActivityRunnable, 1000);
			}
				
		}
	}

	private void setupEvents() {

	}

	private void registerOnCamfind(){

		String deviceId = persistance.getDeviceToken();
		if(deviceId.equals(""))
		{
			deviceId = DeviceUtil.getUUID(getApplicationContext());
			persistance.setDeviceToken(deviceId);
		}
		map.put(Constants._PARAMS_DEVICE_ID,deviceId);
		entryset = map.entrySet();

		consumer = new OAuthConsumer(Constants.CONSUMERKEY, Constants.SECRETKEY);		
		accessor = new OAuthAccessor(consumer);

		message = new OAuthMessage(Constants.GETREQUESTMETHOD, Constants.CAMFIND_STATUS_URL, entryset);

		try {
			message.addRequiredParameters(accessor);
			authorizationHeader = message.getAuthorizationHeader();
			PrintLog.debug(TAG,"authorizationHeader: "+authorizationHeader);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (OAuthException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}

		AsyncHttpClient asyncHttpClient = new AsyncHttpClient();
		asyncHttpClient.addHeader(Constants._AUTHORIZATION, authorizationHeader);
		RequestParams requestParams = new RequestParams();
		requestParams.put(Constants._PARAMS_DEVICE_ID, deviceId + "");

		asyncHttpClient.get(Constants.CAMFIND_STATUS_URL,
				requestParams, new AsyncHttpResponseHandler(){

			@Override
			public void onStart() {
				super.onStart();
				PrintLog.debug(TAG, "start");
			}


			@Override
			public void onSuccess(String content) {
				super.onSuccess(content);
				PrintLog.debug(TAG, "Content: "+content);
				persistance.setRegister(true);
			}

			@Override
			public void onFailure(Throwable error, String content) {
				super.onFailure(error, content);
				PrintLog.debug(TAG, "Failure Content: "+content);
				isNetworkFailure = true;
			}

			@Override
			public void onFinish() {
				super.onFinish();
				handler.post(startActivityRunnable);
				PrintLog.debug(TAG, "finish");
			}
		});
	}


	Runnable startActivityRunnable = new Runnable() {
		@Override
		public void run() 
		{
			if(isNetworkFailure)
				startActivity(new Intent(SplashActivity.this,NetworkErrorActivity.class));
			else
				startActivity(new Intent(SplashActivity.this,Camera.class));
			finish();
		}
	};
}
