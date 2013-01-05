package cx.hell.android.pdfview;

import android.app.Application;
import android.util.Log;
import cx.hell.android.lib.pdf.PDF;

public class APVApplication extends Application {
    
    private final static String TAG = "cx.hell.android.pdfview";

    public void onCreate() {
        super.onCreate();
        PDF.setApplicationContext(this); // PDF class needs application context to load assets
    }
    
    /**
     * Called by system when low on memory.
     * Currently only logs.
     */
    public void onLowMemory() {
        super.onLowMemory();
        Log.w(TAG, "onLowMemory"); // TODO: free some memory (caches) in native code
    }

}
