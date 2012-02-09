package com.TwentyCode.android.ExaltedDice;

import java.util.Random;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.DatabaseUtils;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.TwentyCodes.android.exception.ExceptionHandler;

public class ExaltedDice extends Activity implements OnClickListener, OnItemClickListener, DatabaseListener {

	private static final int MENU_QUIT = Menu.FIRST;
	private static final int MENU_CLEAR = Menu.FIRST + 1;
	private static final String TAG = "ExaltedDice";
	public static final String KEY_GAME_NAME = "game_name";
	public static final String KEY_GAME_ID = "game_id";
	private static final int DELETE = 0;
	private static final int SETTINGS = Menu.FIRST + 2;

	private String[] mDiceValues;
	private ListView mListView;
	private NumberPicker mNumberPicker;
	private NumberPicker mDPicker;
	private Database mDb;
	private String mGameName;
	private long mGameId;
	private RollHistoryDatabaseAdapter mListAdapter;
	private SharedPreferences mSettings;
	private String[] mModValues;
	private NumberPicker mModPicker;
	private ProgressBar mRollProgress;
	private View mRollButton;
	
	/**
	 * Applies the presets from the provided roll
	 * @param id of the roll
	 * @author ricky barrette
	 */
	private void applyRollPresets(long id) {
		ContentValues roll = mDb.getGameHistoryInfo(mGameName, (int) (id));
		try{
			mNumberPicker.setValue(roll.getAsInteger(Database.KEY_NUMBER));
			mDPicker.setValue(parseD(roll.getAsString(Database.KEY_D_TYPE)));
			mModPicker.setValue(parseMod(roll.getAsString(Database.KEY_MOD).replace("'", "")));
		} catch(NullPointerException e){
			mModPicker.setValue(parseMod("+0"));
		}
	}
	
	/**
	 * also implemented OnClickListener
	 * 
	 * @author ricky barrette 3-27-2010
	 * @author - WWPowers 3-26-2010
	 */
	@Override
	public void onClick(View v){
		switch(v.getId()){
			case R.id.roll_button:
				rollDice();
				break;
		}
	}

	/**
	 * (non-Javadoc)
	 * @see android.app.Activity#onContextItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		switch(item.getItemId()){
			case DELETE:
				mDb.deleteRoll(mGameId, info.id+1);
				break;
		}
		return super.onContextItemSelected(item);
	}

	/**
	 * Called when the activity is first created. starts gui and sets up buttons
	 * 
	 * @author ricky barrette 3-27-2010
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(this));
		super.onCreate(savedInstanceState);
		Log.i(TAG, "onCreate()");
		setContentView(R.layout.main);
		
		/*
		 * The following is for api 11 and up
		 */
		if(Integer.valueOf(android.os.Build.VERSION.SDK) > 11){
			ActionBar actionBar = getActionBar();
			actionBar.setDisplayHomeAsUpEnabled(true);
		}
		
		Intent i = this.getIntent();
		if(i != null)
			if(i.hasExtra(KEY_GAME_NAME)){
				mGameName = i.getStringExtra(KEY_GAME_NAME);
				mGameId = i.getLongExtra(KEY_GAME_ID, -1);
				this.setTitle(mGameName);
			}
		
		mSettings = getSharedPreferences(Settings.SETTINGS, Context.MODE_WORLD_WRITEABLE);
		
		mDiceValues = getResources().getStringArray(R.array.dice_types);
		mModValues = getResources().getStringArray(R.array.mods);
		
		mListView = (ListView) findViewById(R.id.list);
		mListView.setOnItemClickListener(this);
		mListView.setStackFromBottom(true);

		mDPicker = (NumberPicker) findViewById(R.id.d_Picker);
		mDPicker.setMinValue(0);
		mDPicker.setMaxValue(mDiceValues.length -1);
		mDPicker.setDisplayedValues(mDiceValues);
		mDPicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
		
		mNumberPicker = (NumberPicker) findViewById(R.id.number_Picker);
		mNumberPicker.setMaxValue(999);
		mNumberPicker.setMinValue(1);
		mNumberPicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
		
		mModPicker = (NumberPicker) findViewById(R.id.mod_Picker);
		mModPicker.setMinValue(0);
		mModPicker.setMaxValue(mModValues.length -1);
		mModPicker.setDisplayedValues(mModValues);
		mModPicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
		
		mRollProgress = (ProgressBar) findViewById(R.id.roll_progress);
		
