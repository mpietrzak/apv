 package cx.hell.android.pdfview;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import cx.hell.android.lib.pagesview.FindResult;
import cx.hell.android.lib.pagesview.PagesView;


/**
 * Document display activity.
 */
public class OpenFileActivity extends Activity {
	
	private final static String TAG = "cx.hell.android.pdfview";
	
	private final static int[] zoomAnimations = {
		R.anim.zoom_disappear, R.anim.zoom_almost_disappear, R.anim.zoom
	};
	
	private final static int[] pageNumberAnimations = {
		R.anim.page_disappear, R.anim.page_almost_disappear, R.anim.page, 
		R.anim.page_show_always
	};
	
	private PDF pdf = null;
	private PagesView pagesView = null;
	private PDFPagesProvider pdfPagesProvider = null;
	
	private MenuItem aboutMenuItem = null;
	private MenuItem gotoPageMenuItem = null;
	private MenuItem rotateLeftMenuItem = null;
	private MenuItem rotateRightMenuItem = null;
	private MenuItem findTextMenuItem = null;
	private MenuItem clearFindTextMenuItem = null;
	private MenuItem chooseFileMenuItem = null;
	private MenuItem optionsMenuItem = null;
	
	private EditText pageNumberInputField = null;
	private EditText findTextInputField = null;
	
	private LinearLayout findButtonsLayout = null;
	private Button findPrevButton = null;
	private Button findNextButton = null;
	private Button findHideButton = null;
	

	// currently opened file path
	private String filePath = "/";
	
	private String findText = null;
	private Integer currentFindResultPage = null;
	private Integer currentFindResultNumber = null;

	// zoom buttons, layout and fade animation
	private ImageButton zoomDownButton;
	private ImageButton zoomUpButton;
	private Animation zoomAnim;
	private LinearLayout zoomLayout;

	// page number display
	private TextView pageNumberTextView;
	private Animation pageNumberAnim;

	private int fadeStartOffset = 7000; 
	
	private Boolean invert = false;
	
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
        
        // Get display metrics
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        
        // use a relative layout to stack the views
        RelativeLayout layout = new RelativeLayout(this);

