package cx.hell.android.lib.pagesview;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import cx.hell.android.pdfview.Bookmark;
import cx.hell.android.pdfview.Options;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnDoubleTapListener;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Scroller;

/**
 * View that simplifies displaying of paged documents.
 * TODO: redesign zooms, pages, margins, layout
 * TODO: use more floats for better align, or use more ints for performance ;) (that is, really analyse what should be used when)
 */
public class PagesView extends View implements 
View.OnTouchListener, OnImageRenderedListener, View.OnKeyListener {
	/**
	 * Logging tag.
	 */
	private static final String TAG = "cx.hell.android.pdfview";
	
	/* Experiments show that larger tiles are faster, but the gains do drop off,
	 * and must be balanced against the size of memory chunks being requested.
	 */
	private static final int MIN_TILE_WIDTH = 256;
	private static final int MAX_TILE_WIDTH = 640;
	private static final int MIN_TILE_HEIGHT = 128;
	private static final int MAX_TILE_PIXELS = 640*360;
	
//	private final static int MAX_ZOOM = 4000;
//	private final static int MIN_ZOOM = 100;
	
	/**
	 * Space between screen edge and page and between pages.
	 */
	private final static int MARGIN = 10;
	
	/* zoom steps */
	float step = 1.414f;
	
	/* volume keys page */
	boolean pageWithVolume = true;
	
	private Activity activity = null;
	
	/**
	 * Source of page bitmaps.
	 */
	private PagesProvider pagesProvider = null;
	
	
	@SuppressWarnings("unused")
	private long lastControlsUseMillis = 0;
	
	private int colorMode;
	
	private float maxRealPageSize[] = {0f, 0f};
	private float realDocumentSize[] = {0f, 0f};
	
	/**
	 * Current width of this view.
	 */
	private int width = 0;
	
	/**
	 * Current height of this view.
	 */
	private int height = 0;
	
	/**
	 * Position over book, not counting drag.
	 * This is position of viewports center, not top-left corner. 
	 */
	private int left = 0;
	
	/**
	 * Position over book, not counting drag.
	 * This is position of viewports center, not top-left corner.
	 */
	private int top = 0;
	
	/**
	 * Current zoom level.
	 * 1000 is 100%.
	 */
	private int zoomLevel = 1000;
	
	/**
	 * Current rotation of pages.
	 */
	private int rotation = 0;
	
	/**
	 * Base scaling factor - how much shrink (or grow) page to fit it nicely to screen at zoomLevel = 1000.
	 * For example, if we determine that 200x400 image fits screen best, but PDF's pages are 400x800, then
	 * base scaling would be 0.5, since at base scaling, without any zoom, page should fit into screen nicely.
	 */
	private float scaling0 = 0f;
	
	/**
	 * Page sized obtained from pages provider.
	 * These do not change.
	 */
	private int pageSizes[][];
	
	/**
	 * Find mode.
	 */
	private boolean findMode = false;

	/**
	 * Paint used to draw find results.
	 */
	private Paint findResultsPaint = null;
	
	/**
	 * Currently displayed find results.
	 */
	private List<FindResult> findResults = null;

	/**
	 * hold the currently displayed page 
	 */
	private int currentPage = 0;
	
	/**
	 * avoid too much allocations in rectsintersect()
	 */
	private static Rect r1 = new Rect();
	

	/**
	 * Bookmarked page to go to.
	 */
	private int bookmarkedPage = 0;
	
	/**
	 * Construct this view.
	 * @param activity parent activity
	 */
	
	private boolean volumeUpIsDown = false;
	private boolean volumeDownIsDown = false;
	
	private GestureDetector gestureDetector = null;
	private Scroller scroller = null;
	
	public PagesView(Activity activity) {
		super(activity);
		this.activity = activity;
		this.lastControlsUseMillis = System.currentTimeMillis();
		this.findResultsPaint = new Paint();
		this.findResultsPaint.setARGB(0xd0, 0xc0, 0, 0);
		this.findResultsPaint.setStyle(Paint.Style.FILL);
		this.findResultsPaint.setAntiAlias(true);
		this.findResultsPaint.setStrokeWidth(3);
		this.setOnTouchListener(this);
		this.setOnKeyListener(this);
		activity.setDefaultKeyMode(Activity.DEFAULT_KEYS_SEARCH_LOCAL);
		
		this.scroller = new Scroller(activity);
		
		this.gestureDetector = new GestureDetector(activity, 
				new GestureDetector.OnGestureListener() {

					public boolean onDown(MotionEvent e) {
						scroller.forceFinished(true);
						return true;
					}

					public boolean onFling(MotionEvent e1, MotionEvent e2,
							float velocityX, float velocityY) {
						doFling(velocityX, velocityY);
						return true;
					}

					public void onLongPress(MotionEvent e) {
					}

					public boolean onScroll(MotionEvent e1, MotionEvent e2,
							float distanceX, float distanceY) {
						
						doScroll((int)distanceX, (int)distanceY);
						return true;
					}

					public void onShowPress(MotionEvent e) {
					}

					public boolean onSingleTapUp(MotionEvent e) {
						return false;
					}
		});
		
		gestureDetector.setOnDoubleTapListener(new OnDoubleTapListener() {
			public boolean onDoubleTap(MotionEvent e) {
				left += e.getX() - width/2;
				top += e.getY() - height/2;
				invalidate();
				zoomUpBig();				
				return false;
			}

			public boolean onDoubleTapEvent(MotionEvent e) {
				return false;
			}

			public boolean onSingleTapConfirmed(MotionEvent e) {				
				return false;
			}});
	}
	
	public void setStartBookmark(Bookmark b, String bookmarkName) {
		if (b != null) {
			this.rotation = b.getLastRotation(bookmarkName);

			int zoom = b.getLastZoom(bookmarkName);
			if (0<zoom) {
				this.zoomLevel = zoom;
			}
			
			int page = b.getLastPage(bookmarkName);
			if (0<page) {
				this.bookmarkedPage = page;
				this.currentPage = page;
			}
		}
	}
		
	/**
	 * Handle size change event.
	 * Update base scaling, move zoom controls to correct place etc.
	 * @param w new width
	 * @param h new height
	 * @param oldw old width
	 * @param oldh old height
	 */
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		this.width = w;
		this.height = h;
		if (this.scaling0 == 0f) {
			this.scaling0 = Math.min(
					((float)this.height - 2*MARGIN) / (float)this.pageSizes[0][1],
					((float)this.width - 2*MARGIN) / (float)this.pageSizes[0][0]);
		}
		if (oldw == 0 && oldh == 0) {
			this.left = this.width / 2;

			if (bookmarkedPage == 0) {
				this.top  = this.height / 2;
			}
			else {
				Point pos = getPagePositionInDocumentWithZoom(bookmarkedPage);
				this.top = pos.y + this.height / 2;
			}
		}
	}
	
	public void setPagesProvider(PagesProvider pagesProvider) {
		this.pagesProvider = pagesProvider;
		if (this.pagesProvider != null) {
			this.pageSizes = this.pagesProvider.getPageSizes();
			
			maxRealPageSize[0] = 0f;
			maxRealPageSize[1] = 0f;
			realDocumentSize[0] = 0f;
			realDocumentSize[1] = 0f;
			
			for (int i = 0; i < this.pageSizes.length; i++) 
				for (int j = 0; j<2; j++) {
					if (pageSizes[i][j] > maxRealPageSize[j])
						maxRealPageSize[j] = pageSizes[i][j];
					realDocumentSize[j] += pageSizes[i][j]; 
				}
			
			if (this.width > 0 && this.height > 0) {
				this.scaling0 = Math.min(
						((float)this.height - 2*MARGIN) / (float)this.pageSizes[0][1],
						((float)this.width - 2*MARGIN) / (float)this.pageSizes[0][0]);
				this.left = this.width / 2;
				this.top = this.height / 2;
			}
		} else {
			this.pageSizes = null;
		}
		this.pagesProvider.setOnImageRenderedListener(this);
	}
	
	/**
	 * Draw view.
	 * @param canvas what to draw on
	 */
	
	int prevTop = -1;
	int prevLeft = -1;
	
	public void onDraw(Canvas canvas) {
		this.drawPages(canvas);
		if (this.findMode) this.drawFindResults(canvas);
	}
	
	/**
	 * Get current maximum page width by page number taking into account zoom and rotation
	 */
	private int getCurrentMaxPageWidth() {
		float realpagewidth = this.maxRealPageSize[this.rotation % 2 == 0 ? 0 : 1];
		return (int)(realpagewidth * scaling0 * (this.zoomLevel*0.001f));
	}
	
	/**
	 * Get current maximum page height by page number taking into account zoom and rotation
	 */
	private int getCurrentMaxPageHeight() {
		float realpageheight = this.maxRealPageSize[this.rotation % 2 == 0 ? 1 : 0];
		return (int)(realpageheight * scaling0 * (this.zoomLevel*0.001f));
	}
	
	/**
	 * Get current maximum page width by page number taking into account zoom and rotation
	 */
	private int getCurrentDocumentHeight() {
		float realheight = this.realDocumentSize[this.rotation % 2 == 0 ? 1 : 0];
		/* we add pageSizes.length to account for round-off issues */
		return (int)(realheight * scaling0 * (this.zoomLevel*0.001f) +  
			(pageSizes.length - 1) * this.getCurrentMargin());
	}
	
	/**
	 * Get current page width by page number taking into account zoom and rotation
	 * @param pageno 0-based page number
	 */
	private int getCurrentPageWidth(int pageno) {
		float realpagewidth = (float)this.pageSizes[pageno][this.rotation % 2 == 0 ? 0 : 1];
		float currentpagewidth = realpagewidth * scaling0 * (this.zoomLevel*0.001f);
		return (int)currentpagewidth;
	}
	
	/**
	 * Get current page height by page number taking into account zoom and rotation.
	 * @param pageno 0-based page number
	 */
	private float getCurrentPageHeight(int pageno) {
		float realpageheight = (float)this.pageSizes[pageno][this.rotation % 2 == 0 ? 1 : 0];
		float currentpageheight = realpageheight * scaling0 * (this.zoomLevel*0.001f);
		return currentpageheight;
	}
	
	private float getCurrentMargin() {
		return (float)MARGIN * this.zoomLevel * 0.001f;
	}
	
	/**
	 * This takes into account zoom level.
	 */
	private Point getPagePositionInDocumentWithZoom(int page) {
		float margin = this.getCurrentMargin();
		float left = margin;
		float top = 0;
		for(int i = 0; i < page; ++i) {
			top += this.getCurrentPageHeight(i);
		}
		top += (page+1) * margin;
		
		return new Point((int)left, (int)top);
	}
	
	/**
	 * Calculate screens (viewports) top-left corner position over document.
	 */
	private Point getScreenPositionOverDocument() {
		float top = this.top - this.height / 2;
		float left = this.left - this.width / 2;
		return new Point((int)left, (int)top);
	}
	
	/**
	 * Calculate current page position on screen in pixels.
	 * @param page base-0 page number
	 */
	private Point getPagePositionOnScreen(int page) {
		if (page < 0) throw new IllegalArgumentException("page must be >= 0: " + page);
		if (page >= this.pageSizes.length) throw new IllegalArgumentException("page number too big: " + page);
		
		Point pagePositionInDocument = this.getPagePositionInDocumentWithZoom(page);
		Point screenPositionInDocument = this.getScreenPositionOverDocument();
		
		return new Point(
					pagePositionInDocument.x - screenPositionInDocument.x,
					pagePositionInDocument.y - screenPositionInDocument.y
				);
	}
	
	@Override
	public void computeScroll() {
		if (this.scroller.computeScrollOffset()) {
			left = this.scroller.getCurrX();
			top = this.scroller.getCurrY();
			((cx.hell.android.pdfview.OpenFileActivity)activity).showPageNumber(false);
			postInvalidate();
		}
	}
	
	/**
	 * Draw pages.
	 * Also collect info what's visible and push this info to page renderer.
	 */
	private void drawPages(Canvas canvas) {
		Rect src = new Rect(); /* TODO: move out of drawPages */
		Rect dst = new Rect(); /* TODO: move out of drawPages */
		int pageWidth = 0;
		int pageHeight = 0;
		float pagex0, pagey0, pagex1, pagey1; // in doc, counts zoom
		float x, y; // on screen
		int viewx0, viewy0; // view over doc
		LinkedList<Tile> visibleTiles = new LinkedList<Tile>();
		float currentMargin = this.getCurrentMargin();
		float renderAhead = this.pagesProvider.getRenderAhead();
		
		if (this.pagesProvider != null) {
			viewx0 = left - width/2;
			viewy0 = top - height/2;
			
			int pageCount = this.pageSizes.length;
			
			/* We now adjust the position to make sure we don't scroll too
			 * far away from the document text.
			 */
			int oldviewx0 = viewx0;
			int oldviewy0 = viewy0;
			
			viewx0 = adjustPosition(viewx0, width, (int)currentMargin, 
					getCurrentMaxPageWidth());
			viewy0 = adjustPosition(viewy0, height, (int)currentMargin,
					(int)getCurrentDocumentHeight());
			
			left += viewx0 - oldviewx0;
			top += viewy0 - oldviewy0;
			
			float currpageoff = currentMargin;
			
			this.currentPage = -1;
			
			int firstVisiblePage = -1;
			int lastVisiblePage = -1;
			
			pagey0 = 0;
			
			for(int i = 0; i < pageCount; ++i) {
				// is page i visible?

				pageWidth = this.getCurrentPageWidth(i);
				pageHeight = (int) this.getCurrentPageHeight(i);
				
				pagex0 = currentMargin;
				pagex1 = (int)(currentMargin + pageWidth);
				pagey0 = currpageoff;
				pagey1 = (int)(currpageoff + pageHeight);
				
				if (rectsintersect(
							(int)pagex0, (int)pagey0, (int)pagex1, (int)pagey1, // page rect in doc
							viewx0, viewy0, viewx0 + this.width, 
							viewy0 + (int)(renderAhead*this.height) // viewport rect in doc, or close enough to it 
						))
				{
					if (this.currentPage == -1)  {
						// remember the currently displayed page
						this.currentPage = i;
					}
					
					x = pagex0 - viewx0;
					y = pagey0 - viewy0;
					
					int[] tileSizes = new int[2];
					getGoodTileSizes(tileSizes, pageWidth, pageHeight);
					
					for(int tileix = 0; tileix < (pageWidth + tileSizes[0]-1) / tileSizes[0]; ++tileix)
						for(int tileiy = 0; tileiy < (pageHeight + tileSizes[1]-1) / tileSizes[1]; ++tileiy) {
							
							dst.left = (int)(x + tileix*tileSizes[0]);
							dst.top = (int)(y + tileiy*tileSizes[1]);
							dst.right = dst.left + tileSizes[0];
							dst.bottom = dst.top + tileSizes[1];	
						
							if (dst.intersects(0, 0, this.width, (int)(renderAhead*this.height))) {

								Tile tile = new Tile(i, (int)(this.zoomLevel * scaling0), 
										tileix*tileSizes[0], tileiy*tileSizes[1], this.rotation,
										tileSizes[0], tileSizes[1]);
								if (dst.intersects(0, 0, this.width, this.height)) {
									Bitmap b = this.pagesProvider.getPageBitmap(tile);
									if (b != null) {
										//Log.d(TAG, "  have bitmap: " + b + ", size: " + b.getWidth() + " x " + b.getHeight());
										src.left = 0;
										src.top = 0;
										src.right = b.getWidth();
										src.bottom = b.getHeight();
										
										if (dst.right > x + pageWidth) {
											src.right = (int)(b.getWidth() * (float)((x+pageWidth)-dst.left) / (float)(dst.right - dst.left));
											dst.right = (int)(x + pageWidth);
										}
										
										if (dst.bottom > y + pageHeight) {
											src.bottom = (int)(b.getHeight() * (float)((y+pageHeight)-dst.top) / (float)(dst.bottom - dst.top));
											dst.bottom = (int)(y + pageHeight);
										}
										
										drawBitmap(canvas, b, src, dst);
										
									}
								}
								visibleTiles.add(tile);
							}
						}
				}
				
				
				/* move to next page */
				currpageoff += currentMargin + this.getCurrentPageHeight(i);
			}
			this.pagesProvider.setVisibleTiles(visibleTiles);
		}
	}
		
	private void drawBitmap(Canvas canvas, Bitmap b, Rect src, Rect dst) {
		if (colorMode != Options.COLOR_MODE_NORMAL) {
			Paint paint = new Paint();
			Bitmap out;
			
			if (b.getConfig() == Bitmap.Config.ALPHA_8) {
				out = b.copy(Bitmap.Config.ARGB_8888, false);
			}
			else {
				out = b;
			}
			
			paint.setColorFilter(new 
					ColorMatrixColorFilter(new ColorMatrix(
							Options.getColorModeMatrix(this.colorMode))));

			canvas.drawBitmap(out, src, dst, paint);
			
			if (b.getConfig() == Bitmap.Config.ALPHA_8) {
				out.recycle();
			}
		}
		else {
			canvas.drawBitmap(b, src, dst, null);
		}
	}

	/**
	 * Draw find results.
	 * TODO prettier icons
	 * TODO message if nothing was found
	 * @param canvas drawing target
	 */
	private void drawFindResults(Canvas canvas) {
		if (!this.findMode) throw new RuntimeException("drawFindResults but not in find results mode");
		if (this.findResults == null || this.findResults.isEmpty()) {
			Log.w(TAG, "nothing found");
			return;
		}
		for(FindResult findResult: this.findResults) {
			if (findResult.markers == null || findResult.markers.isEmpty())
				throw new RuntimeException("illegal FindResult: find result must have at least one marker");
			Iterator<Rect> i = findResult.markers.iterator();
			Rect r = null;
			Point pagePosition = this.getPagePositionOnScreen(findResult.page);
			float pagex = pagePosition.x;
			float pagey = pagePosition.y;
			float z = (this.scaling0 * (float)this.zoomLevel * 0.001f);
			while(i.hasNext()) {
				r = i.next();
				canvas.drawLine(
						r.left * z + pagex, r.top * z + pagey,
						r.left * z + pagex, r.bottom * z + pagey,
						this.findResultsPaint);
				canvas.drawLine(
						r.left * z + pagex, r.bottom * z + pagey,
						r.right * z + pagex, r.bottom * z + pagey,
						this.findResultsPaint);
				canvas.drawLine(
						r.right * z + pagex, r.bottom * z + pagey,
						r.right * z + pagex, r.top * z + pagey,
						this.findResultsPaint);
//			canvas.drawRect(
//					r.left * z + pagex,
//					r.top * z + pagey,
//					r.right * z + pagex,
//					r.bottom * z + pagey,
//					this.findResultsPaint);
//			Log.d(TAG, "marker lands on: " +
//					(r.left * z + pagex) + ", " +
//					(r.top * z + pagey) + ", " + 
//					(r.right * z + pagex) + ", " +
//					(r.bottom * z + pagey) + ", ");
			}
		}
	}

	/**
	 * Handle touch event coming from Android system.
	 */
	public boolean onTouch(View v, MotionEvent event) {
		this.lastControlsUseMillis = System.currentTimeMillis();
		
		gestureDetector.onTouchEvent(event);
		return true;
	}
	
	/**
	 * Handle keyboard events
	 */
	public boolean onKey(View v, int keyCode, KeyEvent event) {
		if (this.pageWithVolume && event.getAction() == KeyEvent.ACTION_UP) {
			/* repeat is a little too fast sometimes, so trap these on up */
			switch(keyCode) {
				case KeyEvent.KEYCODE_VOLUME_UP:
					volumeUpIsDown = false;
					return true;
				case KeyEvent.KEYCODE_VOLUME_DOWN:
					volumeDownIsDown = false;
					return true;
			}
		}
		
		if (event.getAction() == KeyEvent.ACTION_DOWN) {
			switch (keyCode) {
			case KeyEvent.KEYCODE_SEARCH:
				((cx.hell.android.pdfview.OpenFileActivity)activity).showFindDialog();
				return true;
			case KeyEvent.KEYCODE_VOLUME_DOWN:
				if (!this.pageWithVolume)
					return false;
				if (!volumeDownIsDown) {
					/* Disable key repeat as on some devices the keys are a little too
					 * sticky for key repeat to work well.  TODO: Maybe key repeat disabling
					 * should be an option?  
					 */
					this.top += this.getHeight() - 16;
					this.invalidate();
				}
				volumeDownIsDown = true;
				return true;
			case KeyEvent.KEYCODE_VOLUME_UP:
				if (!this.pageWithVolume)
					return false;
				if (!volumeUpIsDown) {
					this.top -= this.getHeight() - 16;
					this.invalidate();
				}
				volumeUpIsDown = true;
				return true;
			case KeyEvent.KEYCODE_DPAD_UP:
			case KeyEvent.KEYCODE_DEL:
			case KeyEvent.KEYCODE_K:
				this.top -= this.getHeight() - 16;
				this.invalidate();
				return true;
			case KeyEvent.KEYCODE_DPAD_DOWN:
			case KeyEvent.KEYCODE_SPACE:
			case KeyEvent.KEYCODE_J:
				this.top += this.getHeight() - 16;
				this.invalidate();
				return true;
			case KeyEvent.KEYCODE_H:
				this.left -= this.getWidth() / 4;
				this.invalidate();
				return true;
			case KeyEvent.KEYCODE_L:
				this.left += this.getWidth() / 4;
				this.invalidate();
				return true;
			case KeyEvent.KEYCODE_DPAD_LEFT:
				scrollToPage(currentPage - 1);
				return true;
			case KeyEvent.KEYCODE_DPAD_RIGHT:
				scrollToPage(currentPage + 1);
				return true;
			case KeyEvent.KEYCODE_O:
				this.zoomLevel /= 1.1f;
				this.left /= 1.1f;
				this.top /= 1.1f;
				this.invalidate();
				return true;
			case KeyEvent.KEYCODE_P:
				this.zoomLevel *= 1.1f;
				this.left *= 1.1f;
				this.top *= 1.1f;
				this.invalidate();
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Test if specified rectangles intersect with each other.
	 * Uses Androids standard Rect class.
	 */
	private static boolean rectsintersect(
			int r1x0, int r1y0, int r1x1, int r1y1,
			int r2x0, int r2y0, int r2x1, int r2y1) {
		r1.set(r1x0, r1y0, r1x1, r1y1);
		return r1.intersects(r2x0, r2y0, r2x1, r2y1);
	}
	
	/**
	 * Used as a callback from pdf rendering code.
	 * TODO: only invalidate what needs to be painted, not the whole view
	 */
	public void onImagesRendered(Map<Tile,Bitmap> renderedTiles) {
		Rect rect = new Rect(); /* TODO: move out of onImagesRendered */

		int viewx0 = left - width/2;
		int viewy0 = top - height/2;
		
		int pageCount = this.pageSizes.length;
		float currentMargin = this.getCurrentMargin();
		
		viewx0 = adjustPosition(viewx0, width, (int)currentMargin, 
				getCurrentMaxPageWidth());
		viewy0 = adjustPosition(viewy0, height, (int)currentMargin,
				(int)getCurrentDocumentHeight());
		
		float currpageoff = currentMargin;
		float renderAhead = this.pagesProvider.getRenderAhead();

		float pagex0;
		float pagex1;
		float pagey0 = 0;
		float pagey1;
		float x;
		float y;
		int pageWidth;
		int pageHeight;
		
		for(int i = 0; i < pageCount; ++i) {
			// is page i visible?

			pageWidth = this.getCurrentPageWidth(i);
			pageHeight = (int) this.getCurrentPageHeight(i);
			
			pagex0 = currentMargin;
			pagex1 = (int)(currentMargin + pageWidth);
			pagey0 = currpageoff;
			pagey1 = (int)(currpageoff + pageHeight);
			
			if (rectsintersect(
						(int)pagex0, (int)pagey0, (int)pagex1, (int)pagey1, // page rect in doc
						viewx0, viewy0, viewx0 + this.width, 
						viewy0 + this.height  
					))
			{
				x = pagex0 - viewx0;
				y = pagey0 - viewy0;
				
				for (Tile tile: renderedTiles.keySet()) {
					if (tile.getPage() == i) {
						Bitmap b = renderedTiles.get(tile); 
						
						rect.left = (int)(x + tile.getX());
						rect.top = (int)(y + tile.getY());
						rect.right = rect.left + b.getWidth();
						rect.bottom = rect.top + b.getHeight();	
					
						if (rect.intersects(0, 0, this.width, (int)(renderAhead*this.height))) {
							Log.v(TAG, "New bitmap forces redraw");
							postInvalidate();
							return;
						}
					}
				}
				
			}
			currpageoff += currentMargin + this.getCurrentPageHeight(i);
		}
		Log.v(TAG, "New bitmap does not require redraw");
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
	synchronized public void scrollToPage(int page) {
		this.left = this.width / 2;
		float top = this.height / 2;
		for(int i = 0; i < page; ++i) {
			top += this.getCurrentPageHeight(i);
		}
		if (page > 0)
			top += (float)MARGIN * this.zoomLevel * 0.001f * (float)(page); 
		this.top = (int)top;
		this.currentPage = page;
		this.invalidate();
	}
	
//	/**
//	 * Compute what's currently visible.
//	 * @return collection of tiles that define what's currently visible
//	 */
//	private Collection<Tile> computeVisibleTiles() {
//		LinkedList<Tile> tiles = new LinkedList<Tile>();
//		float viewx = this.left + (this.dragx1 - this.dragx);
//		float viewy = this.top + (this.dragy1 - this.dragy);
//		float pagex = MARGIN;
//		float pagey = MARGIN;
//		float pageWidth;
//		float pageHeight;
//		int tileix;
//		int tileiy;
//		int thisPageTileCountX;
//		int thisPageTileCountY;
//		float tilex;
//		float tiley;
//		for(int page = 0; page < this.pageSizes.length; ++page) {
//			
//			pageWidth = this.getCurrentPageWidth(page);
//			pageHeight = this.getCurrentPageHeight(page);
//			
//			thisPageTileCountX = (int)Math.ceil(pageWidth / TILE_SIZE);
//			thisPageTileCountY = (int)Math.ceil(pageHeight / TILE_SIZE);
//			
//			if (viewy + this.height < pagey) continue; /* before first visible page */
//			if (viewx > pagey + pageHeight) break; /* after last page */
//
//			for(tileix = 0; tileix < thisPageTileCountX; ++tileix) {
//				for(tileiy = 0; tileiy < thisPageTileCountY; ++tileiy) {
//					tilex = pagex + tileix * TILE_SIZE;
//					tiley = pagey + tileiy * TILE_SIZE;
//					if (rectsintersect(viewx, viewy, viewx+this.width, viewy+this.height,
//							tilex, tiley, tilex+TILE_SIZE, tiley+TILE_SIZE)) {
//						tiles.add(new Tile(page, this.zoomLevel, (int)tilex, (int)tiley, this.rotation));
//					}
//				}
//			}
//			
//			/* move to next page */
//			pagey += this.getCurrentPageHeight(page) + MARGIN;
//		}
//		return tiles;
//	}
//	synchronized Collection<Tile> getVisibleTiles() {
//		return this.visibleTiles;
//	}
	
	/**
	 * Rotate pages.
	 * Updates rotation variable, then invalidates view.
	 * @param rotation rotation
	 */
	synchronized public void rotate(int rotation) {
		this.rotation = (this.rotation + rotation) % 4;
		this.invalidate();
	}
	
	/**
	 * Set find mode.
	 * @param m true if pages view should display find results and find controls
	 */
	synchronized public void setFindMode(boolean m) {
		if (this.findMode != m) {
			this.findMode = m;
			if (!m) {
				this.findResults = null;
			}
		}
	}
	
	/**
	 * Return find mode.
	 * @return find mode - true if view is currently in find mode
	 */
	public boolean getFindMode() {
		return this.findMode;
	}

//	/**
//	 * Ask pages provider to focus on next find result.
//	 * @param forward direction of search - true for forward, false for backward
//	 */
//	public void findNext(boolean forward) {
//		this.pagesProvider.findNext(forward);
//		this.scrollToFindResult();
//		this.invalidate();
//	}
	
	/**
	 * Move viewport position to find result (if any).
	 * Does not call invalidate().
	 */
	public void scrollToFindResult(int n) {
		if (this.findResults == null || this.findResults.isEmpty()) return;
		Rect center = new Rect();
		FindResult findResult = this.findResults.get(n);
		for(Rect marker: findResult.markers) {
			center.union(marker);
		}
		int page = findResult.page;
		int x = 0;
		int y = 0;
		for(int p = 0; p < page; ++p) {
			Log.d(TAG, "adding page " + p + " to y: " + this.pageSizes[p][1]);
			y += this.pageSizes[p][1];
		}
		x += (center.left + center.right) / 2;
		y += (center.top + center.bottom) / 2;
		
		float margin = this.getCurrentMargin();
	
		this.left = (int)(x * scaling0 * this.zoomLevel * 0.001f + margin);
		this.top = (int)(y * scaling0 * this.zoomLevel * 0.001f + (page+1)*margin);
	}
	
	/**
	 * Get the current page number
	 * 
	 * @return the current page. 0-based
	 */
	public int getCurrentPage() {
		return currentPage;
	}
	
	/**
	 * Get the current zoom level
	 * 
	 * @return the current zoom level
	 */
	public int getCurrentZoom() {
		return zoomLevel;
	}
	
	/**
	 * Get the current rotation
	 * 
	 * @return the current rotation
	 */
	public int getRotation() {
		return rotation;
	}
	
	/**
	 * Get page count.
	 */
	public int getPageCount() {
		return this.pageSizes.length;
	}
	
	/**
	 * Set find results.
	 */
	public void setFindResults(List<FindResult> results) {
		this.findResults = results;
	}
	
	/**
	 * Get current find results.
	 */
	public List<FindResult> getFindResults() {
		return this.findResults;
	}
	
	private void doFling(float vx, float vy) {
		float avx = vx > 0 ? vx : -vx;
		float avy = vy > 0 ? vy : -vy;
		
		if (avx < .25 * avy) {
			vx = 0;
		}
		else if (avy < .25 * avx) {
			vy = 0;
		}
		
		int margin = (int)getCurrentMargin();
		int minx = this.width/2 + getLowerBound(this.width, margin, 
				getCurrentMaxPageWidth());
		int maxx = this.width/2 + getUpperBound(this.width, margin, 
				getCurrentMaxPageWidth());
		int miny = this.height/2 + getLowerBound(this.width, margin,
				  getCurrentDocumentHeight());
		int maxy = this.height/2 + getUpperBound(this.width, margin,
				  getCurrentDocumentHeight());

		this.scroller.fling(this.left, this.top, 
				(int)-vx, (int)-vy,
				minx, maxx,
				miny, maxy);
		invalidate();
	}
	
	private void doScroll(int dx, int dy) {
		this.left += dx;
		this.top += dy;
		invalidate();
	}
	
	/**
	 * Zoom down one level
	 */
	public void zoomDown() {
		this.zoomLevel /= step;
		this.left /= step;
		this.top /= step;
		Log.d(TAG, "zoom level changed to " + this.zoomLevel);
		this.invalidate();		
	}

	/**
	 * Zoom down big 
	 */
	public void zoomDownBig() {
		this.zoomLevel /= 2;
		this.left /= 2;
		this.top /= 2;
		Log.d(TAG, "zoom level changed to " + this.zoomLevel);
		this.invalidate();		
	}

	/**
	 * Zoom up one level
	 */
	public void zoomUp() {
		this.zoomLevel *= step;
		this.left *= step;
		this.top *= step;
		Log.d(TAG, "zoom level changed to " + this.zoomLevel);
		this.invalidate();
	}

	/**
	 * Zoom up big level
	 */
	public void zoomUpBig() {
		this.zoomLevel *= 2;
		this.left *= 2;
		this.top *= 2;
		Log.d(TAG, "zoom level changed to " + this.zoomLevel);
		this.invalidate();
	}

	/* zoom to width */
	public void zoomWidth() {
		int page = currentPage < 0 ? 0 : currentPage;
		int pageWidth = getCurrentPageWidth(page);
		this.top = (this.top - this.height / 2) * this.width / pageWidth + this.height / 2;
		this.zoomLevel = this.zoomLevel * this.width / pageWidth;
		this.left = (int) (this.width/2 + getCurrentMargin());
		this.invalidate();		
	}

	/* zoom to fit */
	public void zoomFit() {
		int page = currentPage < 0 ? 0 : currentPage;
		int z1 = this.zoomLevel * this.width / getCurrentPageWidth(page);
		int z2 = (int)(this.zoomLevel * this.height / getCurrentPageHeight(page));
		this.zoomLevel = z2 < z1 ? z2 : z1;
		Point pos = getPagePositionInDocumentWithZoom(page);
		this.left = this.width/2 + pos.x;
		this.top = this.height/2 + pos.y;
		this.invalidate();		
	}

	/**
	 * Set zoom
	 */
	public void setZoomLevel(int zoomLevel) {
		if (this.zoomLevel == zoomLevel)
			return;
		this.zoomLevel = zoomLevel;
		Log.d(TAG, "zoom level changed to " + this.zoomLevel);
		this.invalidate();
	}
	
	
	/**
	 * Set rotation
	 */
	public void setRotation(int rotation) {
		if (this.rotation == rotation)
			return;
		this.rotation = rotation;
		Log.d(TAG, "rotation changed to " + this.rotation);
		this.invalidate();
	}
	
	
	public void setColorMode(int colorMode) {
		this.colorMode = colorMode;
		this.invalidate();
	}


	public void setZoomIncrement(float step) {
		this.step = step;
	}
	
	public void setPageWithVolume(boolean pageWithVolume) {
		this.pageWithVolume = pageWithVolume;
	}
	

	private void getGoodTileSizes(int[] sizes, int pageWidth, int pageHeight) {
		sizes[0] = getGoodTileSize(pageWidth, MIN_TILE_WIDTH, MAX_TILE_WIDTH);		
		sizes[1] = getGoodTileSize(pageHeight, MIN_TILE_HEIGHT, MAX_TILE_PIXELS / sizes[0]); 
	}
	
	private int getGoodTileSize(int pageSize, int minSize, int maxSize) {
		if (pageSize <= 2)
			return 2;
		if (pageSize <= maxSize)
			return pageSize;
		int numInPageSize = (pageSize + maxSize - 1) / maxSize;
		int proposedSize = (pageSize + numInPageSize - 1) / numInPageSize;
		if (proposedSize < minSize)
			return minSize;
		else
			return proposedSize;
	}
	
	/* Get the upper and lower bounds for the viewpoint.  The document itself is
	 * drawn from margin to margin+docDim.   
	 */
	private int getLowerBound(int screenDim, int margin, int docDim) {
		if (docDim <= screenDim) {
			/* all pages can and do fit */
			return margin + docDim - screenDim;
		}
		else {
			/* document is too wide/tall to fit */
			return 0; 
		}
	}
	
	private int getUpperBound(int screenDim, int margin, int docDim) {
		if (docDim <= screenDim) {
			/* all pages can and do fit */
			return margin;
		}
		else {
			/* document is too wide/tall to fit */
			return 2 * margin + docDim - screenDim;
		}
	}
	
	private int adjustPosition(int pos, int screenDim, int margin, int docDim) {
		int min = getLowerBound(screenDim, margin, docDim);
		int max = getUpperBound(screenDim, margin, docDim);
		
		if (pos < min)
			return min;
		else if (max < pos)
			return max;
		else
			return pos;
	}
}