		mRollButton = findViewById(R.id.roll_button);
		mRollButton.setOnClickListener(this);
	}

	/**
	 * (non-Javadoc)
	 * @see android.app.Activity#onCreateContextMenu(android.view.ContextMenu, android.view.View, android.view.ContextMenu.ContextMenuInfo)
	 */
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		menu.add(0, DELETE, Menu.FIRST, R.string.delete);
		super.onCreateContextMenu(menu, v, menuInfo);
	}

	/**
	 * creates a menu with a quit option
	 * 
	 * @author WWPowers 3-27-2010
	 */
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(1, MENU_CLEAR, 0, R.string.clear_history).setIcon(android.R.drawable.ic_menu_close_clear_cancel);
		menu.add(1, SETTINGS, 0, R.string.settings).setIcon(android.R.drawable.ic_menu_preferences);;
		menu.add(1, MENU_QUIT, 0, R.string.quit).setIcon(android.R.drawable.ic_menu_revert);
		return true;
	}

    /**
	 * rolls same amount of dice as previous roll
	 * @author ricky barrette
	 */
	@Override
	public void onItemClick(AdapterView<?> arg0, View v, int position, long id) {
		applyRollPresets(id + 1);
		
		if(mSettings.getBoolean(Settings.KEY_ROLL_AGAIN, true))
			rollDice();
	}

	/**
	 * handles menu selection
	 * 
	 * @author WWPowers 3-27-2010
	 * @author ricky barrette 3-27-2010
	 */
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
	            Intent intent = new Intent(this, GameListActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	            startActivity(intent);
	            return true;
			case MENU_QUIT:
				quitDialog();
				return true;
			case MENU_CLEAR:
				mDb.clearHistory(mGameId);
				return true;
			case SETTINGS:
				startActivity(new Intent(this, Settings.class).putExtras(getIntent().getExtras()));
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

    /**
	 * (non-Javadoc)
	 * @see android.app.Activity#onPause()
	 */
	@Override
	protected void onStop() {
		mDb.close();
		super.onStop();
	}
    /**
	 * resorts application state after rotation
	 * @author ricky barrette
	 */
	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
	  super.onRestoreInstanceState(savedInstanceState);
	  mDPicker.setValue(savedInstanceState.getInt("d"));
	  mNumberPicker.setValue(savedInstanceState.getInt("number"));
	  mModPicker.setValue(savedInstanceState.getInt("mod"));
	}

    /**
	 * (non-Javadoc)
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		refresh();
		applyRollPresets(mDb.getGameRollCount(mGameId));
		
		if(mSettings.getBoolean(Settings.KEY_ROLL_MOD, true)){
			mModPicker.setVisibility(View.VISIBLE);
		} else {
			mModPicker.setVisibility(View.GONE);
			mModPicker.setValue(parseMod("+0"));
		}
		super.onResume();
	}

    /**
	 * (non-Javadoc)
	 * @see android.app.Activity#onStart()
	 */
	@Override
	protected void onStart() {
		mDb = new Database(this, this);
		mListAdapter = new RollHistoryDatabaseAdapter(mGameId, mDb, this);
		mListView.setAdapter(mListAdapter);
		super.onStart();
	}

	/**
	 * saves application state before rotation
	 * @author ricky barrette
	 */
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		savedInstanceState.putInt("d", mDPicker.getValue());
		savedInstanceState.putInt("number", mNumberPicker.getValue());
		savedInstanceState.putInt("mod", mModPicker.getValue());
		super.onSaveInstanceState(savedInstanceState);
	}

	/**
	 * Parses the string d type to the appropriate value
	 * @param d type
	 * @return value for d picker
	 * @author ricky barrette
	 */
    private int parseD(String d) {
		for(int i = 0; i < mDiceValues.length; i++)
			if (mDiceValues[i].equalsIgnoreCase(d))
				return i;
		return 0;
	}
    
    /**
	 * Parses the string mod to the appropriate value
	 * @param mod 
	 * @return value for d picker
	 * @author ricky barrette
	 */
    private int parseMod(String mod) {
		for(int i = 0; i < mModValues.length; i++)
			if (mModValues[i].equalsIgnoreCase(mod))
				return i;
		return 0;
	}

	/**
	 * displays a quit dialog
	 * 
	 * @author ricky barrette 3-28-2010
	 * @author WWPowers 3-27-2010
	 */
	public void quitDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.do_you_want_to_quit).setCancelable(
				false).setPositiveButton(android.R.string.yes,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						ExaltedDice.this.finish();
					}
				}).setNegativeButton(android.R.string.no,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});
		builder.show();
	}
	
	/**
	 * Refreshes the list view
	 * @author ricky barrette
	 */
	public void refresh(){
		mListAdapter.notifyDataSetChanged();
		mListView.setSelection(mDb.getGameRollCount(mGameId));
	}

	/**
	 * returns a custom string containing dice rolls and number of successes
	 * 
	 * @param int times
	 * @return String resultsString
	 * @author ricky barrette
	 */
	public ContentValues results(int times) {
		Log.i(TAG, "results()");
		ContentValues rolled = new ContentValues();
		StringBuffer resultsString = new StringBuffer();
		StringBuffer rolls = new StringBuffer();
		long total = 0;
		
		int[] roll = rollGen(times);
		
		for (int item : roll) {
			rolls.append(item + ", ");
			total = total + item;
		}

		resultsString.append(getString(R.string.total)+ total);
		
		if(mSettings.getBoolean(Settings.KEY_ROLL_MOD, true))
			resultsString.append(getString(R.string.total_plus_mod)+ (total + Integer.parseInt(mModValues[mModPicker.getValue()].replace("+", ""))));
		
		if(mSettings.getBoolean(Settings.KEY_CALC_SUCCESSES, true))
			resultsString.append(getString(R.string.sucesses)+ successes(roll));
			
		rolled.put(Database.KEY_LOG, resultsString.toString());
		
		rolled.put(Database.KEY_ROLLED, getString(R.string.rolls)+ rolls.toString());
		return rolled;
	}

	/**
	 * Performs a dice roll and logs it's information in the database
	 * 
	 * @author ricky barrette
	 */
	public void rollDice() {
		mListAdapter.notifyDataSetInvalidated();
		mRollButton.setEnabled(false);
		mRollProgress.setVisibility(View.VISIBLE);
		
//		new Thread( new Runnable() {
//			@Override
//			public void run(){
				// vibrate for 50 milliseconds
				vibrate(50);
				
				int rollId = mDb.getGameRollCount(mGameId) +1;
				
				ContentValues roll = new ContentValues();
				roll.put(Database.KEY_D_TYPE, mDiceValues[mDPicker.getValue()]);
				roll.put(Database.KEY_NUMBER, mNumberPicker.getValue());
				roll.putAll(results(mNumberPicker.getValue()));
				
				roll.put(Database.KEY_MOD,  DatabaseUtils.sqlEscapeString(mModValues[mModPicker.getValue()]));
				
				mDb.updateGame(mGameId, mGameName, roll, rollId);
//			}
//		}).start();
	}

	/**
	 * generates an array containing dice rolls
	 * @param int times
	 * @return int[] roll
	 * @author ricky barrette
	 */
	public int[] rollGen(int times) {
		Log.i(TAG, "rollGen()" + times);
		int[] roll = new int[times];
		Random random = new Random();
		for (int i = 0; i < times; i++) {
			roll[i] = random.nextInt(Integer.parseInt(mDiceValues[mDPicker.getValue()].substring(1))) + 1;
		}
		return roll;
	}

	/**
	 * counts each dice roll that is greater than or equal to 7 as a success. 10
	 * gets another success (for a total of 2)
	 * 
	 * @param int[] roll
	 * @return int successes
	 * @author ricky barrette
	 */
	public int successes(int[] roll) {
		Log.i(TAG, "successes()");
		int intSuccesses = 0;
		for (int i = 0; i < roll.length; i++) {
			if (roll[i] >= 7)
				intSuccesses++;
			if(mSettings.getBoolean(Settings.KEY_TENS_COUNT_TWICE, true))
				if (roll[i] == 10)
					intSuccesses++;
		}
		return intSuccesses;
	}

	/**
	 * displays toast message with a long duration
	 * 
	 * @param msg
	 * @author ricky barrette 3-26-2010
	 * @author WWPowers 3-26-2010
	 */
	public void toastLong(CharSequence msg) {
		Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
	}

	/**
	 * starts Vibrator service and then vibrates for x milliseconds
	 * @param Long
	 *            milliseconds
	 * @author ricky barrette
	 */
	public void vibrate(long milliseconds) {
		Log.i(TAG, "vibrate() for " + milliseconds);
		Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		vib.vibrate(milliseconds);
	}

	@Override
	public void onDatabaseUpgradeComplete() {
		// do nothing
		
	}

	@Override
	public void onDeletionComplete() {
		this.runOnUiThread(new Runnable(){
			@Override
			public void run(){
				refresh();
			}
		});
	}

	@Override
	public void onRestoreComplete() {
		// do nothing
	}

	@Override
	public void onDatabaseUpgrade() {
		//do nothing		
	}

	@Override
	public void onDatabaseInsertComplete() {
		this.runOnUiThread(new Runnable(){
			@Override
			public void run(){
				mRollProgress.setVisibility(View.GONE);
				mRollButton.setEnabled(true);
				refresh();
			}
		});
	}
}