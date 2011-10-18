package com.mash.andlisca;

import java.util.ArrayList;
import java.util.List;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

public class AndliscaActivity extends Activity {
    private static final String TAG = "Andlisca::Activity";
    
    private AndliscaView	mView;    

    private MenuItem            mItemSave;
    private MenuItem            mItemClear;
    private MenuItem			mItemFocusAuto;
    private MenuItem			mItemFocusMacro;
    private MenuItem			mItemFocusInfinity;
    private MenuItem			mItemFocusFixed;
    private MenuItem			mItemFocusEdof;
    private MenuItem	 		mItemFocusContinuous;   
    private MenuItem			mItemInfo;    
    private MenuItem	 		mItemFrontCamera;
    private MenuItem	 		mItemBackCamera;
    private MenuItem	 		mItemSafeMode;
    private MenuItem	 		mItemFPS;
    
    private List<MenuItem>		mItemResolutions;
    private List<MenuItem>		mItemSizes;
    private List<Camera.Size>	mResolutions; 
    
    private PowerManager.WakeLock mWakeLock;
        
    
    private static final int 	DIALOG_INFO=0;
          
    public AndliscaActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        mView = new AndliscaView(this);
        setContentView(mView);
        registerForContextMenu(mView);
        
        final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "DoNotDim");
        mWakeLock.acquire();        
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    	mWakeLock.release();
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    	mWakeLock.acquire();
    }    

    @Override
    protected void onDestroy() {
    	mWakeLock.release();
    	super.onResume();    	
    }    

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "onCreateOptionsMenu");        
        mItemSave = menu.add("Save");
        mItemClear = menu.add("Clear");
        mItemInfo = menu.add("Info");         
        return true;
    }
 
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "Menu Item selected " + item);
        if (item == mItemSave) {
        	Toast.makeText(this,mView.saveBitmap(), Toast.LENGTH_SHORT).show();
        } else if (item == mItemClear)
        	mView.clearImage();          
        /* else if (item == mItemFocusAuto)
        	mView.AutofocusNow();          
        else if (item == mItemFocusMacro)
        	mView.setFocusMode(Camera.Parameters.FOCUS_MODE_MACRO); 
        else if (item == mItemFocusInfinity)
        	mView.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY);         
        else if (item == mItemFocusFixed)
        	mView.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED); 
        else if (item == mItemFocusEdof)
        	mView.setFocusMode(Camera.Parameters.FOCUS_MODE_EDOF); 
        else if (mResolutions != null) {
        	if (mItemResolutions.contains(item))
        		mView.setResolution(mResolutions.get(mItemResolutions.indexOf(item))); 
        */
    	else if (item == mItemInfo) {
    		Log.i(TAG,"ABOUT");
    		showDialog(DIALOG_INFO);
    	}        
        return true;
    }

    @Override
    public void onCreateContextMenu(
    		ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) 
    {
    	menu.setHeaderTitle("Settings");
        if (mView.getFocusModes() != null) {
        	Menu focusMenu = menu.addSubMenu("Focus modes");
        	Log.i(TAG,"modes "+ mView.getFocusModes());
	        if (mView.getFocusModes().contains(Camera.Parameters.FOCUS_MODE_AUTO))
	        	mItemFocusAuto = focusMenu.add("(Auto)Focus Now!"); 
	        if (mView.getFocusModes().contains(Camera.Parameters.FOCUS_MODE_MACRO))
	        	mItemFocusMacro = focusMenu.add("Macro");
	        if (mView.getFocusModes().contains(Camera.Parameters.FOCUS_MODE_FIXED))
	        	mItemFocusFixed = focusMenu.add("Fixed");
	        if (mView.getFocusModes().contains(Camera.Parameters.FOCUS_MODE_INFINITY))
	        	mItemFocusInfinity = focusMenu.add("Infinity");
	        if (mView.getFocusModes().contains(Camera.Parameters.FOCUS_MODE_EDOF))
	        		mItemFocusEdof= focusMenu.add("EDOF");
	        // new in android 2.3
	        if (Build.VERSION.SDK_INT>=9) { 
	        	if (mView.getFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO))
	        		mItemFocusContinuous = focusMenu.add("continuous");
	        }      
        }
        
        mItemSizes = new ArrayList<MenuItem>();
        Menu sizeMenu = menu.addSubMenu("Line size");
        for (int i=1; i<=5; i++) {
    		mItemSizes.add(sizeMenu.add(i + "px"));
    	}               
        
        mResolutions = mView.getResolutions();
        mItemResolutions = new ArrayList<MenuItem>();
        
        if (mResolutions != null) {
        	Menu resolutionMenu = menu.addSubMenu("Camera resolution");
        	for (Camera.Size size : mResolutions) {
        		mItemResolutions.add(resolutionMenu.add(size.width + "p"));
        	} 
        }   

        // new in android 2.3
        if (Build.VERSION.SDK_INT>=9) { 
        	if (mView.hasMultipleCameras()) {
        		Menu cameraMenu = menu.addSubMenu("Choose Camera");
        		mItemFrontCamera =  cameraMenu.add("Front facing camera");
        		mItemBackCamera =  cameraMenu.add("Back facing camera");
        	}
        }    
        
        if (mView.isInSafeMode())
        	mItemSafeMode = menu.add("Turn Safe/Save Mode off");
        else
        	mItemSafeMode = menu.add("Turn Safe/Save Mode on");
        
        if (mView.showsFPS())
        	mItemFPS = menu.add("Hide FPS");
        else
        	mItemFPS = menu.add("Show FPS");        
        
        Log.i(TAG,"resolutions: " + mResolutions);
    }
 
    @Override
    public Dialog onCreateDialog(int id) {
        switch(id) {
        case DIALOG_INFO:
        	AlertDialog.Builder builder = new AlertDialog.Builder(this);
        	builder.setMessage(
        			"Andlisca is an application for slitscan or linescan photography. " +
        			"Use the context menu to change focus and resolution settings.\n" +
        			"\n" +
        			"written by Michael Aschauer (http://m.ash.to)\n"        			
        			)
            	   .setTitle("Info")
        	       .setCancelable(true)
        	       .setNeutralButton("Ok", new DialogInterface.OnClickListener() {
        	           public void onClick(DialogInterface dialog, int id) {
        	                dialog.cancel();
        	           }
        	       });
        	return builder.create();
        	
        	/*
        	Context mContext = getApplicationContext();
        	dialog = new Dialog(mContext);

            dialog.setContentView(R.layout.about);
            dialog.setTitle("About");

            TextView text = (TextView) dialog.findViewById(R.id.text);
            text.setText("Andlisca ....");
            */
        default:
        	return null;
        }
        
    }    
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        Log.i(TAG, "Menu Item selected " + item);
        if (item == mItemSave) {
        	Toast.makeText(this,mView.saveBitmap(), Toast.LENGTH_SHORT).show();
        }
        else if (item == mItemClear)
        	mView.clearImage();      
        else if (item == mItemSafeMode)
        	mView.toggleSafeMode();  
        else if (item == mItemFPS)
        	mView.toggleFPSDisplay();        
        else if (item == mItemFocusAuto)
        	mView.AutofocusNow();          
        else if (item == mItemFocusMacro)
        	mView.setFocusMode(Camera.Parameters.FOCUS_MODE_MACRO); 
        else if (item == mItemFocusInfinity)
        	mView.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY);         
        else if (item == mItemFocusFixed)
        	mView.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED); 
        else if (item == mItemFocusEdof)
        	mView.setFocusMode(Camera.Parameters.FOCUS_MODE_EDOF);
        else if (mItemSizes.contains(item)) 
        		mView.setLineHeight(mItemSizes.indexOf(item)+1); 
        
        if (mResolutions != null) {   
        	if (mItemResolutions.contains(item)) {
        		Log.i(TAG, "item found");
        		Log.i(TAG, "item found "+ mResolutions.get(mItemResolutions.indexOf(item)).width);
        		mView.setResolution(mResolutions.get(mItemResolutions.indexOf(item)));
        	} 
        }
      
        // new in android 2.3
        if (Build.VERSION.SDK_INT>=9) { 
        	if (item == mItemFocusContinuous)
        		mView.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        	else if (item == mItemFrontCamera)
        		mView.setCamera("front");
        	else if (item == mItemBackCamera)
        		mView.setCamera("back");        	
        }                
        return true;
    }
    
}
