package com.TwentyCode.android.ExaltedDice;

import java.util.Random;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.Toast;

import com.TwentyCodes.android.exception.ExceptionHandler;

public class ExaltedDice extends Activity implements OnClickListener, OnItemClickListener {

	private static final int MENU_QUIT = Menu.FIRST;
	private static final int MENU_CLEAR = Menu.FIRST + 1;
	private static final String TAG = "ExaltedDice";
	public static final String KEY_GAME_NAME = "game_name";
	public static final String KEY_GAME_ID = "game_id";
	private static final String[] DICE_VALUES = { "D2", "D3", "D4", "D6", "D8", "D10", "D12", "D20", "D100" };
	private static final int DELETE = 0;
	private static final int SETTINGS = Menu.FIRST + 2;

	private ListView mListView;
	private NumberPicker mNumberPicker;
	private NumberPicker mDPicker;
	private Database mDb;
	private String mGameName;
	private long mGameId;
	private RollHistoryDatabaseAdapter mListAdapter;
	private boolean isNewGame;
	
	/**
	 * clears the rollHistory List array and refreshes the listview
	 * @author ricky barrette
	 */
	private void clearHistory() {
		mDb.clearHistory(mGameId);
		refresh();
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
		Intent i = this.getIntent();
		if(i != null)
			if(i.hasExtra(KEY_GAME_NAME)){
				mGameName = i.getStringExtra(KEY_GAME_NAME);
				mGameId = i.getLongExtra(KEY_GAME_ID, -1);
				this.setTitle(mGameName);
				
				Log.v(TAG, "game name ="+mGameName);
				Log.v(TAG, "game id ="+mGameId);
				
			}
		
		mListView = (ListView) findViewById(R.id.list);
		mListView.setOnItemClickListener(this);
		mListView.setStackFromBottom(true);

		mDPicker = (NumberPicker) findViewById(R.id.d_Picker);
		mDPicker.setMinValue(0);
		mDPicker.setMaxValue(DICE_VALUES.length -1);
		mDPicker.setDisplayedValues(DICE_VALUES);
		mDPicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
		
		mNumberPicker = (NumberPicker) findViewById(R.id.number_Picker);
		mNumberPicker.setMaxValue(999);
		mNumberPicker.setMinValue(1);
		mNumberPicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
		
		findViewById(R.id.roll_button).setOnClickListener(this);

		/*
		 * display hello message
		 */
		mListView.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, getResources().getStringArray(R.array.hello_msg)));
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
		menu.add(1, MENU_CLEAR, 0, "Clear Roll History");
		menu.add(1, SETTINGS, 0, "Settings");
		menu.add(1, MENU_QUIT, 0, "Quit");
		return true;
	}

    /**
	 * rolls same amount of dice as previous roll
	 * @author ricky barrette
	 */
	@Override
	public void onItemClick(AdapterView<?> arg0, View v, int position, long id) {
		ContentValues roll = mDb.getGameHistoryInfo(mGameName, (int) (id + 1));
		mNumberPicker.setValue(roll.getAsInteger(Database.KEY_NUMBER));
		mDPicker.setValue(parseD(roll.getAsString(Database.KEY_D_TYPE)));
		
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
			case MENU_QUIT:
				quitDialog();
				return true;
			case MENU_CLEAR:
				clearHistory();
				return true;
			case SETTINGS:
				startActivity(new Intent(this, Settings.class));
				break;
		}
		return false;
	}

    /**
	 * (non-Javadoc)
	 * @see android.app.Activity#onPause()
	 */
	@Override
	protected void onPause() {
		mDb.close();
		super.onPause();
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
	}

    /**
	 * (non-Javadoc)
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		if(mDb.getGameRollCount(mGameId) > 0){
			isNewGame = false;
			refresh();
		} else
			isNewGame = true;
		super.onResume();
	}

    /**
	 * (non-Javadoc)
	 * @see android.app.Activity#onStart()
	 */
	@Override
	protected void onStart() {
		mDb = new Database(this);
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
		super.onSaveInstanceState(savedInstanceState);
	}

	/**
	 * Parses the string d type to the appropriate value
	 * @param d type
	 * @return value for d picker
	 * @author ricky barrette
	 */
    private int parseD(String d) {
		for(int i = 0; i < DICE_VALUES.length; i++)
			if (DICE_VALUES[i].equalsIgnoreCase(d))
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
		builder.setMessage("Are you sure you want to quit?").setCancelable(
				false).setPositiveButton("Yes",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						ExaltedDice.this.finish();
					}
				}).setNegativeButton("No",
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
		if(!isNewGame){
			mListView.setAdapter(mListAdapter);
			isNewGame = false;
		}
		mListAdapter.notifyDataSetChanged();
	}

	/**
	 * returns a custom string containing dice rolls and number of successes
	 * 
	 * @param int times
	 * @return String resultsString
	 * @author ricky barrette
	 */
	public String results(int times) {
		Log.i(TAG, "results()");
		StringBuffer resultsString = new StringBuffer();
		StringBuffer rolls = new StringBuffer();
		long total = 0;
		
		int[] roll = rollGen(times);
		
		for (int item : roll) {
			rolls.append(item + ", ");
			total = total + item;
		}

		resultsString.append("Total: "+ total);
		resultsString.append("\nSuccesses: "+ successes(roll));
		resultsString.append("\nRolls: "+ rolls.toString());
		return resultsString.toString();
	}

	/**
	 * Performs a dice roll and logs it's information in the database
	 * 
	 * @author ricky barrette
	 */
	public void rollDice() {
		// vibrate for 50 milliseconds
		vibrate(50);
		
		int rollId = mDb.getGameRollCount(mGameId) +1;
		
		ContentValues roll = new ContentValues();
		roll.put(Database.KEY_D_TYPE, DICE_VALUES[mDPicker.getValue()]);
		roll.put(Database.KEY_NUMBER, mNumberPicker.getValue());
		roll.put(Database.KEY_LOG, results(mNumberPicker.getValue()));
		
		mDb.updateGame(mGameId, mGameName, roll, rollId);
		
		refresh();
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
			roll[i] = random.nextInt(Integer.parseInt(DICE_VALUES[mDPicker.getValue()].substring(1))) + 1;
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
		Log.i(TAG, "toastLong()");
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