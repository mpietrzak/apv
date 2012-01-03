package cx.hell.android.pdfview;

// #ifdef pro
// import java.lang.reflect.InvocationTargetException;
// import java.lang.reflect.Method;
// 
// import android.util.Log;
// import android.view.View;
// #endif

/**
 * Find newer methods using reflection and call them if found.
 */
final public class AndroidReflections {

// #ifdef pro
// 	private final static String TAG = "cx.hell.android.pdfview";
// 
// 	public static void setScrollbarFadingEnabled(View view, boolean fadeScrollbars) {
// 		Class<View> viewClass = View.class;
// 		Method sfeMethod = null;
// 		try {
// 			sfeMethod = viewClass.getMethod("setScrollbarFadingEnabled", boolean.class);
// 		} catch (NoSuchMethodException e) {
// 			// nwm
// 			Log.d(TAG, "View.setScrollbarFadingEnabled not found");
// 			return;
// 		}
// 		try {
// 			sfeMethod.invoke(view, fadeScrollbars);
// 		} catch (InvocationTargetException e) {
// 			/* should not throw anything according to Android Reference */
// 			/* TODO: ui error handling */
// 			throw new RuntimeException(e);
// 		} catch (IllegalAccessException e) {
// 			/* TODO: wat do? */
// 			Log.w(TAG, "View.setScrollbarFadingEnabled exists, but is not visible: " + e);
// 		}
// 	}
// #endif
	
}
