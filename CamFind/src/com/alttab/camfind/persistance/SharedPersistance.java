package com.alttab.camfind.persistance;


import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

public class SharedPersistance {

		
	private SharedPreferences sharedPreferences;
	private Editor mEditor;
	public Context context;
	
	
	private String REGISTER				= "register";
	private String DEVICETOKEN			= "devicetoken";
	
	public SharedPersistance(Context context){
		this.context=context;
		sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		mEditor = sharedPreferences.edit();
	}
		
	public void setRegister(boolean login)
	{
		mEditor.putBoolean(REGISTER,login);
		mEditor.commit();
	}
	
	public boolean getRegister(){
		return sharedPreferences.getBoolean(REGISTER, false);
	}

	public String getDeviceToken()
	{
		return sharedPreferences.getString(DEVICETOKEN, "");
	}
	
	public void setDeviceToken(String id){
		mEditor.putString(DEVICETOKEN,id);
		mEditor.commit();
	}
	
}