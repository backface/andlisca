package com.mash.andlisca;

import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.opencv.android.Utils;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.SurfaceHolder;

class AndliscaView extends AndliscaViewBase implements OnSharedPreferenceChangeListener{
	private static final String TAG = "Andlisca::View";
    private Mat mYuv;
    private Mat mRgba;    
    private Mat mScanImage;
    private int cLine=0;
    private int mRows=0;
    private int mLineHeight=1;
    public boolean mAutosave;
    private int	maxBufferSize = 4096;  
    SharedPreferences preferences; 
    
    public AndliscaView(Context context) {
        super(context);
        mAutosave=false;
        preferences = PreferenceManager.getDefaultSharedPreferences(this.getContext());
        preferences.registerOnSharedPreferenceChangeListener(this);
    }
    
    @Override
    public void surfaceChanged(SurfaceHolder _holder, int format, int width, int height) {
 
        mCameraId = Integer.valueOf(preferences.getString("camera_id", "0")); 
        mResolutionId = Integer.valueOf(preferences.getString("cam" + mCameraId + "_resolution_id", "-1"));
        Log.i(TAG,"resolution id is " + mResolutionId);
        
    	super.surfaceChanged(_holder, format, width, height);

        showsFPS(preferences.getBoolean("show_fps", false));             
        setLineHeight(Integer.valueOf(preferences.getString("slit_size", "1")));  
        setMaxBufferSize(Integer.valueOf(preferences.getString("max_buffer_size", "720")));
        setAutoSave(preferences.getBoolean("autosave", false));  
        setFocusModeById(Integer.valueOf(preferences.getString("cam" + mCameraId + "_focus_mode_id", "0")));  
    	
        synchronized (this) {
            // initialize Mats before usage
            mYuv = new Mat(getFrameHeight() + getFrameHeight() / 2, getFrameWidth(), CvType.CV_8UC1);
            mRgba = new Mat();
            mScanImage = new Mat();
            cLine = 0;
        }
       
    }

    @Override
    protected Bitmap processFrame(byte[] data) {
    	
        if(mResolutionChanged) {
        	mYuv.release();       
        	mYuv = new Mat(getFrameHeight() + getFrameHeight() / 2, getFrameWidth(), CvType.CV_8UC1);
        	mRgba.release();
            mRgba = new Mat();
            clearImage(); 
            mResolutionChanged = false;
        }
        
        mYuv.put(0, 0, data);
        Imgproc.cvtColor(mYuv, mRgba, Imgproc.COLOR_YUV420sp2RGB, 4);

        for(int i=0; i<mLineHeight; i++) {
        	mScanImage.push_back(mRgba.row(getFrameHeight()/2-mLineHeight/2+i));        	
        }
	        
	    if (cLine > 0 )
	    	mRows = mScanImage.rows() - 1;
	    else
	    	mRows = 0;
	    
	   	for (int i=0; i < getFrameHeight()/2;  i++) {
	       	if (i < mRows	)
	       		mScanImage.row(mRows - i).
	       			copyTo(mRgba.row(getFrameHeight()/2+i));
	       	else
	       		mRgba.row(getFrameHeight()/2+i).setTo(new Scalar(0,0,0,255));
	    }
	    cLine++;
        
        Core.line(mRgba, 
      		new Point(0, getFrameHeight()/2), 
       		new Point(getFrameWidth(), getFrameHeight()/2),
       		new Scalar(255,0,0,255),
       		1);                    
        
        if (isFrontCamera())
        	Core.flip(mRgba.clone(), mRgba, 1);
        
        Bitmap bmp = Bitmap.createBitmap(
        		getFrameWidth(), 
        		getFrameHeight(), 
        		Bitmap.Config.ARGB_8888);    

        if (mAutosave && mScanImage.rows() >= maxBufferSize) {
        	saveBitmap();
        	clearImage();

        	/* another fancy image but performance decrease */
        	/*
        	Mat tmp = new Mat();
        	tmp = mScanImage.rowRange(mLineHeight,mScanImage.rows()).clone();
        	mScanImage.release();
        	mScanImage   tmp.clone();
        	tmp.release();
        	*/
        } else if (maxBufferSize != 0 && mScanImage.rows() >= maxBufferSize) {
        	clearImage();
        }
        
        if (Utils.matToBitmap(mRgba, bmp)) {       
            return bmp;
        }
        
        bmp.recycle();
        return null;
    }

