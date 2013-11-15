/*
 * Copyright (C) 2012 Lightbox
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alttab.camfind.device;

import static com.alttab.camfind.manager.Util.Assert;

import java.io.IOException;

import android.hardware.Camera;
import android.hardware.Camera.Size;

import com.alttab.camfind.manager.CameraHardwareException;
import com.common.PrintLog;

public class LGOptimus2XCameraHolder extends CameraHolder {
	private static final int CAMERA_REAR = 0;
	private static final int CAMERA_FRONT = 1;

	protected LGOptimus2XCameraHolder() {
    	super();
        mNumberOfCameras = 2;
    }
	
	@Override
	public int getCameraOrientation(int cameraId, int orientationSensorValue) {
		PrintLog.debug(TAG, "ori="+orientationSensorValue);
		if (mCameraId == CAMERA_REAR) {
			if (orientationSensorValue == 270 || orientationSensorValue == 90) {
				return 90;
			} else {
				return 270;
			}
		}
		return 0;
	}

	@Override
	public int getNumberOfCameras() {
		return mNumberOfCameras;
	}

	@Override
	public boolean isFrontFacing(int cameraId) {
		return (cameraId == CAMERA_FRONT);
	}

	@Override
	public synchronized Camera open(int cameraId) throws CameraHardwareException {
		Assert(mUsers == 0);
        if (mCameraDevice != null && mCameraId != cameraId) {
            mCameraDevice.release();
            mCameraDevice = null;
            mCameraId = -1;
        }
        if (mCameraDevice == null) {
            try {
                PrintLog.debug(TAG, "open camera " + cameraId);
                mCameraDevice = openCamera(cameraId);
                mCameraId = cameraId;
            } catch (RuntimeException e) {
                PrintLog.debug(TAG, "fail to connect Camera");
                throw new CameraHardwareException(e);
            }
            mParameters = mCameraDevice.getParameters();
        } else {
            try {
            	mCameraDevice = openCamera(cameraId);
                mCameraId = cameraId;
                mCameraDevice.reconnect();
            } catch (IOException e) {
                PrintLog.debug(TAG, "reconnect failed.");
                throw new CameraHardwareException(e);
            }
            mCameraDevice.setParameters(mParameters);
        }
        ++mUsers;
        mHandler.removeMessages(RELEASE_CAMERA);
        mKeepBeforeTime = 0;
        return mCameraDevice;
	}
	
	private static final String CAMERA_SENSOR = "camera-sensor";
	private Camera openCamera(int cameraId) {
		Camera camera = null;
		if (cameraId == CAMERA_FRONT) {
			camera = Camera.open();
			camera.setDisplayOrientation(90);
			Camera.Parameters parameters = camera.getParameters();
			parameters.set(CAMERA_SENSOR, 2);
			camera.setParameters(parameters);
			camera.setDisplayOrientation(270);
		} else if (cameraId == CAMERA_REAR) {
			camera = Camera.open();
			camera.setDisplayOrientation(90);
			Camera.Parameters parameters = camera.getParameters();
			parameters.set(CAMERA_SENSOR, 0);
			camera.setParameters(parameters);
			camera.setDisplayOrientation(0);
		}
		
		return camera;
	}

	@Override
	public double getAspectRatio() {
		Size size = mParameters.getPictureSize();
		if (mCameraId == CAMERA_FRONT) {
			return ((double) size.height / size.width);
		} else if (mCameraId == CAMERA_REAR) {
			return ((double) size.width / size.height);
		}
		return 4.0/3;
	}

	@Override
	public int getFrontFacingCameraId() {
		return CAMERA_FRONT;
	}

	@Override
	public int getRearFacingCameraId() {
		return CAMERA_REAR;
	}
}
