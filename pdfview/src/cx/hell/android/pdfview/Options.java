package cx.hell.android.pdfview;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class Options extends PreferenceActivity {
	public final static String PREF_TAG = "Options";
	public final static String PREF_PAGE_WITH_VOLUME = "pageWithVolume";
	public final static String PREF_ZOOM_INCREMENT = "zoomIncrement";
	public final static String PREF_INVERT = "invert";
	public final static String PREF_ZOOM_ANIMATION = "zoomAnimation";
	public final static String PREF_DIRS_FIRST = "dirsFirst";
	public final static String PREF_SHOW_EXTENSION = "showExtension";
	public final static String PREF_ORIENTATION = "orientation";
	public final static String PREF_FULLSCREEN = "fullscreen";
	public final static String PREF_PAGE_ANIMATION = "pageAnimation";
	public final static String PREF_FADE_SPEED = "fadeSpeed";
	public final static String PREF_RENDER_AHEAD = "renderAhead";
	public final static String PREF_GRAY = "gray";
	public final static String PREF_COLOR_MODE = "colorMode";
	public final static String PREF_OMIT_IMAGES = "omitImages";
	public final static String PREF_VERTICAL_SCROLL_LOCK = "verticalScrollLock";
	public final static String PREF_BOX = "boxType";
	
	public final static int COLOR_MODE_NORMAL = 0;
	public final static int COLOR_MODE_INVERT = 1;
	public final static int COLOR_MODE_GRAY = 2;
	public final static int COLOR_MODE_INVERT_GRAY = 3;
	public final static int COLOR_MODE_BLACK_ON_YELLOWISH = 4;
	public final static int COLOR_MODE_GREEN_ON_BLACK = 5;
	public final static int COLOR_MODE_RED_ON_BLACK = 6;
	private final static int[] foreColors = { 
		Color.BLACK, Color.WHITE, Color.BLACK, Color.WHITE,
		Color.BLACK, Color.GREEN, Color.RED };
	private final static int[] backColors = {
		Color.WHITE, Color.BLACK, Color.WHITE, Color.BLACK,
		Color.rgb(239, 219, 189),
		Color.BLACK, Color.BLACK };
	
	private static final float[][] colorMatrices = {
		null, /* COLOR_MODE_NORMAL */
		
		{-1.0f, 0.0f, 0.0f, 0.0f, 255.0f, /* COLOR_MODE_INVERT */
		0.0f, -1.0f, 0.0f, 0.0f, 255.0f,
		0.0f, 0.0f, -1.0f, 0.0f, 255.0f,
		0.0f, 0.0f, 0.0f, 0.0f, 255.0f}, 
		
		{0.0f, 0.0f, 0.0f, 0.0f, 255.0f, /* COLOR_MODE_GRAY */
		0.0f, 0.0f, 0.0f, 0.0f, 255.0f,
		0.0f, 0.0f, 0.0f, 0.0f, 255.0f,
		0.0f, 0.0f, 0.0f, 1.0f, 0.0f},
		
		{0.0f, 0.0f, 0.0f, 0.0f, 255.0f, /* COLOR_MODE_INVERT_GRAY */
		0.0f, 0.0f, 0.0f, 0.0f, 255.0f,
		0.0f, 0.0f, 0.0f, 0.0f, 255.0f,
		0.0f, 0.0f, 0.0f, -1.0f, 255.0f}, 

		{0.0f, 0.0f, 0.0f, 0.0f, 239.0f, /* COLOR_MODE_BLACK_ON_YELLOWISH */
		0.0f, 0.0f, 0.0f, 0.0f, 219.0f,
		0.0f, 0.0f, 0.0f, 0.0f, 189.0f,
		0.0f, 0.0f, 0.0f, 1.0f, 0.0f},
		
		{0.0f, 0.0f, 0.0f, 0.0f, 0f, /* COLOR_MODE_GREEN_ON_BLACK */
		0.0f, 0.0f, 0.0f, 0.0f, 255.0f,
		0.0f, 0.0f, 0.0f, 0.0f, 0f,
		0.0f, 0.0f, 0.0f, -1.0f, 255.0f}, 

		{0.0f, 0.0f, 0.0f, 0.0f, 255.0f, /* COLOR_MODE_RED_ON_BLACK */
		0.0f, 0.0f, 0.0f, 0.0f, 0f,
		0.0f, 0.0f, 0.0f, 0.0f, 0f,
		0.0f, 0.0f, 0.0f, -1.0f, 255.0f} 
	};

	public static float[] getColorModeMatrix(int colorMode) {
		return colorMatrices[colorMode];
	}
	
	public static boolean isGray(int colorMode) {
		return COLOR_MODE_GRAY <= colorMode;
	}
	
	public static int getForeColor(int colorMode) {
		return foreColors[colorMode];
	}
	
	public static int getBackColor(int colorMode) {
		return backColors[colorMode];
	}
	
	public static int getColorMode(SharedPreferences pref) {
		return Integer.parseInt(pref.getString(Options.PREF_COLOR_MODE, "0"));
	}
	
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
