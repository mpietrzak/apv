package cx.hell.android.lib.pagesview;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import cx.hell.android.pdfview.R;

/**
 * View that simplifies displaying of paged documents.
 * TODO: redesign zooms, pages, marings, layout
 * TODO: use more floats for better align
 */
public class PagesView extends View implements View.OnTouchListener, OnImageRenderedListener {
	
	/**
	 * Tile size.
	 */
	public static final int TILE_SIZE = 256;

//	/**
//	 * Const for logging.
//	 */
//	private final static String TAG = "cx.hell.android.pdfview"; 
	
	/**
	 * Then fade starts.
	 */
	private final static long CONTROLS_FADE_START = 3000;
	
	/**
	 * How long should fade be visible. Currently unused.
	 */
	private final static long CONTROLS_FADE_DURATION = 1000;
	
//	private final static int MAX_ZOOM = 4000;
//	private final static int MIN_ZOOM = 100;
	
	/**
	 * Space between screen edge and page and between pages.
	 */
	private final static int MARGIN = 10;

	private Drawable zoomMinusDrawable = null;
	private Drawable zoomPlusDrawable = null;
	
	private Activity activity = null;
	private PagesProvider pagesProvider = null;
	private long lastControlsUseMillis = 0;
	
	private int width = 0;
	private int height = 0;
	
	/**
	 * Flag: are we being moved around by dragging.
	 */
	private boolean inDrag = false;
	
	/**
	 * Drag start pos.
	 */
	private int dragx = 0;
	
	/**
	 * Drag start pos.
	 */
	private int dragy = 0;
	
	/**
	 * Drag pos.
	 */
	private int dragx1 = 0;
	
	/**
	 * Drag pos.
	 */
	private int dragy1 = 0;
	
	/**
	 * Position over book, not counting drag. 
	 */
	private int left = 0;
	
	/**
	 * Position over book, not counting drag.
	 */
	private int top = 0;
	
	/**
	 * Current zoom level.
	 */
	private int zoomLevel = 1000;
	
	/**
	 * Base scalling factor - how much shrink (or grow) page to fit it nicely to screen at zoomLevel = 1.0.
	 * For example, if we determine that 200x400 image fits screen best, but PDF's pages are 400x800, then
	 * base scaling would be 0.5, since at base scalling, without any zoom, page should fit into screen nicely.
	 */
	private float scalling0 = 0f;
	
	private int pageSizes[][];

	public PagesView(Activity activity) {
		super(activity);
		this.activity = activity;
		this.lastControlsUseMillis = System.currentTimeMillis();
		this.loadZoomControls();
		this.setOnTouchListener(this);
	}
	
	private void loadZoomControls() {
		this.zoomMinusDrawable = this.getResources().getDrawable(R.drawable.btn_zoom_down);
		if (this.zoomMinusDrawable == null) throw new RuntimeException("couldn't load zoomMinusDrawable");
		this.zoomPlusDrawable = this.getResources().getDrawable(R.drawable.btn_zoom_up);
		if (this.zoomPlusDrawable == null) throw new RuntimeException("couldn't load zoomPlusDrawable");
		this.setZoomControlsBounds();
		this.zoomMinusDrawable.setState(new int[] { android.R.attr.state_enabled });
		this.zoomPlusDrawable.setState(new int[] { android.R.attr.state_enabled });
	}
	
	private void setZoomControlsBounds() {
		this.zoomMinusDrawable.setBounds(
				this.getWidth() / 2 - this.zoomMinusDrawable.getIntrinsicWidth(),
				this.getHeight() - this.zoomMinusDrawable.getIntrinsicHeight(),
				this.getWidth() / 2,
				this.getHeight());
		this.zoomPlusDrawable.setBounds(
				this.getWidth() / 2,
				this.getHeight() - this.zoomPlusDrawable.getIntrinsicHeight(),
				this.getWidth() / 2 + this.zoomPlusDrawable.getIntrinsicWidth(),
				this.getHeight());
	}
	
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		this.width = w;
		this.height = h;
		this.scalling0 = ((float)this.height - 2*MARGIN) / (float)this.pageSizes[0][1];
		this.setZoomControlsBounds();
	}
	
