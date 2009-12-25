package cx.hell.android.pdfview;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import android.graphics.Bitmap;
import android.util.Log;
import cx.hell.android.lib.pagesview.OnImageRenderedListener;
import cx.hell.android.lib.pagesview.PagesProvider;
import cx.hell.android.lib.pagesview.PagesView;
import cx.hell.android.lib.pagesview.RenderingException;

public class PDFPagesProvider extends PagesProvider {

	private final static String TAG = "cx.hell.android.pdfview";
	
	/**
	 * Smart page-bitmap cache.
	 * Stores up to approx MAX_CACHE_SIZE_BYTES of images.
	 * Dynamically drops oldest unused bitmaps.
	 * TODO: Return high resolution bitmap if no exact res is available.
	 * Bitmap images are tiled - tile size is specified in PagesView.TILE_SIZE.
	 */
	private static class BitmapCache {
		
		private static final int MAX_CACHE_SIZE_BYTES = 2*1024*1024;
		
		private static class BitmapCacheKey {
			int pagenum;
			int zoom;
			int tilex;
			int tiley;
			private int _hashCode;
			
			BitmapCacheKey(int pagenum, int zoom, int tilex, int tiley) {
				this.pagenum = pagenum;
				this.zoom = zoom;
				this.tilex = tilex;
				this.tiley = tiley;
				this._hashCode = (this.pagenum + ":" + this.zoom + ":" + this.tilex + ":" + this.tiley).hashCode();
			}
			
			public boolean equals(Object o) {
				if (! (o instanceof BitmapCacheKey)) return false;
				BitmapCacheKey k = (BitmapCacheKey) o;
				//Log.d("cx.hell.android.pdfview2.pagecache", "equals(" + this + ", " + k);
				return (
							this._hashCode == k._hashCode
							&& this.pagenum == k.pagenum
							&& this.zoom == k.zoom
							&& this.tilex == k.tilex
							&& this.tiley == k.tiley
						);
			}
			
			public int hashCode() {
				//Log.d("cx.hell.android.pdfview2.pagecache", "hashCode(" + this + ")");
				return this._hashCode;
				
			}
			
			public String toString() {
				return "BitmapCacheKey(" +
					this.pagenum + ", " +
					this.zoom + ", " +
					this.tilex + ", " +
					this.tiley + ")";
			}
		}
		
		/**
		 * Cache value - tuple with data and properties.
		 */
		private static class BitmapCacheValue {
			public Bitmap bitmap;
			public long millisAdded;
			public long millisAccessed;
			public BitmapCacheValue(Bitmap bitmap, long millisAdded) {
				this.bitmap = bitmap;
				this.millisAdded = millisAdded;
				this.millisAccessed = millisAdded;
			}
		}
		
		private Map<BitmapCacheKey, BitmapCacheValue> bitmaps;
		
		private long hits;
		private long misses;
		
		BitmapCache() {
			this.bitmaps = new HashMap<BitmapCacheKey, BitmapCacheValue>();
			this.hits = 0;
			this.misses = 0;
		}
		
		Bitmap get(int page, int zoom, int x, int y) {
			//Log.d("cx.hell.android.pdfview2.pagecache", "get(" + width + ", " + height + ", " + pageno + ")");
			BitmapCacheKey k = new BitmapCacheKey(page, zoom, x, y);
			BitmapCacheValue v = this.bitmaps.get(k);
			Bitmap b = null;
			if (v != null) {
				// yeah
				b = v.bitmap;
				assert b != null;
				v.millisAccessed = System.currentTimeMillis();
				this.hits += 1;
			} else {
				// le fu
				this.misses += 1;
			}
			if ((this.hits + this.misses) % 100 == 0 && (this.hits > 0 || this.misses > 0)) {
				Log.d("cx.hell.android.pdfview2.pagecache", "hits: " + hits + ", misses: " + misses + ", hit ratio: " + (float)(hits) / (float)(hits+misses) +
						", size: " + this.bitmaps.size());
			}
			return b;
		}
		
		synchronized void put(int page, int zoom, int x, int y, Bitmap bitmap) {
			while (this.willExceedCacheSize(bitmap) && !this.bitmaps.isEmpty()) {
				this.removeOldest();
			}
			this.bitmaps.put(new BitmapCacheKey(page, zoom, x, y), new BitmapCacheValue(bitmap, System.currentTimeMillis()));
			//Log.d("cx.hell.android.pdfview2.pagecache", "put(" + width + ", " + height + ", " + pageno + ")");
		}
		