        // the PDF view
        this.pagesView = new PagesView(this);
        this.pdf = this.getPDF();
        this.pdfPagesProvider = new PDFPagesProvider(pdf, 
        		PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Options.PREF_GRAY, false));
        pagesView.setPagesProvider(pdfPagesProvider);
        layout.addView(pagesView);
        
        // the find buttons
        this.findButtonsLayout = new LinearLayout(this);
        this.findButtonsLayout.setOrientation(LinearLayout.HORIZONTAL);
        this.findButtonsLayout.setVisibility(View.GONE);
        this.findButtonsLayout.setGravity(Gravity.CENTER);
        this.findPrevButton = new Button(this);
        this.findPrevButton.setText("Prev");
        this.findButtonsLayout.addView(this.findPrevButton);
        this.findNextButton = new Button(this);
        this.findNextButton.setText("Next");
        this.findButtonsLayout.addView(this.findNextButton);
        this.findHideButton = new Button(this);
        this.findHideButton.setText("Hide");
        this.findButtonsLayout.addView(this.findHideButton);
        this.setFindButtonHandlers();
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
        		RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        lp.addRule(RelativeLayout.CENTER_HORIZONTAL);
        lp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        layout.addView(this.findButtonsLayout, lp);

        Log.v("start", "go4");
        // the zoom buttons
        zoomLayout = new LinearLayout(this);
        zoomLayout.setOrientation(LinearLayout.HORIZONTAL);
		zoomDownButton = new ImageButton(this);
		zoomDownButton.setImageDrawable(getResources().getDrawable(R.drawable.btn_zoom_down));
		zoomDownButton.setBackgroundColor(Color.TRANSPARENT);
		zoomLayout.addView(zoomDownButton, (int)(80 * metrics.density), (int)(50 * metrics.density));	// TODO: remove hardcoded values
		zoomUpButton = new ImageButton(this);
		zoomUpButton.setImageDrawable(getResources().getDrawable(R.drawable.btn_zoom_up));
		zoomUpButton.setBackgroundColor(Color.TRANSPARENT);
		zoomLayout.addView(zoomUpButton, (int)(80 * metrics.density), (int)(50 * metrics.density));
		lp = new RelativeLayout.LayoutParams(
        		RelativeLayout.LayoutParams.WRAP_CONTENT, 
        		RelativeLayout.LayoutParams.WRAP_CONTENT);
        lp.addRule(RelativeLayout.CENTER_HORIZONTAL);
		lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        setZoomButtonHandlers();
		layout.addView(zoomLayout,lp);
		
        this.pageNumberTextView = new TextView(this);
        this.pageNumberTextView.setBackgroundColor(invert ? Color.BLACK : Color.WHITE);
        this.pageNumberTextView.setTextColor(invert ? Color.WHITE : Color.BLACK);
        this.pageNumberTextView.setTextSize(8f*metrics.density);
        this.pageNumberTextView.setVisibility(View.GONE);
        lp = new RelativeLayout.LayoutParams(
        		RelativeLayout.LayoutParams.WRAP_CONTENT, 
        		RelativeLayout.LayoutParams.WRAP_CONTENT);
        lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        lp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        layout.addView(this.pageNumberTextView, lp);
        
		// display this
        this.setContentView(layout);
        
        // go to last viewed page
        gotoLastPage();
        
        // send keyboard events to this view
        pagesView.setFocusable(true);
        pagesView.setFocusableInTouchMode(true);
    }

	/** 
	 * Save the current page before exiting
	 */
	@Override
	protected void onPause() {
		saveLastPage();
		super.onPause();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		Options.setOrientation(this);
		
		SharedPreferences options = PreferenceManager.getDefaultSharedPreferences(this);

		this.pdfPagesProvider.setGray(options.getBoolean(Options.PREF_GRAY, false));
		
		pagesView.setZoomIncrement(
				Float.parseFloat(options.getString(Options.PREF_ZOOM_INCREMENT, "1.414")));
		pagesView.setRenderAhead(options.getBoolean(Options.PREF_RENDER_AHEAD, true));
		this.invert = options.getBoolean(Options.PREF_INVERT, false);
		pagesView.setInvert(this.invert);
		pagesView.setPageWithVolume(options.getBoolean(Options.PREF_PAGE_WITH_VOLUME, true));
		pagesView.invalidate();
		zoomAnim = AnimationUtils.loadAnimation(this,
				zoomAnimations[
				    Integer.parseInt(options.getString(Options.PREF_ZOOM_ANIMATION, "2"))]);		
		pageNumberAnim = AnimationUtils.loadAnimation(this,
				pageNumberAnimations[
				    Integer.parseInt(options.getString(Options.PREF_PAGE_ANIMATION, "3"))]);
		fadeStartOffset = 1000 * Integer.parseInt(options.getString(Options.PREF_FADE_SPEED, "7"));
		
		if (options.getBoolean(Options.PREF_FULLSCREEN, false))
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		else
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        showAnimated();
	}

    /**
     * Set handlers on findNextButton and findHideButton.
     */
    private void setFindButtonHandlers() {
    	this.findPrevButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				OpenFileActivity.this.findPrev();
			}
    	});
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
     * Set handlers on zoom level buttons
     */
    private void setZoomButtonHandlers() {
    	this.zoomDownButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				pagesView.zoomDown();
			}
    	});
    	this.zoomUpButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				pagesView.zoomUp();
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
			Recent recent = new Recent(this);
			recent.add(0, filePath);
			recent.commit();
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
    		this.clearFind();
    	} else if (menuItem == this.chooseFileMenuItem) {
    		startActivity(new Intent(this, ChooseFileActivity.class));
    	} else if (menuItem == this.optionsMenuItem) {
    		startActivity(new Intent(this, Options.class));
		} 
    	return false;
    }
    
    /**
     * Intercept touch events to handle the zoom buttons animation
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
    	int action = event.getAction();
    	if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_DOWN) {
    		showAnimated();
    	}
		return super.dispatchTouchEvent(event);    	
    };
    
    public boolean dispatchKeyEvent(KeyEvent event) {
    	int action = event.getAction();
    	if (action == KeyEvent.ACTION_UP || action == KeyEvent.ACTION_DOWN) {
    		showAnimated();
    	}
		return super.dispatchKeyEvent(event);    	
    };
    
    /**
     * Show zoom buttons and page number
     */
    private void showAnimated() {
    	zoomAnim.setStartOffset(fadeStartOffset);
		zoomAnim.setFillAfter(true);
		zoomLayout.startAnimation(zoomAnim);

    	pageNumberAnim.setStartOffset(fadeStartOffset);
		pageNumberAnim.setFillAfter(true);
		pageNumberTextView.setText(""+(this.pagesView.getCurrentPage()+1)+"/"+
				this.pdfPagesProvider.getPageCount());
        pageNumberTextView.setVisibility(View.VISIBLE);
		pageNumberTextView.startAnimation(pageNumberAnim);     	
    }
    
    /**
     * Hide the find buttons
     */
    private void clearFind() {
		this.currentFindResultPage = null;
		this.currentFindResultNumber = null;
    	this.pagesView.setFindMode(false);
		this.findButtonsLayout.setVisibility(View.GONE);
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
    	this.pageNumberInputField.setText("" + (this.pagesView.getCurrentPage() + 1));
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
    	Button page1Button = new Button(this);
    	page1Button.setText(getResources().getString(R.string.page) +" 1");
    	page1Button.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				d.dismiss();
				OpenFileActivity.this.gotoPage(0);
			}
    	});
    	Button lastPageButton = new Button(this);
    	lastPageButton.setText(getResources().getString(R.string.page) +" "+pagecount);
    	lastPageButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				d.dismiss();
				OpenFileActivity.this.gotoPage(pagecount-1);
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
    	contents.addView(page1Button, params);
    	contents.addView(lastPageButton, params);
    	d.setContentView(contents);
    	d.show();
    }
    
    private void gotoPage(int page, int zoom) {
    	Log.i(TAG, "rewind to page " + page);
    	if (this.pagesView != null) {
    		this.pagesView.scrollToPage(page);
    		if (zoom > 0)
    			this.pagesView.setZoomLevel(zoom);
            showAnimated();
    	}
    }
    
    /**
     * Called after submitting go to page dialog.
     * @param page page number, 0-based
     */
    private void gotoPage(int page) {
    	gotoPage(page, this.pagesView.getCurrentZoom());
    }
    
    /**
     * Goto the last open page if possible
     */
    private void gotoLastPage() {
    	Log.v(TAG, "getting last page for "+filePath);
        Bookmark b = new Bookmark(this.getApplicationContext()).open();
        int lastpage = b.getLastPage(filePath);
        int zoom = b.getLastZoom(filePath);
        b.close();
        if (lastpage > 0) {
        	Handler mHandler = new Handler();
        	Runnable mUpdateTimeTask = new GotoPageThread(lastpage, zoom);
        	mHandler.postDelayed(mUpdateTimeTask, 2000);
        }    	
    }

    /**
     * Save the last page in the bookmarks
     */
    private void saveLastPage() {
        Bookmark b = new Bookmark(this.getApplicationContext()).open();
        b.setLast(filePath, pagesView.getCurrentPage(), pagesView.getCurrentZoom());
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
    	this.clearFindTextMenuItem = menu.add(R.string.clear_find_text);
    	this.chooseFileMenuItem = menu.add(R.string.choose_file);
    	this.optionsMenuItem = menu.add(R.string.options);
    	/* The following appear on the second page.  The find item can safely be kept
    	 * there since it can also be accessed from the search key on most devices.
    	 */
		this.findTextMenuItem = menu.add(R.string.find_text);
    	this.aboutMenuItem = menu.add(R.string.about);
    	return true;
    }
        
	/**
	 * Thread to delay the gotoPage action when opening a PDF file
	 */
	private class GotoPageThread implements Runnable {
		int page;
		int zoom;

		public GotoPageThread(int page, int zoom) {
			this.page = page;
			this.zoom = zoom;
		}

		public void run() {
			gotoPage(page, zoom);
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
    public void showFindDialog() {
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
    	this.findText = text;
    	this.find(true);
    }
    
    /**
     * Called when user presses "next" button in find panel.
     */
    private void findNext() {
    	this.find(true);
    }

    /**
     * Called when user presses "prev" button in find panel.
     */
    private void findPrev() {
    	this.find(false);
    }
    
    /**
     * Called when user presses hide button in find panel.
     */
    private void findHide() {
    	if (this.pagesView != null) this.pagesView.setFindMode(false);
    	this.currentFindResultNumber = null;
    	this.currentFindResultPage = null;
    	this.findButtonsLayout.setVisibility(View.GONE);
    }

    /**
     * Helper class that handles search progress, search cancelling etc.
     */
	static class Finder implements Runnable, DialogInterface.OnCancelListener, DialogInterface.OnClickListener {
		private OpenFileActivity parent = null;
		private boolean forward;
		private AlertDialog dialog = null;
		private String text;
		private int startingPage;
		private int pageCount;
		private boolean cancelled = false;
		/**
		 * Constructor for finder.
		 * @param parent parent activity
		 */
		public Finder(OpenFileActivity parent, boolean forward) {
			this.parent = parent;
			this.forward = forward;
			this.text = parent.findText;
			this.pageCount = parent.pagesView.getPageCount();
			if (parent.currentFindResultPage != null) {
				if (forward) {
					this.startingPage = (parent.currentFindResultPage + 1) % pageCount;
				} else {
					this.startingPage = (parent.currentFindResultPage - 1 + pageCount) % pageCount;
				}
			} else {
				this.startingPage = parent.pagesView.getCurrentPage();
			}
		}
		public void setDialog(AlertDialog dialog) {
			this.dialog = dialog;
		}
		public void run() {
			int page = -1;
			this.createDialog();
			this.showDialog();
			for(int i = 0; i < this.pageCount; ++i) {
				if (this.cancelled) {
					this.dismissDialog();
					return;
				}
				page = (startingPage + pageCount + (this.forward ? i : -i)) % this.pageCount;
				Log.d(TAG, "searching on " + page);
				this.updateDialog(page);
				List<FindResult> findResults = this.findOnPage(page);
				if (findResults != null && !findResults.isEmpty()) {
					Log.d(TAG, "found something at page " + page + ": " + findResults.size() + " results");
					this.dismissDialog();
					this.showFindResults(findResults, page);
					return;
				}
			}
			/* TODO: show "nothing found" message */
			this.dismissDialog();
		}
		/**
		 * Called by finder thread to get find results for given page.
		 * Routed to PDF instance.
		 * If result is not empty, then finder loop breaks, current find position
		 * is saved and find results are displayed.
		 * @param page page to search on
		 * @return results 
		 */
		private List<FindResult> findOnPage(int page) {
			if (this.text == null) throw new IllegalStateException("text cannot be null");
			return this.parent.pdf.find(this.text, page);
		}
		private void createDialog() {
			this.parent.runOnUiThread(new Runnable() {
				public void run() {
					String title = Finder.this.parent.getString(R.string.searching_for).replace("%1", Finder.this.text);
					String message = Finder.this.parent.getString(R.string.page_of).replace("%1", String.valueOf(Finder.this.startingPage)).replace("%2", String.valueOf(pageCount));
			    	AlertDialog.Builder builder = new AlertDialog.Builder(Finder.this.parent);
			    	AlertDialog dialog = builder
			    		.setTitle(title)
			    		.setMessage(message)
			    		.setCancelable(true)
			    		.setNegativeButton(R.string.cancel, Finder.this)
			    		.create();
			    	dialog.setOnCancelListener(Finder.this);
			    	Finder.this.dialog = dialog;
				}
			});
		}
		public void updateDialog(final int page) {
			this.parent.runOnUiThread(new Runnable() {
				public void run() {
					String message = Finder.this.parent.getString(R.string.page_of).replace("%1", String.valueOf(page)).replace("%2", String.valueOf(pageCount));
					Finder.this.dialog.setMessage(message);
				}
			});
		}
		public void showDialog() {
			this.parent.runOnUiThread(new Runnable() {
				public void run() {
					Finder.this.dialog.show();
				}
			});
		}
		public void dismissDialog() {
			final AlertDialog dialog = this.dialog;
			this.parent.runOnUiThread(new Runnable() {
				public void run() {
					dialog.dismiss();
				}
			});
		}
		public void onCancel(DialogInterface dialog) {
			Log.d(TAG, "onCancel(" + dialog + ")");
			this.cancelled = true;
		}
		public void onClick(DialogInterface dialog, int which) {
			Log.d(TAG, "onClick(" + dialog + ")");
			this.cancelled = true;
		}
		private void showFindResults(final List<FindResult> findResults, final int page) {
			this.parent.runOnUiThread(new Runnable() {
				public void run() {
					int fn = Finder.this.forward ? 0 : findResults.size()-1;
					Finder.this.parent.currentFindResultPage = page;
					Finder.this.parent.currentFindResultNumber = fn;
					Finder.this.parent.pagesView.setFindResults(findResults);
					Finder.this.parent.pagesView.setFindMode(true);
					Finder.this.parent.pagesView.scrollToFindResult(fn);
					Finder.this.parent.findButtonsLayout.setVisibility(View.VISIBLE);					
					Finder.this.parent.pagesView.invalidate();
				}
			});
		}
	};
    
    /**
     * GUI for finding text.
     * Used both on initial search and for "next" and "prev" searches.
     * Displays dialog, handles cancel button, hides dialog as soon as
     * something is found.
     * @param 
     */
    private void find(boolean forward) {
    	if (this.currentFindResultPage != null) {
    		/* searching again */
    		int nextResultNum = forward ? this.currentFindResultNumber + 1 : this.currentFindResultNumber - 1;
    		if (nextResultNum >= 0 && nextResultNum < this.pagesView.getFindResults().size()) {
    			/* no need to really find - just focus on given result and exit */
    			this.currentFindResultNumber = nextResultNum;
    			this.pagesView.scrollToFindResult(nextResultNum);
    			this.pagesView.invalidate();
    			return;
    		}
    	}

    	/* finder handles next/prev and initial search by itself */
    	Finder finder = new Finder(this, forward);
    	Thread finderThread = new Thread(finder);
    	finderThread.start();
    }
}
