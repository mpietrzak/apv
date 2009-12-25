package cx.hell.android.lib.pagesview;

import android.graphics.Bitmap;

public interface OnImageRenderedListener {
	void onImageRendered(int pageNumber, int zoom, int tilex, int tiley, Bitmap bitmap);
	void onRenderingException(RenderingException reason);
}
