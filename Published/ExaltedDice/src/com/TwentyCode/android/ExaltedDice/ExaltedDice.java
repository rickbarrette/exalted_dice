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
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.text.method.NumberKeyListener;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

public class ExaltedDice extends Activity implements OnClickListener, OnLongClickListener, OnItemClickListener {

	private EditText dice;
	private ListView listview;
	private ArrayList<String> rollHistory = new ArrayList<String>();
	private ArrayList<Integer> rolled = new ArrayList<Integer>();
	private int intSuccesses;
    private int mCurrent;
    private static boolean mIncrement;
    private static boolean mDecrement;
    private InputFilter mNumberInputFilter;
	private String[] mDisplayedValues;
	private Formatter mFormatter;
	//the speed in milliseconds for dice increment or decrement
	private long mSpeed = 300;
	//the least and most dice allowed
	private int mStart = 1;
	private int mEnd = 999;
	private static final int MENU_QUIT = Menu.FIRST;
	private static final int MENU_CLEAR = Menu.FIRST + 1;
	protected PostMortemReportExceptionHandler mDamageReport = new PostMortemReportExceptionHandler(this);
	private static final String TAG = "ExaltedDice";
	
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
    
	private static final char[] DIGIT_CHARACTERS = new char[] {
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'
    };

	public interface Formatter {
        String toString(int value);
    }
	private class NumberPickerInputFilter implements InputFilter {
        public CharSequence filter(CharSequence source, int start, int end,
                Spanned dest, int dstart, int dend) {
            if (mDisplayedValues == null) {
                return mNumberInputFilter.filter(source, start, end, dest, dstart, dend);
            }
            CharSequence filtered = String.valueOf(source.subSequence(start, end));
            String result = String.valueOf(dest.subSequence(0, dstart)) + filtered
                    + dest.subSequence(dend, dest.length());
            String str = String.valueOf(result).toLowerCase();
            for (String val : mDisplayedValues) {
                val = val.toLowerCase();
                if (val.startsWith(str)) {
                    return filtered;
                }
            }
            return "";
        }
    }

	private class NumberRangeKeyListener extends NumberKeyListener {

        @Override
        public CharSequence filter(CharSequence source, int start, int end,  Spanned dest, int dstart, int dend) {

            CharSequence filtered = super.filter(source, start, end, dest, dstart, dend);
            if (filtered == null) {
                filtered = source.subSequence(start, end);
            }

            String result = String.valueOf(dest.subSequence(0, dstart))
                    + filtered
                    + dest.subSequence(dend, dest.length());

            if ("".equals(result)) {
                return result;
            }
            int val = getSelectedPos(result);

            /* Ensure the user can't type in a value greater
             * than the max allowed. We have to allow less than min
             * as the user might want to delete some numbers
             * and then type a new number.
             */
            if (val > mEnd) {
                return "";
            } else {
                return filtered;
            }
        }

        @Override
        protected char[] getAcceptedChars() {
            return DIGIT_CHARACTERS;
        }

        // XXX This doesn't allow for range limits when controlled by a
        // soft input method!
        public int getInputType() {
            return InputType.TYPE_CLASS_NUMBER;
        }
    }
	
	/**
	 * stops decrementing of dice after longpress
	 * @author ricky barrette
	 */
	public static void cancelDecrement() {
		mDecrement = false;
	}

	/**
	 * stop incrementing of dice after longpress
	 * @author ricky barrette
	 */
	public static void cancelIncrement() {
		mIncrement = false;
	}

	protected void changeCurrent(int current) {

        // Wrap around the values if we go past the start or end
        if (current > mEnd) {
            current = mStart;
        } else if (current < mStart) {
            current = mEnd;
        }
        mCurrent = current;
        updateView();
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

	private String formatNumber(int value) {
        return (mFormatter != null)
                ? mFormatter.toString(value)
                : String.valueOf(value);
    }

	/**
     * @return the current value.
     */
    public int getCurrent() {
        return mCurrent;
    }

	private int getSelectedPos(String str) {
        if (mDisplayedValues == null) {
            return Integer.parseInt(str);
        } else {
            for (int i = 0; i < mDisplayedValues.length; i++) {

                /* Don't force the user to type in jan when ja will do */
                str = str.toLowerCase();
                if (mDisplayedValues[i].toLowerCase().startsWith(str)) {
                    return mStart + i;
                }
            }

            /* The user might have typed in a number into the month field i.e.
             * 10 instead of OCT so support that too.
             */
            try {
                return Integer.parseInt(str);
            } catch (NumberFormatException e) {

                /* Ignore as if it's not a number we don't care */
            }
        }
        return mStart;
    }

	/**
	 * also implemented OnClickListener
	 * 
	 * @author ricky barrette 3-27-2010
	 * @author - WWPowers 3-26-2010
	 */
	@Override
	public void onClick(View v){
		
		//get the number from the edit text
		try {
			mCurrent = Integer.parseInt(dice.getText().toString());
		} catch (NumberFormatException e) {
			e.printStackTrace();
		}
		
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
		dice = (EditText) findViewById(R.id.dice);
        InputFilter inputFilter = new NumberPickerInputFilter();
        mNumberInputFilter = new NumberRangeKeyListener();
        dice.setFilters(new InputFilter[] {inputFilter});
		
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
		
		//get the number from the edit text
		try {
			mCurrent = Integer.parseInt(dice.getText().toString());
		} catch (NumberFormatException e) {
			e.printStackTrace();
		}
		
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
		
		//get the number from the edit text
		try {
			mCurrent = Integer.parseInt(dice.getText().toString());
		} catch (NumberFormatException e) {
			e.printStackTrace();
		}
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

    public void setCurrent(int current) {
        mCurrent = current;
        updateView();
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

    protected void updateView() {

        /* If we don't have displayed values then use the
         * current number else find the correct value in the
         * displayed values for the current number.
         */
        if (mDisplayedValues == null) {
        	dice.setText(formatNumber(mCurrent));
        } else {
        	dice.setText(mDisplayedValues[mCurrent - mStart]);
        }
        dice.setSelection(dice.getText().length());
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