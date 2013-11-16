/**
 * 
 */
package com.alttab.camfind.activities;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.media.AudioManager;
import android.media.CameraProfile;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.MessageQueue;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.alttab.camfind.R;
import com.alttab.camfind.adapter.SearchHistoryAdapter;
import com.alttab.camfind.bean.SearchBean;
import com.alttab.camfind.database.DBSQLite;
import com.alttab.camfind.device.CameraHolder;
import com.alttab.camfind.manager.CameraApplication;
import com.alttab.camfind.manager.CameraHardwareException;
import com.alttab.camfind.manager.CameraSettings;
import com.alttab.camfind.manager.ComboPreferences;
import com.alttab.camfind.manager.CustomisedListView;
import com.alttab.camfind.manager.FocusRectangle;
import com.alttab.camfind.manager.ImageManager;
import com.alttab.camfind.manager.MenuHelper;
import com.alttab.camfind.manager.NoSearchActivity;
import com.alttab.camfind.manager.OnScreenHint;
import com.alttab.camfind.manager.ParameterUtils;
import com.alttab.camfind.manager.PreviewFrameLayout;
import com.alttab.camfind.manager.ShutterButton;
import com.alttab.camfind.manager.Util;
import com.alttab.camfind.ui.CameraHeadUpDisplay;
import com.alttab.camfind.ui.GLRootView;
import com.alttab.camfind.ui.ZoomControllerListener;
import com.common.DateConversion;
import com.common.PrintLog;
import com.nineoldandroids.animation.ObjectAnimator;
public class Camera extends NoSearchActivity implements ShutterButton.OnShutterButtonListener, SurfaceHolder.Callback{

	private static final String TAG = "Camera Activity";
	
	private static final int CROP_MSG = 1;
	private static final int FIRST_TIME_INIT = 2;
	private static final int RESTART_PREVIEW = 3;
	private static final int CLEAR_SCREEN_DELAY = 4;
	private static final int SET_CAMERA_PARAMETERS_WHEN_IDLE = 5;
	private static final int UPDATE_PARAM_INITIALIZE = 1;
	private static final int UPDATE_PARAM_ZOOM = 2;
	private static final int UPDATE_PARAM_PREFERENCE = 4;
	private static final int UPDATE_PARAM_ALL = -1;

	private int mUpdateSet;

	private static final float DEFAULT_CAMERA_BRIGHTNESS = 0.7f;
	private static final int SCREEN_DELAY = 2 * 60 * 1000;
	private static final int FOCUS_BEEP_VOLUME = 100;

	private static final int ZOOM_STOPPED = 0;
	private static final int ZOOM_START = 1;
	private static final int ZOOM_STOPPING = 2;

	private int mZoomState = ZOOM_STOPPED;
	private boolean mSmoothZoomSupported = false;
	private int mZoomValue;  
	private int mTargetZoomValue;

	private Parameters mParameters;
	private Parameters mInitialParams;

	private int mOrientation = OrientationEventListener.ORIENTATION_UNKNOWN;
	private int mOrientationCompensation = 90;
	private ComboPreferences mPreferences;

	private static final int IDLE = 1;
	private static final int SNAPSHOT_IN_PROGRESS = 2;

	private int mStatus = IDLE;
	private static final String sTempCropFilename = "crop-temp";

	private android.hardware.Camera mCameraDevice;
	private ContentProviderClient mMediaProviderClient;
	private SurfaceView mSurfaceView;
	private SurfaceHolder mSurfaceHolder = null;
	private ShutterButton mShutterButton,mImageMenu,mImageHistory;
	private FocusRectangle mFocusRectangle;
	private ToneGenerator mFocusToneGenerator;
	private GestureDetector mGestureDetector;
	private boolean mStartPreviewFail = false;

	private GLRootView mGLRootView;

	private ImageView mImageMute,mImageSpeech,mImageTranslate,mImageAbout;
	private CustomisedListView listview_history;
	private SearchHistoryAdapter adapter;

	private ImageCapture mImageCapture = null;

	private boolean mPreviewing;
	private boolean mPausing;
	private boolean mFirstTimeInitialized;
	private boolean mIsImageCaptureIntent;

	private static final int FOCUS_NOT_STARTED = 0;
	private static final int FOCUSING = 1;
	private static final int FOCUSING_SNAP_ON_FINISH = 2;
	private static final int FOCUS_SUCCESS = 3;
	private static final int FOCUS_FAIL = 4;
	private int mFocusState = FOCUS_NOT_STARTED;

	private ContentResolver mContentResolver;
	private boolean mDidRegister = false;

	private final ShutterCallback mShutterCallback = new ShutterCallback();
	private final PostViewPictureCallback mPostViewPictureCallback =
			new PostViewPictureCallback();
	private final RawPictureCallback mRawPictureCallback =
			new RawPictureCallback();
	private final AutoFocusCallback mAutoFocusCallback =
			new AutoFocusCallback();
	private final PreviewFrameCallback mPreviewFrameCallback = new PreviewFrameCallback();
	private final ZoomListener mZoomListener = (Build.VERSION.SDK_INT >= 0x00000008) ? new ZoomListener() : null;

	/**
	 * Use the ErrorCallback to capture the crash count
	 * on the mediaserver
	 */
	private final ErrorCallback mErrorCallback = new ErrorCallback();

	private long mFocusStartTime;
	private long mFocusCallbackTime;
	private long mCaptureStartTime;
	private long mShutterCallbackTime;
	private long mPostViewPictureCallbackTime;
	private long mRawPictureCallbackTime;
	private long mJpegPictureCallbackTime;
	private int mPicturesRemaining;

	/**
	 * These latency time are for the CameraLatency test.
	 */
	public long mAutoFocusTime;
	public long mShutterLag;
	public long mShutterToPictureDisplayedTime;
	public long mPictureDisplayedToJpegCallbackTime;
	public long mJpegCallbackFinishTime;

	/**Add for test*/
	public static boolean mMediaServerDied = false;

	/**Focus mode. Options are pref_camera_focusmode_entryvalues.*/
	private String mFocusMode;
	private String mSceneMode;

	private final Handler mHandler = new MainHandler();
	private CameraHeadUpDisplay mHeadUpDisplay;

	private int mCameraId;
	private AudioManager mAudioManager;

