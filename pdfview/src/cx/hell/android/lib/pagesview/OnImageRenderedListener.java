package cx.hell.android.lib.pagesview;

import android.graphics.Bitmap;

/**
 * Allow renderer to notify view that new bitmaps are ready.
 * Implemented by PagesView.
 */
public interface OnImageRenderedListener {
	void onImageRendered(int pageNumber, int zoom, int tilex, int tiley, Bitmap bitmap);
	void onRenderingException(RenderingException reason);
}
