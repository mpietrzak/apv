package cx.hell.android.pdfview;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import android.graphics.Bitmap;
import android.util.Log;
import cx.hell.android.lib.pagesview.OnImageRenderedListener;
import cx.hell.android.lib.pagesview.PagesProvider;
import cx.hell.android.lib.pagesview.PagesView;
import cx.hell.android.lib.pagesview.RenderingException;
import cx.hell.android.lib.pagesview.Tile;

/**
 * Provide rendered bitmaps of pages.
 */
public class PDFPagesProvider extends PagesProvider {

	/**
	 * Const used by logging.
	 */
	private final static String TAG = "cx.hell.android.pdfview";

	/**
	 * Smart page-bitmap cache.
	 * Stores up to approx MAX_CACHE_SIZE_BYTES of images.
	 * Dynamically drops oldest unused bitmaps.
	 * TODO: Return high resolution bitmaps if no exact res is available.
	 * Bitmap images are tiled - tile size is specified in PagesView.TILE_SIZE.
	 */
	private static class BitmapCache {

		/**
		 * Max size of bitmap cache.
		 */
		private static final int MAX_CACHE_SIZE_BYTES = 5*1024*1024;
		
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
		
		/**
		 * Stores cached bitmaps.
		 */
		private Map<Tile, BitmapCacheValue> bitmaps;
		
		/**
		 * Stats logging - number of cache hits.
		 */
		private long hits;
		
		/**
		 * Stats logging - number of misses.
		 */
		private long misses;
		
		BitmapCache() {
			this.bitmaps = new HashMap<Tile, BitmapCacheValue>();
			this.hits = 0;
			this.misses = 0;
		}
		
		/**
		 * Get cached bitmap.
		 * @param k cache key
		 * @return bitmap found in cache or null if there's no matching bitmap
		 */
		Bitmap get(Tile k) {
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
				Log.d("cx.hell.android.pdfview.pagecache", "hits: " + hits + ", misses: " + misses + ", hit ratio: " + (float)(hits) / (float)(hits+misses) +
						", size: " + this.bitmaps.size());
			}
			return b;
		}
		
		synchronized void put(Tile tile, Bitmap bitmap) {
			while (this.willExceedCacheSize(bitmap) && !this.bitmaps.isEmpty()) {
				this.removeOldest();
			}
			this.bitmaps.put(tile, new BitmapCacheValue(bitmap, System.currentTimeMillis()));
		}
		
		/**
		 * Check if cache contains specified bitmap tile. Doesn't update last-used timestamp.
		 * @return true if cache contains specified bitmap tile
		 */
		synchronized boolean contains(Tile tile) {
			return this.bitmaps.containsKey(tile);
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
		 * Get estimated sum of byte sizes of bitmaps stored in cache currently.
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
			Iterator<Tile> i = this.bitmaps.keySet().iterator();
			long minmillis = 0;
			Tile oldest = null;
			while(i.hasNext()) {
				Tile k = i.next();
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
			Log.d("cx.hell.android.pdfview.pagecache", "removed oldest (" + oldest + ")");
		}
	}
	
	private static class RendererWorker implements Runnable {
		
		private PDFPagesProvider pdfPagesProvider;
		
		private Collection<Tile> tiles;
		
		/**
		 * Loop exit flag.
		 */
		private boolean shouldStop = false;
		
		RendererWorker(PDFPagesProvider pdfPagesProvider) {
			this.pdfPagesProvider = pdfPagesProvider;
			this.shouldStop = false;
		}
		
		/**
		 * Stop as soon as possible.
		 */
		public void pleaseStop() {
			this.shouldStop = true;
		}
		
		synchronized void setTiles(Collection<Tile> tiles) {
			{
				this.tiles = tiles;
				this.notify();
			}
		}
		