	private DBSQLite db;
	private final int duration = 500;
	private ArrayList<SearchBean> searchBeansArrayList;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.camera);
		init();
		setupDefaults();
	}


	private void init()
	{
		db 					= new DBSQLite(this);
		mAudioManager 		= (AudioManager) getSystemService(AUDIO_SERVICE);
		mImageMute			= (ImageView)findViewById(R.id.iv_cmi_mute);
		mImageSpeech		= (ImageView)findViewById(R.id.iv_cmi_speech);
		mImageTranslate		= (ImageView)findViewById(R.id.iv_cmi_translate);
		mImageAbout			= (ImageView)findViewById(R.id.iv_cmi_about);

		searchBeansArrayList= new ArrayList<SearchBean>();
		listview_history	= (CustomisedListView)findViewById(R.id.listview_history);
		adapter				= new SearchHistoryAdapter(Camera.this,searchBeansArrayList);

		mSurfaceView 		= (SurfaceView) findViewById(R.id.camera_preview);
		mPreferences 		= new ComboPreferences(this);
		CameraSettings.upgradeGlobalPreferences(mPreferences.getGlobal());
		mCameraId 			= CameraSettings.readPreferredCameraId(mPreferences);
		mPreferences.setLocalId(this, mCameraId);
		CameraSettings.upgradeLocalPreferences(mPreferences.getLocal());
	}
	@SuppressWarnings("deprecation")
	private void setupDefaults()
	{

		hideMenu();
		listview_history.setAdapter(adapter);

		/** we need to reset exposure for the preview*/
		resetExposureCompensation();


		/**
		 * To reduce startup time, we start the preview in another thread.
		 * We make sure the preview is started at the end of onCreate.
		 */
		Thread startPreviewThread = new Thread(new Runnable() {
			public void run() {
				try {
					mStartPreviewFail = false;
					startPreview();
				} catch (CameraHardwareException e) {
					/**
					 * In eng build, we throw the exception so that test tool
					 * can detect it and report it
					 */
					if ("eng".equals(Build.TYPE)) {
						throw new RuntimeException(e);
					}
					mStartPreviewFail = true;
				}
			}
		});
		startPreviewThread.start();

		/**
		 * don't set mSurfaceHolder here. We have it set ONLY within
		 * surfaceChanged / surfaceDestroyed, other parts of the code
		 * assume that when it is set, the surface is also set.
		 */
		SurfaceHolder holder = mSurfaceView.getHolder();
		holder.addCallback(this);
		holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

		mIsImageCaptureIntent = isImageCaptureIntent();


		/**
		 * Make sure preview is started.
		 */
		try {
			startPreviewThread.join();
			if (mStartPreviewFail) {
				showCameraErrorAndFinish();
				return;
			}
		} catch (InterruptedException ex) {
			// ignore
		}
	}


	private void showMenu()
	{
		ObjectAnimator.ofFloat(mImageMute, "translationX",100,-220,-20).setDuration(duration).start();
		ObjectAnimator.ofFloat(mImageSpeech, "translationX",100,-180, -20).setDuration(duration).start();
		ObjectAnimator.ofFloat(mImageTranslate, "translationX",100, -140, -20).setDuration(duration).start();
		ObjectAnimator.ofFloat(mImageAbout, "translationX", 100, -100, -20).setDuration(duration).start();
	}
	private void hideMenu()
	{
		ObjectAnimator.ofFloat(mImageMute, "translationX", 200).setDuration(duration).start();
		ObjectAnimator.ofFloat(mImageSpeech, "translationX", 200).setDuration(duration).start();
		ObjectAnimator.ofFloat(mImageTranslate, "translationX", 200).setDuration(duration).start();
		ObjectAnimator.ofFloat(mImageAbout, "translationX", 200).setDuration(duration).start();
	}
	private boolean isMenuShow;
	private boolean isHistoryShow;

	@SuppressLint("HandlerLeak")
	public void onShutterButtonClick(ShutterButton button) {
		PrintLog.debug(TAG,"Id: -> "+button.getId()+": "+mPausing);
		if (mPausing) {
			return;
		}
		switch (button.getId()) {
		case R.id.shutter_button:
			doSnap();
			break;
		case R.id.iv_camera_menu:

			PrintLog.debug(TAG, "isHistoryshow: "+isHistoryShow);
			PrintLog.debug(TAG, "isMenuShow: "+isMenuShow);

			if(isHistoryShow)
			{
				showHistory(false);
				isHistoryShow =!isHistoryShow;
			}

			if(!isMenuShow)
				showMenu();
			else
				hideMenu();
			isMenuShow = !isMenuShow;
			break;
		case R.id.iv_camera_history:

			PrintLog.debug(TAG, "isHistoryshow: "+isHistoryShow);
			PrintLog.debug(TAG, "isMenuShow: 	"+isMenuShow);

			if(isMenuShow)
			{
				hideMenu();
				isMenuShow = !isMenuShow;
			}

			if(isHistoryShow)
				showHistory(false);
			else
				showHistory(true);

			isHistoryShow =!isHistoryShow;

			break;
		}

	}

	@SuppressLint("HandlerLeak")
	private class MainHandler extends Handler 
	{
		@Override
		public void handleMessage(Message msg) 
		{
			switch (msg.what) 
			{
			case RESTART_PREVIEW: 
			{
				PrintLog.debug(TAG, "Main Handler  Restart Preview ");
				restartPreview();
				if (mJpegPictureCallbackTime != 0) {
					long now = System.currentTimeMillis();
					mJpegCallbackFinishTime = now - mJpegPictureCallbackTime;
					PrintLog.debug(TAG, "mJpegCallbackFinishTime = "+ mJpegCallbackFinishTime + "ms");
					mJpegPictureCallbackTime = 0;
				}
				break;
			}

			case CLEAR_SCREEN_DELAY: 
			{
				PrintLog.debug(TAG, "Main Handler  clear screen delay");
				getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
				break;
			}

			case FIRST_TIME_INIT: {
				PrintLog.debug(TAG, "Main Handler  init firts time");
				initializeFirstTime();
				break;
			}

			case SET_CAMERA_PARAMETERS_WHEN_IDLE: 
			{
				PrintLog.debug(TAG, "Main Handler  set camerapaameter when idle");
				setCameraParametersWhenIdle(0);
				break;
			}
			}
		}
	}

	private void resetExposureCompensation() {
		PrintLog.debug(TAG, "resetExposureCompensation");
		String value = mPreferences.getString(CameraSettings.KEY_EXPOSURE,
				CameraSettings.EXPOSURE_DEFAULT_VALUE);
		if (!CameraSettings.EXPOSURE_DEFAULT_VALUE.equals(value)) {
			Editor editor = mPreferences.edit();
			editor.putString(CameraSettings.KEY_EXPOSURE, "0");
			editor.commit();
			if (mHeadUpDisplay != null) {
				mHeadUpDisplay.reloadPreferences();
			}
		}
	}

	private void keepMediaProviderInstance() {
		PrintLog.debug(TAG, "keepMediaProviderInstance");
		/**
		 * We want to keep a reference to MediaProvider in camera's lifecycle.
		 * ContentResolver calls.
		 */
		if (mMediaProviderClient == null) {
			mMediaProviderClient = getContentResolver()
					.acquireContentProviderClient(MediaStore.AUTHORITY);
		}
	}


	private ArrayList<SearchBean> tempArrayList;
	private void showHistory(boolean visibleAllItem)
	{

		searchBeansArrayList.clear();
		tempArrayList = db.getAllHistorys();
		if(tempArrayList.size()>0)
		{
			if(visibleAllItem)
			{
				for (int i = tempArrayList.size()-1; i >=0; i--) 
				{
					searchBeansArrayList.add(tempArrayList.get(i));
				}
			}
			else
			{
				searchBeansArrayList.add(tempArrayList.get(tempArrayList.size()-1));
			}
		}

		adapter.notifyDataSetChanged();
	}

	/**
	 * Snapshots can only be taken after this is called. It should be called
	 * once only. We could have done these things in onCreate() but we want
	 * make preview screen appear as soon as possible. 
	 */
	private void initializeFirstTime() 
	{
		PrintLog.debug(TAG, "initializeFirstTime");
		if (mFirstTimeInitialized) return;

		/**
		 * Create orientation listenter. This should be done first because it
		 * takes some time to get first orientation.
		 */
		keepMediaProviderInstance();
		checkStorage();

		/**
		 * Initialize last picture button.
		 */
		mContentResolver = getContentResolver();

		showHistory(false);

		/**
		 * Initialize shutter button.
		 */
		mShutterButton = (ShutterButton) findViewById(R.id.shutter_button);
		mShutterButton.setOnShutterButtonListener(this);
		mShutterButton.setVisibility(View.VISIBLE);

		mImageMenu 		= (ShutterButton)findViewById(R.id.iv_camera_menu);
		mImageMenu.setOnShutterButtonListener(this);
		mImageMenu.setVisibility(View.VISIBLE);

		mImageHistory		= (ShutterButton)findViewById(R.id.iv_camera_history);
		mImageHistory.setOnShutterButtonListener(this);
		mImageHistory.setVisibility(View.VISIBLE);

		mFocusRectangle = (FocusRectangle) findViewById(R.id.focus_rectangle);
		updateFocusIndicator();

		initializeScreenBrightness();
		installIntentFilter();
		initializeFocusTone();
		initializeZoom();
		mHeadUpDisplay = new CameraHeadUpDisplay(this);
		initializeHeadUpDisplay();
		mFirstTimeInitialized = true;
		changeHeadUpDisplayState();
		addIdleHandler();
		setInitialOrientation();
	}


	private void addIdleHandler() 
	{
		PrintLog.debug(TAG, "addIdleHandler");
		MessageQueue queue = Looper.myQueue();
		queue.addIdleHandler(new MessageQueue.IdleHandler() {
			public boolean queueIdle() {
				ImageManager.ensureOSXCompatibleFolder();
				return false;
			}
		});
	}

	/**
	 * If the activity is paused and resumed, this method will be called in
	 * onResume.
	 */
	private void initializeSecondTime() 
	{
		PrintLog.debug(TAG, "initializeSecondTime");
		/**
		 * Create orientation listenter. This should be done first because it
		 * takes some time to get first orientation.
		 */
		installIntentFilter();
		initializeFocusTone();
		initializeZoom();
		changeHeadUpDisplayState();

		keepMediaProviderInstance();
		checkStorage();
	}

	private void initializeZoom() {
		if (!isZoomSupported()) return;

		mSmoothZoomSupported = isSmoothZoomSupported();
		mCameraDevice.setZoomChangeListener(mZoomListener);
	}

	private static final String KEY_ZOOM_SUPPORTED = "zoom-supported";
	private static final String KEY_SMOOTH_ZOOM_SUPPORTED = "smooth-zoom-supported";
	private static final String KEY_MAX_ZOOM = "max-zoom";
	private static final String TRUE = "true";
	private boolean isZoomSupported() {
		if (Build.VERSION.SDK_INT <= 0x00000007) {
			return false;
		}

		String str = mParameters.get(KEY_ZOOM_SUPPORTED);
		return (str != null && TRUE.equals(str) && getMaxZoom() > 0);
	}
	public boolean isSmoothZoomSupported() {
		if (Build.VERSION.SDK_INT <= 0x00000007) {
			return false;
		}

		String str = mParameters.get(KEY_SMOOTH_ZOOM_SUPPORTED);
		return (str != null && TRUE.equals(str));
	}
	public int getMaxZoom() {
		return mParameters.getInt(KEY_MAX_ZOOM);
	}

	private void onZoomValueChanged(int index) {
		if (mSmoothZoomSupported) {
			if (mTargetZoomValue != index && mZoomState != ZOOM_STOPPED) {
				mTargetZoomValue = index;
				if (mZoomState == ZOOM_START) {
					mZoomState = ZOOM_STOPPING;
					mCameraDevice.stopSmoothZoom();
				}
			} else if (mZoomState == ZOOM_STOPPED && mZoomValue != index) {
				mTargetZoomValue = index;
				mCameraDevice.startSmoothZoom(index);
				mZoomState = ZOOM_START;
			}
		} else {
			mZoomValue = index;
			setCameraParametersWhenIdle(UPDATE_PARAM_ZOOM);
		}
	}

	private float[] getZoomRatios() {
		if(!isZoomSupported()) return null;
		List<Integer> zoomRatios = mParameters.getZoomRatios();
		float result[] = new float[zoomRatios.size()];
		for (int i = 0, n = result.length; i < n; ++i) {
			result[i] = (float) zoomRatios.get(i) / 100f;
		}
		return result;
	}


	@Override
	public boolean dispatchTouchEvent(MotionEvent m) {
		if (!super.dispatchTouchEvent(m) && mGestureDetector != null) {
			return mGestureDetector.onTouchEvent(m);
		}
		return true;
	}

	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) 
		{
			PrintLog.debug(TAG, "BroadcastReceiver");
			String action = intent.getAction();
			if (action.equals(Intent.ACTION_MEDIA_MOUNTED)
					|| action.equals(Intent.ACTION_MEDIA_UNMOUNTED)
					|| action.equals(Intent.ACTION_MEDIA_CHECKING)) {
				checkStorage();
			} else if (action.equals(Intent.ACTION_MEDIA_SCANNER_FINISHED)) {
				checkStorage();
			}
		}
	};

	private final class ShutterCallback
	implements android.hardware.Camera.ShutterCallback {
		public void onShutter() {
			PrintLog.debug(TAG, "ShutterCallback");
			mShutterCallbackTime = System.currentTimeMillis();
			mShutterLag = mShutterCallbackTime - mCaptureStartTime;
			PrintLog.debug(TAG, "mShutterLag = " + mShutterLag + "ms");
			clearFocusState();
		}
	}

	private final class PostViewPictureCallback implements PictureCallback {
		public void onPictureTaken(
				byte [] data, android.hardware.Camera camera) {
			PrintLog.debug(TAG, "PostViewPictureCallback ");
			mPostViewPictureCallbackTime = System.currentTimeMillis();
			PrintLog.debug(TAG, "mShutterToPostViewCallbackTime = "
					+ (mPostViewPictureCallbackTime - mShutterCallbackTime)
					+ "ms");
		}
	}

	private final class RawPictureCallback implements PictureCallback {
		public void onPictureTaken(
				byte [] rawData, android.hardware.Camera camera) {
			PrintLog.debug(TAG, "RawPictureCallback ");
			mRawPictureCallbackTime = System.currentTimeMillis();
			PrintLog.debug(TAG, "mShutterToRawCallbackTime = "
					+ (mRawPictureCallbackTime - mShutterCallbackTime) + "ms");
		}
	}

	private final class JpegPictureCallback implements PictureCallback {

		public JpegPictureCallback() 
		{
			PrintLog.debug(TAG, "JpegPictureCallback");
		}

		public void onPictureTaken(final byte [] jpegData, final android.hardware.Camera camera) {
			PrintLog.debug(TAG, "onPictureTaken");
			if (isSoundFXDisabled()) {
				mAudioManager.setStreamMute(AudioManager.STREAM_SYSTEM, false);
			}

			if (mPausing) {
				return;
			}


			mJpegPictureCallbackTime = System.currentTimeMillis();

			/**
			 * If postview callback has arrived, the captured image is displayed
			 * in postview callback. If not, the captured image is displayed in
			 * raw picture callback.
			 */
			if (mPostViewPictureCallbackTime != 0) {
				mShutterToPictureDisplayedTime =
						mPostViewPictureCallbackTime - mShutterCallbackTime;
				mPictureDisplayedToJpegCallbackTime =
						mJpegPictureCallbackTime - mPostViewPictureCallbackTime;
			} else {
				mShutterToPictureDisplayedTime =
						mRawPictureCallbackTime - mShutterCallbackTime;
				mPictureDisplayedToJpegCallbackTime =
						mJpegPictureCallbackTime - mRawPictureCallbackTime;
			}
			PrintLog.debug(TAG, "mPictureDisplayedToJpegCallbackTime = "
					+ mPictureDisplayedToJpegCallbackTime + "ms");
			mHeadUpDisplay.setEnabled(true);

			if (!mIsImageCaptureIntent) 
			{
				/**
				 * We want to show the taken picture for a while, so we wait
				 * for at least 1.2 second before restarting the preview.
				 */
				long delay = ((CameraHolder.instance().isFrontFacing(mCameraId)) ? 1200 : 400) - mPictureDisplayedToJpegCallbackTime;
				if (delay < 0) {
					restartPreview();
				} else {
					mHandler.sendEmptyMessageDelayed(RESTART_PREVIEW, delay);
				}
			}
			mImageCapture.storeImage(jpegData, camera);

			/**
			 * Calculate this in advance of each shot so we don't add to shutter
			 * latency. It's true that someone else could write to the SD card in
			 * the mean time and fill it, but that could have happened between the
			 * shutter press and saving the JPEG too.
			 */
			calculatePicturesRemaining();

			if (mPicturesRemaining < 1) {
				updateStorageHint(mPicturesRemaining);
			}

			if (!mHandler.hasMessages(RESTART_PREVIEW)) {
				long now = System.currentTimeMillis();
				mJpegCallbackFinishTime = now - mJpegPictureCallbackTime;
				PrintLog.debug(TAG, "mJpegCallbackFinishTime = "
						+ mJpegCallbackFinishTime + "ms");
				mJpegPictureCallbackTime = 0;
			}

			PrintLog.debug(TAG, "After : "+mIsImageCaptureIntent+": "+mAnimationDone);

		}
	}

	private final class PreviewFrameCallback implements PreviewCallback {
		public PreviewFrameCallback() {}

		@Override
		public void onPreviewFrame(byte[] data, android.hardware.Camera camera) {
			mImageCapture.capture();
		}

	}

	private final class AutoFocusCallback implements android.hardware.Camera.AutoFocusCallback 
	{
		public void onAutoFocus(boolean focused, android.hardware.Camera camera) 
		{

			PrintLog.debug(TAG, "AutoFocusCallback");
			mFocusCallbackTime = System.currentTimeMillis();
			mAutoFocusTime = mFocusCallbackTime - mFocusStartTime;
			if (mFocusState == FOCUSING_SNAP_ON_FINISH) 
			{
				/**
				 * Take the picture no matter focus succeeds or fails. No need
				 * to play the AF sound if we're about to play the shutter
				 * sound.
				 */
				if (focused) {
					mFocusState = FOCUS_SUCCESS;
				} else {
					mFocusState = FOCUS_FAIL;
				}
				mImageCapture.onSnap();
			} else if (mFocusState == FOCUSING) {
				/**
				 * User is half-pressing the focus key. Play the focus tone.
				 * Do not take the picture now.
				 */
				if (!isSoundFXDisabled()) {
					ToneGenerator tg = mFocusToneGenerator;
					if (tg != null) {
						tg.startTone(ToneGenerator.TONE_PROP_BEEP2);
					}
				}
				if (focused) {
					mFocusState = FOCUS_SUCCESS;
				} else {
					mFocusState = FOCUS_FAIL;
				}
			} else if (mFocusState == FOCUS_NOT_STARTED) 
			{
				/**
				 * User has released the focus key before focus completes.
				 * Do nothing.
				 */
			}
			updateFocusIndicator();
		}
	}

	private static final class ErrorCallback
	implements android.hardware.Camera.ErrorCallback {
		public void onError(int error, android.hardware.Camera camera) {
			if (error == android.hardware.Camera.CAMERA_ERROR_SERVER_DIED) {
				mMediaServerDied = true;
				PrintLog.debug(TAG, "media server died");
			}
		}
	}

	private final class ZoomListener
	implements android.hardware.Camera.OnZoomChangeListener {
		public void onZoomChange(
				int value, boolean stopped, android.hardware.Camera camera) {
			PrintLog.debug(TAG, "Zoom changed: value=" + value + ". stopped="+ stopped);
			mZoomValue = value;
			/**
			 * Keep mParameters up to date. We do not getParameter again in
			 * takePicture. If we do not do this, wrong zoom value will be set.
			 */
			mParameters.setZoom(value);
			/**
			 * We only care if the zoom is stopped. mZooming is set to true when
			 * we start smooth zoom.
			 */
			if (stopped && mZoomState != ZOOM_STOPPED) {
				if (value != mTargetZoomValue) {
					mCameraDevice.startSmoothZoom(mTargetZoomValue);
					mZoomState = ZOOM_START;
				} else {
					mZoomState = ZOOM_STOPPED;
				}
			}
		}
	}

	private class ImageCapture {

		private Uri mLastContentUri;


		/**
		 * Returns the rotation degree in the jpeg header.
		 */
		private int storeImage(byte[] data) 
		{
			try {
				PrintLog.debug(TAG, "storeImage DATA");
				long dateTaken = System.currentTimeMillis();
				String title = createName(dateTaken);
				String filename = title + ".jpg";
				int[] degree = new int[1];
				mLastContentUri = ImageManager.addImage(mContentResolver,title,dateTaken,null, ImageManager.CAMERA_IMAGE_BUCKET_NAME, filename,null, data,degree);
				return degree[0];
			} catch (Exception ex) {
				PrintLog.error(TAG, "Exception while compressing image."+ ex);
				return 0;
			}
		}
		public String getRealPathFromURI(Uri uri) {
			Cursor cursor = getContentResolver().query(uri, null, null, null, null); 
			cursor.moveToFirst(); 
			int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
			return cursor.getString(idx); 
		}
		public String getRealThumbnailPathFromURI(Uri uri)
		{

			Cursor cursor = MediaStore.Images.Thumbnails.queryMiniThumbnail(
					getContentResolver(), Long.parseLong(uri.getLastPathSegment()),
					MediaStore.Images.Thumbnails.MINI_KIND,
					null );
			if( cursor != null && cursor.getCount() > 0 ) {
				cursor.moveToFirst();//**EDIT**
				String path = cursor.getString( cursor.getColumnIndex( MediaStore.Images.Thumbnails.DATA ) );

				return path;
			}
			return "";
		}
		@SuppressWarnings("unused")
		public int storeImage(final byte[] data,android.hardware.Camera camera) {
			PrintLog.debug(TAG, "storeImage BYTE ARRAY");
			if (!mIsImageCaptureIntent) 
			{
				int degree = storeImage(data);
				PrintLog.debug(TAG, "Degree: "+degree);
				sendBroadcast(new Intent("com.android.camera.NEW_PICTURE", mLastContentUri));

				Bitmap bitmap = MediaStore.Images.Thumbnails.getThumbnail(
						getContentResolver(), Long.parseLong(mLastContentUri.getLastPathSegment()),
						MediaStore.Images.Thumbnails.MINI_KIND,
						(BitmapFactory.Options) null );
				Cursor cursor = MediaStore.Images.Thumbnails.queryMiniThumbnail(
						getContentResolver(), Long.parseLong(mLastContentUri.getLastPathSegment()),
						MediaStore.Images.Thumbnails.MINI_KIND,
						null );

				String imagePath =getRealPathFromURI(mLastContentUri);
				String imageThumbnailPath =getRealThumbnailPathFromURI(mLastContentUri);
				String imageTime =DateConversion.getCurrentDateTime();

				PrintLog.debug(TAG, "ImagePath: "+imagePath);
				PrintLog.debug(TAG, "ImageThumbnailPath: "+imageThumbnailPath);
				PrintLog.debug(TAG, "ImageTime: "+imageTime);

				db.addHistory(new SearchBean(imageTime, "Not identify yet.",imagePath,imageThumbnailPath));
				showHistory(false);

				return degree;
			}
			return 0;
		}

		/**
		 * Initiate the capture of an image.
		 */
		public void initiate() 
		{
			if (mCameraDevice == null) {
				return;
			}

			mCameraDevice.setOneShotPreviewCallback(mPreviewFrameCallback);
		}


		private void capture() {
			/**
			 * See android.hardware.Camera.Parameters.setRotation for
			 * documentation.
			 */
			int rotation = 0;
			if (mOrientation != OrientationEventListener.ORIENTATION_UNKNOWN) {
				CameraHolder holder = CameraHolder.instance();
				if (holder.isFrontFacing(mCameraId)) {
					rotation = (holder.getCameraOrientation(mCameraId, mOrientation) - mOrientation + 360) % 360;
				} else {
					rotation = (holder.getCameraOrientation(mCameraId, mOrientation) + mOrientation) % 360;
				}
			}
			mParameters.setRotation(rotation);

			/**
			 * Clear previous GPS location from the parameters.
			 */
			mParameters.removeGpsData();

			/**
			 * We always encode GpsTimeStamp
			 */
			mParameters.setGpsTimestamp(System.currentTimeMillis() / 1000);

			mCameraDevice.setParameters(mParameters);

			if (isSoundFXDisabled()) {
				mAudioManager.setStreamMute(AudioManager.STREAM_SYSTEM, true);
			}

			mCameraDevice.takePicture(mShutterCallback, mRawPictureCallback,
					mPostViewPictureCallback, new JpegPictureCallback());
			mPreviewing = false;
		}

		public void onSnap() 
		{
			/**
			 * If we are already in the middle of taking a snapshot then ignore.
			 */
			if (mPausing || mStatus == SNAPSHOT_IN_PROGRESS) {
				return;
			}
			mCaptureStartTime = System.currentTimeMillis();
			mPostViewPictureCallbackTime = 0;
			mHeadUpDisplay.setEnabled(false);
			mStatus = SNAPSHOT_IN_PROGRESS;

			mImageCapture.initiate();
		}

	}

	public boolean isSoundFXDisabled() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		return prefs.getBoolean("disable_shutter_sound", true);
	}

	public boolean isPreviewAnimationDisable() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		return prefs.getBoolean("disable_preview_animation", true);
	}

	public boolean saveDataToFile(String filePath, byte[] data) {
		FileOutputStream f = null;
		try {
			f = new FileOutputStream(filePath);
			f.write(data);
		} catch (IOException e) {
			return false;
		} finally {
			MenuHelper.closeSilently(f);
		}
		return true;
	}


	@SuppressLint("SimpleDateFormat")
	private String createName(long dateTaken) {
		Date date = new Date(dateTaken);
		SimpleDateFormat dateFormat = new SimpleDateFormat(getString(R.string.image_file_name_format));

		return dateFormat.format(date);
	}


	private void changeHeadUpDisplayState() {
		/**
		 * If the camera resumes behind the lock screen, the orientation
		 * will be portrait. That causes OOM when we try to allocation GPU
		 * memory for the GLSurfaceView again when the orientation changes. So,
		 * we delayed initialization of HeadUpDisplay until the orientation
		 * becomes landscape.
		 */
		Configuration config = getResources().getConfiguration();
		if (config.orientation == Configuration.ORIENTATION_LANDSCAPE
				&& !mPausing && mFirstTimeInitialized) {
			if (mGLRootView == null) attachHeadUpDisplay();
		} else if (mGLRootView != null) {
			detachHeadUpDisplay();
		}
	}

	private void overrideHudSettings(final String flashMode,
			final String whiteBalance, final String focusMode) {
		mHeadUpDisplay.overrideSettings(
				CameraSettings.KEY_FLASH_MODE, flashMode,
				CameraSettings.KEY_WHITE_BALANCE, whiteBalance,
				CameraSettings.KEY_FOCUS_MODE, focusMode);
	}

	private void updateSceneModeInHud() {
		/**
		 * If scene mode is set, we cannot set flash mode, white balance, and
		 * focus mode, instead, we read it from driver
		 */
		if (!Parameters.SCENE_MODE_AUTO.equals(mSceneMode)) {
			overrideHudSettings(mParameters.getFlashMode(),
					mParameters.getWhiteBalance(), mParameters.getFocusMode());
		} else {
			overrideHudSettings(null, null, null);
		}
	}

	private void initializeHeadUpDisplay() {
		CameraSettings settings = new CameraSettings(this, mInitialParams,
				CameraHolder.instance());
		mHeadUpDisplay.initialize(this,
				settings.getPreferenceGroup(R.xml.camera_preferences),
				getZoomRatios(), mOrientationCompensation);
		if (isZoomSupported()) {
			mHeadUpDisplay.setZoomListener(new ZoomControllerListener() {
				public void onZoomChanged(
						int index, float ratio, boolean isMoving) {
					onZoomValueChanged(index);
				}
			});
		}
		updateSceneModeInHud();
	}


	private void attachHeadUpDisplay() {
		mHeadUpDisplay.setOrientation(mOrientationCompensation);
		if (isZoomSupported()) {
			mHeadUpDisplay.setZoomIndex(mZoomValue);
		}
		FrameLayout frame = (FrameLayout) findViewById(R.id.frame);
		mGLRootView = new GLRootView(this);
		mGLRootView.setContentPane(mHeadUpDisplay);
		frame.addView(mGLRootView);
	}

	private void detachHeadUpDisplay() {
		mHeadUpDisplay.collapse();
		((ViewGroup) mGLRootView.getParent()).removeView(mGLRootView);
		mGLRootView = null;
	}

	public static int roundOrientation(int orientation) {
		return ((orientation + 45) / 90 * 90) % 360;
	}


	private void setInitialOrientation() {
		PrintLog.debug(TAG, "setInitialOrientation");
		int orientation = ((CameraApplication)getApplication()).getLastKnownOrientation();
		PrintLog.debug(TAG, "setInitialOrientation : "+orientation+" = default: "+android.view.OrientationEventListener.ORIENTATION_UNKNOWN);
		mOrientation = roundOrientation(orientation);
		PrintLog.debug(TAG, "mOrinen:"+mOrientation);
		/**
		 * When the screen is unlocked, display rotation may change. Always
		 * calculate the up-to-date orientationCompensation.
		 */
		int orientationCompensation = mOrientation+ Util.getDisplayRotation(Camera.this);

		PrintLog.debug(TAG, "orientationCompensation :"+orientationCompensation );
		if (mOrientationCompensation != orientationCompensation) {
			mOrientationCompensation = orientationCompensation;
			mHeadUpDisplay.setOrientation(mOrientationCompensation);
		}
	}

	@Override
	public void onStop() {
		super.onStop();
		if (mMediaProviderClient != null) {
			mMediaProviderClient.release();
			mMediaProviderClient = null;
		}
	}

	private void checkStorage() {
		calculatePicturesRemaining();
		updateStorageHint(mPicturesRemaining);
	}


	private void doCancel() {
		setResult(RESULT_CANCELED, new Intent());
		finish();
	}

	public void onShutterButtonFocus(ShutterButton button, boolean pressed) {
		if (mPausing) {
			return;
		}
		switch (button.getId()) {
		case R.id.shutter_button:
			doFocus(pressed);
			break;
		}
	}


	private OnScreenHint mStorageHint;

	private void updateStorageHint(int remaining) {
		String noStorageText = null;

		if (remaining == MenuHelper.NO_STORAGE_ERROR) {
			String state = Environment.getExternalStorageState();
			if (state == Environment.MEDIA_CHECKING) {
				noStorageText = getString(R.string.preparing_sd);
			} else {
				noStorageText = getString(R.string.no_storage);
			}
		} else if (remaining == MenuHelper.CANNOT_STAT_ERROR) {
			noStorageText = getString(R.string.access_sd_fail);
		} else if (remaining < 1) {
			noStorageText = getString(R.string.not_enough_space);
		}

		if (noStorageText != null) {
			if (mStorageHint == null) {
				mStorageHint = OnScreenHint.makeText(this, noStorageText);
			} else {
				mStorageHint.setText(noStorageText);
			}
			mStorageHint.show();
		} else if (mStorageHint != null) {
			mStorageHint.cancel();
			mStorageHint = null;
		}
	}

	private void installIntentFilter() {
		/**
		 * install an intent filter to receive SD card related events.
		 */
		IntentFilter intentFilter =
				new IntentFilter(Intent.ACTION_MEDIA_MOUNTED);
		intentFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
		intentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
		intentFilter.addAction(Intent.ACTION_MEDIA_CHECKING);
		intentFilter.addDataScheme("file");
		registerReceiver(mReceiver, intentFilter);
		mDidRegister = true;
	}

	private void initializeFocusTone() {
		/**
		 * Initialize focus tone generator.
		 */
		try {
			mFocusToneGenerator = new ToneGenerator(
					AudioManager.STREAM_SYSTEM, FOCUS_BEEP_VOLUME);
		} catch (Throwable ex) {
			Log.w(TAG, "Exception caught while creating tone generator: ", ex);
			mFocusToneGenerator = null;
		}
	}

	public static final String SCREEN_BRIGHTNESS_MODE = "screen_brightness_mode";
	public static final int SCREEN_BRIGHTNESS_MODE_AUTOMATIC = 0x00000001;
	public static final int SCREEN_BRIGHTNESS_MODE_MANUAL = 0x00000000;
	private void initializeScreenBrightness() {
		Window win = getWindow();
		/**
		 * Overright the brightness settings if it is automatic
		 */
		int mode;
		if (Build.VERSION.SDK_INT >= 0x00000008) {
			mode = Settings.System.getInt(
					getContentResolver(),
					Settings.System.SCREEN_BRIGHTNESS_MODE,
					Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
			if (mode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
				WindowManager.LayoutParams winParams = win.getAttributes();
				winParams.screenBrightness = DEFAULT_CAMERA_BRIGHTNESS;
				win.setAttributes(winParams);
			}
		} else {
			mode = Settings.System.getInt(
					getContentResolver(),
					SCREEN_BRIGHTNESS_MODE,
					SCREEN_BRIGHTNESS_MODE_MANUAL);
			if (mode == SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
				WindowManager.LayoutParams winParams = win.getAttributes();
				winParams.screenBrightness = DEFAULT_CAMERA_BRIGHTNESS;
				win.setAttributes(winParams);
			}
		}
	}

	@Override
	protected void onResume() {
		PrintLog.debug(TAG, "onResume");
		super.onResume();
		((CameraApplication)getApplication()).requestLocationUpdate(false);

		mPausing = false;
		mJpegPictureCallbackTime = 0;
		mZoomValue = 0;
		mImageCapture = new ImageCapture();

		/**
		 * Start the preview if it is not started.
		 */
		if (!mPreviewing && !mStartPreviewFail) {
			resetExposureCompensation();
			if (!restartPreview()) return;
		}

		if (mSurfaceHolder != null) {
			/**
			 * If first time initialization is not finished, put it in the
			 * message queue.
			 */
			if (!mFirstTimeInitialized) {
				mHandler.sendEmptyMessage(FIRST_TIME_INIT);
			} else {
				initializeSecondTime();
			}
		}
		keepScreenOnAwhile();
	}

	@Override
	public void onConfigurationChanged(Configuration config) {
		PrintLog.debug(TAG, "onConfigurationChanged");
		super.onConfigurationChanged(config);
		changeHeadUpDisplayState();
	}


	@Override
	protected void onPause() {
		mPausing = true;

		mAudioManager.setStreamMute(AudioManager.STREAM_SYSTEM, false);

		stopPreview();
		/**
		 * Close the camera now because other activities may need to use it.
		 */
		closeCamera();
		resetScreenOn();
		changeHeadUpDisplayState();

		if (mDidRegister) {
			unregisterReceiver(mReceiver);
			mDidRegister = false;
		}

		if (mFocusToneGenerator != null) {
			mFocusToneGenerator.release();
			mFocusToneGenerator = null;
		}

		if (mStorageHint != null) {
			mStorageHint.cancel();
			mStorageHint = null;
		}

		/**
		 * If we are in an image capture intent and has taken
		 * a picture, we just clear it in onPause.
		 */
		mImageCapture = null;

		/**
		 * Remove the messages in the event queue.
		 */
		mHandler.removeMessages(RESTART_PREVIEW);
		mHandler.removeMessages(FIRST_TIME_INIT);

		super.onPause();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) 
	{
		PrintLog.debug(TAG, "onActivityResult");
		if (requestCode == CROP_MSG) {
			Intent intent = new Intent();
			if (data != null) {
				Bundle extras = data.getExtras();
				if (extras != null) {
					intent.putExtras(extras);
				}
			}
			setResult(resultCode, intent);
			finish();

			File path = getFileStreamPath(sTempCropFilename);
			path.delete();
		}
	}

	private boolean canTakePicture() {
		return isCameraIdle() && mPreviewing && (mPicturesRemaining > 0);
	}

	private void autoFocus() 
	{
		/**
		 * Initiate autofocus only when preview is started and snapshot is not
		 * in progress.
		 */
		PrintLog.debug(TAG, "AutoFoucs");
		if (canTakePicture()) {
			mHeadUpDisplay.setEnabled(false);
			PrintLog.debug(TAG, "Start autofocus.");
			mFocusStartTime = System.currentTimeMillis();
			mFocusState = FOCUSING;
			updateFocusIndicator();
			mCameraDevice.autoFocus(mAutoFocusCallback);
		}
	}

	private void cancelAutoFocus() {
		/**
		 * User releases half-pressed focus key.
		 */
		if (mStatus != SNAPSHOT_IN_PROGRESS && (mFocusState == FOCUSING
				|| mFocusState == FOCUS_SUCCESS || mFocusState == FOCUS_FAIL)) {
			PrintLog.debug(TAG, "Cancel autofocus.");
			mHeadUpDisplay.setEnabled(true);
			mCameraDevice.cancelAutoFocus();
		}
		if (mFocusState != FOCUSING_SNAP_ON_FINISH) {
			clearFocusState();
		}
	}

	private void clearFocusState() {
		mFocusState = FOCUS_NOT_STARTED;
		updateFocusIndicator();
	}

	private void updateFocusIndicator() {
		if (mFocusRectangle == null) return;

		if (mFocusState == FOCUSING || mFocusState == FOCUSING_SNAP_ON_FINISH) {
			mFocusRectangle.showStart();
		} else if (mFocusState == FOCUS_SUCCESS) {
			mFocusRectangle.showSuccess();
		} else if (mFocusState == FOCUS_FAIL) {
			mFocusRectangle.showFail();
		} else {
			mFocusRectangle.clear();
		}
	}

	@Override
	public void onBackPressed() {
		if (!isCameraIdle()) 
		{
			/**
			 * ignore backs while we're taking a picture
			 */
			return;
		} 
		doCancel();
		super.onBackPressed();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_FOCUS:
			if (mFirstTimeInitialized && event.getRepeatCount() == 0) {
				doFocus(true);
			}
			return true;
		case KeyEvent.KEYCODE_CAMERA:
			if (mFirstTimeInitialized && event.getRepeatCount() == 0) {
				doSnap();
			}
			return true;
		case KeyEvent.KEYCODE_DPAD_CENTER:
			/**
			 * If we get a dpad center event without any focused view, move
			 * the focus to the shutter button and press it.
			 */
			if (mFirstTimeInitialized && event.getRepeatCount() == 0) {
				/**
				 * Start auto-focus immediately to reduce shutter lag. After
				 * the shutter button gets the focus, doFocus() will be
				 * called again but it is fine.
				 */
				if (mHeadUpDisplay.collapse()) return true;
				doFocus(true);
				if (mShutterButton.isInTouchMode()) {
					mShutterButton.requestFocusFromTouch();
				} else {
					mShutterButton.requestFocus();
				}
				mShutterButton.setPressed(true);
			}
			return true;
		}

		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_FOCUS:
			if (mFirstTimeInitialized)
			{
				doFocus(false);
			}
			return true;
		}
		return super.onKeyUp(keyCode, event);
	}

	private void doSnap() {
		PrintLog.debug(TAG, "doSnap: mHeadUpDisplay=" + mHeadUpDisplay.collapse());
		if (mHeadUpDisplay.collapse())
			return;

		/**
		 * If the user has half-pressed the shutter and focus is completed, we
		 * can take the photo right away. If the focus mode is infinity, we can
		 * also take the photo.
		 */
		if (mFocusMode.equals(Parameters.FOCUS_MODE_INFINITY)
				|| mFocusMode.equals(Parameters.FOCUS_MODE_FIXED)
				|| mFocusMode.equals(ParameterUtils.FOCUS_MODE_EDOF)
				|| (mFocusState == FOCUS_SUCCESS
				|| mFocusState == FOCUS_FAIL)) {
			PrintLog.debug(TAG, "doSnap: mImageCapture.onSnap ");
			mImageCapture.onSnap();
		} else if (mFocusState == FOCUSING) {
			/**
			 * Half pressing the shutter (i.e. the focus button event) will
			 * already have requested AF for us, so just request capture on
			 * focus here.
			 */
			mFocusState = FOCUSING_SNAP_ON_FINISH;
			PrintLog.debug(TAG, "doSnap: focusing");
		} else if (mFocusState == FOCUS_NOT_STARTED) {
			PrintLog.debug(TAG, "doSnap: FOCUS_NOT_STARTED");
			/**
			 * Focus key down event is dropped for some reasons. Just ignore.
			 */
		}
	}

	private void doFocus(boolean pressed) 
	{
		/**
		 * Do the focus if the mode is not infinity.
		 */
		if (mHeadUpDisplay.collapse()) return;
		if (!(mFocusMode.equals(Parameters.FOCUS_MODE_INFINITY)
				|| mFocusMode.equals(Parameters.FOCUS_MODE_FIXED)
				|| mFocusMode.equals(ParameterUtils.FOCUS_MODE_EDOF))) {
			if (pressed) {
				/**
				 * Focus key down.
				 */
				autoFocus();
			} else {  
				/**
				 * Focus key up.
				 */
				cancelAutoFocus();
			}
		}
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
		/**
		 * Make sure we have a surface in the holder before proceeding.
		 */
		// 
		if (holder.getSurface() == null) {
			return;
		}

		/**
		 * We need to save the holder for later use, even when the mCameraDevice
		 * is null. This could happen if onResume() is invoked after this
		 * function.
		 */
		mSurfaceHolder = holder;

		/**
		 * The mCameraDevice will be null if it fails to connect to the camera
		 * hardware. In this case we will show a dialog and then finish the
		 * activity, so it's OK to ignore it.
		 */
		if (mCameraDevice == null) return;

		/**
		 * Sometimes surfaceChanged is called after onPause or before onResume.
		 * Ignore it.
		 */
		if (mPausing || isFinishing()) return;

		if (mPreviewing && holder.isCreating()) 
		{
			/**
			 * Set preview display if the surface is being created and preview
			 * was already started. That means preview display was set to null
			 * and we need to set it now.
			 */
			setPreviewDisplay(holder);
		} else {
			/**
			 * 1. Restart the preview if the size of surface was changed. The
			 * framework may not support changing preview display on the fly.
			 * 2. Start the preview now if surface was destroyed and preview
			 * stopped.
			 * Set the preview frame aspect ratio according to the picture size.
			 */
			mHandler.sendEmptyMessageDelayed(RESTART_PREVIEW, 1000);
		}

		/**
		 * If first time initialization is not finished, send a message to do
		 * it later. We want to finish surfaceChanged as soon as possible to let
		 * user see preview first.
		 */
		if (!mFirstTimeInitialized) {
			mHandler.sendEmptyMessage(FIRST_TIME_INIT);
		} else {
			initializeSecondTime();
		}
	}

	public void surfaceCreated(SurfaceHolder holder) {
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		stopPreview();
		mSurfaceHolder = null;
	}

	private void closeCamera() {
		if (mCameraDevice != null) {
			CameraHolder.instance().release();
			if (Build.VERSION.SDK_INT >= 0x00000008) {
				mCameraDevice.setZoomChangeListener(null);
			}
			mCameraDevice = null;
			mPreviewing = false;
		}
	}

	private void ensureCameraDevice() throws CameraHardwareException 
	{
		if (mCameraDevice == null) {
			mCameraDevice = CameraHolder.instance().open(mCameraId);
			mInitialParams = mCameraDevice.getParameters();
			PrintLog.debug(TAG, "------------------> mCaremaId: "+mCameraId);	
		}
	}

	private void showCameraErrorAndFinish() {
		Resources ress = getResources();
		Util.showFatalErrorAndFinish(Camera.this,
				ress.getString(R.string.camera_error_title),
				ress.getString(R.string.cannot_connect_camera));
	}

	private boolean restartPreview() {
		try {
			startPreview();
		} catch (CameraHardwareException e) {
			showCameraErrorAndFinish();
			return false;
		}
		return true;
	}

	private boolean mAnimationDone = false;

	private void setPreviewDisplay(SurfaceHolder holder) {
		try {
			mCameraDevice.setPreviewDisplay(holder);

		} catch (Throwable ex) {
			closeCamera();
			throw new RuntimeException("setPreviewDisplay failed", ex);
		}
	}

	private void startPreview() throws CameraHardwareException {
		if (mPausing || isFinishing()) return;
		ensureCameraDevice();
		/**
		 * If we're previewing already, stop the preview first (this will blank
		 * the screen).
		 */
		if (mPreviewing) stopPreview();

		setPreviewDisplay(mSurfaceHolder);
		/**
		 *Util.setCameraDisplayOrientation(this, mCameraId, mCameraDevice); 
		 */
		setCameraParameters(UPDATE_PARAM_ALL);

		mCameraDevice.setErrorCallback(mErrorCallback);

		try {
			PrintLog.debug(TAG, "startPreview");
			mCameraDevice.startPreview();
		} catch (Throwable ex) {
			closeCamera();
			throw new RuntimeException("startPreview failed", ex);
		}
		mPreviewing = true;
		mZoomState = ZOOM_STOPPED;
		mStatus = IDLE;
	}

	private void stopPreview() {
		if (mCameraDevice != null && mPreviewing) {
			PrintLog.debug(TAG, "stopPreview");
			mCameraDevice.stopPreview();
		}
		mPreviewing = false;
		/**
		 * If auto focus was in progress, it would have been canceled.
		 */
		clearFocusState();
	}

	@SuppressWarnings("deprecation")
	private Size getOptimalPreviewSize(List<Size> sizes, double targetRatio) {
		final double ASPECT_TOLERANCE = 0.05;
		if (sizes == null) return null;

		Size optimalSize = null;
		double minDiff = Double.MAX_VALUE;

		/**
		 * Because of bugs of overlay and layout, we sometimes will try to
		 * layout the viewfinder in the portrait orientation and thus get the
		 * wrong size of mSurfaceView. When we change the preview size, the
		 * new overlay will be created before the old one closed, which causes
		 * an exception. For now, just get the screen size
		 */
		Display display = getWindowManager().getDefaultDisplay();
		int targetHeight = Math.min(display.getHeight(), display.getWidth());

		if (targetHeight <= 0) {
			/**
			 * We don't know the size of SurefaceView, use screen height
			 */
			WindowManager windowManager = (WindowManager)
					getSystemService(Context.WINDOW_SERVICE);
			targetHeight = windowManager.getDefaultDisplay().getHeight();
		}
		/**
		 * Try to find an size match aspect ratio and size
		 */
		for (Size size : sizes) {
			double ratio = (double) size.width / size.height;
			if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
			if (Math.abs(size.height - targetHeight) < minDiff) {
				optimalSize = size;
				minDiff = Math.abs(size.height - targetHeight);
			}
		}

		/**
		 * Cannot find the one match the aspect ratio, ignore the requirement
		 */
		if (optimalSize == null) {
			PrintLog.debug(TAG, "No preview size match the aspect ratio");
			minDiff = Double.MAX_VALUE;
			for (Size size : sizes) {
				if (Math.abs(size.height - targetHeight) < minDiff) {
					optimalSize = size;
					minDiff = Math.abs(size.height - targetHeight);
				}
			}
		}
		return optimalSize;
	}

	private static boolean isSupported(String value, List<String> supported) {
		return supported == null ? false : supported.indexOf(value) >= 0;
	}

	@SuppressWarnings("deprecation")
	private void updateCameraParametersInitialize() {
		/**
		 * Reset preview frame rate to the maximum because it may be lowered by
		 * video camera application.
		 */
		List<Integer> frameRates = mParameters.getSupportedPreviewFrameRates();
		if (frameRates != null) {
			Integer max = Collections.max(frameRates);
			mParameters.setPreviewFrameRate(max);
		}

	}

	private void updateCameraParametersZoom() {
		/**
		 * Set zoom.
		 */
		if (ParameterUtils.isZoomSupported(mParameters)) {
			ParameterUtils.setZoom(mParameters, mZoomValue);
		}
	}

	private void updateCameraParametersPreference() {
		/**
		 * Set picture size.
		 */
		String pictureSize = mPreferences.getString(
				CameraSettings.KEY_PICTURE_SIZE, null);
		if (pictureSize == null) {
			CameraSettings.initialCameraPictureSize(this, mParameters);
		} else {
			List<Size> supported = mParameters.getSupportedPictureSizes();
			CameraSettings.setCameraPictureSize(
					pictureSize, supported, mParameters);
		}

		/**
		 * Set the preview frame aspect ratio according to the picture size.
		 */
		PreviewFrameLayout frameLayout =
				(PreviewFrameLayout) findViewById(R.id.frame_layout);
		PrintLog.debug(TAG, "Ratio: "+CameraHolder.instance().getAspectRatio());
		frameLayout.setAspectRatio(CameraHolder.instance().getAspectRatio());
		/**
		 * Set a preview size that is closest to the viewfinder height and has
		 * the right aspect ratio.
		 */
		List<Size> sizes = mParameters.getSupportedPreviewSizes();
		Size size = mParameters.getPictureSize();
		Size optimalSize = getOptimalPreviewSize(
				sizes, (double) size.width / size.height);
		if (optimalSize != null) {
			Size original = mParameters.getPreviewSize();
			if (!original.equals(optimalSize)) {
				mParameters.setPreviewSize(optimalSize.width, optimalSize.height);

				/**
				 * Zoom related settings will be changed for different preview
				 * sizes, so set and read the parameters to get lastest values
				 */
				mCameraDevice.setParameters(mParameters);
				mParameters = mCameraDevice.getParameters();
			}
		}

		/**
		 * Since change scene mode may change supported values,
		 * Set scene mode first,
		 */
		mSceneMode = mPreferences.getString(
				CameraSettings.KEY_SCENE_MODE,
				getString(R.string.pref_camera_scenemode_default));
		if (isSupported(mSceneMode, mParameters.getSupportedSceneModes())) {
			if (!mParameters.getSceneMode().equals(mSceneMode)) {
				mParameters.setSceneMode(mSceneMode);
				mCameraDevice.setParameters(mParameters);

				/**
				 * Setting scene mode will change the settings of flash mode,
				 * white balance, and focus mode. Here we read back the
				 * parameters, so we can know those settings.
				 */
				mParameters = mCameraDevice.getParameters();
			}
		} else {
			mSceneMode = mParameters.getSceneMode();
			if (mSceneMode == null) {
				mSceneMode = Parameters.SCENE_MODE_AUTO;
			}
		}

		/**
		 * Set JPEG quality.
		 */
		String jpegQuality = mPreferences.getString(
				CameraSettings.KEY_JPEG_QUALITY,
				getString(R.string.pref_camera_jpegquality_default));
		mParameters.setJpegQuality(JpegEncodingQualityMappings.getQualityNumber(jpegQuality));

		/**
		 * For the following settings, we need to check if the settings are
		 * still supported by latest driver, if not, ignore the settings.
		 * Set color effect parameter.
		 */
		String colorEffect = mPreferences.getString(
				CameraSettings.KEY_COLOR_EFFECT,
				getString(R.string.pref_camera_coloreffect_default));
		if (isSupported(colorEffect, mParameters.getSupportedColorEffects())) {
			mParameters.setColorEffect(colorEffect);
		}

		/**
		 * Set exposure compensation
		 */
		String exposure = mPreferences.getString(
				CameraSettings.KEY_EXPOSURE,
				getString(R.string.pref_exposure_default));
		try {
			int value = Integer.parseInt(exposure);
			int max = ParameterUtils.getMaxExposureCompensation(mParameters);
			int min = ParameterUtils.getMinExposureCompensation(mParameters);
			if (value >= min && value <= max) {
				ParameterUtils.setExposureCompensation(mParameters, value);
			} else {
				Log.w(TAG, "invalid exposure range: " + exposure);
			}
		} catch (NumberFormatException e) {
			Log.w(TAG, "invalid exposure: " + exposure);
		}

		if (mHeadUpDisplay != null) updateSceneModeInHud();

		if (Parameters.SCENE_MODE_AUTO.equals(mSceneMode)) {
			/**
			 * Set flash mode.
			 */
			String flashMode = mPreferences.getString(
					CameraSettings.KEY_FLASH_MODE,
					getString(R.string.pref_camera_flashmode_default));
			List<String> supportedFlash = mParameters.getSupportedFlashModes();
			if (isSupported(flashMode, supportedFlash)) {
				mParameters.setFlashMode(flashMode);
			} else {
				flashMode = mParameters.getFlashMode();
				if (flashMode == null) {
					flashMode = getString(
							R.string.pref_camera_flashmode_no_flash);
				}
			}

			/**
			 * Set white balance parameter.
			 */
			String whiteBalance = mPreferences.getString(
					CameraSettings.KEY_WHITE_BALANCE,
					getString(R.string.pref_camera_whitebalance_default));
			if (isSupported(whiteBalance,
					mParameters.getSupportedWhiteBalance())) {
				mParameters.setWhiteBalance(whiteBalance);
			} else {
				whiteBalance = mParameters.getWhiteBalance();
				if (whiteBalance == null) {
					whiteBalance = Parameters.WHITE_BALANCE_AUTO;
				}
			}

			/**
			 * Set focus mode.
			 */
			mFocusMode = mPreferences.getString(
					CameraSettings.KEY_FOCUS_MODE,
					getString(R.string.pref_camera_focusmode_default));
			if (isSupported(mFocusMode, mParameters.getSupportedFocusModes())) {
				mParameters.setFocusMode(mFocusMode);
			} else {
				mFocusMode = mParameters.getFocusMode();
				if (mFocusMode == null) {
					mFocusMode = Parameters.FOCUS_MODE_AUTO;
				}
			}
		} else {
			mFocusMode = mParameters.getFocusMode();
		}
	}

	/**
	 * We separate the parameters into several subsets, so we can update only
	 * the subsets actually need updating. The PREFERENCE set needs extra
	 * locking because the preference can be changed from GLThread as well.
	 */
	private void setCameraParameters(int updateSet) {
		mParameters = mCameraDevice.getParameters();

		if ((updateSet & UPDATE_PARAM_INITIALIZE) != 0) {
			updateCameraParametersInitialize();
		}

		if ((updateSet & UPDATE_PARAM_ZOOM) != 0) {
			updateCameraParametersZoom();
		}

		if ((updateSet & UPDATE_PARAM_PREFERENCE) != 0) {
			updateCameraParametersPreference();
		}

		Parameters oldParameters = mCameraDevice.getParameters();
		try {
			mCameraDevice.setParameters(mParameters);
		} catch (IllegalArgumentException e) {
			mCameraDevice.setParameters(oldParameters);
			mParameters = oldParameters;
			mZoomValue = 0;
			Log.w(TAG, e);
		}
	}

	/**
	 * If the Camera is idle, update the parameters immediately, otherwise
	 * accumulate them in mUpdateSet and update later.
	 */
	private void setCameraParametersWhenIdle(int additionalUpdateSet) {
		mUpdateSet |= additionalUpdateSet;
		if (mCameraDevice == null) {
			/**
			 * We will update all the parameters when we open the device, so
			 * we don't need to do anything now.
			 */
			mUpdateSet = 0;
			return;
		} else if (isCameraIdle()) {
			setCameraParameters(mUpdateSet);
			mUpdateSet = 0;
		} else {
			if (!mHandler.hasMessages(SET_CAMERA_PARAMETERS_WHEN_IDLE)) {
				mHandler.sendEmptyMessageDelayed(
						SET_CAMERA_PARAMETERS_WHEN_IDLE, 1000);
			}
		}
	}

	private boolean isCameraIdle() {
		PrintLog.debug(TAG,""+mStatus+" = "+IDLE+": "+mFocusState+" = "+FOCUS_NOT_STARTED);
		return mStatus == IDLE && mFocusState == FOCUS_NOT_STARTED;
	}

	private boolean isImageCaptureIntent() {
		String action = getIntent().getAction();
		return MediaStore.ACTION_IMAGE_CAPTURE.equals(action) ;
	}

	private int calculatePicturesRemaining() {
		mPicturesRemaining = MenuHelper.calculatePicturesRemaining();
		return mPicturesRemaining;
	}


	@Override
	public void onUserInteraction() {
		super.onUserInteraction();
		keepScreenOnAwhile();
	}

	private void resetScreenOn() {
		mHandler.removeMessages(CLEAR_SCREEN_DELAY);
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}

	private void keepScreenOnAwhile() {
		mHandler.removeMessages(CLEAR_SCREEN_DELAY);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		mHandler.sendEmptyMessageDelayed(CLEAR_SCREEN_DELAY, SCREEN_DELAY);
	}

	protected void onRestorePreferencesClicked() {
		if (mPausing) return;
		Runnable runnable = new Runnable() {
			public void run() {
				mHeadUpDisplay.restorePreferences(mParameters);
			}
		};
		MenuHelper.confirmAction(this,
				getString(R.string.confirm_restore_title),
				getString(R.string.confirm_restore_message),
				runnable);
	}
}


