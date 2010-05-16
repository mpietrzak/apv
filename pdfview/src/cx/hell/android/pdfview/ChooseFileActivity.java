package cx.hell.android.pdfview;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

/**
 * Minimalistic file browser.
 */
public class ChooseFileActivity extends Activity implements OnItemClickListener {
	
	/**
	 * Logging tag.
	 */
	private final static String TAG = "cx.hell.android.pdfview";
	
	private String currentPath = Environment.getExternalStorageDirectory().getAbsolutePath();
	
	private TextView pathTextView = null;
	private ListView filesListView = null;
	private FileFilter fileFilter = null;
	private ArrayAdapter<String> fileListAdapter = null;
	
	private MenuItem aboutMenuItem = null;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	
    	this.fileFilter = new FileFilter() {
    		public boolean accept(File f) {
    			return (f.isDirectory() || f.getName().toLowerCase().endsWith(".pdf"));
    		}
    	};
    	
    	this.setContentView(R.layout.filechooser);

    	this.pathTextView = (TextView) this.findViewById(R.id.path);
    	this.filesListView = (ListView) this.findViewById(R.id.files);
    	this.fileListAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
    	this.filesListView.setAdapter(this.fileListAdapter);
    	this.filesListView.setOnItemClickListener(this);
    	this.update();
    }
    
    /**
     * Reset list view and list adapter to reflect change to currentPath.
     */
    private void update() {
    	this.pathTextView.setText(this.currentPath);
    	File files[] = new File(this.currentPath).listFiles(this.fileFilter);
    	Arrays.sort(files, new Comparator<File>() {
    		public int compare(File f1, File f2) {
    			return f1.getName().toLowerCase().compareTo(f2.getName().toLowerCase());
			}
    	});
    	
    	this.fileListAdapter.clear();
    	this.fileListAdapter.add("..");
    	for(int i = 0; i < files.length; ++i) this.fileListAdapter.add(files[i].getName());
    	this.filesListView.setSelection(0);
    }
    
    @SuppressWarnings("unchecked")
	public void onItemClick(AdapterView parent, View v, int position, long id) {
    	String filename = (String) this.filesListView.getItemAtPosition(position);
    	File clickedFile = null;
    	try {
    		clickedFile = new File(this.currentPath, filename).getCanonicalFile();
    	} catch (IOException e) {
    		throw new RuntimeException(e);
    	}
    	if (clickedFile.isDirectory()) {
    		Log.d(TAG, "change dir to " + clickedFile);
    		this.currentPath = clickedFile.getAbsolutePath();
    		this.update();
    	} else {
    		Log.i(TAG, "post intent to open file " + clickedFile);
    		Intent intent = new Intent();
    		intent.setDataAndType(Uri.fromFile(clickedFile), "application/pdf");
    		intent.setClass(this, OpenFileActivity.class);
    		intent.setAction("android.intent.action.VIEW");
    		this.startActivity(intent);
    	}
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
    	if (menuItem == this.aboutMenuItem) {
			Intent intent = new Intent();
			intent.setClass(this, AboutPDFViewActivity.class);
			this.startActivity(intent);
    		return true;
    	}
    	return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	this.aboutMenuItem = menu.add("About");
    	return true;
    }
}