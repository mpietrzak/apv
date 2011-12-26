package cx.hell.android.lib.pdf;

import java.io.File;
import java.io.FileDescriptor;
import java.util.List;

import cx.hell.android.lib.pagesview.FindResult;


/**
 * Native PDF - interface to native code.
 */
public class PDF {
	static {
        System.loadLibrary("pdfview2");
	}
	
	/**
	 * Simple size class used in JNI to simplify parameter passing.
	 * This shouldn't be used anywhere outside of pdf-related code.
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
	
	// #ifdef APVPRO
	/**
	 * Java version of fz_outline.
	 */
	public static class Outline {
		public String title;
		public int page;
		public Outline next;
		public Outline down;
		
		public String toString() {
			String s = "Outline(title: " + this.title + ", page: " + page;
			if (this.down != null) {
				s += "down: " + this.down;
			}
			if (this.next != null) {
				s += "next: " + this.next;
			}
			s += ")";
			return s;
		}
	}
	// #endif
	
	/**
	 * Holds pointer to native pdf_t struct.
	 */
	private int pdf_ptr = -1;
	private int invalid_password = 0;
	
	public boolean isValid() {
		return pdf_ptr != 0;
	}
	
	public boolean isInvalidPassword() {
		return invalid_password != 0;
	}

	/**
	 * Parse bytes as PDF file and store resulting pdf_t struct in pdf_ptr.
	 * @return error code
	 */
/*	synchronized private native int parseBytes(byte[] bytes, int box); */
	
	/**
	 * Parse PDF file.
	 * @param fileName pdf file name
	 * @return error code
	 */
	synchronized private native int parseFile(String fileName, int box, String password);
	
	/**
	 * Parse PDF file.
	 * @param fd opened file descriptor
	 * @return error code
	 */
	synchronized private native int parseFileDescriptor(FileDescriptor fd, int box, String password);

	/**
	 * Construct PDF structures from bytes stored in memory.
	 */
/*	public PDF(byte[] bytes, int box) {
		this.parseBytes(bytes, box);
	} */
	
	/**
	 * Construct PDF structures from file sitting on local filesystem.
	 */
	public PDF(File file, int box) {
		this.parseFile(file.getAbsolutePath(), box, "");
	}
	
	/**
	 * Construct PDF structures from opened file descriptor.
	 * @param file opened file descriptor
	 */
	public PDF(FileDescriptor file, int box) {
		this.parseFileDescriptor(file, box, "");
	}
	
	/**
	 * Return page count from pdf_t struct.
	 */
	synchronized public native int getPageCount();
	
	/**
	 * Render a page.
	 * @param n page number, starting from 0
	 * @param zoom page size scaling
	 * @param left left edge
	 * @param right right edge
	 * @param passes requested size, used for size of resulting bitmap
	 * @return bytes of bitmap in Androids format
	 */
	synchronized public native int[] renderPage(int n, int zoom, int left, int top, 
			int rotation, boolean gray, boolean skipImages, PDF.Size rect);
	
	/**
	 * Get PDF page size, store it in size struct, return error code.
	 * @param n 0-based page number
	 * @param size size struct that holds result
	 * @return error code
	 */
	synchronized public native int getPageSize(int n, PDF.Size size);
	
	/**
	 * Export PDF to a text file.
	 */
//	synchronized public native void export();

	/**
	 * Find text on given page, return list of find results.
	 */
	synchronized public native List<FindResult> find(String text, int page);
	
	/**
	 * Clear search.
	 */
	synchronized public native void clearFindResult();
	
	/**
	 * Find text on page, return find results.
	 */
	synchronized public native List<FindResult> findOnPage(int page, String text);

	// #ifdef APVPRO
	/**
	 * Get document outline.
	 */
	synchronized public native Outline getOutline();
	// #endif
	
	/**
	 * Free memory allocated in native code.
	 */
	synchronized private native void freeMemory();

	public void finalize() {
		try {
			super.finalize();
		} catch (Throwable e) {
		}
		this.freeMemory();
	}
}
