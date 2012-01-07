package cx.hell.android.lib.view;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import cx.hell.android.pdfview.R;

/**
 * Simple tree view used by APV for Table of Contents.
 * 
 * TODO: move data handling (getItemAtPosition) from TreeView to TreeAdapter,
 * 	     as it should be; TreeView should query TreeAdapter, not other way around :/
 *       however, it would be cool to have self contained simple-api class for all of tree related stuff
 */
public class TreeView extends ListView {
	
	public final static String TAG = "cx.hell.android.lib.view";
	
	/**
	 * Tree node model. Contains links to first child and to next element.
	 */
	public interface TreeNode {
		
		/**
		 * Get universal id in form "0.2.1".
		 */
		public String getStringId();
		/**
		 * Get numeric id.
		 */
		public long getId();
		/**
		 * Get next element.
		 */
		public TreeNode getNext();
		/**
		 * Get down element.
		 */
		public TreeNode getDown();
		/**
		 * Return true if this node has children.
		 */
		public boolean hasChildren();
		
		/**
		 * Get list of children or null if there are no children.
		 */
		public List<TreeNode> getChildren();

		/**
		 * Get text of given node.
		 * @return text that shows up on list
		 */
		public String getText();
		
		/**
		 * Return 0-based level (depth) of this tree node.
		 * Top level elements have level 0, children of top level elements have level 1 etc.
		 */
		public int getLevel();
	}
	
	private final static class TreeAdapter extends BaseAdapter {
		
		/**
		 * Parent TreeView.
		 * Each ListAdapter is bound to exactly one TreeView,
		 * since TreeView is general purpose class that also holds tree data and tree state.
		 */
		private TreeView parent = null;
		
		public TreeAdapter(TreeView parent) {
			this.parent = parent;
		}

		public int getCount() {
			return this.parent.getVisibleCount();
		}

		public Object getItem(int position) {
			TreeNode node = this.parent.getTreeNodeAtPosition(position);
			Log.d(TAG, "tree node at position " + position + ": " + node);
			return node;
		}

		public long getItemId(int position) {
			TreeNode node = this.parent.getTreeNodeAtPosition(position);
			Log.d(TAG, "tree node at position " + position + ": " + node);
			return node.getId();
		}

		public int getItemViewType(int position) {
			return 0;
		}

		/**
		 * Get view for list item for given position.
		 */
		public View getView(final int position, View convertView, ViewGroup parent) {
			final TreeNode node = this.parent.getTreeNodeAtPosition(position);
			if (node == null) throw new RuntimeException("no node at position " + position);
			final LinearLayout l = new LinearLayout(parent.getContext());
			l.setOrientation(LinearLayout.HORIZONTAL);
			Button b = new Button(parent.getContext());
			if (node.hasChildren()) {
				if (this.parent.isOpen(node)) {
					b.setBackgroundResource(R.drawable.minus);
					//b.setText("-");
					b.setOnClickListener(new OnClickListener() {
						public void onClick(View v) {
							Log.d(TAG, "click on " + position + ": " + node);
							TreeAdapter.this.parent.close(node);
							TreeAdapter.this.notifyDataSetChanged();
							//TreeAdapter.this.parent.invalidate();
						}
					});
				} else {
					b.setBackgroundResource(R.drawable.plus);
					//b.setText("+");
					b.setOnClickListener(new OnClickListener() {
						public void onClick(View v) {
							Log.d(TAG, "click on " + position + ": " + node);
							TreeAdapter.this.parent.open(node);
							TreeAdapter.this.notifyDataSetChanged();
							//TreeAdapter.this.parent.invalidate();
						}
					});
				}
			} else {
				b.setBackgroundColor(Color.TRANSPARENT);
				b.setEnabled(false);
			}
			l.addView(b);
			TextView tv = new TextView(parent.getContext());
			tv.setClickable(true);
			tv.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					TreeAdapter.this.parent.performItemClick(l, position, node.getId());
				}
			});
			tv.setText(node.getText());
			l.addView(tv);
			return l;
		}

		public int getViewTypeCount() {
			return 1;
		}

		public boolean hasStableIds() {
			return true;
		}

//		public boolean isEmpty() {
//			return false;
//		}

//		public void registerDataSetObserver(DataSetObserver observer) {
//			Log.d(TAG, "registerDataSetObserver(" + observer + ")");
//			super.registerDataSetObserver(observer);
//		}
//
//		public void unregisterDataSetObserver(DataSetObserver observer) {
//			Log.d(TAG, "unregisterDataSetObserver(" + observer + ")");
//			super.unregisterDataSetObserver(observer);
//		}

//		public boolean areAllItemsEnabled() {
//			return true;
//		}

//		public boolean isEnabled(int position) {
//			return true;
//		}
	};
	
	/**
	 * Tree root.
	 */
	private TreeNode root = null;
	
	/**
	 * State: either open or closed.
	 * If not found in map, then it's closed.
	 * Root is always open, so it's not stored in this map.
	 */
	private Map<TreeNode, Boolean> state = null;
	
	/**
	 * Construct this tree view.
	 */
	public TreeView(Context context) {
		super(context);
		this.setClickable(true);
		
	}

	/**
	 * Set contents.
	 */
	public synchronized void setTree(TreeNode root) {
		if (root == null) throw new IllegalArgumentException("tree root can not be null");
		
		this.root = root;
		this.state = new HashMap<TreeNode, Boolean>();
		TreeAdapter adapter = new TreeAdapter(this);
		this.setAdapter(adapter);
	}
	
	/**
	 * Check if given node is open.
	 * Root node is always open.
	 * Node state is checked in this.state.
	 * If node state is not found in this.state, then it's assumed given node is closed.
	 * @return true if given node is open, false otherwise
	 */
	public synchronized boolean isOpen(TreeNode node) {
		if (node == null) {
			return true;
		} else if (this.state.containsKey(node)) {
			return this.state.get(node);
		} else {
			return false;
		}
	}
	
	public synchronized void open(TreeNode node) {
		this.state.put(node, true);
	}
	
	public synchronized void close(TreeNode node) {
		this.state.remove(node);
	}
	
	/**
	 * Count visible tree elements.
	 */
	public synchronized int getVisibleCount() {
		Stack<TreeNode> stack = new Stack<TreeNode>();
		stack.push(this.root);
		int count = 0;
		while(!stack.empty()) {
			count += 1;
			TreeNode node = stack.pop();
			if (this.isOpen(node) && node.hasChildren()) {
				/* node is open - also count children */
				stack.push(node.getDown());
			}
			/* now count other elements at this level */
			if (node.getNext() != null) stack.push(node.getNext());
		}
		return count;
	}
	
	/**
	 * Get text for position taking account current state.
	 * Iterates over tree taking state into account until position-th element is found.
	 * @param position 0-based position in list
	 * @return text of currently visible item at given position
	 */
	public synchronized TreeNode getTreeNodeAtPosition(int position) {
		Stack<TreeNode> stack = new Stack<TreeNode>();
		stack.push(this.root);
		int i = 0;
		TreeNode found = null;
		while(!stack.empty() && found == null) {
			TreeNode node = stack.pop();
			if (i == position) {
				found = node;
				break;
			}
			if (node.getNext() != null) stack.push(node.getNext());
			if (node.getDown() != null && this.isOpen(node)) {
				stack.push(node.getDown());
			}
			i++;
		}
		return found;
	}
}
