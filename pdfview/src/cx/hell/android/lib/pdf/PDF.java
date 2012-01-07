package cx.hell.android.lib.pdf;

import java.io.File;
import java.io.FileDescriptor;
import java.util.List;

import cx.hell.android.lib.pagesview.FindResult;

// #ifdef pro
// import java.util.ArrayList;
// import java.util.HashMap;
// import java.util.Stack;
// import cx.hell.android.lib.view.TreeView;
// import cx.hell.android.lib.view.TreeView.TreeNode;
// #endif


/**
 * Native PDF - interface to native code.
 */
public class PDF {
	static {
        System.loadLibrary("pdfview2");
	}
	
	/**
	 * Simple size class used in JNI to simplify parameter passing.
	 * This shouldn't be used anywhere outside of pdf-related code.
	 */
	public static class Size implements Cloneable {
		public int width;
		public int height;
		
		public Size() {
			this.width = 0;
			this.height = 0;
		}
		
		public Size(int width, int height) {
			this.width = width;
			this.height = height;
		}
		
		public Size clone() {
			return new Size(this.width, this.height);
		}
	}
	
	// #ifdef pro
// 	/**
// 	 * Java version of fz_outline.
// 	 */
// 	public static class Outline implements TreeView.TreeNode {
// 
// 		/**
// 		 * Very special kind of "persistent" id.
// 		 * 
// 		 * This should be the same each time Outline is parsed for every given file.
// 		 * This will eventually be used to save TOC view state for each file.
// 		 * Format is: "x.y.z(...)", where x, y, z are 0-based indexes in current tree level.
// 		 * For example, first top level element of TOC will have string id "0".
// 		 * Third child of second top level element will have id "1.2".
// 		 * 
// 		 * Another example of TOC tree:
// 		 * <ul>
// 		 * 	<li>
// 		 * 		0
// 		 * 		<ul>
// 		 * 			<li>0.0</li>
// 		 * 			<li>0.1</li>
// 		 * 			<li>0.2</li>
// 		 * 		</ul>
// 		 * 	</li>
// 		 * 	<li>1</li>
// 		 * 	<li>
// 		 * 		2
// 		 * 		<ul>
// 		 * 			<li>2.0</li>
// 		 * 		</ul>
// 		 * 	</li>
// 		 * 
// 		 * This value is not returnet from native code, so it is not always mandatory.
// 		 * 
// 		 * Usually TOC should not be changed after it's returned from getOutline method.
// 		 * Numbers must be updated if any change is made to the structure.
// 		 * 
// 		 * Structure consistency is not in any way enforced.
// 		 */ 
// 		private String stringId;
// 		
// 		/**
// 		 * Numeric id. Used in TreeView.
// 		 * Must uniquely identify each element in tree.
// 		 */
// 		private long id;
// 		
// 		public String title;
// 		public int page;
// 		public Outline next;
// 		public Outline down;
// 		
// 		/**
// 		 * Set string id.
// 		 * @param stringId new string id
// 		 */
// 		public void setStringId(String stringId) {
// 			this.stringId = stringId;
// 		}
// 		
// 		/**
// 		 * Return this.stringId.
// 		 * @see stringId.
// 		 */
// 		public String getStringId() {
// 			return this.stringId;
// 		}
// 		
// 		/**
// 		 * Set id.
// 		 * @param id new id
// 		 */
// 		public void setId(long id) {
// 			this.id = id;
// 		}
// 		
// 		/**
// 		 * Get numeric id.
// 		 * @see id
// 		 */
// 		public long getId() {
// 			return this.id;
// 		}
// 		
// 		/**
// 		 * Get next element in tree.
// 		 */
// 		public TreeNode getNext() {
// 			return this.next;
// 		}
// 		
// 		/**
// 		 * Get first child.
// 		 */
// 		public TreeNode getDown() {
// 			return this.down;
// 		}
// 		
// 		/**
// 		 * Return true if this outline element has children.
// 		 * @return true if has children
// 		 */
// 		public boolean hasChildren() {
// 			return this.down != null;
// 		}
// 		
// 		/**
// 		 * Get list of children of this tree node.
// 		 */
// 		public List<TreeNode> getChildren() {
// 			ArrayList<TreeNode> children = new ArrayList<TreeNode>();
// 			for(Outline child = this.down; child != null; child = child.next) {
// 				children.add(child);
// 			}
// 			return children;
// 		}
// 		
// 		/**
// 		 * Return text.
// 		 */
// 		public String getText() {
// 			return this.title;
// 		}
// 		
// 		/**
// 		 * Get level.
// 		 * Currently it splits stringId by dots and returns length of resulting array minus 1.
// 		 * This way "1.1" gives 1 and "3" gives 0 etc.
// 		 * TODO: optimize
// 		 */
// 		public int getLevel() {
// 			if (stringId != null) {
// 				String[] ids = this.stringId.split("\\.");
// 				return ids.length - 1;
// 			} else {
// 				throw new IllegalStateException("can't get level if stringId is not set");
// 			}
// 		}
// 	}
	// #endif

	/**
	 * Holds pointer to native pdf_t struct.
	 */
	private int pdf_ptr = -1;
	private int invalid_password = 0;
	
