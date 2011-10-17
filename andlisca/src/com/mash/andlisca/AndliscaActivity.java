package com.mash.andlisca;

import java.util.ArrayList;
import java.util.List;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
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
    private MenuItem			mItemSize1px;
    private MenuItem			mItemSize2px;
    private MenuItem			mItemSize3px;
    private MenuItem			mItemSize4px;
    private MenuItem			mItemSize5px;
    private MenuItem			mItemFPSToggle; 
    private MenuItem			mItemAbout;
   
    private List<MenuItem>		mItemResolutions;
    private List<Camera.Size>	mResolutions; 
    
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
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "onCreateOptionsMenu");        
        mItemSave = menu.add("Save");
        mItemClear = menu.add("Clear");
        mItemAbout = menu.add("Info");         
        return true;
    }
 
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "Menu Item selected " + item);
        if (item == mItemSave) {
        	Toast.makeText(this,mView.saveBitmap(), Toast.LENGTH_SHORT).show();
        }
        else if (item == mItemClear)
        	mView.clearImage();          
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
        else if (item == mItemSize1px)
        	mView.setLineHeight(1); 
        else if (item == mItemSize2px)
        	mView.setLineHeight(2);
        else if (item == mItemSize3px)
        	mView.setLineHeight(3);
        else if (item == mItemSize4px)
        	mView.setLineHeight(4);        
        else if (item == mItemSize5px)
        	mView.setLineHeight(5);  
        else if (mResolutions != null) {
        	if (mItemResolutions.contains(item))
        		mView.setResolution(mResolutions.get(mItemResolutions.indexOf(item))); 
    	} else if (item == mItemAbout) {
    		showDialog(DIALOG_INFO);
    	}        
        return true;
    }

    @Override
    public void onCreateContextMenu(
    		ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) 
    {
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
	        	//} else if (focusMode == "continous-video") {
	        	//	mItemFocusContinous = focusMenu.add("Continous Video");
	        if (mView.getFocusModes().contains(Camera.Parameters.FOCUS_MODE_EDOF))
	        		mItemFocusEdof= focusMenu.add("EDOF");
        }
        
        Menu sizeMenu = menu.addSubMenu("Line size");
        mItemSize1px = sizeMenu.add("1px");
        mItemSize2px = sizeMenu.add("2px");
        mItemSize3px = sizeMenu.add("3px");
        mItemSize4px = sizeMenu.add("4px");
        mItemSize5px = sizeMenu.add("5px");
                
        mResolutions = mView.getResolutions();
        mItemResolutions = new ArrayList<MenuItem>();
        if (mResolutions != null) {
        	Menu resolutionMenu = menu.addSubMenu("Camera resolution");
        	for (Camera.Size size : mResolutions) {
        		mItemResolutions.add(resolutionMenu.add(size.width + "p"));
        	} 
        }   
        //mItemFPSToggle = menu.add("Show/Hide FPS");
    }
 
    @Override
    protected Dialog onCreateDialog(int id) {
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
        	       .setNegativeButton("Ok", new DialogInterface.OnClickListener() {
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
        else if (item == mItemSize1px)
        	mView.setLineHeight(1); 
        else if (item == mItemSize2px)
        	mView.setLineHeight(2);
        else if (item == mItemSize3px)
        	mView.setLineHeight(3);
        else if (item == mItemSize4px)
        	mView.setLineHeight(4);        
        else if (item == mItemSize5px)
        	mView.setLineHeight(5);  
        else if (mResolutions != null) {
        	if (mItemResolutions.contains(item))
        		mView.setResolution(mResolutions.get(mItemResolutions.indexOf(item))); 
        }
        /*} else if (item == mItemFPSToggle) {
        	mView.toggleFPSDisplay();
        }*/
        return true;
    }
    
}