/**
 * Provide a mapping for Jpeg encoding quality levels
 * from String representation to numeric representation.
 */
class JpegEncodingQualityMappings 
{
	private static final String TAG = "JpegEncodingQualityMappings";

	private static final String NORMAL = "normal";
	private static final String FINE = "fine";
	private static final String SUPERFINE = "superfine";

	private static final int DEFAULT_QUALITY = 85;
	private static HashMap<String, Integer> mHashMap =
			new HashMap<String, Integer>();

	static {
		mHashMap.put(NORMAL,    (Build.VERSION.SDK_INT >= 0x00000008) ? CameraProfile.QUALITY_LOW : 0x00000000);
		mHashMap.put(FINE,      (Build.VERSION.SDK_INT >= 0x00000008) ? CameraProfile.QUALITY_MEDIUM : 0x00000001);
		mHashMap.put(SUPERFINE, (Build.VERSION.SDK_INT >= 0x00000008) ? CameraProfile.QUALITY_HIGH : 0x00000002);
	}

	private static String[] mQualityStrings = {SUPERFINE, FINE, NORMAL};
	private static int[] mQualityNumbers = {85, 75, 65};

	/**
	 * Retrieve and return the Jpeg encoding quality number
	 * for the given quality level.
	 */
	public static int getQualityNumber(String jpegQuality) {
		Integer quality = mHashMap.get(jpegQuality);
		if (quality == null) {
			Log.w(TAG, "Unknown Jpeg quality: " + jpegQuality);
			return DEFAULT_QUALITY;
		}
		if (Build.VERSION.SDK_INT >= 0x00000008) {
			return CameraProfile.getJpegEncodingQualityParameter(quality.intValue());
		} else {
			/**
			 * Find the index of the input string
			 */
			int index = Util.indexOf(mQualityStrings, jpegQuality);

			if (index == -1 || index > mQualityNumbers.length - 1) {
				return DEFAULT_QUALITY;
			}

			try {
				return mQualityNumbers[index];
			} catch (NumberFormatException ex) {
				return DEFAULT_QUALITY;
			}
		}
	}
}
