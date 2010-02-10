package cx.hell.android.lib.pagesview;

import java.util.LinkedList;
import java.util.Map;

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

	/**
	 * Logging tag.
	 */
	private static final String TAG = "cx.hell.android.pdfview";
	
	/**
	 * When fade starts.
	 */
	private final static long CONTROLS_FADE_START = 3000;
	
	/**
	 * How long should fade be visible..
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
	
	/**
	 * Source of page bitmaps.
	 */
	private PagesProvider pagesProvider = null;
	private long lastControlsUseMillis = 0;
	
	/**
	 * Current width of this view.
	 */
	private int width = 0;
	
	/**
	 * Current height of this view.
	 */
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
	 * 1000 is 100%.
	 */
	private int zoomLevel = 1000;
	
	/**
	 * Current rotation of pages.
	 */
	private int rotation = 0;
	
	/**
	 * Base scalling factor - how much shrink (or grow) page to fit it nicely to screen at zoomLevel = 1000.
	 * For example, if we determine that 200x400 image fits screen best, but PDF's pages are 400x800, then
	 * base scaling would be 0.5, since at base scalling, without any zoom, page should fit into screen nicely.
	 */
	private float scalling0 = 0f;
	
	/**
	 * Page sized obtained from pages provider.
	 * These do not change.
	 */
	private int pageSizes[][];
	
	/**
	 * Construct this view.
	 * @param activity parent activity
	 */
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
	
	/**
	 * Update zoom controls position and size based on current width and height.
	 */
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
		if (this.scalling0 == 0f) {
			this.scalling0 = Math.min(
					((float)this.height - 2*MARGIN) / (float)this.pageSizes[0][1],
					((float)this.width - 2*MARGIN) / (float)this.pageSizes[0][0]);
		}
		if (oldw == 0 && oldh == 0) {
			this.left = this.width / 2;
			this.top = this.height / 2;
		}
		this.setZoomControlsBounds();
	}
	
	public void setPagesProvider(PagesProvider pagesProvider) {
		this.pagesProvider = pagesProvider;
		if (this.pagesProvider != null) {
			this.pageSizes = this.pagesProvider.getPageSizes();
			if (this.width > 0 && this.height > 0) {
				this.scalling0 = Math.min(
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
	@Override
	public void onDraw(Canvas canvas) {
		this.drawPages(canvas);
		this.drawControls(canvas);
	}
	
	/**
	 * Get current page width by page number taking into account zoom and rotation
	 * @param pageno 0-based page number
	 */
	private int getCurrentPageWidth(int pageno) {
		float realpagewidth = (float)this.pageSizes[pageno][this.rotation % 2 == 0 ? 0 : 1];
		float currentpagewidth = realpagewidth * scalling0 * (this.zoomLevel*0.001f);
		return (int)currentpagewidth;
	}
	
	/**
	 * Get current page height by page number taking into account zoom and rotation.
	 * @param pageno 0-based page number
	 */
	private float getCurrentPageHeight(int pageno) {
		float realpageheight = (float)this.pageSizes[pageno][this.rotation % 2 == 0 ? 1 : 0];
		float currentpageheight = realpageheight * scalling0 * (this.zoomLevel*0.001f);
		return currentpageheight;
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
		int dragoffx, dragoffy;
		int viewx0, viewy0; // view over doc
		LinkedList<Tile> visibleTiles = new LinkedList<Tile>();
		float currentMargin = (float)MARGIN * this.zoomLevel * 0.001f;
		if (this.pagesProvider != null) {

			dragoffx = (inDrag) ? (dragx1 - dragx) : 0;
			dragoffy = (inDrag) ? (dragy1 - dragy) : 0;
			
			viewx0 = left - dragoffx - width/2;
			viewy0 = top - dragoffy - height/2;
			
			int pageCount = this.pageSizes.length;
			float currpageoff = currentMargin;
			
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
							viewx0, viewy0, viewx0 + this.width, viewy0 + this.height // viewport rect in doc 
						))
				{

					x = pagex0 - viewx0;
					y = pagey0 - viewy0;
					
					for(int tileix = 0; tileix < pageWidth / TILE_SIZE + 1; ++tileix)
						for(int tileiy = 0; tileiy < pageHeight / TILE_SIZE + 1; ++tileiy) {
							
							dst.left = (int)(x + tileix*TILE_SIZE);
							dst.top = (int)(y + tileiy*TILE_SIZE);
							dst.right = dst.left + TILE_SIZE;
							dst.bottom = dst.top + TILE_SIZE;	
						
							if (dst.intersects(0, 0, this.width, this.height)) {
								/* tile is visible */
								Tile tile = new Tile(i, (int)(this.zoomLevel * scalling0), tileix*TILE_SIZE, tileiy*TILE_SIZE, this.rotation);
								Bitmap b = this.pagesProvider.getPageBitmap(tile);
								if (b != null) {
									//Log.d(TAG, "  have bitmap: " + b + ", size: " + b.getWidth() + " x " + b.getHeight());
									src.left = 0;
									src.top = 0;
									src.right = b.getWidth();
									src.bottom = b.getWidth();
									
									if (dst.right > x + pageWidth) {
										src.right = (int)(b.getWidth() * (float)((x+pageWidth)-dst.left) / (float)(dst.right - dst.left));
										dst.right = (int)(x + pageWidth);
									}
									
									if (dst.bottom > y + pageHeight) {
										src.bottom = (int)(b.getHeight() * (float)((y+pageHeight)-dst.top) / (float)(dst.bottom - dst.top));
										dst.bottom = (int)(y + pageHeight);
									}
									
									//this.fixOfscreen(dst, src);
									canvas.drawBitmap(b, src, dst, null);
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
			Log.d(TAG, "onTouch(ACTION_DOWN)");
			int x = (int)event.getX();
			int y = (int)event.getY();
			if (this.zoomMinusDrawable.getBounds().contains(x,y)) {
				float step = 0.5f;
				this.zoomLevel *= step;
//				float cx, cy;
//				cx = this.left + this.width / 2;
//				cy = this.top + this.height / 2;
//				cx = cx * step;
//				cy = cy * step;
//				this.left = (int)(cx - this.width / 2f);
//				this.top = (int)(cy - this.height / 2f);
				this.left *= step;
				this.top *= step;
				Log.d(TAG, "zoom level changed to " + this.zoomLevel);
				this.invalidate();
			} else if (this.zoomPlusDrawable.getBounds().contains(x,y)) {
				float step = 2f;
				this.zoomLevel *= step;
				this.left *= step;
				this.top *= step;
//				float cx, cy;
//				cx = this.left + this.width / 2;
//				cy = this.top + this.height / 2;
//				cx = cx * step;
//				cy = cy * step;
//				this.left = (int)(cx - this.width / 2f);
//				this.top = (int)(cy - this.height / 2f);
				Log.d(TAG, "zoom level changed to " + this.zoomLevel);
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
		Rect r1 = new Rect();
		r1.set(r1x0, r1y0, r1x1, r1y1);
		return r1.intersects(r2x0, r2y0, r2x1, r2y1);
	}

//	/**
//	 * Test if specified rectangles intersect with each other.
//	 * Uses Androids standard RectF class.
//	 */
//	private static boolean rectsintersect(
//			float r1x0, float r1y0, float r1x1, float r1y1,
//			float r2x0, float r2y0, float r2x1, float r2y1) {
//		RectF r1 = new RectF(r1x0, r1y0, r1x1, r1y1);
//		return r1.intersects(r2x0, r2y0, r2x1, r2y1);
//	}
	
	/**
	 * Used as a callback from pdf rendering code.
	 * TODO: only invalidate what needs to be painted, not the whole view
	 */
	public void onImagesRendered(Map<Tile,Bitmap> renderedTiles) {
		Log.d(TAG, "there are " + renderedTiles.size() + " new rendered tiles");
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
		this.left = this.width / 2;
		float top = this.height / 2;
		for(int i = 0; i < page; ++i) {
			top += this.getCurrentPageHeight(i);
		}
		if (page > 0)
			top += (float)MARGIN * this.zoomLevel * 0.001f * (float)(page); 
		this.top = (int)top;
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
	
	synchronized public void rotate(int rotation) {
		this.rotation = (this.rotation + rotation) % 4;
		this.invalidate();
	}	
}

