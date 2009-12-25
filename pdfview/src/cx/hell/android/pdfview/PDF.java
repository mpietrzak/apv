package cx.hell.android.pdfview;


/**
 * Native PDF - interface to native code.
 * TODO: properly release resources
 */
public class PDF {
	static {
        System.loadLibrary("pdfview2");
	}
	
	/**
	 * Simple size class used in JNI to simplify parameter passing.
	 * This shouldn't be used anywhere outide of pdf-related code.
	 */
	public static class Size implements Cloneable {
		public int width;
		public int height;
		
		public Size() {
			this.width = 0;
			this.height = 0;
		}
		
		public Size(int width, int height) {
			this.width = width;
			this.height = height;
		}
		
		public Size clone() {
			return new Size(this.width, this.height);
		}
	}
	
	@SuppressWarnings("unused")
	private int pdf_ptr = 0;
	
	private native int parseBytes(byte[] bytes);
	
	public PDF(byte[] bytes) {
		this.parseBytes(bytes);
	}
	
	public native int getPageCount();
	
	/**
	 * Render a page.
	 * @param n page number, starting from 0
	 * @param zoom page size scalling
	 * @param left left edge
	 * @param right right edge
	 * @param passes requested size, used for size of resulting bitmap
	 */
	public native int[] renderPage(int n, int zoom, int left, int top, PDF.Size rect);
	
	public native int getPageSize(int n, PDF.Size size);
	
	/**
	 * Free memory allocated in native code.
	 */
	private native void freeMemory();
	
	public void finalize() {
		this.freeMemory();
	}
}
