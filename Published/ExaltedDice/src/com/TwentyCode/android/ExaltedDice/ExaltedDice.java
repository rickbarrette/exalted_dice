package com.TwentyCode.android.ExaltedDice;

import java.util.ArrayList;
import java.util.Random;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class ExaltedDice extends Activity implements OnClickListener, OnLongClickListener, OnItemClickListener {

	private TextView dice;
	private ListView listview;

	private ArrayList<String> rollHistory = new ArrayList<String>();
	private ArrayList<Integer> rolled = new ArrayList<Integer>();

	private int intSuccesses;
	private int mCurrent;
	private static boolean mIncrement;
	private static boolean mDecrement;
	
	//the speed in milliseconds for dice increment or decrement
	private long mSpeed = 300;
	
	//the least and most dice allowed
	private int mStart = 1;
	private int mEnd = 999;
	
	private Handler mHandler;
	//this runnable will be used to increment or decrement the amount of dice being rolled
    private final Runnable mRunnable = new Runnable() {
		public void run() {
            if (mIncrement) {
                changeCurrent(mCurrent + 1);
                mHandler.postDelayed(this, mSpeed);
            } else if (mDecrement) {
                changeCurrent(mCurrent - 1);
                mHandler.postDelayed(this, mSpeed);
            }
        }
    };

	private static final String TAG = "ExaltedDice";

	/**
	 * menu options
	 */
	private static final int MENU_QUIT = Menu.FIRST;
	private static final int MENU_CLEAR = Menu.FIRST + 1;

	protected PostMortemReportExceptionHandler mDamageReport = new PostMortemReportExceptionHandler(this);
	
	 protected void changeCurrent(int current) {
	        // Wrap around the values if we go past the start or end
	    if (current > mEnd) {
	        current = mStart;
	    } else if (current < mStart) {
	        current = mEnd;
	    }
	    mCurrent = current;
	    dice.setText("" + mCurrent);
	}

	/**
	 * checks for the following errors:
	 * 
	 * checks string if it is blank, if so changes string to "1"
	 * 
	 * look at each char in string individually to see if int or not if the char
	 * is an in add it to stringDice, example: a string of "1.2-3" would result
	 * in a stringDice of "123"
	 * 
	 * also if there are any zeros preceding an int, they will be removed
	 * example: a string of "000102" would result in a stringDice of "102"
	 * 
	 * if there are any typos removed, use toast to inform the user that we
	 * assumed that when they typed "0001.2-3" they meant to type "123"
	 * 
	 * limits stringDice to 3 chars (limiting the number of dice to 999) if
	 * stringDice is longer than 3 chars, it is changed to "1" also toast is
	 * displayed to inform user of the 999 dice rule.
	 * 
	 * @param String
	 *            string
	 * @return int numDice
	 * @author ricky barrette
	 */
	public int checkForErrors(String string) {
		Log.i(TAG, "checkForErrors()");

		int numDice = 0;
		char charDice;
		StringBuffer stringDice = new StringBuffer();
		boolean errors = false;
		boolean zeroCheck = true;
		boolean isNumber = false;

		/**
		 * if textField is left blank, change value to 1
		 */
		if (string.length() == 0)
			string += "" + 1;

		/**
		 * look at each char in string individually to see if int or not if the
		 * char is an in add it to stringDice, example: a string of "1.2-3"
		 * would result in a stringDice of "123"
		 * 
		 * also if there are any zeros preceding an int, they also will be
		 * removed example: a string of "00-01.02" would result in a stringDice
		 * of "102"
		 */
		for (int i = 0; i < string.length(); i++) {
			// get the char
			charDice = string.charAt(i);

			/**
			 * try to parse the char, to see it it is an int or not.
			 */
			try {

				/**
				 * we are going to borrow numDice instead of creating another
				 * addressing space
				 */

				numDice = Integer.parseInt(Character.toString(charDice));

				// -------------------------------------------------------------------------------------

				/**
				 * zero check to remove any zeros preceding an int value. true =
				 * zero check on, false = zero check off
				 */
				if (zeroCheck == true) {
					if (numDice != 0) {
						zeroCheck = false;
						isNumber = true;
						stringDice.append(numDice);
					}
				} else
					stringDice.append(numDice);

			} catch (NumberFormatException nFE) {
				errors = true;
			}

		}

		/**
		 * notify user if there were no ints
		 */
		if (isNumber == false) {
			toastLong("You inputed: \" "
					+ string
					+ " \", which contains no numbers, we will roll one dice for you.");
			stringDice.append(1);
			/**
			 * prevent error message from displaying
			 */
			errors = false;
		}

		/**
		 * notify user if there were typos
		 */
		if (errors == true)
			toastLong("You inputed: \" " + string
					+ " \", we think you meant: "
					+ stringDice.toString());

		// -----------------------------------------------------------------------------------------

		/**
		 * limit number to 999
		 */

		if (stringDice.length() > 3) {

			toastLong("Sorry, I can not roll " + stringDice
					+ " dice. Try Rolling Between 1 - 999 dice.");

			/**
			 * set number of dice to 1
			 */
			numDice = 1;

		} else
			numDice = Integer.parseInt(stringDice.toString());

		stringDice = null;
		System.gc();
		return numDice;
	}

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
		if (v.getId() == R.id.up)
			changeCurrent(mCurrent + 1);

		if (v.getId() == R.id.down)
			changeCurrent(mCurrent - 1);

		if (v.getId() == R.id.roll)
			rollDice();
	}

	/**
	 * Called when the activity is first created. starts gui and sets up buttons
	 * 
	 * @author ricky barrette 3-27-2010
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		mDamageReport.run();
        Thread.setDefaultUncaughtExceptionHandler(mDamageReport);
		
		super.onCreate(savedInstanceState);
		Log.i(TAG, "onCreate()");
		setContentView(R.layout.main);
		
		mHandler = new Handler();
		
		/*
		 * views and listeners
		 */
		dice = (TextView) findViewById(R.id.dice);
		listview = (ListView) findViewById(R.id.list);
		NumberPickerButton btAddDice = (NumberPickerButton) findViewById(R.id.up);
		NumberPickerButton btSubtractDice = (NumberPickerButton) findViewById(R.id.down);
		Button btRollDice = (Button) findViewById(R.id.roll);
		btAddDice.setOnClickListener(this);
		btSubtractDice.setOnClickListener(this);
		btRollDice.setOnClickListener(this);
		listview.setOnItemClickListener(this);
		btAddDice.setOnLongClickListener(this);
		btSubtractDice.setOnLongClickListener(this);

		/*
		 * shake Listener
		 */