		/**
		 * Estimate bitmap memory size.
		 * This is just a guess.
		 */
		private static int getBitmapSizeInCache(Bitmap bitmap) {
			assert bitmap.getConfig() == Bitmap.Config.RGB_565;
			return bitmap.getWidth() * bitmap.getHeight() * 2;
		}
		
		/**
		 * Get estimated sum of sizes of bitmaps stored in cache currently.
		 */
		private synchronized int getCurrentCacheSize() {
			int size = 0;
			Iterator<BitmapCacheValue> it = this.bitmaps.values().iterator();
			while(it.hasNext()) {
				BitmapCacheValue bcv = it.next();
				Bitmap bitmap = bcv.bitmap;
				size += getBitmapSizeInCache(bitmap);
			}
			return size;
		}
		
		/**
		 * Determine if adding this bitmap would grow cache size beyond max size.
		 */
		private synchronized boolean willExceedCacheSize(Bitmap bitmap) {
			return (this.getCurrentCacheSize() + BitmapCache.getBitmapSizeInCache(bitmap) > MAX_CACHE_SIZE_BYTES);
		}
		
		/**
		 * Remove oldest bitmap cache value.
		 */
		private void removeOldest() {
			Iterator<BitmapCacheKey> i = this.bitmaps.keySet().iterator();
			long minmillis = 0;
			BitmapCacheKey oldest = null;
			while(i.hasNext()) {
				BitmapCacheKey k = i.next();
				BitmapCacheValue v = this.bitmaps.get(k);
				if (oldest == null) {
					oldest = k;
					minmillis = v.millisAccessed;
				} else {
					if (minmillis > v.millisAccessed) {
						minmillis = v.millisAccessed;
						oldest = k;
					}
				}
			}
			if (oldest == null) throw new RuntimeException("couldnt find oldest");
			// also recycle bitmap
			BitmapCacheValue v = this.bitmaps.get(oldest);
			v.bitmap.recycle();
			this.bitmaps.remove(oldest);
			Log.d("cx.hell.android.pdfview2.pagecache", "removed oldest (" + oldest + ")");
		}
		
		/**
		 * Touch a bitmap in cache to mark that it's been used.
		 * @return true if entry was found.
		 */
		private synchronized boolean touch(int page, int zoom, int x, int y) {
			BitmapCacheKey k = new BitmapCacheKey(page, zoom, x, y);
			BitmapCacheValue v = this.bitmaps.get(k);
			if (v != null) {
				v.millisAccessed = System.currentTimeMillis();
				return true;
			} else
				return false;
		}
	}
	
	/**
	 * Renderer task - specifies what should be rendered.
	 */
	private static class RenderingTask {
		public int pageno;
		public int zoom;
		public int tilex;
		public int tiley;
		public RenderingTask(int page, int zoom, int x, int y) {
			this.pageno = page;
			this.zoom = zoom;
			this.tilex = x;
			this.tiley = y;
		}
	}
	
	private static class RendererWorker implements Runnable {
		private PDFPagesProvider pdfPagesProvider;
		private BlockingQueue<RenderingTask> tasks = null;
		private boolean shouldStop = false;
		
		RendererWorker(PDFPagesProvider pdfPagesProvider) {
			this.pdfPagesProvider = pdfPagesProvider;
			this.shouldStop = false;
			this.tasks = new LinkedBlockingQueue<RenderingTask>();
		}
		
		public void pleaseStop() {
			this.shouldStop = true;
		}
		
		public void addTask(int page, int zoom, int x, int y) {
			this.tasks.add(new RenderingTask(page, zoom, x, y));
		}
		
		public void run() {
			while(true) {
				if (this.shouldStop) break;
				RenderingTask task = null;
				try {
					task = this.tasks.poll(10, TimeUnit.SECONDS);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
				if (task == null) {
					Log.d(TAG, "renderer worker: still noting to do");
					continue;
				}
				Log.d("cx.hell.android.pdfview2", "got rendering task: " +
						"page: " + task.pageno + ", " +
						"zoom: " + task.zoom + ", " +
						"x: " + task.tilex + ", " +
						"y: " + task.tiley);
				if (! this.pdfPagesProvider.alreadyRendered(task.pageno, task.zoom, task.tilex, task.tiley)) {
					Bitmap tile = null;
					try {
						tile = this.pdfPagesProvider.renderBitmap(task.pageno, task.zoom, task.tilex, task.tiley);
						this.pdfPagesProvider.publishBitmap(task.pageno, task.zoom, task.tilex, task.tiley, tile);
					} catch (RenderingException e) {
						this.pdfPagesProvider.publishRenderingException(e);
					}
				}
			}
		}
	}
	
