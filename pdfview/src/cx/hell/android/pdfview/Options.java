package cx.hell.android.pdfview;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class Options extends PreferenceActivity {
	final static String PREF_TAG = "Options";
	final static String PREF_PAGE_WITH_VOLUME = "pageWithVolume";
	final static String PREF_ZOOM_INCREMENT = "zoomIncrement";
	final static String PREF_INVERT = "invert";
	final static String PREF_ZOOM_ANIMATION = "zoomAnimation";
	final static String PREF_DIRS_FIRST = "dirsFirst";
	final static String PREF_SHOW_EXTENSION = "showExtension";
	
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		
		addPreferencesFromResource(R.xml.options);
	}
}
