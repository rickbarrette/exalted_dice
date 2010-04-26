package com.TwentyCode.android.ExaltedDice;

import java.util.ArrayList;
import java.util.Random;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
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

	private int intSuccesses;
	public int intDice;

	private ArrayList<String> rollHistory = new ArrayList<String>();
	private ArrayList<Integer> rolled = new ArrayList<Integer>();

	private final int ADD_DICE = R.id.up;
	private final int SUB_DICE = R.id.down;
	private final int ROLL_DICE = R.id.roll;

	private static final int MENU_QUIT = Menu.FIRST;
	private static final int MENU_CLEAR = Menu.FIRST + 1;

	protected PostMortemReportExceptionHandler mDamageReport = new PostMortemReportExceptionHandler(this);

	public void addDiceRolled() {
		vibrate(50);
		intDice = checkForErrors((dice.getText()).toString());
		intDice = moreDice(intDice, 1);
		dice.setText("" + intDice);
		System.gc();
	}

	public void addDiceRolledonLongClick() {
		vibrate(75);
		intDice = checkForErrors((dice.getText()).toString());
		intDice = moreDice(intDice, 10);
		dice.setText("" + intDice);
		System.gc();
	}

	public int checkForErrors(String string) {

		int numDice = 0;
		char charDice;
		StringBuffer stringDice = new StringBuffer();
		boolean errors = false;
		boolean zeroCheck = true;
		boolean isNumber = false;

		if (string.length() == 0)
			string += "" + 1;

		for (int i = 0; i < string.length(); i++) {
			// get the char
			charDice = string.charAt(i);

			try {

				numDice = Integer.parseInt(Character.toString(charDice));

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


		if (isNumber == false) {
			toastLong("You inputed: \" "
					+ string
					+ " \", which contains no numbers, we will roll one dice for you.");
			stringDice.append(1);

			errors = false;
		}

		if (errors == true)
			toastLong("You inputed: \" " + string
					+ " \", we are assuming you meant: "
					+ stringDice.toString());

		if (stringDice.length() > 3) {

			toastLong("Sorry, I can not roll " + stringDice
					+ " dice. Try Rolling Between 1 - 999 dice.");

			numDice = 1;

		} else
			numDice = Integer.parseInt(stringDice.toString());

		stringDice = null;
		System.gc();
		return numDice;
	}

	private void clearHistory() {
		rollHistory.clear();
		rolled.clear();
		listview.setAdapter(new ArrayAdapter<String>(this, R.layout.list_row, rollHistory));
	}

	public int lessDice(int number, int lowerBy) {

		if (number <= lowerBy)
			lowerBy = number - 1;

		if (number > 1)
			number = number - lowerBy;

		System.gc();
		return number;
	}

	public int moreDice(int number, int raiseBy) {
		if (number > 989)
			if (raiseBy > (number - 989))
				raiseBy = 999 - number;
		if (number < 999)
			number = number + raiseBy;

		System.gc();
		return number;
	}

	@Override
	public void onClick(View v) {

		if (v.getId() == ADD_DICE)
			addDiceRolled();

		if (v.getId() == SUB_DICE)
			subtractDiceRolled();

		if (v.getId() == ROLL_DICE)
			rollDice();
		
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		mDamageReport.run();
        Thread.setDefaultUncaughtExceptionHandler(mDamageReport);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		dice = (TextView) findViewById(R.id.dice);
		listview = (ListView) findViewById(R.id.list);
		Button btAddDice = (Button) findViewById(R.id.up);
		Button btSubtractDice = (Button) findViewById(R.id.down);
		Button btRollDice = (Button) findViewById(R.id.roll);
		btAddDice.setOnClickListener(this);
		btSubtractDice.setOnClickListener(this);
		btRollDice.setOnClickListener(this);
		listview.setOnItemClickListener(this);
		btAddDice.setOnLongClickListener(this);
		btSubtractDice.setOnLongClickListener(this);
		listview.setAdapter(new ArrayAdapter<String>(this, R.layout.list_row, getResources().getStringArray(R.array.hello_msg)));
		
		System.gc();
		
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(1, MENU_CLEAR, 0, "Clear Roll History");
		menu.add(1, MENU_QUIT, 0, "Quit");
		return true;
	}


	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		if(rolled.size() != 0){
			dice.setText("" + rolled.get(arg2));
			rollDice();
		}
	}

	public boolean onLongClick(View v) {
		if (v.getId() == ADD_DICE) {
			addDiceRolledonLongClick();

		} else if (v.getId() == SUB_DICE) {
			subtractDiceRolledonLongClick();

		} else {
			return false;
		}
		return true;
	}

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

	@Override
	public void onPause() {
		super.onPause();
		System.gc();
	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
	  super.onRestoreInstanceState(savedInstanceState);
	  rollHistory = savedInstanceState.getStringArrayList("roll_history");
	  dice.setText(savedInstanceState.getString("dice"));
	  rolled = savedInstanceState.getIntegerArrayList("rolled");
	  listview.setAdapter(new ArrayAdapter<String>(this, R.layout.list_row, rollHistory));
	}

	@Override
	public void onResume() {
		super.onResume();
	}

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
	  savedInstanceState.putStringArrayList("roll_history", rollHistory);
	  savedInstanceState.putString("dice", dice.getText().toString());
	  savedInstanceState.putIntegerArrayList("rolled", rolled);
	  super.onSaveInstanceState(savedInstanceState);
	}


	@Override
	public void onStop() {
		super.onStop();
		System.gc();
	}

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

	public String results(int times) {
		StringBuffer resultsString = new StringBuffer();
		resultsString.append("Rolled "+ times +" dice\n");
		int[] roll = rollGen(times);
		resultsString.append("Successes: "+ successes(roll) +"\n");
		resultsString.append("Rolled: ");
		for (int i = 0; i < roll.length; i++) {
			resultsString.append(roll[i] + ", ");
		}
		System.gc();
		return resultsString.toString();
	}

	public void rollDice() {
		vibrate(50);
		intDice = checkForErrors((dice.getText()).toString());
		dice.setText("" + intDice);
		rolled.add(0, intDice);
		rollHistory.add(0, results(intDice));
		listview.setAdapter(new ArrayAdapter<String>(this, R.layout.list_row, rollHistory));
		System.gc();
	}

	public int[] rollGen(int times) {
		int[] roll = new int[times];
		Random random = new Random();
		for (int i = 0; i < times; i++) {
			roll[i] = random.nextInt(10) + 1;
		}
		System.gc();
		return roll;
	}

	public void subtractDiceRolled() {
		vibrate(50);
		intDice = checkForErrors((dice.getText()).toString());
		intDice = lessDice(intDice, 1);
		dice.setText("" + intDice);
		System.gc();
	}

	public void subtractDiceRolledonLongClick() {
		vibrate(75);
		intDice = checkForErrors((dice.getText()).toString());
		intDice = lessDice(intDice, 10);
		dice.setText("" + intDice);
		System.gc();

	}


	public int successes(int[] roll) {
		intSuccesses = 0;
		for (int i = 0; i < roll.length; i++) {
			if (roll[i] >= 7)
				intSuccesses++;
			if (roll[i] == 10)
				intSuccesses++;
		}
		System.gc();
		return intSuccesses;
	}


	public void toastLong(CharSequence msg) {
		Toast toast = Toast.makeText(this, msg, Toast.LENGTH_LONG);
		toast.show();
	}


	public void vibrate(long milliseconds) {
		Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		vib.vibrate(milliseconds);
	}

}