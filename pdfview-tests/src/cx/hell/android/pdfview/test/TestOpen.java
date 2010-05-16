package cx.hell.android.pdfview.test;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;

import android.os.Environment;
import android.test.AndroidTestCase;
import android.util.Log;
import cx.hell.android.pdfview.PDF;

public class TestOpen extends AndroidTestCase {
		
	private final static String TAG = "cx.hell.android.pdfview.test";
	
	/**
	 * PDF files directory relative to sdcard directory.
	 */
	private final static String PDF_FILES_PATH = "data/cx.hell.android.pdfview/test/pdf";
	
	/**
	 * Get absolute path to test file whose basename is name.
	 */
	private String getAbsoluteTestFilePath(String name) {
		File f = new File(Environment.getExternalStorageDirectory() + File.separator + PDF_FILES_PATH + File.separator + name);
		return f.getAbsolutePath();
	}
	
	public void testOpenByFileDescriptor() throws Throwable {
		String path = this.getAbsoluteTestFilePath("hell.cx.pdf");
		FileInputStream i = new FileInputStream(path);
		FileDescriptor fd = i.getFD();
		Exception exception = null;
		try {
			PDF pdf = new PDF(fd);
			Log.d(TAG, "opened " + path + ": " + pdf);
		} catch (Exception e) {
			exception = e;
			Log.d(TAG, "failed to open " + path + ", got " + e);
		}
		assertNull(exception);
	}
	
	public void testGetPageCount() throws Throwable {
		String path = this.getAbsoluteTestFilePath("hell.cx.pdf");
		FileInputStream i = new FileInputStream(path);
		FileDescriptor fd = i.getFD();
		Exception exception = null;
		int pageCount = -1;
		try {
			PDF pdf = new PDF(fd);
			Log.d(TAG, "opened " + path + ": " + pdf);
			pageCount = pdf.getPageCount();
			Log.d(TAG, "page count is " + path + ": " + pageCount);
		} catch (Exception e) {
			exception = e;
			Log.d(TAG, "failed to open " + path + ", got " + e);
		}
		assertNull(exception);
		assertEquals(1, pageCount);
	}
}


