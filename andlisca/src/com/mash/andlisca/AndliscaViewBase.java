package com.mash.andlisca;

import java.io.IOException;
import java.util.List;

import com.mash.tools.FpsMeter;

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
    
    private int            		mFrontCameraId;
    private int            		mBackCameraId;

    protected int            	mResolutionId=-1;
    protected int            	mCameraId=0;
    protected int            	mFocusModeId=0;
    protected int            	mWhiteBalanceId=0;
    
    private List<String>		mFocusModes;
    private List<Camera.Size> 	mResolutions;
    private Camera.Size[][]		mAllResolutions;
    private String[][] 			mAllFocusmodes;
    
    //private String[] supportedFocusModes = {"auto","macro","infinity","fixed"};
    //private String[] supportedWhiteBalanceModes = {"auto","cloudy","indoor","fixed"};    
    
    protected static boolean 	mResolutionChanged=false;
    private boolean 			mHasMultipleCameras = false;
    private boolean 			showFPS = false;
	private boolean mFlashIsOn = false;   

    public AndliscaViewBase(Context context) 
    {
        super(context);
        mHolder = getHolder();
        mHolder.addCallback(this);
        mFps = new FpsMeter();        
        if (Log.isLoggable(TAG, Log.INFO)) { 
        	Log.i(TAG, "Instantiated new  " + this.getClass());
        	Log.i(TAG, "Android version " + Build.VERSION.SDK_INT);
        }
        initCameras();
    }
    
    public void surfaceChanged(SurfaceHolder _holder, int format, int width, int height) 
    {
    	if (Log.isLoggable(TAG, Log.INFO)) 
    		Log.i(TAG, "surfaceChanged");   
        
    	mCanvasWidth = width;
        mCanvasHeight = height;        
    	openCamera(mCameraId, mResolutionId);
    	
    }

    public void surfaceCreated(SurfaceHolder holder) 
    {   
    	    	
        if (Log.isLoggable(TAG, Log.INFO)) 
        	Log.i(TAG, "surfaceCreated");

        (new Thread(this)).start();
    }

    public void surfaceDestroyed(SurfaceHolder holder) 
    {
    	if (Log.isLoggable(TAG, Log.INFO)) 
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

    public void run() 
    {
        mThreadRun = true;
        
        if (Log.isLoggable(TAG, Log.INFO)) 
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
                	
                	/*
                	//flip canvas in portrait mode
                	if(isFrontCamera())  {
                		canvas.rotate(-90, canvas.getWidth()/2, canvas.getHeight()/2);
                		canvas.scale(1,-1,canvas.getWidth()/2, canvas.getHeight()/2);
                	} else {
                		canvas.rotate(90, canvas.getWidth()/2, canvas.getHeight()/2);
            		}
            		*/
                	
                	canvas.drawBitmap(bmp, (canvas.getWidth() - getFrameWidth()) / 2, (canvas.getHeight() - getFrameHeight()) / 2, null);
                	
                	/*
                	if(isFrontCamera()) {                		
                		canvas.scale(1,-1,canvas.getWidth()/2, canvas.getHeight()/2);
                		canvas.rotate(90, canvas.getWidth()/2, canvas.getHeight()/2);
                	} else { 
                		canvas.rotate(-90, canvas.getWidth()/2, canvas.getHeight()/2);
                	}
                	*/
                	
                	if (showFPS)
                		mFps.draw(canvas, canvas.getWidth()/2+20, canvas.getHeight()-25);
                	
                	mHolder.unlockCanvasAndPost(canvas); 
                }
                bmp.recycle();
            }
        }
    }    


    public void initCameras() 
    {
    	if (Build.VERSION.SDK_INT>=9) {    				       	    		
        	mNumberOfCameras = Camera.getNumberOfCameras();
        	
        	Log.i(TAG, "available cameras:");
        	Log.i(TAG, "Found " + mNumberOfCameras + " cameras.");
        	
        	if (mNumberOfCameras > 1) {
        		mHasMultipleCameras = true;
        		// Find the ID of the default camera
        		CameraInfo cameraInfo = new CameraInfo();
        		
        		mAllResolutions = new Camera.Size[mNumberOfCameras][];
        		mAllFocusmodes = new String[mNumberOfCameras][];

        		for (int i = 0; i < mNumberOfCameras; i++) {
        			Camera.getCameraInfo(i, cameraInfo);       			        			
        			if (cameraInfo.facing == CameraInfo.CAMERA_FACING_BACK) {
        				mBackCameraId = i;
        				if (mCameraId == -1) mCameraId = i;
        				Log.i(TAG, "Camera " + i + " is back facing.");
        				
        			} else if (cameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT) {
        				mFrontCameraId = i;
        				Log.i(TAG, "Camera " + i + " is front facing.");
        			}
    				mCamera = Camera.open(i);
    				
    				List<Camera.Size> m = mCamera.getParameters().getSupportedPreviewSizes();
    				mAllResolutions[i] = new Camera.Size[m.size()];
    				for (int j=0;j<m.size();j++) {
    					mAllResolutions[i][j] = m.get(j);
    				}
    				
    				List<String> l = mCamera.getParameters().getSupportedFocusModes();
    				mAllFocusmodes[i] = new String[l.size()];
    				for (int j=0;j<l.size();j++) {
    					mAllFocusmodes[i][j] = l.get(j);
    				}				
    				
    				mCamera.release();
    				mCamera = null;
        		}        		
        	}
    	}
    }
    
    public void openCamera(int id, int resolution_id) 
    {    	
    	if (mCamera != null) {
    		mCamera.stopPreview();
    		mCamera.setPreviewCallback(null);
    		mCamera.release();
    	}
    	
        if (Build.VERSION.SDK_INT>=9) {
            mCamera = Camera.open(id);
            mCameraId = id;
        } else {
        	mCamera = Camera.open();
        }
        
        mFocusModes = getFocusModes();
        mResolutions = mCamera.getParameters().getSupportedPreviewSizes();
        
        //mFocusModes = getFocusModes();
        
        Log.i(TAG, "opening camera:" + id);
    	if (Log.isLoggable(TAG, Log.INFO)) {
        	Log.i(TAG,"  enabled focus mode:" + mCamera.getParameters().getFocusMode());
        	Log.i(TAG,"  has autofocus: " + hasAutofocus());
            Log.i(TAG,"  available focus modes: " + mCamera.getParameters().getSupportedFocusModes()); 
        	Log.i(TAG,"  supported preview formats: " + mCamera.getParameters().getSupportedPreviewFormats());    	
        	Log.i(TAG,"  current preview format :" + mCamera.getParameters().getPreviewFormat());  
        	
        	if (Build.VERSION.SDK_INT>=9) {
        		Log.i(TAG,"  supported preview fps range:" + mCamera.getParameters().getSupportedPreviewFpsRange());        		        		
        	}
        	Log.i(TAG,"  supported preview Framerates: " + mCamera.getParameters().getSupportedPreviewFrameRates());
        	Log.i(TAG,"  current preview Framerates: " + mCamera.getParameters().getPreviewFrameRate());
        	Log.i(TAG,"  min exposure compensation: " + mCamera.getParameters().getMinExposureCompensation());
        	Log.i(TAG,"  max exposure compensation: " + mCamera.getParameters().getMaxExposureCompensation());
        	Log.i(TAG,"  current exposure compensation: " + mCamera.getParameters().getExposureCompensation());
        	Log.i(TAG,"  supported white balance settings: " + mCamera.getParameters().getSupportedWhiteBalance());
        	Log.i(TAG,"  current white balance: " + mCamera.getParameters().getWhiteBalance());
        	Log.i(TAG,"  supported flash modes: " + mCamera.getParameters().getSupportedFlashModes());        	
        	Log.i(TAG,"  zoom supported: " + mCamera.getParameters().isZoomSupported());
        	if (mCamera.getParameters().isZoomSupported()) {
        		Log.i(TAG,"  max zoom: " + mCamera.getParameters().getMaxZoom());
        		Log.i(TAG,"  current zoom: " + mCamera.getParameters().getZoom());
        		Log.i(TAG,"  zoom ratios: " + mCamera.getParameters().getZoomRatios());
        	}
        	Log.i(TAG,"  supported color effects: " + mCamera.getParameters().getSupportedColorEffects());
    	}       	 	
    	
    	if(resolution_id==-1)
    		setBestMatchingResolution();
    	else 
    		setResolutionById(resolution_id);
    		
		mResolutionChanged = true;
	
		mCamera.setPreviewCallback(new PreviewCallback() {
			public void onPreviewFrame(byte[] data, Camera camera) {
				synchronized (AndliscaViewBase.this) {
					mFrame = data;
					AndliscaViewBase.this.notify();
				}
			}
		});    	
		
		try {
			mCamera.setPreviewDisplay(null);
		} catch (IOException e) {
			Log.e(TAG, "mCamera.setPreviewDisplay fails: " + e);
		}		
		mCamera.startPreview();	
    }
    
    public void openCamera(String type) 
    {
    	if (type == "front") {
    		Log.i(TAG, "open front facing camera:");        		
    		openCamera(mFrontCameraId, mResolutionId);
    	} else  {
    		Log.i(TAG, "open back facing camera:");
    		openCamera(mBackCameraId, mResolutionId);
    	}
    }    

    public CharSequence[] getCameraStrings() {
    	Log.i(TAG, "get CameraStrings");
    	String[] arr = new String[mNumberOfCameras];
    	
    	if (Build.VERSION.SDK_INT < 9)
    		return null;
    	
    	CameraInfo cameraInfo = new CameraInfo();
		for (int i = 0; i < mNumberOfCameras; i++) {
			Camera.getCameraInfo(i, cameraInfo);       			        			
			if (cameraInfo.facing == CameraInfo.CAMERA_FACING_BACK) {
				arr[i] = "Back facing";
			} else if (cameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT) {
				arr[i] = "Front facing";
			} else {
				arr[i] = "unknown camera";
			}
		}   
   		return arr;		
	}
    
    public CharSequence[] getCameraIds() {
    	Log.i(TAG, "get CameraIds");
    	String[] arr = new String[mNumberOfCameras];
    	
    	if (Build.VERSION.SDK_INT < 9)
    		return null;
    	
		for (int i = 0; i < mNumberOfCameras; i++) {
			arr[i] = "" + i;
		}   
   		return arr;		
	}    
    
    public int getCameraId() {
    	return mCameraId;
    }
    
    protected boolean isFrontCamera() 
    { 
    	if (Build.VERSION.SDK_INT < 9)
    		return false;
    	else
    		return mCameraId == mFrontCameraId;
    }    
    
    public List<String> getFocusModes() 
    {
        return mCamera.getParameters().getSupportedFocusModes();
    }    

    public String getFocusMode() 
    {
        return mCamera.getParameters().getFocusMode();
    }
    
    public CharSequence[] getFocusModesStrings(int id) 
    {
    	Log.i(TAG,"getFocusModesStrings");
    	Log.i(TAG,"FOCUS MODES are: " + mAllFocusmodes);
    	String[] resArray = new String[mAllFocusmodes[id].length];
    	int i = 0;
   		for (String mode : mAllFocusmodes[id]) {
    		resArray[i] = mode;
   			i++;
    	}
   		return resArray;
    }       
    
    public CharSequence[] getFocusModesIds(int id) 
    {
    	Log.i(TAG,"getFocusModesIds");
    	String[] resArray = new String[mAllFocusmodes[id].length];
    	int i = 0;
   		for (String mode : mAllFocusmodes[id]) {
    		resArray[i] = "" +i;
   			i++;
    	}
   		return resArray;
    }      
    
    public boolean setFocusMode(String mode) 
    {
    	Camera.Parameters params = mCamera.getParameters();
    	if (mFocusModes.contains(mode)) {
    		params.setFocusMode(mode);
    		mCamera.setParameters(params);
    		AndliscaActivity.setChangeFocusButton(mode);
    		if (Log.isLoggable(TAG, Log.INFO)) 
    			Log.i(TAG, "set Focus Mode to " + mode);
    		return true;
    	} else {
    		if (Log.isLoggable(TAG, Log.WARN)) 
    			Log.w(TAG, "FAILED in setting Focus Mode to " + mode);
    		return false;
    	}    	
    }
    
    public boolean setFocusModeById(int id) 
    {
    	Camera.Parameters params = mCamera.getParameters();
   		if (mFocusModes.size() > 0 && id >= 0) {
   			if (Log.isLoggable(TAG, Log.INFO)) 
   				Log.i(TAG, "set Focus Mode to " + id);
   			
   			params.setFocusMode(mFocusModes.get(id));
   			mCamera.setParameters(params);
   			AndliscaActivity.setChangeFocusButton(mFocusModes.get(id));
    	}
   		return true;
    }   
    
    public boolean cycleFocusMode() 
    {
    	Camera.Parameters params = mCamera.getParameters();
    	
    	Log.i(TAG, "cycle Focus Mode to " + getFocusModes().size());
   		
    	if (getFocusModes().size() > 1) {
   			int pos = mFocusModes.indexOf(getFocusMode()) + 1;  			
   			if (pos > getFocusModes().size()-1)
   				pos = 0;
   			
   			params.setFocusMode(mFocusModes.get(pos));
   			
   			if (Log.isLoggable(TAG, Log.INFO)) 
   				Log.i(TAG, "set Focus Mode to " + mFocusModes.get(pos));
   			
   			params.setFocusMode(mFocusModes.get(pos));
   			mCamera.setParameters(params);
   			AndliscaActivity.setChangeFocusButton(mFocusModes.get(pos));
    	}
   		return true;
    }      

    public void toggleFocusModes(int i) 
    {
    	if (mAllFocusmodes[i].length > 1) {
  		
    	}
    }
    
	public void AutofocusNow() 
	{
		if (getFocusMode() != Camera.Parameters.FOCUS_MODE_AUTO) 
			setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO); 
		mCamera.autoFocus(null);
	}    
	
	public boolean hasAutofocus() 
	{
		return mFocusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO);
	} 	
    
    public List<Camera.Size> getResolutions() 
    {
    	mResolutions = mCamera.getParameters().getSupportedPreviewSizes();
        return mResolutions;
    }    
    
    public CharSequence[] getResolutionStrings(int id) 
    {
    	Log.i(TAG,"getResolutionStrings");
    	String[] resArray = new String[mAllResolutions[id].length+1];
    	int i = 0;
    	resArray[i] = "auto";
   		for (Camera.Size size : mAllResolutions[id]) {
   			i++;
    		resArray[i] = size.width + "x" + size.height;
    	}
   		return resArray;
    }   

    public CharSequence[] getResolutionIds(int id) 
    {
    	Log.i(TAG,"getResolutionIds");
    	String[] resArray = new String[mAllResolutions[id].length+1];
    	resArray[0] = "-1";
   		for (int i=0; i<mAllResolutions[id].length; i++) {            
    		resArray[i+1] = "" + i;
    	}
   		return resArray;
    }   
    
    public void setResolutionById(int id) 
    {
    	if (id >= 0)
    		setResolution(mResolutions.get(id));
    	else
    		setBestMatchingResolution();
    }
    
    public void setBestMatchingResolution() 
    {	
    	Camera.Parameters params = mCamera.getParameters();
    	
                           
        if (Log.isLoggable(TAG, Log.INFO)) 
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
                if (Log.isLoggable(TAG, Log.INFO)) 
                	Log.i(TAG, size.width + "x" + size.height);
            }
        }
        
        params.setPreviewSize(getFrameWidth(), getFrameHeight()); 
        mCamera.setParameters(params);
        if (Log.isLoggable(TAG, Log.INFO)) 
        	Log.i(TAG, "setting resolution: " + getFrameWidth() + "x" + getFrameHeight());  	
    }    
    
    
    public void setResolution(Camera.Size size) 
    {
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
 
       
    
    public int getFrameWidth() 
    {
        return mFrameWidth;
    }

    public int getFrameHeight() 
    {
        return mFrameHeight;
    }

    public int getCanvasHeight() 
    {
        return mCanvasHeight;
    }

    public int getCanvasWidth() {
        return mCanvasWidth;
    }    
    
    public void toggleFPSDisplay() 
    {
    	if (showFPS) 
    		showFPS = false;
    	else
    		showFPS = true;
    }   
    
    public boolean showsFPS() {
    	return showFPS;
    } 
    
    public void showsFPS(boolean state) 
    {
    	showFPS = state;
    }            
    
    public boolean hasMultipleCameras() 
    {
    	return mHasMultipleCameras;
    }
    
    public boolean isFlashOn() {
    	return mFlashIsOn;
    }

	public void toggleFlash() {
		Camera.Parameters params = mCamera.getParameters();
    	if (mFlashIsOn) {
    		mFlashIsOn = false;
    		params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
    	} else {
    		mFlashIsOn = true;
    		params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
    	}  
    	mCamera.setParameters(params);
	}
}