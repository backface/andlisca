package com.mash.andlisca;

import java.io.IOException;
import java.util.List;

import com.mash.andlisca.FpsMeter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.PreviewCallback;
import android.os.Build;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public abstract class AndliscaViewBase 
	extends SurfaceView 
	implements SurfaceHolder.Callback, Runnable {

	private static final String TAG = "Andlisca::ViewBase";

    private static Camera		mCamera;
    private SurfaceHolder       mHolder;
    private static int         	mFrameWidth;
    private static int          mFrameHeight;
    private int         		mCanvasWidth;
    private int          		mCanvasHeight;
    
    private byte[]              mFrame;
    private boolean             mThreadRun;
    private FpsMeter            mFps;
    private int            		mNumberOfCameras=1;
    private int            		mDefaultCameraId;
    private int            		mCameraId;
    private int            		mFrontCameraId;
    private int            		mBackCameraId;
    //private String			mfocusMode;
    
    private List<String>		mFocusModes;
    private List<Camera.Size> 	mResolutions;
    
    protected static boolean 	mResolutionChanged=false;
    private boolean 			mHasMultipleCameras = false;
    private boolean 				showFPS = false;    

    public AndliscaViewBase(Context context) {
        super(context);
        mHolder = getHolder();
        mHolder.addCallback(this);
        mFps = new FpsMeter();        
        Log.i(TAG, "Instantiated new  " + this.getClass());
        Log.i(TAG, "Android version  " + Build.VERSION.SDK_INT);
    }

    public int getFrameWidth() {
        return mFrameWidth;
    }

    public int getFrameHeight() {
        return mFrameHeight;
    }

    public List<String> getFocusModes() {
        return mCamera.getParameters().getSupportedFocusModes();
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
    	Log.i(TAG, "setting resolution: " + getFrameWidth() + "x" + getFrameHeight());  
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
 
    protected boolean isFrontCamera() { 
    	if (Build.VERSION.SDK_INT < 9)
    		return false;
    	else
    		return mCameraId == mFrontCameraId;
    }
    public void setCamera(String type) {
    	if (Build.VERSION.SDK_INT>=9) {
        	mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.release();
            
        	if (type == "front") {
        		Log.i(TAG, "open front facing camera:");        		
        		mCamera = Camera.open(mFrontCameraId);
        		Camera.Parameters params = mCamera.getParameters();
        		params.setRotation(0);
        		mCamera.setParameters(params);
        		mCameraId = mFrontCameraId;
        	} else  {
        		Log.i(TAG, "open back facing camera:");
        		mCamera = Camera.open(mBackCameraId);
        		Camera.Parameters params = mCamera.getParameters();
        		params.setRotation(90);
        		mCamera.setParameters(params);
        		mCameraId = mBackCameraId;
        	}
        	Log.i(TAG, "fcous modes:" + mCamera.getParameters().getFocusMode());
            Log.i(TAG, "available focus modes: " + mFocusModes); 
        	Log.i(TAG,"supported preview formats: " + mCamera.getParameters().getSupportedPreviewFormats().toString());    	
        	Log.i(TAG,"current:" + mCamera.getParameters().getPreviewFormat());    	
        	Log.i(TAG,"supported preview fps range:" + mCamera.getParameters().getSupportedPreviewFpsRange().toString());
        	Log.i(TAG,"supported preview Framerates: " + mCamera.getParameters().getSupportedPreviewFrameRates());
        	Log.i(TAG,"current: " + mCamera.getParameters().getPreviewFrameRate());           	
        	
        	setBestResolution();
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
    }    
    
    public void toggleFPSDisplay() {
    	if (showFPS) 
    		showFPS = false;
    	else
    		showFPS = true;
    }   
    
    public boolean showsFPS() {
    	return showFPS;
    } 
    
    public void setBestResolution() {	
    	Camera.Parameters params = mCamera.getParameters();
        mResolutions = params.getSupportedPreviewSizes();
                           
        Log.i(TAG, "available resolutions:");
        // selecting optimal camera preview size
        {
            double minDiff = Double.MAX_VALUE;
            for (Camera.Size size : mResolutions) {
                if (Math.abs(size.height - mCanvasHeight) < minDiff) {
                    mFrameWidth = size.width;
                    mFrameHeight = size.height;
                    minDiff = Math.abs(size.height - mCanvasHeight);
                }                    
                Log.i(TAG, size.width + "x" + size.height);
            }
        }
        
        params.setPreviewSize(getFrameWidth(), getFrameHeight()); 
        mCamera.setParameters(params);
        Log.i(TAG, "setting resolution: " + getFrameWidth() + "x" + getFrameHeight());  	
    }
    
    public void surfaceChanged(SurfaceHolder _holder, int format, int width, int height) {
        Log.i(TAG, "surfaceChanged");
        if (mCamera != null) {
            mFrameWidth = width;
            mFrameHeight = height;
            mCanvasWidth = width;
            mCanvasHeight = height;

            //choose resolution that fits screen
            setBestResolution();
            
            // ask for focus modes
            Camera.Parameters params = mCamera.getParameters();
            mFocusModes = params.getSupportedFocusModes();
         

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
    	if (Build.VERSION.SDK_INT>=9) {
    		mDefaultCameraId = 0;
    		
        	Log.i(TAG, "available cameras:");
        	mNumberOfCameras = Camera.getNumberOfCameras();
        	Log.i(TAG, "Found " + mNumberOfCameras + " cameras.");
        	if (mNumberOfCameras > 0) {
        		mHasMultipleCameras = true;
        		// Find the ID of the default camera
        		CameraInfo cameraInfo = new CameraInfo();
        		for (int i = 0; i < mNumberOfCameras; i++) {
        			Camera.getCameraInfo(i, cameraInfo);
        			Log.i(TAG, "info " + cameraInfo.toString());
        			if (cameraInfo.facing == CameraInfo.CAMERA_FACING_BACK) {
        				mDefaultCameraId = i;
        				mBackCameraId = i;
        				mCameraId = i;
        			} else if (cameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT) {
        				mFrontCameraId = i;
        			}
        		}        		
        	}
        	mCamera = Camera.open(mDefaultCameraId); 
        } else {
        	mCamera = Camera.open();
        }

        mCamera.setPreviewCallback(new PreviewCallback() {
            public void onPreviewFrame(byte[] data, Camera camera) {
                synchronized (AndliscaViewBase.this) {
                    mFrame = data;
                    AndliscaViewBase.this.notify();
                }
            }
        });
        Log.i(TAG, "surfaceCreated");
        
        
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
                	
                	
                	//flip canvas 
                	if(isFrontCamera())  {
                		canvas.rotate(-90, canvas.getWidth()/2, canvas.getHeight()/2);
                		canvas.scale(1,-1,canvas.getWidth()/2, canvas.getHeight()/2);
                	} else {
                		canvas.rotate(90, canvas.getWidth()/2, canvas.getHeight()/2);
            		}
                	
                	canvas.drawBitmap(bmp, (canvas.getWidth() - getFrameWidth()) / 2, (canvas.getHeight() - getFrameHeight()) / 2, null);
                	
                	if(isFrontCamera()) {                		
                		canvas.scale(1,-1,canvas.getWidth()/2, canvas.getHeight()/2);
                		canvas.rotate(90, canvas.getWidth()/2, canvas.getHeight()/2);
                	} else { 
                		canvas.rotate(-90, canvas.getWidth()/2, canvas.getHeight()/2);
                	}
                	if (showFPS)
                		mFps.draw(canvas, canvas.getWidth()/2+20, canvas.getHeight()-25);
                	
                	mHolder.unlockCanvasAndPost(canvas); 
                }
                bmp.recycle();
            }
        }
    }
    
    public boolean hasMultipleCameras() {
    	return mHasMultipleCameras;
    }
    

}