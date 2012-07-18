/**
 * GameListActivity.java
 * @date Feb 4, 2012
 * @author ricky barrette
 * @author Twenty Codes, LLC
 */
package com.TwentyCode.android.ExaltedDice;

import com.TwentyCodes.android.exception.ExceptionHandler;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
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
import android.widget.ArrayAdapter;
import android.widget.ListView;

/**
 * This activity will be used to display a list of games to the user.
 * 
 * @author ricky barrette
 */
public class GameListActivity extends Activity implements OnClickListener, DatabaseListener, OnItemClickListener {

	private ListView mList;
	private Database mDb;

	@Override
	public void onClick(View v) {
		if(!mDb.isOpen())
			mDb = new Database(this, this);
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
			case R.id.menu_delete:
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
		Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(this));
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
		MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.game_list_context_menu, menu);
		super.onCreateContextMenu(menu, v, menuInfo);
	}

	/**
	 * (non-Javadoc)
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.game_list_menu, menu);
		return true;
	}

	@Override
	public void onDatabaseInsertComplete() {
		// TODO Auto-generated method stub
		
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
		startActivity(new Intent(this, ExaltedDice.class)
		.putExtra(ExaltedDice.KEY_GAME_NAME, mDb.getGameName(id +1))
		.putExtra(ExaltedDice.KEY_GAME_ID, id+1)
		.putExtra(ExaltedDice.KEY_GAME_MODE, mDb.getGameOptions(id+1).getAsString(Database.KEY_MODE)));
	}

	/**
	 * (non-Javadoc)
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()){
			case R.id.menu_settings:
				startActivity(new Intent(this, Settings.class));
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
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
