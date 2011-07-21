package cx.hell.android.pdfview;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class Options extends PreferenceActivity {
	final static String PREF_TAG = "Options";
	final static String PREF_PAGE_WITH_VOLUME = "pageWithVolume";
	final static String PREF_ZOOM_INCREMENT = "zoomIncrement";
	final static String PREF_INVERT = "invert";
	final static String PREF_ZOOM_ANIMATION = "zoomAnimation";
	final static String PREF_DIRS_FIRST = "dirsFirst";
	final static String PREF_SHOW_EXTENSION = "showExtension";
	final static String PREF_ORIENTATION = "orientation";
	
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
			activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
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
