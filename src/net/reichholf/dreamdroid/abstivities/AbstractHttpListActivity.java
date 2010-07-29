/* © 2010 Stephan Reichholf <stephan at reichholf dot net>
 * 
 * Licensed under the Create-Commons Attribution-Noncommercial-Share Alike 3.0 Unported
 * http://creativecommons.org/licenses/by-nc-sa/3.0/
 */

package net.reichholf.dreamdroid.abstivities;

import java.util.ArrayList;
import java.util.HashMap;

import net.reichholf.dreamdroid.CustomExceptionHandler;
import net.reichholf.dreamdroid.helpers.ExtendedHashMap;
import net.reichholf.dreamdroid.helpers.SimpleHttpClient;
import android.app.ListActivity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.widget.SimpleAdapter;
import android.widget.Toast;

/**
 * @author sreichholf
 * 
 */
public abstract class AbstractHttpListActivity extends ListActivity {
	// public static ArrayList<ExtendedHashMap> DATA = new
	// ArrayList<ExtendedHashMap>();

	protected ArrayList<ExtendedHashMap> mList = new ArrayList<ExtendedHashMap>();
	protected SimpleAdapter mAdapter;
	protected ExtendedHashMap mData;
	protected Bundle mExtras;
	protected SimpleHttpClient mShc;
	protected final String sData = "data";

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		CustomExceptionHandler.register(this);
		
		mExtras = this.getIntent().getExtras();

		if (mExtras != null) {
			HashMap<String, Object> map = (HashMap<String, Object>) mExtras.getSerializable("data");
			if (map != null) {
				mData = new ExtendedHashMap();
				mData.putAll(map);
			}
		}

		if (savedInstanceState != null) {
			Object retained = getLastNonConfigurationInstance();
			if (retained instanceof HashMap) {
				mShc = (SimpleHttpClient) ((HashMap<String, Object>) retained).get("shc");
			}
		} else {
			setClient();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onKeyDown(int, android.view.KeyEvent)
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		return super.onKeyDown(keyCode, event);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onRetainNonConfigurationInstance()
	 */
	@Override
	public Object onRetainNonConfigurationInstance() {
		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put("shc", mShc);

		return map;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
	 */
	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putSerializable("list", mList);
		outState.putSerializable("data", mData);

		super.onSaveInstanceState(outState);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.ListActivity#onRestoreInstanceState(android.os.Bundle)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);

		mList = (ArrayList<ExtendedHashMap>) savedInstanceState.getSerializable("list");

		HashMap map = (HashMap) savedInstanceState.getSerializable("data");
		mData = new ExtendedHashMap();
		if (map != null) {
			mData.putAll(map);
		}
	}

	/**
	 * 
	 */
	private void setClient() {
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		mShc = SimpleHttpClient.getInstance(sp);
	}

	/**
	 * @param key
	 * @return
	 */
	public String getDataForKey(String key) {
		if (mData != null) {
			return (String) mData.get(key);
		}

		return null;
	}

	/**
	 * @param key
	 * @param dfault
	 * @return
	 */
	public String getDataForKey(String key, String dfault) {
		if (mData != null) {
			String str = (String) mData.get(key);
			if (str != null) {
				return str;
			}
		}
		return dfault;
	}

	/**
	 * @param key
	 * @param dfault
	 * @return
	 */
	public boolean getDataForKey(String key, boolean dfault) {
		if (mData != null) {
			Boolean b = (Boolean) mData.get(key);
			if (b != null) {
				return b.booleanValue();
			}
		}

		return dfault;
	}

	/**
	 * @param toastText
	 */
	protected void showToast(String toastText) {
		Toast toast = Toast.makeText(this, toastText, Toast.LENGTH_LONG);
		toast.show();
	}

	/**
	 * @param toastText
	 */
	protected void showToast(CharSequence toastText) {
		Toast toast = Toast.makeText(this, toastText, Toast.LENGTH_LONG);
		toast.show();
	}
}