		synchronized Collection<Tile> popTiles() {
			if (this.tiles == null || this.tiles.isEmpty()) {
				try {
					this.wait();
					if (this.shouldStop) return null;
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
			/* debug */
			if (this.tiles == null || this.tiles.isEmpty()) {
				throw new RuntimeException("Worker has been woken up, but there are no tiles!"); 
			}
			Tile tile = this.tiles.iterator().next();
			this.tiles.remove(tile);
			return Collections.singleton(tile);
		}
		
		public void run() {
			while(true) {
				Collection<Tile> tiles = this.popTiles(); /* this can block */
				if (this.shouldStop) break;
				try {
					Map<Tile,Bitmap> renderedTiles = this.pdfPagesProvider.renderTiles(tiles);
					/* should we publish bitmaps if we've been asked to stop? */
					if (this.shouldStop) break;
					this.pdfPagesProvider.publishBitmaps(renderedTiles);
				} catch (RenderingException e) {
					this.pdfPagesProvider.publishRenderingException(e);
				}
				if (this.shouldStop) break;
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
	
	private Map<Tile,Bitmap> renderTiles(Collection<Tile> tiles) throws RenderingException {
		Map<Tile,Bitmap> renderedTiles = new HashMap<Tile,Bitmap>();
		Iterator<Tile> i = tiles.iterator();
		Tile tile = null;
		while(i.hasNext()) {
			tile = i.next();
			Log.d(TAG, "rendering tile " + tile);
			renderedTiles.put(tile, this.renderBitmap(tile));
		}
		return renderedTiles;
	}
	
	/**
	 * Really render bitmap. Takes time, should be done in background thread. Calls native code (through PDF object).
	 */
	private Bitmap renderBitmap(Tile tile) throws RenderingException {
		Log.d(TAG, "renderBitmap(" + tile.getPage() + ", " + tile.getZoom() + ", " + tile.getX() + ", " + tile.getY() + ", " + tile.getRotation() + ")");
		Bitmap b, btmp;
		PDF.Size size = new PDF.Size(PagesView.TILE_SIZE, PagesView.TILE_SIZE);
		int[] pagebytes = pdf.renderPage(tile.getPage(), tile.getZoom(), tile.getX(), tile.getY(), tile.getRotation(), size); /* native */
		if (pagebytes == null) throw new RenderingException("Couldn't render page " + tile.getPage());
		Log.d(TAG, "got int buf, size: " + pagebytes.length);
		Log.d(TAG, "dimensions: " + size.width + " x " + size.height);
		
		b = Bitmap.createBitmap(pagebytes, size.width, size.height, Bitmap.Config.ARGB_8888);

		/* TODO: analyze if it's really needed - I'm trying to convert 8888 to 565 to save mem */
		btmp = b.copy(Bitmap.Config.RGB_565, true);
		if (btmp == null) throw new RuntimeException("bitmap copy failed");
		b.recycle();
		b = btmp;
		
		this.bitmapCache.put(tile, b);
		return b;
	}
	
	private void publishBitmaps(Map<Tile,Bitmap> renderedTiles) {
		if (this.onImageRendererListener != null) {
			this.onImageRendererListener.onImagesRendered(renderedTiles);
		} else {
			Log.w(TAG, "we've got new bitmaps, but there's no one to notify about it!");
		}
	}
	
	private void publishRenderingException(RenderingException e) {
		if (this.onImageRendererListener != null) {
			this.onImageRendererListener.onRenderingException(e);
		}
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
	 * @param rotation rotation
	 * @return rendered tile; tile represents rect of TILE_SIZE x TILE_SIZE pixels,
	 * but might be of different size (should be scaled when painting) 
	 */
	@Override
	public Bitmap getPageBitmap(Tile tile) {
		Bitmap b = null;
		b = this.bitmapCache.get(tile);
		if (b != null) return b;
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
	
	/**
	 * View informs provider what's currently visible.
	 * Compute what should be rendered and pass that info to renderer worker thread, possibly waking up worker.
	 * @param tiles specs of whats currently visible
	 */
	public void setVisibleTiles(Collection<Tile> tiles) {
		List<Tile> newtiles = null;
		for(Tile tile: tiles) {
			if (!this.bitmapCache.contains(tile)) {
				if (newtiles == null) newtiles = new LinkedList<Tile>();
				newtiles.add(tile);
			}
		}
		if (newtiles != null) {
			this.rendererWorker.setTiles(newtiles);
		}
	}
}
