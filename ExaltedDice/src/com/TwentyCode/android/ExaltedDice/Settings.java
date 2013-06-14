/**
 * Settings.java
 * @date Feb 6, 2012
 * @author ricky barrette
 * @author Twenty Codes, LLC
 */
package com.TwentyCode.android.ExaltedDice;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.view.MenuItem;

/**
 * This activity will be used to allow the user to fine tune exalted dice
 * 
 * TODO
 *  + game specific settings?
 * @author ricky barrette
 */
public class Settings extends PreferenceActivity implements OnPreferenceClickListener {
	
	private static final CharSequence EMAIL = "email";
	
	public static final String KEY_ROLL_AGAIN = "roll_again";
	public static final String KEY_TENS_COUNT_TWICE = "tens_count_twice";
	public static final String KEY_COLOR = "color";
	public static final String KEY_ONES_SUBRACT = "ones_subtract";
	public static final String KEY_SUCCESS_AFTER = "success_after";
	public static final String KEY_EXALTED = "exalted";
//	public static final String KEY_ = "";
	

	/**
	 * generates the exception repost email intent
	 * @param report
	 * @return intent to start users email client
	 * @author ricky barrette
	 */
	private Intent generateEmailIntent() {
		/*
		 * get the build information, and build the string
		 */
		PackageManager pm = this.getPackageManager();
		PackageInfo pi;
		try {
			pi = pm.getPackageInfo(this.getPackageName(), 0);
		} catch (NameNotFoundException eNnf) {
			//doubt this will ever run since we want info about our own package
			pi = new PackageInfo();
			pi.versionName = "unknown";
			pi.versionCode = 1;
		}
		
		Intent intent = new Intent(Intent.ACTION_SEND);
		String theSubject = this.getString(R.string.app_name);
		String theBody = "\n\n\n"+ Build.FINGERPRINT +"\n"+ this.getString(R.string.app_name)+" "+pi.versionName+" build "+pi.versionCode;
		intent.putExtra(Intent.EXTRA_EMAIL,new String[] {this.getString(R.string.email)});
		intent.putExtra(Intent.EXTRA_TEXT, theBody);
		intent.putExtra(Intent.EXTRA_SUBJECT, theSubject);
		intent.setType("message/rfc822");
		return intent;
	}

    	/**
		 * (non-Javadoc)
		 * @see android.preference.PreferenceActivity#onCreate(android.os.Bundle)
		 */
		@SuppressLint("NewApi") @Override
		protected void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			//load preferences xml. this load relies on only wether the app is full or not. it will show the check license option if full and leave it out if lite
			addPreferencesFromResource(R.xml.settings);
			
			/*
			 * The following is for api 11 and up
			 */
			if(Integer.valueOf(android.os.Build.VERSION.SDK_INT) > 11){
				ActionBar actionBar = getActionBar();
				if(actionBar != null)
					actionBar.setDisplayHomeAsUpEnabled(true);
			}

			Intent i = this.getIntent();
			if(i != null){
				if(i.hasExtra(ExaltedDice.KEY_GAME_NAME))
					//set shared_prefs name
					getPreferenceManager().setSharedPreferencesName(i.getStringExtra(ExaltedDice.KEY_GAME_NAME));
				
				if(i.hasExtra(ExaltedDice.KEY_GAME_MODE))
					/*
			    	 * EXALTED
			    	 */
			    	if(i.getStringExtra(ExaltedDice.KEY_GAME_MODE).equals(getString(R.string.game_mode_exalted))){
			    		this.findPreference(KEY_ONES_SUBRACT).setEnabled(true);
			    		this.findPreference(KEY_TENS_COUNT_TWICE).setEnabled(true);
			    		this.findPreference(KEY_SUCCESS_AFTER).setEnabled(true);
			    	}
			}
			
			this.findPreference(EMAIL).setOnPreferenceClickListener(this);
		}
    	
    	/**
    	 * Called when a options item has been selected
    	 * (non-Javadoc)
    	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
    	 */
    	public boolean onOptionsItemSelected(MenuItem item) {
    		switch (item.getItemId()) {
    			case android.R.id.home:
    	            Intent intent = new Intent(this, ExaltedDice.class)
    	            	.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
    	            	.putExtras(getIntent().getExtras());
    	            startActivity(intent);
    	            return true;
    			default:
    				return super.onOptionsItemSelected(item);
    		}
    	}
    	
    	/**
    	 * called when the email preference button is clicked
    	 */
    	@Override
    	public boolean onPreferenceClick(Preference preference) {
    		this.startActivity(generateEmailIntent());
    		return false;
    	}
}