package cx.hell.android.pdfview;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import cx.hell.android.lib.pagesview.FindResult;
import cx.hell.android.lib.pagesview.PagesView;


/**
 * Document display activity.
 */
public class OpenFileActivity extends Activity {
	
	private final static String TAG = "cx.hell.android.pdfview";
	
	private PDF pdf = null;
	private PagesView pagesView = null;
	private PDFPagesProvider pdfPagesProvider = null;
	
	private MenuItem aboutMenuItem = null;
	private MenuItem gotoPageMenuItem = null;
	private MenuItem rotateLeftMenuItem = null;
	private MenuItem rotateRightMenuItem = null;
	private MenuItem findTextMenuItem = null;
	private MenuItem clearFindTextMenuItem = null;
	
	private EditText pageNumberInputField = null;
	private EditText findTextInputField = null;
	
	private LinearLayout findButtonsLayout = null;
	private Button findNextButton = null;
	private Button findHideButton = null;
	// currently opened file path
	private String filePath = "/";
	
    /**
     * Called when the activity is first created.
     * TODO: initialize dialog fast, then move file loading to other thread
     * TODO: add progress bar for file load
     * TODO: add progress icon for file rendering
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        
        this.findButtonsLayout = new LinearLayout(this);
        this.findButtonsLayout.setOrientation(LinearLayout.HORIZONTAL);
        this.findButtonsLayout.setVisibility(View.GONE);
        this.findButtonsLayout.setGravity(Gravity.CENTER);
        this.findNextButton = new Button(this);
        this.findNextButton.setText("Next");
        this.findButtonsLayout.addView(this.findNextButton);
        this.findHideButton = new Button(this);
        this.findHideButton.setText("Hide");
        this.findButtonsLayout.addView(this.findHideButton);
        this.setFindButtonHandlers();
        layout.addView(this.findButtonsLayout);
        
        this.pagesView = new PagesView(this);
        this.pdf = this.getPDF();
        this.pdfPagesProvider = new PDFPagesProvider(pdf);
        pagesView.setPagesProvider(pdfPagesProvider);
        layout.addView(pagesView);
        
        this.setContentView(layout);
        gotoLastPage();
    }

	/** 
	 * Save the current page before exiting
	 */
	@Override
	protected void onPause() {
		saveLastPage();
		super.onPause();
	}

