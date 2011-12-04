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
	public final static String PREF_SIDE_MARGINS = "sideMargins2"; // sideMargins was boolean
	public final static String PREF_TOP_MARGIN = "topMargin";
	public final static String PREF_EXTRA_CACHE = "extraCache";
	public final static String PREF_DOUBLE_TAP = "doubleTap";
	
	public final static String PREF_VOLUME_PAIR = "volumePair";
	public final static String PREF_ZOOM_PAIR = "zoomPair";
	public final static String PREF_LONG_ZOOM_PAIR = "longZoomPair";
	public final static String PREF_UP_DOWN_PAIR = "upDownPair";
	public final static String PREF_LEFT_RIGHT_PAIR = "leftRightPair";
	public final static String PREF_RIGHT_UP_DOWN_PAIR = "rightUpDownPair";
	public final static String PREF_EINK = "eink";
	public final static String PREF_NOOK2 = "nook2";
	public final static String PREF_KEEP_ON = "keepOn";
	public final static String PREF_SHOW_ZOOM_ON_SCROLL = "showZoomOnScroll";
	
	public final static int PAGE_NUMBER_DISABLED = 100;
	public final static int ZOOM_BUTTONS_DISABLED = 100;
	
	public final static int DOUBLE_TAP_NONE = 0;
	public final static int DOUBLE_TAP_ZOOM = 1;
	public final static int DOUBLE_TAP_ZOOM_IN_OUT = 2;
	
	public final static int PAIR_NONE = 0;
	public final static int PAIR_SCREEN = 1;
	public final static int PAIR_PAGE = 2;
	public final static int PAIR_ZOOM_1020 = 3;
	public final static int PAIR_ZOOM_1050 = 4;
	public final static int PAIR_ZOOM_1100 = 5;
	public final static int PAIR_ZOOM_1200 = 6;
	public final static int PAIR_ZOOM_1414 = 7;
	public final static int PAIR_ZOOM_2000 = 8;
	public final static int PAIR_PAGE_TOP = 9;
	public final static int PAIR_SCREEN_REV = 10;
	public final static int PAIR_PAGE_REV = 11;
	public final static int PAIR_PAGE_TOP_REV = 12;
	
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
	
	public static int getIntFromString(SharedPreferences pref, String option, int def) {
		return Integer.parseInt(pref.getString(option, ""+def));
		
	}

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
		return getIntFromString(pref, PREF_COLOR_MODE, 0);
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
	
	/* returns true when the calling app is responsible for monitoring */
	public static boolean setOrientation(Activity activity) {
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
		case 3:
			activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_BEHIND);
			return true;
		default:
			break;
		}
		return false;
	}
}