	public boolean isValid() {
		return pdf_ptr != 0;
	}
	
	public boolean isInvalidPassword() {
		return invalid_password != 0;
	}

	/**
	 * Parse bytes as PDF file and store resulting pdf_t struct in pdf_ptr.
	 * @return error code
	 */
/*	synchronized private native int parseBytes(byte[] bytes, int box); */
	
	/**
	 * Parse PDF file.
	 * @param fileName pdf file name
	 * @return error code
	 */
	synchronized private native int parseFile(String fileName, int box, String password);
	
	/**
	 * Parse PDF file.
	 * @param fd opened file descriptor
	 * @return error code
	 */
	synchronized private native int parseFileDescriptor(FileDescriptor fd, int box, String password);

	/**
	 * Construct PDF structures from bytes stored in memory.
	 */
/*	public PDF(byte[] bytes, int box) {
		this.parseBytes(bytes, box);
	} */
	
	/**
	 * Construct PDF structures from file sitting on local filesystem.
	 */
	public PDF(File file, int box) {
		this.parseFile(file.getAbsolutePath(), box, "");
	}
	
	/**
	 * Construct PDF structures from opened file descriptor.
	 * @param file opened file descriptor
	 */
	public PDF(FileDescriptor file, int box) {
		this.parseFileDescriptor(file, box, "");
	}
	
	/**
	 * Return page count from pdf_t struct.
	 */
	synchronized public native int getPageCount();
	
	/**
	 * Render a page.
	 * @param n page number, starting from 0
	 * @param zoom page size scaling
	 * @param left left edge
	 * @param right right edge
	 * @param passes requested size, used for size of resulting bitmap
	 * @return bytes of bitmap in Androids format
	 */
	synchronized public native int[] renderPage(int n, int zoom, int left, int top, 
			int rotation, boolean gray, boolean skipImages, PDF.Size rect);
	
	/**
	 * Get PDF page size, store it in size struct, return error code.
	 * @param n 0-based page number
	 * @param size size struct that holds result
	 * @return error code
	 */
	synchronized public native int getPageSize(int n, PDF.Size size);
	
	/**
	 * Export PDF to a text file.
	 */
//	synchronized public native void export();

	/**
	 * Find text on given page, return list of find results.
	 */
	synchronized public native List<FindResult> find(String text, int page);
	
	/**
	 * Clear search.
	 */
	synchronized public native void clearFindResult();
	
	/**
	 * Find text on page, return find results.
	 */
	synchronized public native List<FindResult> findOnPage(int page, String text);

	// #ifdef pro
// 	/**
// 	 * Get document outline.
// 	 */
// 	synchronized public native Outline getOutlineNative();
// 	
// 	/**
// 	 * Get outline.
// 	 * Calls getOutlineNative and then calculates stringIds and ids.
// 	 * @return outline with correct stringId and id fields set.
// 	 */
// 	synchronized public Outline getOutline() {
// 		Outline outlineRoot = this.getOutlineNative();
// 		Stack<Outline> stack = new Stack<Outline>();
// 		stack.push(outlineRoot);
// 		long id = 0;
// 		while(!stack.empty()) {
// 			Outline node = stack.pop();
// 			if (node == null) throw new RuntimeException("internal error");
// 			node.setId(id);
// 			id++;
// 			if (node.next != null) stack.push(node.next);
// 			if (node.down != null) stack.push(node.down);
// 		}
// 		
// 		/* create parent map */
// 		HashMap<Long, Outline> parentMap = new HashMap<Long, Outline>();
// 		HashMap<Long, Integer> order = new HashMap<Long, Integer>();
// 		int i = 0;
// 		for(Outline child = outlineRoot; child != null; child = child.next) {
// 			order.put(child.getId(), i);
// 			i++;
// 		}
// 		stack.clear();
// 		stack.push(outlineRoot);
// 		while(!stack.empty()) {
// 			Outline node = stack.pop();
// 			i = 0;
// 			for(Outline child = node.down; child != null; child = child.next) {
// 				parentMap.put(child.getId(), node);
// 				stack.push(child);
// 				order.put(child.getId(), i);
// 				i++;
// 			}
// 		}
// 		
// 		/* now for each node create string id */
// 		stack.clear();
// 		stack.push(outlineRoot);
// 		while(!stack.empty()) {
// 			Outline node = stack.pop();
// 			if (node.next != null) stack.push(node.next);
// 			if (node.down != null) stack.push(node.down);
// 			String stringId = "";
// 			for(Outline n = node; n != null; n = parentMap.containsKey(n.getId()) ? parentMap.get(n.getId()) : null) {
// 				stringId += order.get(n.getId());
// 				if (parentMap.containsKey(n.getId())) stringId += ".";
// 			}
// 			node.setStringId(stringId);
// 		}
// 		return outlineRoot;
// 	}
// 	
// 	/**
// 	 * Get page text (usually known as text reflow in some apps). Better text reflow coming... eventually.
// 	 */
// 	synchronized public native String getText(int page);
	// #endif
	
	/**
	 * Free memory allocated in native code.
	 */
	synchronized private native void freeMemory();

	public void finalize() {
		try {
			super.finalize();
		} catch (Throwable e) {
		}
		this.freeMemory();
	}
}
