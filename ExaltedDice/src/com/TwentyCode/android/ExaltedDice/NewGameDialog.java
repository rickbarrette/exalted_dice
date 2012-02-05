/**
 * NewGameDialog.java
 * @date Feb 4, 2012
 * @author ricky barrette
 * @author Twenty Codes, LLC
 */
package com.TwentyCode.android.ExaltedDice;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.EditText;

/**
 * This dialog will be used to allow a user to enter a name for their game
 * @author ricky barrette
 */
public class NewGameDialog extends Dialog implements android.view.View.OnClickListener {

	private Context mContext;
	private Database mDb;
	private EditText mGameName;

	public NewGameDialog(Context context, Database db) {
		super(context);
		mContext = context;
		mDb = db;
		this.setTitle(R.string.new_game);
		this.setContentView(R.layout.new_game_dialog);
		findViewById(R.id.new_game_button).setOnClickListener(this);
		mGameName = (EditText) findViewById(R.id.editText);
	}

	@Override
	public void onClick(View v) {
		findViewById(R.id.progress).setVisibility(View.VISIBLE);
		findViewById(R.id.new_game_button).setEnabled(false);
		
		String name = mGameName.getText().toString();
		
		Intent i = new Intent(mContext, ExaltedDice.class)
			.putExtra(ExaltedDice.KEY_GAME_NAME, name)
			.putExtra(ExaltedDice.KEY_GAME_ID, mDb.insertGame(name));
		
		mContext.startActivity(i);
		this.dismiss();
	}

}
