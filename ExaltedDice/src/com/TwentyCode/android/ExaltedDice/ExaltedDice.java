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
import android.view.MenuInflater;
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

	public static final String KEY_GAME_MODE = "game_mode";
	public static final String KEY_GAME_NAME = "game_name";
	public static final String KEY_GAME_ID = "game_id";

	private static final String TAG = "ExaltedDice";
	private static final int DELETE = 0;

	private String[] mDiceValues;
	private ListView mListView;
	private NumberPicker mNumberPicker;
	private NumberPicker mDPicker;
	private NumberPicker mModPicker;
	private Database mDb;
	private String mGameName;
	private long mGameId;
	private RollHistoryDatabaseAdapter mListAdapter;
	private SharedPreferences mSettings;
	private String[] mModValues;
	private ProgressBar mRollProgress;
	private View mRollButton;
	private boolean isRolling = false;
	private com.TwentyCode.android.ExaltedDice.NumberPicker mCompatDPicker;
	private com.TwentyCode.android.ExaltedDice.NumberPicker mCompatNumberPicker;
	private com.TwentyCode.android.ExaltedDice.NumberPicker mCompatModPicker;
	private boolean isCompat = false;
	private boolean isSuccessesEanbled = false;
	private boolean isRollModEnabled = true;
	private boolean isCalculatingTotal = true;
	
	/**
	 * Applies the presets from the provided roll
	 * @param id of the roll
	 * @author ricky barrette
	 */
	private void applyRollPresets(long id) {
		ContentValues roll = mDb.getGameHistoryInfo(mGameName, (int) (id));
		try{
			if(isCompat){
				mCompatNumberPicker.setValue(roll.getAsInteger(Database.KEY_NUMBER));
				mCompatDPicker.setValue(parseD(roll.getAsString(Database.KEY_D_TYPE)));
				mCompatModPicker.setValue(parseMod(roll.getAsString(Database.KEY_MOD).replace("'", "")));
			} else {
				mNumberPicker.setValue(roll.getAsInteger(Database.KEY_NUMBER));
				mDPicker.setValue(parseD(roll.getAsString(Database.KEY_D_TYPE)));
				mModPicker.setValue(parseMod(roll.getAsString(Database.KEY_MOD).replace("'", "")));
			}
		} catch(NullPointerException e){
			if(isCompat)
				mCompatModPicker.setValue(parseMod("+0"));
			else
				mModPicker.setValue(parseMod("+0"));
		}
	}
	
	/**
	 * Initializes compat pickers for api < 11 
	 * @author ricky barrette
	 */
	private void initCompatPickers() {
		isCompat  = true;
		
		mCompatDPicker = (com.TwentyCode.android.ExaltedDice.NumberPicker) findViewById(R.id.d_Picker);
		mCompatDPicker.setDisplayedValues(mDiceValues);
		mCompatDPicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
		
		mCompatNumberPicker = (com.TwentyCode.android.ExaltedDice.NumberPicker) findViewById(R.id.number_Picker);
		mCompatNumberPicker.setRange(1, 999);
		
		mCompatModPicker = (com.TwentyCode.android.ExaltedDice.NumberPicker) findViewById(R.id.mod_Picker);
		mCompatModPicker.setDisplayedValues(mModValues);
		mCompatModPicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
	}

	/**
	 * Initializes native number pickers api > 11
	 * @author ricky barrette
	 */
	private void initPickers() {
		mDPicker = (NumberPicker) findViewById(R.id.d_Picker);
		mDPicker.setMinValue(0);
		mDPicker.setMaxValue(mDiceValues.length -1);
		mDPicker.setDisplayedValues(mDiceValues);
		mDPicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
		
		mNumberPicker = (NumberPicker) findViewById(R.id.number_Picker);
		mNumberPicker.setMaxValue(999);
		mNumberPicker.setMinValue(1);
		
		mModPicker = (NumberPicker) findViewById(R.id.mod_Picker);
		mModPicker.setMinValue(0);
		mModPicker.setMaxValue(mModValues.length -1);
		mModPicker.setDisplayedValues(mModValues);
		mModPicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
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
		
		mSettings = getSharedPreferences(Settings.SETTINGS, Context.MODE_WORLD_WRITEABLE);
		
		mDiceValues = getResources().getStringArray(R.array.dice_types);
		mModValues = getResources().getStringArray(R.array.mods);

		/*
		 * The following is for api 11 and up
		 * else use compat methods
		 */
		if(Integer.valueOf(android.os.Build.VERSION.SDK_INT) > 11){
			ActionBar actionBar = getActionBar();
			actionBar.setDisplayHomeAsUpEnabled(true);
			initPickers();
		} else 
			initCompatPickers();
		
		Intent i = this.getIntent();
		if(i != null)
			if(i.hasExtra(KEY_GAME_NAME)){
				mGameName = i.getStringExtra(KEY_GAME_NAME);
				mGameId = i.getLongExtra(KEY_GAME_ID, -1);
				this.setTitle(mGameName);
			}
		
		mListView = (ListView) findViewById(R.id.list);
		mListView.setOnItemClickListener(this);
		mListView.setStackFromBottom(true);

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
		MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.dice_roller_menu, menu);
		return true;
	}

    @Override
	public void onDatabaseInsertComplete() {
		isRolling = false;
		this.runOnUiThread(new Runnable(){
			@Override
			public void run(){
				mRollProgress.setVisibility(View.GONE);
				mRollButton.setEnabled(true);
				refresh();
			}
		});
	}

	@Override
	public void onDatabaseUpgrade() {
		//do nothing		
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
			case R.id.menu_clear:
				mDb.clearHistory(mGameId);
				return true;
			case R.id.menu_settings:
				startActivity(new Intent(this, Settings.class).putExtras(getIntent().getExtras()));
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
    
    @Override
	public void onRestoreComplete() {
		// do nothing
	}
    
    /**
   	 * resorts application state after rotation
   	 * @author ricky barrette
   	 */
   	@Override
   	public void onRestoreInstanceState(Bundle savedInstanceState) {
   	  super.onRestoreInstanceState(savedInstanceState);
   	  if(isCompat){
   		  mCompatDPicker.setCurrent(savedInstanceState.getInt("d"));
 		  mCompatNumberPicker.setCurrent(savedInstanceState.getInt("number"));
 		  mCompatModPicker.setCurrent(savedInstanceState.getInt("mod"));
   	  } else {
   		  mDPicker.setValue(savedInstanceState.getInt("d"));
   		  mNumberPicker.setValue(savedInstanceState.getInt("number"));
   		  mModPicker.setValue(savedInstanceState.getInt("mod"));
   	  }
   	}

	/**
	 * (non-Javadoc)
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		refresh();
		applyRollPresets(mDb.getGameRollCount(mGameId));
		
		Intent i = getIntent();
		if(i.hasExtra(KEY_GAME_MODE))
			setGameMode(i.getStringExtra(KEY_GAME_MODE));
		super.onResume();
	}
	
	/**
	 * saves application state before rotation
	 * @author ricky barrette
	 */
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		if(isCompat){
			savedInstanceState.putInt("d", mCompatDPicker.getCurrent());
			savedInstanceState.putInt("number", mCompatNumberPicker.getCurrent());
			savedInstanceState.putInt("mod", mCompatModPicker.getCurrent());
		} else {
			savedInstanceState.putInt("d", mDPicker.getValue());
			savedInstanceState.putInt("number", mNumberPicker.getValue());
			savedInstanceState.putInt("mod", mModPicker.getValue());
		}
		super.onSaveInstanceState(savedInstanceState);
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
	 * (non-Javadoc)
	 * @see android.app.Activity#onPause()
	 */
	@Override
	protected void onStop() {
		mDb.close();
		super.onStop();
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
     * Sets the game mode for this activity
     * @param stringExtra
     * @author ricky barrette
     */
    private void setGameMode(String mode) {
		
    	/*
    	 * EXALTED
    	 */
    	if(mode.equals(getString(R.string.game_mode_exalted))){
    		isSuccessesEanbled = true;
    		isRollModEnabled  = false;
    		isCalculatingTotal  = false;
    		mListAdapter.setRollModEnabled(false);
    		
    		if(isCompat){
    			mCompatModPicker.setVisibility(View.GONE);
    			mCompatDPicker.setCurrent(parseD("D10"));
    			mCompatDPicker.setEnabled(false);
    		} else {
    			mModPicker.setVisibility(View.GONE);
    			mDPicker.setValue(parseD("D10"));
    			mDPicker.setEnabled(false);
    		}
		} 
		
    	/*
    	 * D&D
    	 */
		else if(mode.equals(getString(R.string.game_mode_dd))){
			/*
			 * TODO
			 * nothing
			 */
		}
		
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

		if(isCalculatingTotal)
			resultsString.append(getString(R.string.total)+ total);
		
		if(isRollModEnabled)
			if(isCompat)
				resultsString.append(getString(R.string.total_plus_mod)+ (total + Integer.parseInt(mCompatModPicker.getValue().replace("+", ""))));
			else
				resultsString.append(getString(R.string.total_plus_mod)+ (total + Integer.parseInt(mModValues[mModPicker.getValue()].replace("+", ""))));
		
		if(isSuccessesEanbled)
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
		if(!isRolling){
			isRolling = true;
			
			mListAdapter.notifyDataSetInvalidated();
			mRollButton.setEnabled(false);
			mRollProgress.setVisibility(View.VISIBLE);
			
			new Thread( new Runnable() {
				@Override
				public void run(){
					// vibrate for 50 milliseconds
					vibrate(50);
					
					int rollId = mDb.getGameRollCount(mGameId) +1;
					
					ContentValues roll = new ContentValues();
					
					if(isCompat){
						roll.put(Database.KEY_D_TYPE, mCompatDPicker.getValue());
						roll.put(Database.KEY_NUMBER, mCompatNumberPicker.getCurrent());
						roll.putAll(results(mCompatNumberPicker.getCurrent()));
						if(isRollModEnabled)
							roll.put(Database.KEY_MOD,  DatabaseUtils.sqlEscapeString(mCompatModPicker.getValue()));
					} else{
						roll.put(Database.KEY_D_TYPE, mDiceValues[mDPicker.getValue()]);
						roll.put(Database.KEY_NUMBER, mNumberPicker.getValue());
						roll.putAll(results(mNumberPicker.getValue()));
						if(isRollModEnabled)
							roll.put(Database.KEY_MOD,  DatabaseUtils.sqlEscapeString(mModValues[mModPicker.getValue()]));
					}
					
					
					
					mDb.updateGame(mGameId, mGameName, roll, rollId);
				}
			}).start();
		}
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
			if(isCompat)
				roll[i] = random.nextInt(Integer.parseInt(mCompatDPicker.getValue().substring(1))) + 1;
			else
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
}