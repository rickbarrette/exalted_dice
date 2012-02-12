/**
 * NewGameDialog.java
 * @date Feb 4, 2012
 * @author ricky barrette
 * @author Twenty Codes, LLC
 */
package com.TwentyCode.android.ExaltedDice;

import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;

/**
 * This dialog will be used to allow a user to enter a name for their game
 * @author ricky barrette
 */
public class NewGameDialog extends Dialog implements android.view.View.OnClickListener {

	private final Context mContext;
	private final Database mDb;
	private final EditText mGameName;
	private final Spinner mGameModeSpinner;

	public NewGameDialog(Context context, Database db) {
		super(context);
		mContext = context;
		mDb = db;
		setTitle(R.string.new_game);
		setContentView(R.layout.new_game_dialog);
		findViewById(R.id.new_game_button).setOnClickListener(this);
		mGameName = (EditText) findViewById(R.id.editText);
		mGameModeSpinner = (Spinner) findViewById(R.id.game_mode_spinner);
	}

	@Override
	public void onClick(View v) {
		findViewById(R.id.progress).setVisibility(View.VISIBLE);
		findViewById(R.id.new_game_button).setEnabled(false);
		
		String name = mGameName.getText().toString();
		String gameMode = (String) mGameModeSpinner.getSelectedItem();
		
		/*
		 * create a new game in the database
		 */
		long gameId = mDb.insertGame(name);
		
		/*
		 * Store the game mode
		 */
		ContentValues options = new ContentValues();
		options.put(Database.KEY_MODE, gameMode);
		mDb.updateOptions(gameId, options );

		/*
		 * Start the dice roller activity
		 */
		Intent i = new Intent(mContext, ExaltedDice.class)
		.putExtra(ExaltedDice.KEY_GAME_NAME, name)
		.putExtra(ExaltedDice.KEY_GAME_ID, gameId)
		.putExtra(ExaltedDice.KEY_GAME_MODE, gameMode);
		mContext.startActivity(i);
		
		this.dismiss();
	}

}
