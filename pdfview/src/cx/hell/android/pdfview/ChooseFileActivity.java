package cx.hell.android.pdfview;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
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
	private final static String PREF_TAG = "ChooseFileActivity";
	private final static String PREF_HOME = "Home";
	private final static int HOME_POSITION = 0;
	private final static int RECENT_START = 1;
	
	private String currentPath;
	
	private TextView pathTextView = null;
	private ListView filesListView = null;
	private FileFilter fileFilter = null;
	private ArrayAdapter<String> fileListAdapter = null;
	private Recent recent = null;
	
	private MenuItem aboutMenuItem = null;
	private MenuItem setAsHomeMenuItem = null;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	
    	currentPath = getHome(); 

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
    }
    
    /**
     * Reset list view and list adapter to reflect change to currentPath.
     */
    private void update() {
    	this.pathTextView.setText(this.currentPath);
    	this.fileListAdapter.clear();
    	this.fileListAdapter.add("["+getResources().getString(R.string.go_home)+"]");
    	if (!this.currentPath.equals("/"))
    		this.fileListAdapter.add("..");
    	
    	File files[] = new File(this.currentPath).listFiles(this.fileFilter);
    	if (files != null) {
	    	try {
		    	Arrays.sort(files, new Comparator<File>() {
		    		public int compare(File f1, File f2) {
		    			if (f1 == null) throw new RuntimeException("f1 is null inside sort");
		    			if (f2 == null) throw new RuntimeException("f2 is null inside sort");
		    			try {
		    				return f1.getName().toLowerCase().compareTo(f2.getName().toLowerCase());
		    			} catch (NullPointerException e) {
		    				throw new RuntimeException("failed to compare " + f1 + " and " + f2, e);
		    			}
					}
		    	});
	    	} catch (NullPointerException e) {
	    		throw new RuntimeException("failed to sort file list " + files + " for path " + this.currentPath, e);
	    	}
	    	
	    	for(int i = 0; i < files.length; ++i) this.fileListAdapter.add(files[i].getName());
    	}
    	
    	if (isHome(currentPath)) {
    		recent = new Recent(this);
    		
        	for (int i = 0; i < recent.size(); i++) {
        		this.fileListAdapter.insert(""+(i+1)+": "+(new File(recent.get(i))).getName(), 
        				RECENT_START+i);
        	}
    	}
    	else {
    		recent = null;
    	}
    	
    	this.filesListView.setSelection(0);
    }
    
    public void pdfView(File f) {
		Log.i(TAG, "post intent to open file " + f);
		Intent intent = new Intent();
		intent.setDataAndType(Uri.fromFile(f), "application/pdf");
		intent.setClass(this, OpenFileActivity.class);
		intent.setAction("android.intent.action.VIEW");
		this.startActivity(intent);
    }
    
    private boolean isHome(String path) {
    	File pathFile = new File(path);
    	File homeFile = new File(getHome());
    	try {
			return pathFile.getCanonicalPath().equals(homeFile.getCanonicalPath());
		} catch (IOException e) {
			return false;
		}
    }
    
    private String getHome() {
    	String defaultHome = Environment.getExternalStorageDirectory().getAbsolutePath(); 
		String path = getSharedPreferences(PREF_TAG, 0).getString(PREF_HOME,
							defaultHome);
		if (path.length()>1 && path.endsWith("/")) {
			path = path.substring(0,path.length()-2);
		}

		File pathFile = new File(path);

		if (pathFile.exists() && pathFile.isDirectory())
			return path;
		else
			return defaultHome;
    }
    
    @SuppressWarnings("rawtypes")
	public void onItemClick(AdapterView parent, View v, int position, long id) {
    	File clickedFile;
    	
    	if (position == HOME_POSITION) {
    		clickedFile = new File(getHome());
    	}
    	else if (recent != null && position < RECENT_START + recent.size()) {
    		clickedFile = new File(recent.get(position-RECENT_START));
    	}
    	else {
    		String filename = (String) this.filesListView.getItemAtPosition(position);
    		clickedFile = null;
    		
    		if (filename.equals("..")) {
    			clickedFile = (new File(this.currentPath)).getParentFile();
    		}
    		else {
    			clickedFile = new File(this.currentPath, filename);
    		}
    	}
    	
    	if (!clickedFile.exists())
    		return;
    	
    	if (clickedFile.isDirectory()) {
    		Log.d(TAG, "change dir to " + clickedFile);
    		this.currentPath = clickedFile.getAbsolutePath();
    		this.update();
    	} else {
    		pdfView(clickedFile);
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
    	else if (menuItem == this.setAsHomeMenuItem) {
    		SharedPreferences.Editor edit = getSharedPreferences(PREF_TAG, 0).edit();
    		edit.putString(PREF_HOME, currentPath);
    		edit.commit();
    		return true;
    	}
    	return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	this.setAsHomeMenuItem = menu.add(R.string.set_as_home);
    	this.aboutMenuItem = menu.add("About");
    	return true;
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	this.update();
    }
}