//		ShakeListener mShaker = new ShakeListener(this);
//		mShaker.setOnShakeListener(new ShakeListener.OnShakeListener() {
//			public void onShake() {
//				rollDice();
//			}
//		});
		
		/*
		 * hide keyboard
		 * 
		 * works on the emulator
		 */
		((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(dice.getWindowToken(), 0);  
		
		/*
		 * display hello message
		 */
		listview.setAdapter(new ArrayAdapter<String>(this, R.layout.list_row, getResources().getStringArray(R.array.hello_msg)));
		
		System.gc();
		
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

	/**
	 * rolls same amount of dice as previous roll
	 * @author ricky barrette
	 */
	@Override
	public void onItemClick(AdapterView<?> arg0, View v, int position, long arg3) {
		if(rolled.size() != 0){
			dice.setText("" + rolled.get(position));
			rollDice();
		}
	}

	/**
	 * starts a runnable that will increment or decrement the dice
	 * 
	 * @author ricky barrette 3-27-2010
	 * @param v
	 */
	public boolean onLongClick(View v) {
		if (v.getId() == R.id.up) {
			mIncrement = true;
            mHandler.post(mRunnable);

		} else if (v.getId() == R.id.down) {
			mDecrement = true;
	        mHandler.post(mRunnable);
		} else {
			return false;
		}
		return true;
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
	 * resorts application state after rotation
	 * @author ricky barrette
	 */
	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
	  super.onRestoreInstanceState(savedInstanceState);
	  // Restore UI state from the savedInstanceState.
	  // This bundle has also been passed to onCreate.
	  rollHistory = savedInstanceState.getStringArrayList("roll_history");
	  dice.setText(savedInstanceState.getString("dice"));
	  rolled = savedInstanceState.getIntegerArrayList("rolled");
	  listview.setAdapter(new ArrayAdapter<String>(this, R.layout.list_row, rollHistory));
	}

	/**
	 * saves application state before rotatoin
	 * @author ricky barrette
	 */
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
	  // Save UI state changes to the savedInstanceState.
	  // This bundle will be passed to onCreate if the process is
	  // killed and restarted.
	  savedInstanceState.putStringArrayList("roll_history", rollHistory);
	  savedInstanceState.putString("dice", dice.getText().toString());
	  savedInstanceState.putIntegerArrayList("rolled", rolled);
	  super.onSaveInstanceState(savedInstanceState);
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
		resultsString.append("Rolled "+ times +" dice\n");

		/**
		 * roll the dice
		 */
		int[] roll = rollGen(times);

		/**
		 * add number of successes to resultsString
		 */
		resultsString.append("Successes: "+ successes(roll) +"\n");
		
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

		/**
		 * get string from dice textfield it convert it into int, while checking
		 * for user input errors
		 */
		mCurrent = checkForErrors((dice.getText()).toString());

		// set Dice textfield to finDice
		dice.setText("" + mCurrent);

		rolled.add(0, mCurrent);
		rollHistory.add(0, results(mCurrent));

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
			roll[i] = random.nextInt(10) + 1;
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

	/**
	 * stop incrementing of dice after longpress
	 * @author ricky barrette
	 */
	public static void cancelIncrement() {
		mIncrement = false;
	}

	/**
	 * stops decrementing of dice after longpress
	 * @author ricky barrette
	 */
	public static void cancelDecrement() {
		mDecrement = false;
	}
	
	
}