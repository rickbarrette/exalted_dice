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
import android.view.Menu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

/**
 * This activity will be used to display a list of games to the user.
 * 
 * @author ricky barrette
 */
public class GameListActivity extends Activity implements OnClickListener, DatabaseListener, OnItemClickListener {

	private static final int DELETE = 0;
	private ListView mList;
	private Database mDb;

	@Override
	public void onClick(View v) {
		new NewGameDialog(this, mDb).show();
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
				mDb.deleteGame(info.id+1);
				break;
		}
		return super.onContextItemSelected(item);
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
		mList.setOnItemClickListener(this);
		mList.setEmptyView(findViewById(android.R.id.empty));
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
		 * rename game
		 */
		menu.add(0, DELETE, Menu.FIRST, R.string.delete);
		super.onCreateContextMenu(menu, v, menuInfo);
	}

	@Override
	public void onDatabaseUpgrade() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDatabaseUpgradeComplete() {
		refresh();
	}

	@Override
	public void onDeletionComplete() {
		refresh();
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		startActivity(new Intent(this, ExaltedDice.class).putExtra(ExaltedDice.KEY_GAME_NAME, mDb.getGameName(id +1)).putExtra(ExaltedDice.KEY_GAME_ID, id+1));
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
		refresh();
		super.onResume();
	}

	private void refresh() {
		mList.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mDb.getAllGameTitles()));
	}

}
