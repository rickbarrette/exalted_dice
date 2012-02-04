package com.TwentyCode.android.ExaltedDice;

import java.util.ArrayList;
import java.util.Random;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.NumberPicker.OnValueChangeListener;
import android.widget.Toast;

import com.TwentyCodes.android.exception.ExceptionHandler;

public class ExaltedDice extends Activity implements OnClickListener, OnItemClickListener, OnValueChangeListener, DatabaseListener {

	private ListView listview;
	private ArrayList<String> rollHistory = new ArrayList<String>();
	private ArrayList<Integer> rolled = new ArrayList<Integer>();
	private int intSuccesses;
	private int mRolls = 1;
	private int mD = 2;
	private NumberPicker mNumberPicker;
	private int mCurrentDie;
	private NumberPicker mDPicker;
	private Database mDb;
	private static final int MENU_QUIT = Menu.FIRST;
	private static final int MENU_CLEAR = Menu.FIRST + 1;
	private static final String TAG = "ExaltedDice";
	private static final String[] DICE_VALUES = { "D2", "D3", "D4", "D6", "D8", "D10", "D12", "D20", "D100" };

	/**
	 * clears the rollHistory List array and refreshes the listview
	 * 
	 * @author ricky barrette
	 */
	private void clearHistory() {
		rollHistory.clear();
		rolled.clear();
		listview.setAdapter(new ArrayAdapter<String>(this, R.layout.list_row, rollHistory));
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
		// TODO Auto-generated method stub
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
		
		listview = (ListView) findViewById(R.id.list);
		listview.setOnItemClickListener(this);

		mDPicker = (NumberPicker) findViewById(R.id.d_Picker);
		mDPicker.setMinValue(0);
		mDPicker.setMaxValue(DICE_VALUES.length -1);
		mDPicker.setDisplayedValues(DICE_VALUES);
		mDPicker.setOnValueChangedListener(this);
		mDPicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
		
		mNumberPicker = (NumberPicker) findViewById(R.id.number_Picker);
		mNumberPicker.setMaxValue(999);
		mNumberPicker.setMinValue(1);
		mNumberPicker.setOnValueChangedListener(this);
		mNumberPicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
		
		findViewById(R.id.roll_button).setOnClickListener(this);

		/*
		 * display hello message
		 */
		listview.setAdapter(new ArrayAdapter<String>(this, R.layout.list_row, getResources().getStringArray(R.array.hello_msg)));
		
		System.gc();
		
	}

	/**
	 * (non-Javadoc)
	 * @see android.app.Activity#onCreateContextMenu(android.view.ContextMenu, android.view.View, android.view.ContextMenu.ContextMenuInfo)
	 */
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		// TODO Auto-generated method stub
		super.onCreateContextMenu(menu, v, menuInfo);
	}

	/**
	 * creates a menu with a quit option
	 * 
	 * @author WWPowers 3-27-2010
	 */
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(1, MENU_CLEAR, 0, "Clear Roll History");
		menu.add(1, MENU_QUIT, 0, "Quit");
		return true;
	}

	@Override
	public void onDatabaseUpgrade() {
		// TODO Auto-generated method stub
		
	}

    @Override
	public void onDatabaseUpgradeComplete() {
		// TODO Auto-generated method stub
		
	}

    @Override
	public void onDeletionComplete() {
		// TODO Auto-generated method stub
		
	}
	
    /**
	 * rolls same amount of dice as previous roll
	 * @author ricky barrette
	 */
	@Override
	public void onItemClick(AdapterView<?> arg0, View v, int position, long arg3) {
		if(rolled.size() != 0){
			mNumberPicker.setValue(rolled.get(position));
			rollDice();
		}
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

    @Override
	public void onRestoreComplete() {
		// TODO Auto-generated method stub
		
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
		mDb = new Database(this, this);
		super.onResume();
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

    @Override
	public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
		switch(picker.getId()){
			case R.id.d_Picker:
				mD = Integer.parseInt(DICE_VALUES[newVal].substring(1));
				mCurrentDie = newVal;
				break;
			case R.id.number_Picker:
				mRolls = newVal;
				break;
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
	 * returns a custom string containing dice rolls and number of successes
	 * 
	 * @param int times
	 * @return String resultsString
	 * @author ricky barrette
	 */
	public String results(int times) {
		Log.i(TAG, "results()");
		StringBuffer resultsString = new StringBuffer();
		resultsString.append("Rolled "+ times +" "+DICE_VALUES[mCurrentDie]);

		/**
		 * roll the dice
		 */
		int[] roll = rollGen(times);

		/**
		 * add number of successes to resultsString
		 */
		resultsString.append("\nSuccesses: "+ successes(roll) +"\n");
		
		resultsString.append("Rolled: ");
		/**
		 * add rolled dice results to resultsString
		 */
		for (int i = 0; i < roll.length; i++) {
			resultsString.append(roll[i] + ", ");
		}

		return resultsString.toString();
	}

	/**
	 * Performs a dice roll
	 * 
	 * @author ricky barrette
	 */
	public void rollDice() {
		// vibrate for 50 milliseconds
		vibrate(50);
		
		rolled.add(0, mRolls);
		rollHistory.add(0, results(mRolls));

		listview.setAdapter(new ArrayAdapter<String>(this, R.layout.list_row, rollHistory));
	}

	/**
	 * generates an array containing 10 sided dice rolls
	 * 
	 * @param int times
	 * @return int[] roll
	 * @author ricky barrette
	 */
	public int[] rollGen(int times) {
		Log.i(TAG, "rollGen()" + times);
		int[] roll = new int[times];
		Random random = new Random();
		for (int i = 0; i < times; i++) {
			roll[i] = random.nextInt(mD) + 1;
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
		intSuccesses = 0;
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
	 * 
	 * @param Long
	 *            milliseconds
	 * @author ricky barrette
	 */
	public void vibrate(long milliseconds) {
		Log.i(TAG, "vibrate() for " + milliseconds);
		/**
		 * start vibrator service
		 */
		Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

		/**
		 * Vibrate for x milliseconds
		 */
		vib.vibrate(milliseconds);
	}
}