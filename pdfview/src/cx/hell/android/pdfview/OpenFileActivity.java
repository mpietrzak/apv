package cx.hell.android.pdfview;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import android.app.Activity;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import cx.hell.android.lib.pagesview.PagesView;


/**
 * Minimalistic file browser.
 */
public class OpenFileActivity extends Activity {
	
	private final static String TAG = "cx.hell.android.pdfview";
	
	private PDF pdf = null;
	private PagesView pagesView = null;
	private PDFPagesProvider pdfPagesProvider = null;
	
	private MenuItem aboutMenuItem = null;
	private MenuItem gotoPageMenuItem = null;
	private EditText pageNumberInputField = null;

    /**
     * Called when the activity is first created.
     * TODO: initialize dialog fast, then move file loading
     * to other thread
     * TODO: add progress bar for file load
     * TODO: add progress icon for file rendering
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        
        this.pagesView = new PagesView(this);
        this.pdf = this.getPDF();
        this.pdfPagesProvider = new PDFPagesProvider(pdf);
        pagesView.setPagesProvider(pdfPagesProvider);
        layout.addView(pagesView);
        
        this.setContentView(layout);
    }
    
    /**
     * Return PDF instance wrapping file referenced by Intent.
     * Currently reads all bytes to memory, in future local files
     * should be passed to native code and remote ones should
     * be downloaded to local tmp dir.
     * @return PDF instance
     */
    private PDF getPDF() {
        final Intent intent = getIntent();
		Uri uri = intent.getData();
        byte[] pdfFileBytes = readBytes(uri);
        PDF pdf = new PDF(pdfFileBytes);
        return pdf;
    }
    
    /**
     * Read bytes from uri.
     * TODO: do not load file contents to memory - let underlying native code read content directly
     */
    private byte[] readBytes(Uri uri) {
    	try {
	    	InputStream i = this.openUri(uri);
	    	return StreamUtils.readBytesFully(i);
    	} catch (IOException e) {
    		throw new RuntimeException(e);
    	}
    }
    
    private InputStream openUri(Uri uri) throws IOException {
    	Log.i(TAG, "opening uri " + uri);
    	if (uri.getScheme().equals("http")
    			|| uri.getScheme().equals("https")
    			|| uri.getScheme().equals("file")) {
    		URL url = new URL(uri.toString());
    		URLConnection urlConnection = url.openConnection();
    		InputStream i = urlConnection.getInputStream();
    		return i;
    	} else if (uri.getScheme().equals("content")) {
	    	ContentResolver cr = this.getContentResolver();
	    	InputStream i = new BufferedInputStream(cr.openInputStream(uri));
    		return i; 
    	} else {
    		throw new RuntimeException("don't know how to open " + uri);
    	}
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
    	if (menuItem == this.aboutMenuItem) {
			Intent intent = new Intent();
			intent.setClass(this, AboutPDFViewActivity.class);
			this.startActivity(intent);
    		return true;
    	} else if (menuItem == this.gotoPageMenuItem) {
    		this.showGotoPageDialog();
    	}
    	return false;
    }
    
    private void showGotoPageDialog() {
    	final Dialog d = new Dialog(this);
    	d.setTitle(R.string.goto_page_dialog_title);
    	LinearLayout contents = new LinearLayout(this);
    	contents.setOrientation(LinearLayout.VERTICAL);
    	TextView label = new TextView(this);
    	label.setText("Page number from " + 1 + " to " + this.pdfPagesProvider.getPageCount());
    	this.pageNumberInputField = new EditText(this);
    	Button goButton = new Button(this);
    	goButton.setText(R.string.goto_page_go_button);
    	goButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				int pageNumber = Integer.parseInt(OpenFileActivity.this.pageNumberInputField.getText().toString()) - 1;
				OpenFileActivity.this.gotoPage(pageNumber);
				d.hide();
			}
    	});
    	LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    	params.leftMargin = 5;
    	params.rightMargin = 5;
    	params.bottomMargin = 2;
    	params.topMargin = 2;
    	contents.addView(label, params);
    	contents.addView(pageNumberInputField, params);
    	contents.addView(goButton, params);
    	d.setContentView(contents);
    	d.show();
    }
    
    private void gotoPage(int page) {
    	Log.i(TAG, "rewind to page " + page);
    	if (this.pagesView != null) {
    		this.pagesView.scrollToPage(page);
    	}
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	this.gotoPageMenuItem = menu.add(R.string.goto_page);
    	this.aboutMenuItem = menu.add(R.string.about);
    	return true;
    }
}