	/**
    /**
     * Set handlers on findNextButton and findHideButton.
     */
    private void setFindButtonHandlers() {
    	this.findNextButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				OpenFileActivity.this.findNext();
			}
    	});
    	
    	this.findHideButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				OpenFileActivity.this.findHide();
			}
    	});
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
		filePath = uri.getPath();
		if (uri.getScheme().equals("file")) {
    		return new PDF(new File(filePath));
    	} else if (uri.getScheme().equals("content")) {
    		ContentResolver cr = this.getContentResolver();
    		FileDescriptor fileDescriptor;
			try {
				fileDescriptor = cr.openFileDescriptor(uri, "r").getFileDescriptor();
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e); // TODO: handle errors
			}
    		return new PDF(fileDescriptor);
    	} else {
    		throw new RuntimeException("don't know how to get filename from " + uri);
    	}
    }
    
    /**
     * Handle menu.
     * @param menuItem selected menu item
     * @return true if menu item was handled
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
    	if (menuItem == this.aboutMenuItem) {
			Intent intent = new Intent();
			intent.setClass(this, AboutPDFViewActivity.class);
			this.startActivity(intent);
    		return true;
    	} else if (menuItem == this.gotoPageMenuItem) {
    		this.showGotoPageDialog();
    	} else if (menuItem == this.rotateLeftMenuItem) {
    		this.pagesView.rotate(-1);
    	} else if (menuItem == this.rotateRightMenuItem) {
    		this.pagesView.rotate(1);
    	} else if (menuItem == this.findTextMenuItem) {
    		this.showFindDialog();
    	} else if (menuItem == this.clearFindTextMenuItem) {
    		this.pagesView.setFindMode(false);
    	}
    	return false;
    }
    
    /**
     * Show error message to user.
     * @param message message to show
     */
    private void errorMessage(String message) {
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	AlertDialog dialog = builder.setMessage(message).setTitle("Error").create();
    	dialog.show();
    }
    
    /**
     * Called from menu when user want to go to specific page.
     */
    private void showGotoPageDialog() {
    	final Dialog d = new Dialog(this);
    	d.setTitle(R.string.goto_page_dialog_title);
    	LinearLayout contents = new LinearLayout(this);
    	contents.setOrientation(LinearLayout.VERTICAL);
    	TextView label = new TextView(this);
    	final int pagecount = this.pdfPagesProvider.getPageCount();
    	label.setText("Page number from " + 1 + " to " + pagecount);
    	this.pageNumberInputField = new EditText(this);
    	this.pageNumberInputField.setInputType(InputType.TYPE_CLASS_NUMBER);
    	this.pageNumberInputField.setText("" + this.pagesView.getCurrentPage());
    	Button goButton = new Button(this);
    	goButton.setText(R.string.goto_page_go_button);
    	goButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				int pageNumber = -1;
				try {
					pageNumber = Integer.parseInt(OpenFileActivity.this.pageNumberInputField.getText().toString())-1;
				} catch (NumberFormatException e) {
					/* ignore */
				}
				d.dismiss();
				if (pageNumber >= 0 && pageNumber < pagecount) {
					OpenFileActivity.this.gotoPage(pageNumber);

				} else {
					OpenFileActivity.this.errorMessage("Invalid page number");
				}
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
    
    /**
     * Called after submitting go to page dialog.
     * @param page page number, 0-based
     */
    private void gotoPage(int page) {
    	Log.i(TAG, "rewind to page " + page);
    	if (this.pagesView != null) {
    		this.pagesView.scrollToPage(page);
    	}
    }
    
    /**
     * Goto the last open page if possible
     */
    private void gotoLastPage() {
        Bookmark b = new Bookmark(this.getApplicationContext()).open();
        int lastpage = b.getLast(filePath);
        b.close();
        if (lastpage > 1) {
        	Handler mHandler = new Handler();
        	Runnable mUpdateTimeTask = new GotoPageThread(lastpage);
        	mHandler.postDelayed(mUpdateTimeTask, 2000);
        }    	
    }

    /**
     * Save the last page in the bookmarks
     */
    private void saveLastPage() {
        Bookmark b = new Bookmark(this.getApplicationContext()).open();
        b.setLast(filePath, pagesView.getCurrentPage());
        b.close();
        Log.i(TAG, "last page saved for "+filePath);    
    }
    
    /**
     * Create options menu, called by Android system.
     * @param menu menu to populate
     * @return true meaning that menu was populated
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	super.onCreateOptionsMenu(menu);
    	this.gotoPageMenuItem = menu.add(R.string.goto_page);
    	this.rotateRightMenuItem = menu.add(R.string.rotate_page_left);
    	this.rotateLeftMenuItem = menu.add(R.string.rotate_page_right);
		this.findTextMenuItem = menu.add(R.string.find_text);
    	this.clearFindTextMenuItem = menu.add(R.string.clear_find_text);
    	this.aboutMenuItem = menu.add(R.string.about);
    	return true;
    }
    
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		Log.i(TAG, "onConfigurationChanged(" + newConfig + ")");
	}
    
	/**
	 * Thread to delay the gotoPage action when opening a PDF file
	 */
	private class GotoPageThread implements Runnable {
		int page;

		public GotoPageThread(int page) {
			this.page = page;
		}

		public void run() {
			gotoPage(page - 1);
		}
	}
}
    /**
     * Prepare menu contents.
     * Hide or show "Clear find results" menu item depending on whether
     * we're in find mode.
     * @param menu menu that should be prepared
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	super.onPrepareOptionsMenu(menu);
    	this.clearFindTextMenuItem.setVisible(this.pagesView.getFindMode());
    	return true;
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
      super.onConfigurationChanged(newConfig);
      Log.i(TAG, "onConfigurationChanged(" + newConfig + ")");
    }
    
    /**
     * Show find dialog.
     * Very pretty UI code ;)
     */
    private void showFindDialog() {
    	Log.d(TAG, "find dialog...");
    	final Dialog dialog = new Dialog(this);
    	dialog.setTitle(R.string.find_dialog_title);
    	LinearLayout contents = new LinearLayout(this);
    	contents.setOrientation(LinearLayout.VERTICAL);
    	this.findTextInputField = new EditText(this);
    	this.findTextInputField.setWidth(this.pagesView.getWidth() * 80 / 100);
    	Button goButton = new Button(this);
    	goButton.setText(R.string.find_go_button);
    	goButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				String text = OpenFileActivity.this.findTextInputField.getText().toString();
				OpenFileActivity.this.findText(text);
				dialog.dismiss();
			}
    	});
    	LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    	params.leftMargin = 5;
    	params.rightMargin = 5;
    	params.bottomMargin = 2;
    	params.topMargin = 2;
    	contents.addView(findTextInputField, params);
    	contents.addView(goButton, params);
    	dialog.setContentView(contents);
    	dialog.show();
    }
    
    private void findText(String text) {
    	Log.d(TAG, "findText(" + text + ")");
    	if (this.pdf != null) {
    		this.pdf.findText(text);
    		FindResult r = this.pdf.getCurrentFindResult();
    		if (r != null) {
    			if (this.pagesView != null) this.pagesView.setFindMode(true);
    			this.findButtonsLayout.setVisibility(View.VISIBLE);
    		} else {
    	    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	    	AlertDialog dialog = builder.setMessage("Nothing found").create();
    	    	dialog.show();
    		}
    	}
    }
    
    /**
     * Called when user presses "next" button in find panel.
     */
    private void findNext() {
    	if (this.pagesView != null) {
    		this.pagesView.findNext(true);
    		FindResult r = this.pdf.getCurrentFindResult();
    		if (r == null) {
		    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
		    	AlertDialog dialog = builder.setMessage("Nothing found").create();
		    	dialog.show();
    		}
    	}
    }
    
    /**
     * Called when user presses hide button in find panel.
     */
    private void findHide() {
    	if (this.pagesView != null) this.pagesView.setFindMode(false);
    	this.findButtonsLayout.setVisibility(View.GONE);
    }
}



