package com.mash.andlisca;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.util.Log;
import android.widget.Toast;
 
public class AndliscaPreferences extends PreferenceActivity implements OnSharedPreferenceChangeListener  {
	
		private static final String TAG = "Andlisca::Preferences";
		PreferenceScreen preferenceScreen;
		ListPreference[] cam_resolutions;
		ListPreference[] cam_focus_modes;
		ListPreference cam_id;
			
	
        @Override
        protected void onCreate(Bundle savedInstanceState) {
        	Log.i(TAG,"onCreate Preferences");        	
        	super.onCreate(savedInstanceState);
        	
        	preferenceScreen = getPreferenceScreen();
        	addPreferencesFromResource(R.xml.preferences);
        	
        	cam_resolutions = new ListPreference[2];
        	cam_focus_modes = new ListPreference[2];
        	
        	Bundle bundle = getIntent().getExtras();        	
        	CharSequence[] resolutions = bundle.getCharSequenceArray("cam0_resolutions");
        	CharSequence[] resolutions_values = bundle.getCharSequenceArray("cam0_resolutions_values");
        	CharSequence[] focus_modes = bundle.getCharSequenceArray("cam0_focus_modes");
        	CharSequence[] focus_modes_values = bundle.getCharSequenceArray("cam0_focus_modes_values");
        	int active_camera = bundle.getInt("active_camera");
        	
        	Log.i(TAG,"focus modes: "+ focus_modes);
        	Log.i(TAG,"  values "+ focus_modes_values);
        	
        	for (CharSequence c:focus_modes)
        		Log.i(TAG,"" + c);
        	
        	for (CharSequence c:focus_modes_values)
        		Log.i(TAG,"" + c);
        	
        	cam_resolutions[0] = (ListPreference) findPreference("cam0_resolution_id");      	
        	cam_resolutions[0].setEntries(resolutions); 
        	cam_resolutions[0].setEntryValues(resolutions_values);
        	cam_resolutions[0].setDefaultValue("0");
        	cam_resolutions[0].setEnabled(true);
        	
        	cam_focus_modes[0] = (ListPreference) findPreference("cam0_focus_mode_id");      	
        	cam_focus_modes[0].setEntries(focus_modes); 
        	cam_focus_modes[0].setEntryValues(focus_modes_values);
        	cam_focus_modes[0].setDefaultValue("0");
        	cam_focus_modes[0].setEnabled(true);
        	
        	cam_id = (ListPreference) findPreference("camera_id");   
        	
        	if (!bundle.getBoolean("hasMultipleCameras")) {
        		getPreferenceScreen().removePreference(cam_id);
        		getPreferenceScreen().removePreference(findPreference("cam1_resolution_id"));
        		getPreferenceScreen().removePreference(findPreference("cam1_focus_mode_id"));
        	} else {
        		
        		if (active_camera == 1) {
        			cam_resolutions[0].setEnabled(true);
        			cam_focus_modes[0].setEnabled(true);
        			getPreferenceScreen().removePreference(cam_resolutions[0]);	
        			getPreferenceScreen().removePreference(cam_focus_modes[0]);
        		}
        		
        		CharSequence[] cameras = bundle.getCharSequenceArray("cameras");
            	CharSequence[] cameras_values = bundle.getCharSequenceArray("cameras_values");            	
            	            	
            	cam_id.setEntries(cameras); 
            	cam_id.setEntryValues(cameras_values);
            	cam_id.setDefaultValue("-1"); 
            	cam_id.setEnabled(true);
           		
            	resolutions = bundle.getCharSequenceArray("cam1_resolutions");
            	resolutions_values = bundle.getCharSequenceArray("cam1_resolutions_values");
            	
            	cam_resolutions[1]= (ListPreference) findPreference("cam1_resolution_id");      	
            	cam_resolutions[1].setEntries(resolutions); 
            	cam_resolutions[1].setEntryValues(resolutions_values);
            	cam_resolutions[1].setDefaultValue("-1");
            	cam_resolutions[1].setEnabled(true);
            	
               	focus_modes = bundle.getCharSequenceArray("cam1_focus_modes");
               	focus_modes_values = bundle.getCharSequenceArray("cam1_focus_modes_values");
            	
            	cam_focus_modes[1]= (ListPreference) findPreference("cam1_focus_mode_id");      	
            	cam_focus_modes[1].setEntries(focus_modes); 
            	cam_focus_modes[1].setEntryValues(focus_modes_values);
            	cam_focus_modes[1].setDefaultValue("-1");
            	cam_focus_modes[1].setEnabled(true);            	
            	
            	if (active_camera != 1)  {
            		getPreferenceScreen().removePreference(cam_resolutions[1]);	        		
            		getPreferenceScreen().removePreference(cam_focus_modes[1]);
            	}
        	}
        	Log.i(TAG,"  registerPreferenceChangeListener");
        	getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

        }
        
        public void onSharedPreferenceChanged(SharedPreferences pref, String key) {
        	Log.i(TAG,"preference " + key + " changed!");
        	if (key.equals("camera_id")) {
        		int id = Integer.valueOf(pref.getString("camera_id", "0"));
        		for (int i = 0; i < 2;i++) {
         			if (i == id ) {
         				getPreferenceScreen().addPreference(cam_resolutions[i]);	
         				getPreferenceScreen().addPreference(cam_focus_modes[i]);
        			} else {
        				getPreferenceScreen().removePreference(cam_resolutions[i]);
        				getPreferenceScreen().removePreference(cam_focus_modes[i]);	
        			}
        		}
        	}

        }
        

}