package com.alttab.camfind.util;

import java.io.File;

import android.app.Activity;
import android.content.Context;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.alttab.camfind.R;
import com.androidquery.AQuery;
import com.androidquery.util.AQUtility;

public class LazyLoad {

	private Activity activity;
	private ImageView imageView;
	private ProgressBar progressBar;
	private AQuery aQuery;
	private Context context;

	public LazyLoad(Context context)
	{
		this.context=context;
		aQuery=new AQuery(this.context);
	}

	public LazyLoad(Activity activity)
	{
		this.activity=activity;
		aQuery=new AQuery(this.activity);
	}
	public void LoadImagesFromURI(ImageView imageView,String uri)
	{
		this.imageView = imageView;
		File file = new File(uri);
		aQuery.id(this.imageView).image(file, 100);
		
	}
	public void LoadImages(String url,ImageView imageView,ProgressBar progressBar)
	{
		this.imageView=imageView;
		this.progressBar=progressBar;
		aQuery.id(this.imageView).progress(this.progressBar).image(url, true, true, 0, R.drawable.app_icon, null, 0, 0.0f);

	}
	
	public void clearCache(Context context)
	{
		AQUtility.cleanCacheAsync(context);
	}

	public void clearCache(Activity activity)
	{
		AQUtility.cleanCacheAsync(activity);
	}

}
