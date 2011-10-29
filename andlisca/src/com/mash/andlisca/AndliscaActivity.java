package com.mash.andlisca;

import java.util.ArrayList;
import java.util.List;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
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
    private MenuItem	 		mItemSettings;
    private MenuItem	 		mItemFlash;
    
    private List<MenuItem>		mItemResolutions;
    private List<Camera.Size>	mResolutions; 
    
    protected String[]			resolutions;
          
    private static final int	DIALOG_INFO=0;
    private PowerManager.WakeLock mWakeLock;
    SharedPreferences preferences;
    
    public AndliscaActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	if (Log.isLoggable(TAG, Log.INFO))
    		Log.i(TAG, "onCreate");
        
        super.onCreate(savedInstanceState);
        
        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
                WindowManager.LayoutParams.FLAG_FULLSCREEN);     
        mView = new AndliscaView(this);
        //setContentView(R.layout.main);
       
        registerForContextMenu(mView);
        
        mView.showsFPS(preferences.getBoolean("show_fps", false));
        
         
        
        FrameLayout l = new FrameLayout(this);
        l.addView(mView);
        
        //LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.WRAP_CONTENT);

        
        View btnView = getLayoutInflater().inflate(R.layout.buttons, null);

        //LinearLayout bl = new LinearLayout(this);
       /* 
        ImageButton btn_flash = new ImageButton(this); 
        btn_flash.setImageResource(R.drawable.flash_on);
        btn_flash.setMaxHeight(32);
        btn_flash.setLayoutParams(params);
        btn_flash.setBackgroundColor(0);

        
        ImageButton btn_af = new ImageButton(this); 
        btn_af.setImageResource(R.drawable.auto_focus);
        btn_af.setMaxHeight(32);
        btn_af.setLayoutParams(params);
        btn_af.setBackgroundColor(0);
*/
        
        l.addView(btnView);
        Animation rotateAnim = AnimationUtils.loadAnimation(this, R.anim.rotate);
        LayoutAnimationController animController = new LayoutAnimationController(rotateAnim, 0);
        ((RelativeLayout)btnView.findViewById(R.id.InnerTopLayout)).setLayoutAnimation(animController);
        ((RelativeLayout)btnView.findViewById(R.id.InnerBottomLayout)).setLayoutAnimation(animController);
        ((RelativeLayout)btnView.findViewById(R.id.InnerTextLayout)).setLayoutAnimation(animController);
       
        
        setContentView(l);
        
        final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "DoNotDim");
        mWakeLock.acquire();        
    }
    
    @Override
    protected void onPause() {
    	if (Log.isLoggable(TAG, Log.INFO))
    		Log.i(TAG, "onPause");
    	super.onPause();
    	mWakeLock.release();
    }
    
    @Override
    protected void onResume() {
    	if (Log.isLoggable(TAG, Log.INFO))
    		Log.i(TAG, "onResume");
    	super.onResume();
    	mWakeLock.acquire();
    }    

    @Override
    protected void onDestroy() {
    	if (Log.isLoggable(TAG, Log.INFO))
    		Log.i(TAG, "onDestroy");
    	mWakeLock.release();
    	super.onResume();    	
    }    

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	if (Log.isLoggable(TAG, Log.INFO))
    		Log.i(TAG, "onCreateOptionsMenu");        
        mItemSave = menu.add("Save");
        mItemSave.setIcon(android.R.drawable.ic_menu_save);
        mItemClear = menu.add("Clear");
        mItemClear.setIcon(android.R.drawable.ic_menu_delete);
        mItemSettings = menu.add("Settings");
        mItemSettings.setIcon(android.R.drawable.ic_menu_preferences);
        mItemInfo = menu.add("Info");   
        mItemInfo.setIcon(android.R.drawable.ic_menu_info_details);              
        return true;
    }
 
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	if (Log.isLoggable(TAG, Log.INFO))
    		Log.i(TAG, "Menu Item selected " + item);
        if (item == mItemSave) {
        	Toast.makeText(this,mView.saveBitmap(), Toast.LENGTH_SHORT).show();
        } else if (item == mItemClear) {
        	mView.clearImage();          
    	} else if (item == mItemSettings) {
    		//openContextMenu(mView);
    		
    		openSettings();

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
    	} else if (item == mItemInfo) {
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
        
        /*
        mItemSizes = new ArrayList<MenuItem>();
        Menu sizeMenu = menu.addSubMenu("Line size");
        for (int i=1; i<=5; i++) {
    		mItemSizes.add(sizeMenu.add(i + "px"));
    	}               
        */
        
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
        		Menu cameraMenu = menu.addSubMenu("Choose camera");
        		mItemFrontCamera =  cameraMenu.add("Front facing camera");
        		mItemBackCamera =  cameraMenu.add("Back facing camera");
        	}
        }    
        
        /*
        Menu bufferMenu = menu.addSubMenu("Max buffer width");
        mItemBufferSizes = new ArrayList<MenuItem>();
        for (int i=2; i<=4; i++) {
        	mItemBufferSizes.add(bufferMenu.add( (i*1024) + "px"));
    	}
        mItemBufferSizes.add(bufferMenu.add("No limit"));
         */
      
        if (mView.isFlashOn())
        	mItemFlash = menu.add("Flash off");
        else
        	mItemFlash = menu.add("Flash on");   
        
        /*
        if (mView.isInSafeMode())
        	mItemSafeMode = bufferMenu.add("Turn Safe/Save Mode off");
        else
        	mItemSafeMode = bufferMenu.add("Turn Safe/Save Mode on");
        
                
        if (mView.showsFPS())
        	mItemFPS = menu.add("Hide FPS");
        else
        	mItemFPS = menu.add("Show FPS");        
        */
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
    	if (Log.isLoggable(TAG, Log.INFO))
    		Log.i(TAG, "Menu Item selected " + item);
    	
        if (item == mItemSave) {
        	Toast.makeText(this,mView.saveBitmap(), Toast.LENGTH_SHORT).show();
        }
        else if (item == mItemClear)
        	mView.clearImage();      
        else if (item == mItemSafeMode)
        	mView.toggleAutoSave();  
        else if (item == mItemFPS)
        	mView.toggleFPSDisplay(); 
        else if (item == mItemFlash)
        	mView.toggleFlash();        
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
        /*
        else if (mItemSizes.contains(item)) 
        		mView.setLineHeight(mItemSizes.indexOf(item)+1); 
        else if (mItemBufferSizes.contains(item))  {
    		if(mItemBufferSizes.indexOf(item) < 3)
    			mView.setMaxBufferSize( (mItemBufferSizes.indexOf(item)+2) * 1024);
    		else
    			mView.setMaxBufferSize(0);
        }
        */
        
        if (mResolutions != null) {   
        	if (mItemResolutions.contains(item)) {
        		Log.i(TAG, "item found");
        		Log.i(TAG, "item found "+ mResolutions.get(mItemResolutions.indexOf(item)).width);
        		mView.setResolutionById(mItemResolutions.indexOf(item));
        	} 
        }
      
        // new in android 2.3
        if (Build.VERSION.SDK_INT>=9) { 
        	if (item == mItemFocusContinuous)
        		mView.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        	else if (item == mItemFrontCamera)
        		mView.openCamera("front");
        	else if (item == mItemBackCamera)
        		mView.openCamera("back");        	
        }                
        return true;
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

    public void on_btn_flash_clicked(View view) {
    	mView.toggleFlash();
        if (mView.isFlashOn()) {
        	((ImageButton) view).setImageResource(R.drawable.flash_on);
        } else {
        	((ImageButton) view).setImageResource(R.drawable.flash_off);
        }
    }
    
    public void on_btn_af_clicked(View view) {
    	mView.AutofocusNow();
    	Toast.makeText(this,"Triggerd Autofocus. Focusing now.", Toast.LENGTH_SHORT).show();
    }        
    
    public void on_save(View view) {
    	Toast.makeText(this,mView.saveBitmap(), Toast.LENGTH_SHORT).show();
    }    
    
    public void on_delete(View view) {
    	mView.clearImage();
    	Toast.makeText(this,"Image cleared.", Toast.LENGTH_SHORT).show();
    }   

    public void on_autosave(View view) {
    	if (mView.isAutoSave()) {
    		Toast.makeText(this,"Disable Autosave", Toast.LENGTH_SHORT).show();
    		mView.setAutoSave(false);
    		((ImageButton) view).setImageResource(R.drawable.autosave_off);
    	} else {
    		Toast.makeText(this,"Enable Autosave", Toast.LENGTH_SHORT).show();
    		mView.setAutoSave(true);
    		((ImageButton) view).setImageResource(R.drawable.autosave_on);
    	}
    		
    } 
    
    public void on_settings(View view) {
    	openSettings();
    } 
    
    public void on_info(View view) {
    	showDialog(DIALOG_INFO);
    }     
    
    public void openSettings() {
		Intent settingsActivity = new Intent(getBaseContext(),
                AndliscaPreferences.class);
		    		
		settingsActivity.putExtra("cam0_resolutions",mView.getResolutionStrings(0));
		settingsActivity.putExtra("cam0_resolutions_values",mView.getResolutionIds(0));
		settingsActivity.putExtra("cam0_focus_modes",mView.getFocusModesStrings(0));
		settingsActivity.putExtra("cam0_focus_modes_values",mView.getFocusModesIds(0));
		settingsActivity.putExtra("hasMultipleCameras",mView.hasMultipleCameras());
		settingsActivity.putExtra("cameras",mView.getCameraStrings());
		settingsActivity.putExtra("cameras_values",mView.getCameraIds());
		settingsActivity.putExtra("active_camera",mView.getCameraId());
		
		if(mView.hasMultipleCameras()) {
    		settingsActivity.putExtra("cam1_resolutions",mView.getResolutionStrings(1));
    		settingsActivity.putExtra("cam1_resolutions_values",mView.getResolutionIds(1));
		}
		    		
		startActivity(settingsActivity);		
    
    }
    
}