	private PDF pdf = null;
	private BitmapCache bitmapCache = null;
	private RendererWorker rendererWorker = null;
	private Thread rendererWorkerThread = null;
	private OnImageRenderedListener onImageRendererListener = null;
	
	public PDFPagesProvider(PDF pdf) {
		this.pdf = pdf;
		this.bitmapCache = new BitmapCache();
		this.rendererWorker = new RendererWorker(this);
		this.rendererWorkerThread = new Thread(this.rendererWorker);
		this.rendererWorkerThread.setPriority(Thread.MIN_PRIORITY);
		this.rendererWorkerThread.start();
	}
	
	/**
	 * Really render bitmap. Takes time, should be done in background thread. Calls native code (through PDF object).
	 */
	private Bitmap renderBitmap(int page, int zoom, int x, int y) throws RenderingException {
		Log.d(TAG, "renderBitmap(" + page + ", " + zoom + ", " + x + ", " + y + ")");
		Bitmap b, btmp;
		Log.d("cx.hell.android.pdfview2", "will now render page ;)");
		PDF.Size size = new PDF.Size(PagesView.TILE_SIZE, PagesView.TILE_SIZE);
		int[] pagebytes = pdf.renderPage(page, zoom, x*PagesView.TILE_SIZE, y*PagesView.TILE_SIZE, size); /* native */
		if (pagebytes == null) throw new RenderingException("Couldn't render page " + (page+1));
		Log.d("cx.hell.android.pdfview2", "got int buf, size: " + pagebytes.length);
		Log.d("cx.hell.android.pdfview2", "dimensions: " + size.width + " x " + size.height);
		
		b = Bitmap.createBitmap(pagebytes, size.width, size.height, Bitmap.Config.ARGB_8888);

		/* TODO: analyze if it's really needed */
		btmp = b.copy(Bitmap.Config.RGB_565, true);
		if (btmp == null) throw new RuntimeException("bitmap copy failed");
		b.recycle();
		b = btmp;
		this.bitmapCache.put(page, zoom, x, y, b);
		return b;
	}
	
	private void publishBitmap(int page, int zoom, int x, int y, Bitmap b) {
		if (this.onImageRendererListener != null) {
			this.onImageRendererListener.onImageRendered(page, zoom, x, y, b);
		}
	}
	
	private void publishRenderingException(RenderingException e) {
		if (this.onImageRendererListener != null) {
			this.onImageRendererListener.onRenderingException(e);
		}
	}
	
	private boolean alreadyRendered(int page, int zoom, int x, int y) {
		return this.bitmapCache.touch(page, zoom, x, y);
	}
	
	@Override
	public void setOnImageRenderedListener(OnImageRenderedListener l) {
		this.onImageRendererListener = l;
	}
	
	/**
	 * Provide page bitmap. If there's no bitmap available immediately - add task to rendering queue.
	 * @param pageNumber page number
	 * @param zoom zoom level as int where 1000 equals 100% zoom
	 * @param tilex tile x coord
	 * @param tiley tile y coord
	 * @return rendered tile; tile represents rect of TILE_SIZE x TILE_SIZE pixels,
	 * but might be of different size (should be scaled when painting) 
	 */
	@Override
	public Bitmap getPageBitmap(int pageNumber, int zoom, int tilex, int tiley) {
		Bitmap b = null;
		b = this.bitmapCache.get(pageNumber, zoom, tilex, tiley);
		if (b != null) return b;
		this.rendererWorker.addTask(pageNumber, zoom, tilex, tiley);
		return null;
	}

	@Override
	public int getPageCount() {
		int c = this.pdf.getPageCount();
		if (c <= 0) throw new RuntimeException("failed to load pdf file");
		return c;
	}
	
	@Override
	public int[][] getPageSizes() {
		int cnt = this.getPageCount();
		int[][] sizes = new int[cnt][];
		PDF.Size size = new PDF.Size();
		int err;
		for(int i = 0; i < cnt; ++i) {
			err = this.pdf.getPageSize(i, size);
			if (err != 0) {
				throw new RuntimeException("failed to getPageSize(" + i + ",...), error: " + err);
			}
			sizes[i] = new int[2];
			sizes[i][0] = size.width;
			sizes[i][1] = size.height;
		}
		return sizes;
	}
}
