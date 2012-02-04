/**
 * GameListActivity.java
 * @date Feb 4, 2012
 * @author ricky barrette
 * @author Twenty Codes, LLC
 */
package com.TwentyCode.android.ExaltedDice;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

/**
 * This activity will be used to display a list of games to the user.
 * 
 * @author ricky barrette
 */
public class GameListActivity extends Activity implements OnClickListener, DatabaseListener {

	private ListView mList;
	private Database mDb;

	@Override
	public void onClick(View v) {
		startActivity(new Intent(this, ExaltedDice.class));
	}

	/**
	 * (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.game_list);
		findViewById(R.id.new_game_button).setOnClickListener(this);
		mList = (ListView) findViewById(android.R.id.list);
		registerForContextMenu(mList);
	}

	/**
	 * (non-Javadoc)
	 * @see android.app.Activity#onCreateContextMenu(android.view.ContextMenu, android.view.View, android.view.ContextMenu.ContextMenuInfo)
	 */
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		/* 
		 * TODO
		 * delete game
		 * rename game
		 */
		super.onCreateContextMenu(menu, v, menuInfo);
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
	 * (non-Javadoc)
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		mDb = new Database(this, this);
		mList.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mDb.getAllGameTitles()));
		super.onResume();
	}

}
