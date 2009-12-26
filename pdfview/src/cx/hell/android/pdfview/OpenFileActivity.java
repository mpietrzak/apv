package cx.hell.android.pdfview;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.widget.LinearLayout;
import cx.hell.android.lib.pagesview.PagesView;


public class OpenFileActivity extends Activity {

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        
        PagesView pagesView = new PagesView(this);
        PDF pdf = this.getPDF();
        PDFPagesProvider pdfPagesProvider = new PDFPagesProvider(pdf);
        pagesView.setPagesProvider(pdfPagesProvider);
        layout.addView(pagesView);
        
        this.setContentView(layout);
    }
    
    private PDF getPDF() {
        final Intent intent = getIntent();
		Uri uri = intent.getData();
        byte[] pdfFileBytes = readBytes(uri);
        Log.i("cx.hell.android.pdfview2", "pdf byte count: " + pdfFileBytes.length);
        PDF pdf = new PDF(pdfFileBytes);
        Log.i("cx.hell.android.pdfview2", "pdf: " + pdf);
        Log.i("cx.hell.android.pdfview2", "page count: " + pdf.getPageCount());
        return pdf;
    }
    
    /**
     * Read bytes from uri.
     * TODO: do not load file contens to memory - left underlying native code read content directly
     */
    private byte[] readBytes(Uri uri) {
    	try {
	    	ContentResolver cr = this.getContentResolver();
	    	InputStream i = new BufferedInputStream(cr.openInputStream(uri));
    		return StreamUtils.readBytesFully(i);
    	} catch (IOException e) {
    		/* TODO: user friendly error message */
    		throw new RuntimeException(e);
    	}
    }
}