//	public PagesView(Context context, AttributeSet attrs) {
//		super(context, attrs);
//	}
	
	public void setPagesProvider(PagesProvider pagesProvider) {
		this.pagesProvider = pagesProvider;
		if (this.pagesProvider != null) {
			this.pageSizes = this.pagesProvider.getPageSizes();
			this.scalling0 = ((float)this.height - 2*MARGIN) / (float)this.pageSizes[0][1];
		} else {
			this.pageSizes = null;
		}
		this.pagesProvider.setOnImageRenderedListener(this);
	}
	
	@Override
	public void onDraw(Canvas canvas) {
		this.drawPages(canvas);
		this.drawControls(canvas);
	}
	
	private int getCurrentPageWidth(int pageno) {
		// on default zoom level first page fits perfectly into screen
		float realpagewidth = (float)this.pageSizes[pageno][0];
		float currentpagewidth = realpagewidth * scalling0 * (this.zoomLevel*0.001f);
		return (int)currentpagewidth;
	}
	
	private float getCurrentPageHeight(int pageno) {
		float realpageheight = (float)this.pageSizes[pageno][1];
		float currentpageheight = realpageheight * scalling0 * (this.zoomLevel*0.001f);
		return currentpageheight;
	}
	
	private void drawPages(Canvas canvas) {
		Rect src = new Rect();
		Rect dst = new Rect();
		if (this.pagesProvider != null) {
			int effleft, efftop;
			int dragoffx, dragoffy;
			
			dragoffx = (inDrag) ? (dragx1 - dragx) : 0;
			dragoffy = (inDrag) ? (dragy1 - dragy) : 0;
			
			effleft = left - dragoffx;
			efftop = top - dragoffy;
			
			int pageCount = this.pageSizes.length;
			int currpageoff = MARGIN;
			
			for(int i = 0; i < pageCount; ++i) {
				// is page i visible?
				int pagex0, pagey0, pagex1, pagey1; // in doc
				pagex0 = MARGIN;
				pagex1 = MARGIN + this.getCurrentPageWidth(i);
				pagey0 = currpageoff;
				pagey1 = currpageoff + (int)this.getCurrentPageHeight(i);
				
				if (rectsintersect(
							pagex0, pagey0, pagex1, pagey1, // page rect in doc
							effleft, efftop, effleft + this.width, efftop + this.height // viewport rect in doc 
						))
				{
					int x, y; // x,y on screen
					int w, h; // w,h of page - on screen dimensions
					x = pagex0 - effleft;
					y = pagey0 - efftop;
					w = this.getCurrentPageWidth(i);
					h = (int)this.getCurrentPageHeight(i);
					//Log.d(TAG, String.format(" page %d coordds on screen: %dx%d x %dx%d", i, x, y, x+w, y+h)); 
					for(int tilex = 0; tilex < w / TILE_SIZE + 1; ++tilex)
						for(int tiley = 0; tiley < h / TILE_SIZE + 1; ++tiley) {
							//Log.d(TAG, String.format(" page: %2d, tile: %02dx%02d", i, tilex, tiley));
							//Rect dst = new Rect(x+tilex*TILE_SIZE, y+tiley*TILE_SIZE, x+tilex*TILE_SIZE + TILE_SIZE, y+tiley*TILE_SIZE + TILE_SIZE);
							dst.left =  x + tilex*TILE_SIZE;
							dst.top = y + tiley*TILE_SIZE;
							dst.right = dst.left + TILE_SIZE;
							dst.bottom = dst.top + TILE_SIZE;
						
							//Log.d(TAG, String.format("  dst: %dx%d x %dx%d", dst.left, dst.top, dst.right, dst.bottom));
							if (dst.intersects(0, 0, this.width, this.height)) {
								//Log.d(TAG, "  tile is visible - dst intersects screen");
								Bitmap b = this.pagesProvider.getPageBitmap(i, (int)(this.zoomLevel * scalling0), tilex, tiley);
								if (b != null) {
									//Log.d(TAG, "  have bitmap: " + b + ", size: " + b.getWidth() + " x " + b.getHeight());
									src.left = 0;
									src.top = 0;
									src.right = b.getWidth();
									src.bottom = b.getWidth();
									
									if (dst.right > x + w) {
										src.right = (int)(b.getWidth() * (float)((x+w)-dst.left) / (float)(dst.right - dst.left));
										dst.right = x + w;
									}
									
									if (dst.bottom > y + h) {
										src.bottom = (int)(b.getHeight() * (float)((y+h)-dst.top) / (float)(dst.bottom - dst.top));
										dst.bottom = y + h;
									}
									
									//this.fixOfscreen(dst, src);
									canvas.drawBitmap(b, src, dst, null);
								}
							}
						}
				}
				currpageoff += MARGIN + this.getCurrentPageHeight(i);
			}
		}
	}
	
	private void drawControls(Canvas canvas) {
		long now = System.currentTimeMillis();
		if (now > this.lastControlsUseMillis + CONTROLS_FADE_START + CONTROLS_FADE_DURATION)
			return;
		
		float opacity = 1.0f;
		if (now > this.lastControlsUseMillis + CONTROLS_FADE_START) {
			long sinceFadeStart = now - (this.lastControlsUseMillis + CONTROLS_FADE_START);
			float fade = (float)sinceFadeStart / (float)CONTROLS_FADE_DURATION;
			if (fade < 0.0 || fade > 1.0) throw new RuntimeException("wtf?");
			opacity -= fade;
		}
		this.zoomMinusDrawable.draw(canvas);
		this.zoomPlusDrawable.draw(canvas);
	}

	/**
	 * Handle touch event coming from Android system.
	 */
	public boolean onTouch(View v, MotionEvent event) {
		this.lastControlsUseMillis = System.currentTimeMillis();
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			Log.d("cx.hell.android.pdfview", "onTouch(ACTION_DOWN)");
			int x = (int)event.getX();
			int y = (int)event.getY();
			if (this.zoomMinusDrawable.getBounds().contains(x,y)) {
				float step = 0.5f;
				this.zoomLevel *= step;
				float cx, cy;
				cx = this.left + this.width / 2;
				cy = this.top + this.height / 2;
				cx = cx * step;
				cy = cy * step;
				this.left = (int)(cx - this.width / 2f);
				this.top = (int)(cy - this.height / 2f);
				Log.d("cx.hell.android.pdfview", "zoom level changed to " + this.zoomLevel);
				this.invalidate();
			} else if (this.zoomPlusDrawable.getBounds().contains(x,y)) {
				float step = 2f;
				this.zoomLevel *= step;
				float cx, cy;
				cx = this.left + this.width / 2;
				cy = this.top + this.height / 2;
				cx = cx * step;
				cy = cy * step;
				this.left = (int)(cx - this.width / 2f);
				this.top = (int)(cy - this.height / 2f);
				Log.d("cx.hell.android.pdfview", "zoom level changed to " + this.zoomLevel);
				this.invalidate();
			} else {
				this.dragx = x;
				this.dragy = y;
				this.inDrag = true;
			}
		} else if (event.getAction() == MotionEvent.ACTION_UP) {
			Log.d("cx.hell.android.pdfview2", "onTouch(ACTION_UP)");
			if (this.inDrag) {
				this.inDrag = false;
				this.left -= (this.dragx1 - this.dragx);
				this.top -= (this.dragy1 - this.dragy);
			}
		} else if (event.getAction() == MotionEvent.ACTION_MOVE) {
			//Log.d("cx.hell.android.pdfview2", "onTouch(ACTION_MOVE" + ")");
			if (this.inDrag) {
				this.dragx1 = (int) event.getX();
				this.dragy1 = (int) event.getY();
				this.invalidate();
			}
		}
		return true;
	}
	
	/**
	 * Test if specified rectangles intersect with each other.
	 * Uses Androids standard Rect class.
	 */
	private static boolean rectsintersect(
			int r1x0, int r1y0, int r1x1, int r1y1,
			int r2x0, int r2y0, int r2x1, int r2y1) {
		Rect r1 = new Rect(r1x0, r1y0, r1x1, r1y1);
		return r1.intersects(r2x0, r2y0, r2x1, r2y1);
	}
	
	/**
	 * Used as a callback from pdf rendering code.
	 * TODO: only invalidate what needs to be painted, not the whole view
	 */
	public void onImageRendered(int pageNumber, int zoom, int tilex, int tiley, Bitmap bitmap) {
		this.post(new Runnable() {
			public void run() {
				PagesView.this.invalidate();
			}
		});
	}
	
	/**
	 * Handle rendering exception.
	 * Show error message and then quit parent activity.
	 * TODO: find a proper way to finish an activity when something bad happens in view.
	 */
	public void onRenderingException(RenderingException reason) {
		final Activity activity = this.activity;
		final String message = reason.getMessage();
		this.post(new Runnable() {
			public void run() {
    			AlertDialog errorMessageDialog = new AlertDialog.Builder(activity)
				.setTitle("Error")
				.setMessage(message)
				.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						activity.finish();
					}
				})
				.setOnCancelListener(new DialogInterface.OnCancelListener() {
					public void onCancel(DialogInterface dialog) {
						activity.finish();
					}
				})
				.create();
    			errorMessageDialog.show();
			}
		});
	}
	
	/**
	 * Move current viewport over n-th page.
	 * Page is 0-based.
	 * @param page 0-based page number
	 */
	public void scrollToPage(int page) {
		this.left = 0;
		float top = 0;
		for(int i = 0; i < page; ++i) {
			top += this.getCurrentPageHeight(i);
			if (i > 0) top += MARGIN;
		}
		this.top = (int)top;
		this.invalidate();
	}
}