    @Override
    public void run() {
        super.run();

        synchronized (this) {
            // Explicitly deallocate Mats
            if (mYuv != null)
                mYuv.release();
            if (mRgba != null)
                mRgba.release();
            if (mScanImage != null)
            	mScanImage.release();            
            mYuv = null;
            mRgba = null;
            mScanImage = null;
        }
    }
    
    public boolean isAutoSave() {
    	if(mAutosave)
    		return true;
    	else
    		return false;
    }

    public void toggleAutoSave() {
    	if(mAutosave)
    		mAutosave = false;
    	else {
    		mAutosave = true;
    	}
    }
    
    public void setAutoSave(boolean status) {
    	mAutosave = status;
    }
         
     
    
    public String saveBitmap() {
    	String filename;
	   	Date date = new Date();
	   	SimpleDateFormat sdf = new SimpleDateFormat ("yyyyMMddHHmmss");
	   	filename =  sdf.format(date);
	    String state = Environment.getExternalStorageState();

    	Bitmap save_bmp = Bitmap.createBitmap(getFrameWidth(), mRows,
        		Bitmap.Config.ARGB_8888);
        
	   	if (Environment.MEDIA_MOUNTED.equals(state)) 
        {
		   	try{	
				if (Utils.matToBitmap(mScanImage, save_bmp)) {    		        
			  		Matrix mtx = new Matrix();
			  		if (isFrontCamera())
			  			mtx.postRotate(-90);
			  		else {
			  			mtx.postRotate(90);
			  			mtx.preScale(1, -1);
			  		}
			  		Bitmap rotate_bmp = Bitmap.createBitmap(save_bmp, 
			 				0, 0, getFrameHeight(), mRows, mtx, true);        		   		
	   		
			   		String path = Environment.getExternalStorageDirectory().toString();
			   		OutputStream fOut = null;
			   		
			   		File fpath=new File(path,"andlisca");
			   		
			   		if (!fpath.exists()) fpath.mkdirs();
			   		File file = new File(fpath, filename+".jpg");
			   		fOut = new FileOutputStream(file);		   
			   		
			   		if (Log.isLoggable(TAG, Log.INFO)) 
			   			Log.i(TAG, "saving file to" + file.toString());	  	
			   		if (Log.isLoggable(TAG, Log.INFO)) 
			   			Log.i(TAG, "size is: " + mRows + " x " + getFrameHeight());      	
			   		
			   		rotate_bmp.compress(Bitmap.CompressFormat.JPEG, 90, fOut);
			   		rotate_bmp.recycle();
				    save_bmp.recycle();
			   		
				    fOut.flush();
			   		fOut.close();	
		   			
			   		return "Saved image to: " + file.toString();				
				} else {
					if (Log.isLoggable(TAG, Log.WARN)) Log.w(TAG, "saving image failed.");
					return "saving image failed.";
				}
		   	} catch (Exception e) {
		   		e.printStackTrace();
		   		return "saving image failed. see stack trace";
		   	}		   	
        } else {
        	if (Log.isLoggable(TAG, Log.WARN)) Log.i(TAG, "saving image failed. no external media found");
        	return "Saving image failed. No sdcard found!";
        }
    }
    
    public void clearImage() {
       	mScanImage.release();
       	mScanImage = new Mat();
       	cLine = 0;
       	if (Log.isLoggable(TAG, Log.INFO)) Log.i(TAG, "cleared scanImage!");   
    }
    
    public void setLineHeight(int s) {
    	mLineHeight = s;
    	if (Log.isLoggable(TAG, Log.INFO)) Log.i(TAG, "set LineHeight to " + s + " pixel.");
    }
    
    public void setMaxBufferSize(int s) {
    	if (Log.isLoggable(TAG, Log.INFO)) Log.i(TAG,"setting max buffer to:" + s);
    	maxBufferSize = s;
    }

	public void onSharedPreferenceChanged(SharedPreferences pref,
			String key) {
		// TODO Auto-generated method stub
		if (key == "resolution_id")
			mResolutionId=pref.getInt(key, 0);		
		Log.i(TAG,"preference" +  key + " changed");
	}    
}
