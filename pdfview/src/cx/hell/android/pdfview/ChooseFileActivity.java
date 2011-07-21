package cx.hell.android.pdfview;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

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
	private ArrayList<String> fileList = null;
	private Recent recent = null;
	
	private MenuItem aboutMenuItem = null;
	private MenuItem setAsHomeMenuItem = null;
	private MenuItem deleteMenuItem = null;
	private MenuItem optionsMenuItem = null;
	
	private Boolean dirsFirst = false;
	private Boolean showExtension = false;

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
    	final Activity activity = this;
    	this.fileList = new ArrayList<String>();
    	this.fileListAdapter = new ArrayAdapter<String>(this, 
				R.layout.onelinewithicon, fileList) {
			public View getView(int position, View convertView, ViewGroup parent) {
				View v;				
				
				if (convertView == null) {
	                v = View.inflate(activity, R.layout.onelinewithicon, null);
	            }
				else {
					v = convertView;
				}
				
				String label = fileList.get(position);
				
				if (!showExtension && label.length() > 4 &&
						isFilePosition(position) && 
						label.substring(label.length()-4, label.length()).equalsIgnoreCase(".pdf")) {
					label = label.substring(0, label.length()-4);
				}
				
				v.findViewById(R.id.home).setVisibility(
						position==HOME_POSITION?View.VISIBLE:View.GONE );
				v.findViewById(R.id.upfolder).setVisibility(
						fileList.get(position).equals("..")?View.VISIBLE:View.GONE );
				v.findViewById(R.id.folder).setVisibility(
						(isDirPosition(position) && !
								fileList.get(position).equals(".."))?View.VISIBLE:View.GONE );
				v.findViewById(R.id.recent1).setVisibility(
						(isRecentPosition(position)&&position==RECENT_START+0)
						?View.VISIBLE:View.GONE );
				v.findViewById(R.id.recent2).setVisibility(
						(isRecentPosition(position)&&position==RECENT_START+1)
						?View.VISIBLE:View.GONE );
				v.findViewById(R.id.recent3).setVisibility(
						(isRecentPosition(position)&&position==RECENT_START+2)
						?View.VISIBLE:View.GONE );
				v.findViewById(R.id.recent4).setVisibility(
						(isRecentPosition(position)&&position==RECENT_START+3)
						?View.VISIBLE:View.GONE );
				v.findViewById(R.id.recent5).setVisibility(
						(isRecentPosition(position)&&position==RECENT_START+4)
						?View.VISIBLE:View.GONE ); 
			
				((TextView)v.findViewById(R.id.text)).setText(label);

				return v;
			}				
    	};
       	this.filesListView.setAdapter(this.fileListAdapter);
    	this.filesListView.setOnItemClickListener(this);
    	registerForContextMenu(this.filesListView);
    }
    
    /**
     * Reset list view and list adapter to reflect change to currentPath.
     */
    private void update() {
    	this.pathTextView.setText(this.currentPath);
    	this.fileListAdapter.clear();
    	this.fileListAdapter.add(getResources().getString(R.string.go_home));
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
		    				if (dirsFirst && f1.isDirectory() != f2.isDirectory()) {
		    					if (f1.isDirectory())
		    						return -1;
		    					else
		    						return 1;
		    				}
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
        		this.fileListAdapter.insert((new File(recent.get(i))).getName(), 
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
    
    private boolean isRegularPosition(int position) {
    	if (recent == null)
    		return RECENT_START <= position;
    	else
    		return RECENT_START+recent.size() <= position;
    }
    
    private boolean isFilePosition(int position) {
    	return isRecentPosition(position) ||
    			(isRegularPosition(position) && ! isDirPosition(position));     	
    }
    
    private boolean isDirPosition(int position) {
    	return isRegularPosition(position) &&
    			(new File(currentPath, fileList.get(position))).isDirectory();
    }
    
    private boolean isRecentPosition(int position) {
    	return recent != null && RECENT_START <= position &&
    			position < RECENT_START+recent.size();
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
    	else if (isRecentPosition(position)) {
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
    	else if (menuItem == this.optionsMenuItem){
    		startActivity(new Intent(this, Options.class));
    	}
    	return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	this.setAsHomeMenuItem = menu.add(R.string.set_as_home);
    	this.optionsMenuItem = menu.add(R.string.options);
    	this.aboutMenuItem = menu.add("About");
    	return true;
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	
		SharedPreferences options = PreferenceManager.getDefaultSharedPreferences(this);		
		dirsFirst = options.getBoolean(Options.PREF_DIRS_FIRST, false);
		showExtension = options.getBoolean(Options.PREF_SHOW_EXTENSION, false);
    	
    	this.update();
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
    	int position =  
    		((AdapterView.AdapterContextMenuInfo)item.getMenuInfo()).position;
    	if (item == deleteMenuItem) {
    		if (isRecentPosition(position)) {
    			recent.remove(position - RECENT_START);
    			recent.commit();
    			update();
    		}
    		else if (position != HOME_POSITION) {
    			File clickedFile = new File(this.currentPath, 
    					(String) this.filesListView.getItemAtPosition(position));
    			if (! clickedFile.isDirectory()) {
    				clickedFile.delete();
    				update();
    			}
    		}
    		
    		return true;
    	}
    	return false;
    }
    	
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
    	super.onCreateContextMenu(menu, v, menuInfo);
		deleteMenuItem = menu.add("Delete");
    }
}
