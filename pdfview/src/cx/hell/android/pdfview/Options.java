package cx.hell.android.pdfview;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;

public class Options extends PreferenceActivity {
	final static String PREF_TAG = "Options";
	final static String PREF_PAGE_WITH_VOLUME = "pageWithVolume";
	final static String PREF_ZOOM_INCREMENT = "zoomIncrement";
	final static String PREF_INVERT = "invert";
	final static String PREF_ZOOM_ANIMATION = "zoomAnimation";
	final static String PREF_DIRS_FIRST = "dirsFirst";
	final static String PREF_SHOW_EXTENSION = "showExtension";
	final static String PREF_ORIENTATION = "orientation";
	final static String PREF_FULLSCREEN = "fullscreen";
	final static String PREF_PAGE_ANIMATION = "pageAnimation";
	final static String PREF_FADE_SPEED = "fadeSpeed";
	
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		
		addPreferencesFromResource(R.xml.options);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		setOrientation(this);
	}
	
	public static void setOrientation(Activity activity) {
		SharedPreferences options = PreferenceManager.getDefaultSharedPreferences(activity);
		int orientation = Integer.parseInt(options.getString(PREF_ORIENTATION, "0"));
		switch(orientation) {
		case 0: 
			activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
			break;
		case 1:
			activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			break;
		case 2:
			activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
			break;			
		default:
			break;
		}		
	}
}
