package com.mash.andlisca;

import java.io.IOException;
import java.util.List;

import com.mash.andlisca.FpsMeter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public abstract class AndliscaViewBase 
	extends SurfaceView 
	implements SurfaceHolder.Callback, Runnable {

	private static final String TAG = "Andlisca::SurfaceView";

    private static Camera		mCamera;
    private SurfaceHolder       mHolder;
    private static int         	mFrameWidth;
    private static int          mFrameHeight;
    private byte[]              mFrame;
    private boolean             mThreadRun;
    private FpsMeter            mFps;
    //private String			mfocusMode;
    
    private List<String>		mFocusModes;
    private List<Camera.Size> 	mResolutions;
    
    protected static boolean 	mResolutionChanged=false;
    private boolean 			showFPS = false;
    

    public AndliscaViewBase(Context context) {
        super(context);
        mHolder = getHolder();
        mHolder.addCallback(this);
        mFps = new FpsMeter();        
        Log.i(TAG, "Instantiated new  " + this.getClass());
    }

    public int getFrameWidth() {
        return mFrameWidth;
    }

    public int getFrameHeight() {
        return mFrameHeight;
    }

    public List<String> getFocusModes() {
        return mFocusModes;
    }
    

    public String getFocusMode() {
        return mCamera.getParameters().getFocusMode();
    }
    
    public boolean setFocusMode(String mode) {
    	Camera.Parameters params = mCamera.getParameters();
    	if (mFocusModes.contains(mode)) {
    		params.setFocusMode(mode);
    		mCamera.setParameters(params);
    		Log.i(TAG, "set Focus Mode to " + mode);
    		return true;
    	} else {
    		Log.i(TAG, "FAILED in setting Focus Mode to " + mode);
    		return false;
    	}    	
    }
    
	public void AutofocusNow() {
		if (getFocusMode() != Camera.Parameters.FOCUS_MODE_AUTO) 
			setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO); 
		mCamera.autoFocus(null);
	}    
    
    public List<Camera.Size> getResolutions() {
        return mResolutions;
    }    
    
    public void setResolution(Camera.Size size) {
    	mCamera.stopPreview();
        mCamera.setPreviewCallback(null);
        
    	Camera.Parameters params = mCamera.getParameters();
    	mFrameWidth = size.width;
        mFrameHeight = size.height;    	
    	params.setPreviewSize(getFrameWidth(), getFrameHeight());
    	mCamera.setParameters(params);
    	mResolutionChanged = true;
    	
    	mCamera.setPreviewCallback(new PreviewCallback() {
            public void onPreviewFrame(byte[] data, Camera camera) {
                synchronized (AndliscaViewBase.this) {
                    mFrame = data;
                    AndliscaViewBase.this.notify();
                }
            }
        });    	
    	mCamera.startPreview();
    }        
    
    public void toggleFPSDisplay() {
    	if (showFPS == true) 
    		showFPS = false;
    	else
    		showFPS = true;
    }    
    
    public void surfaceChanged(SurfaceHolder _holder, int format, int width, int height) {
        Log.i(TAG, "surfaceCreated");
        if (mCamera != null) {
            Camera.Parameters params = mCamera.getParameters();
            mResolutions = params.getSupportedPreviewSizes();
            mFrameWidth = width;
            mFrameHeight = height;
            
            /*
            Log.i(TAG, "available cameras:");
            mCamera.getNumberOfCameras();

            // Find the ID of the default camera
            CameraInfo cameraInfo = new CameraInfo();
                for (int i = 0; i < numberOfCameras; i++) {
                    Camera.getCameraInfo(i, cameraInfo);
                    if (cameraInfo.facing == CameraInfo.CAMERA_FACING_BACK) {
                        defaultCameraId = i;
                    }
            }   */         

            Log.i(TAG, "available resolutions:");
            // selecting optimal camera preview size
            {
                double minDiff = Double.MAX_VALUE;
                for (Camera.Size size : mResolutions) {
                    if (Math.abs(size.height - height) < minDiff) {
                        mFrameWidth = size.width;
                        mFrameHeight = size.height;
                        minDiff = Math.abs(size.height - height);
                    }                    
                    Log.i(TAG, size.width + "x" + size.height);
                }
            }
            
            params.setPreviewSize(getFrameWidth(), getFrameHeight());            
            Log.i(TAG, "setting resolution: " + getFrameWidth() + "x" + getFrameHeight());            
            
            // ask for focus modes
            mFocusModes = params.getSupportedFocusModes();
            Log.i(TAG, "available focus modes: " + mFocusModes);                      

            // fix portrait setting
            params.set("orientation", "portrait");
            params.setRotation(90);
                        
            mCamera.setParameters(params);
            try {
				mCamera.setPreviewDisplay(null);
			} catch (IOException e) {
				Log.e(TAG, "mCamera.setPreviewDisplay fails: " + e);
			}
            mCamera.startPreview();
        }
    }

    public void surfaceCreated(SurfaceHolder holder) {
        Log.i(TAG, "surfaceCreated");
        mCamera = Camera.open();
        mCamera.setPreviewCallback(new PreviewCallback() {
            public void onPreviewFrame(byte[] data, Camera camera) {
                synchronized (AndliscaViewBase.this) {
                    mFrame = data;
                    AndliscaViewBase.this.notify();
                }
            }
        });
        (new Thread(this)).start();
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.i(TAG, "surfaceDestroyed");
        mThreadRun = false;
        if (mCamera != null) {
            synchronized (this) {
                mCamera.stopPreview();
                mCamera.setPreviewCallback(null);
                mCamera.release();
                mCamera = null;
            }
        }
    }

    protected abstract Bitmap processFrame(byte[] data);

    public void run() {
        mThreadRun = true;
        Log.i(TAG, "Starting processing thread");
        
        mFps.init();
        
        while (mThreadRun) {
            Bitmap bmp = null;

            synchronized (this) {
                try {
                    this.wait();
                    bmp = processFrame(mFrame);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            mFps.measure();
            
            if (bmp != null) {
                Canvas canvas = mHolder.lockCanvas();
                if (canvas != null) {
                	canvas.drawColor(Color.BLACK);
                	canvas.rotate(90, canvas.getWidth()/2, canvas.getHeight()/2);
                	canvas.drawBitmap(bmp, (canvas.getWidth() - getFrameWidth()) / 2, (canvas.getHeight() - getFrameHeight()) / 2, null);
                	canvas.rotate(-90, canvas.getWidth()/2, canvas.getHeight()/2);            		
                	mFps.draw(canvas, canvas.getWidth()/2+20, canvas.getHeight()-25);
                	mHolder.unlockCanvasAndPost(canvas); 
                }
                bmp.recycle();
            }
        }
    }
    
}