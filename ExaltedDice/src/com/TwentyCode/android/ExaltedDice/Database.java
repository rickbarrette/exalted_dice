/**
 * Database.java
 * @date Feb 4, 2012
 * @author Twenty Codes, LLC
 * @author ricky barrette
 */
package com.TwentyCode.android.ExaltedDice;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

/**
 * This class will be the main interface between Exalted Dice and it's database
 * @author ricky barrette
 */
public class Database {
	
	/**
	 * database version. If this is increased, the database will be upgraded the next time it connects
	 */
	private final int DATABASE_VERSION = 1;

	/*
	 * database file name
	 */
	private final String DATABASE_NAME = "history.db";
	
	/*
	 * database tables
	 */
	private final String GAME_NAME_TABLE = "games";
	private final String GAME_OPTIONS_TABLE = "game_options";
	private final String GAME_HISTORY_TABLE = "history";
	
	/*
	 * Database keys 
	 */
	private final String KEY = "key";
	private final String KEY_VALUE = "value";
	
	/*
	 * database value keys
	 */
	public final static String KEY_NAME = "name";
	public final static String KEY_D_TYPE = "d_type";
	public final static String KEY_NUMBER = "number";
	public final static String KEY_LOG = "log";
	public final static String KEY_ROLL_ID = "roll_id";
	public final static String KEY_MOD = "mod";
	public static final String KEY_ROLLED = "rolled";
	public static final String KEY_MODE = "mode";
//	public static final String KEY_ = "";
//	public static final String KEY_ = "";
//	public static final String KEY_ = "";
//	public static final String KEY_ = "";
//	public static final String KEY_ = "";
//	public static final String KEY_ = "";
	
	private static final String TAG = "Database";

	private Context mContext;
	private SQLiteDatabase mDb;
	public boolean isUpgrading = false;	
	private DatabaseListener mListener;

	/**
	 * A helper class to manage database creation and version management.
	 * @author ricky barrette
	 */
	private class OpenHelper extends SQLiteOpenHelper {
	

		/**
		 * Creates a new OpenHelper
		 * @param context
		 * @author ricky barrette
		 */
		public OpenHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}
	
		/**
		 * Creates the initial database structure 
		 * @param db
		 * @author ricky barrette
		 */
		private void createDatabase(SQLiteDatabase db){
			db.execSQL("CREATE TABLE " + GAME_NAME_TABLE + 
					"(id INTEGER PRIMARY KEY, " +
					KEY_NAME+" TEXT, " +
					KEY_ROLL_ID + " INTEGER)");
			
			db.execSQL("CREATE TABLE " + GAME_HISTORY_TABLE + 
					"(id INTEGER PRIMARY KEY, " +
					KEY_NAME+" TEXT, " +
					KEY_ROLL_ID + " TEXT, "+
					KEY+" TEXT, " +
					KEY_VALUE+" INTEGER)");
			
			db.execSQL("CREATE TABLE " + GAME_OPTIONS_TABLE + 
					"(id INTEGER PRIMARY KEY, " +
					KEY_NAME+" TEXT, " +
					KEY+" TEXT, " +
					KEY_VALUE+" INTEGER)");
		}
		
		/**
		 * called when the database is created for the first time. this will create our game database
		 * (non-Javadoc)
		 * @see android.database.sqlite.SQLiteOpenHelper#onCreate(android.database.sqlite.SQLiteDatabase)
		 * @author ricky barrette
		 */
		@Override
		public void onCreate(SQLiteDatabase db) {
			createDatabase(db);
		}
			
		/**
		 * called when the database needs to be updated
		 * (non-Javadoc)
		 * @see android.database.sqlite.SQLiteOpenHelper#onUpgrade(android.database.sqlite.SQLiteDatabase, int, int)
		 * @author ricky barrette
		 */
		@Override
		public void onUpgrade(final SQLiteDatabase db, final int oldVersion, int newVersion) {
			Log.w(TAG, "Upgrading database from version "+oldVersion+" to "+newVersion);
			
			if(Database.this.mListener != null)
				Database.this.mListener.onDatabaseUpgrade();
			
			Database.this.isUpgrading = true;
			
			final Handler handler =  new Handler(){
				@Override
			    public void handleMessage(Message msg) {
					if(Database.this.mListener != null)
						Database.this.mListener.onDatabaseUpgradeComplete();
			    }
			};
	    	
	    	//upgrade thread
			 new Thread( new Runnable(){
				 @Override
				 public void run(){
					 Looper.prepare();
					switch(oldVersion){
						case 1:
							// upgrade from 1 to 2
						case 2:
							//upgrade from 2 to 3
						case 3:
							//upgrade from 3 to 4
					}
					handler.sendEmptyMessage(0);					
					Database.this.isUpgrading = false;
				}
			 }).start();
		}
	}

	/**
	 * Creates a new Database
	 * @param context
	 * @author ricky barrette
	 */
	public Database(Context context){
		this.mContext = context;
		this.mDb = new OpenHelper(this.mContext).getWritableDatabase();
	}
	
	/**
	 * Creates a new Database
	 * @param context
	 * @param listener
	 * @author ricky barrette
	 */
	public Database(Context context, DatabaseListener listener){
		this.mListener = listener;		
		this.mContext = context;
		this.mDb = new OpenHelper(this.mContext).getWritableDatabase();
	}
	
	/**
	 * Backs up the database to the user's external storage
	 * @return true if successful
	 * @author ricky barrette
	 */
	public boolean backup(){
		File dbFile = new File(Environment.getDataDirectory() + "/data/"+mContext.getPackageName()+"/databases/"+DATABASE_NAME);

		File exportDir = new File(Environment.getExternalStorageDirectory(), "/"+this.mContext.getString(R.string.app_name));
		if (!exportDir.exists()) {
			exportDir.mkdirs();
		}
		File file = new File(exportDir, dbFile.getName());

		try {
			file.createNewFile();
			this.copyFile(dbFile, file);
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	/**
	 * Checks to see if this ringer name is original, if not it renames it
	 * @param name
	 * @return
	 */
	private String checkName(String name){
		List<String> names = this.getAllGameTitles();
		String ringerName = name;
		int count = 1;
		for(int index = 0; index < names.size(); index++ ){
			 if(ringerName.equals(names.get(index))){
				 ringerName = name + count+++"";
				 index = 0;
			 }
		}
		return ringerName;
	}
	
	/**
	 * Clears the history of a game by it's id
	 * @param gameId
	 * @author ricky barrette
	 */
	public void clearHistory(final long gameId) {
		final ProgressDialog progress = ProgressDialog.show(Database.this.mContext, "", Database.this.mContext.getText(R.string.deleteing), true, true);
		
		final Handler handler =  new Handler(){
			@Override
		    public void handleMessage(Message msg) {
				if(Database.this.mListener != null)
					Database.this.mListener.onDeletionComplete();
					progress.dismiss();
		    }
		};
    	
    	//game deleting thread
		 new Thread( new Runnable(){
			 @Override
			 public void run(){
				 Looper.prepare();
		
				/*
				 * get the game name from the id, and then delete all its information from the game history table
				 */
				Database.this.mDb.delete(GAME_HISTORY_TABLE, KEY_NAME +" = "+ DatabaseUtils.sqlEscapeString(getGameName(gameId)), null);
				
				/*
				 * update the game table
				 */
				ContentValues game= new ContentValues();
				
				//store the current roll
				game.put(KEY_ROLL_ID, 0);
				mDb.update(GAME_NAME_TABLE, game, "id" + "= "+ gameId, null);

				
				handler.sendEmptyMessage(0);
			 }
		 }).start();		
	}
	
	/**
	 * Closes the database 
	 * @author ricky barrette
	 */
	public void close() {
		mDb.close();
	}

	/**
	 * Copies a file
	 * @param src file
	 * @param dst file
	 * @throws IOException
	 * @author ricky barrette
	 */
	private void copyFile(File src, File dst) throws IOException {
        FileChannel inChannel = new FileInputStream(src).getChannel();
        FileChannel outChannel = new FileOutputStream(dst).getChannel();
        try {
           inChannel.transferTo(0, inChannel.size(), outChannel);
        } finally {
           if (inChannel != null)
              inChannel.close();
           if (outChannel != null)
              outChannel.close();
        }
     }

	/**
	 * deletes a game by its row id
	 * @param id
	 * @author ricky barrette
	 */
	public void deleteGame(final long id) {
		
		final ProgressDialog progress = ProgressDialog.show(Database.this.mContext, "", Database.this.mContext.getText(R.string.deleteing), true, true);
		
		final String gameName = DatabaseUtils.sqlEscapeString(getGameName(id));
		
		mContext.getSharedPreferences(gameName, Context.MODE_WORLD_WRITEABLE).edit().clear().commit();

		final Handler handler =  new Handler(){
			@Override
		    public void handleMessage(Message msg) {
				if(Database.this.mListener != null)
					Database.this.mListener.onDeletionComplete();
					progress.dismiss();
		    }
		};
    	
    	//game deleting thread
		 new Thread( new Runnable(){
			 @Override
			 public void run(){
				Looper.prepare();
		
				Database.this.mDb.delete(GAME_HISTORY_TABLE, KEY_NAME +" = "+ gameName, null);
				Database.this.mDb.delete(GAME_OPTIONS_TABLE, KEY_NAME +" = "+ gameName, null);
				
				/*
				 * finally delete the game from the game table
				 */
				Database.this.mDb.delete(GAME_NAME_TABLE, "id = "+ id, null);
				updateRowIds(id +1);
				handler.sendEmptyMessage(0);
			 }
		 }).start();
	}
	
	/**
	 * Deletes a roll from the roll history
	 * @param gameId
	 * @param rollId
	 * @author ricky barrette
	 */
	public void deleteRoll(long gameId, long rollId) {
		// TODO Auto-generated method stub
	}
	
	/**
	 * Generates a new ContentValues Object containing the game name, a key, and it's value
	 * @param gameName
	 * @param key
	 * @param value
	 * @return ContentValues Object containing the game name, a key, and it's value
	 * @author ricky barrette
	 */
	private ContentValues generateContentValues(String gameName, String key, Object value) {
		ContentValues values = new ContentValues();
		values.put(KEY_NAME, gameName);
		values.put(KEY, key);
		try {
			values.put(KEY_VALUE, (String) value);
		} catch (ClassCastException e) {
			try {
				values.put(KEY_VALUE, (Boolean) value ? 1 : 0);
			} catch (ClassCastException e1) {
				values.put(KEY_VALUE, (Integer) value);
			}
		}
		return values;
	}
	
	/**
	 * @return a cursor containing all game names
	 * @author ricky barrette
	 */
	public Cursor getAllGames(){
		return this.mDb.query(GAME_NAME_TABLE, new String[] { KEY_NAME }, null, null, null, null, null);
	}
	
	/**
	 * returns all game names in the database, where or not if they are enabled
	 * @return list of all strings in the database table
	 * @author ricky barrette
	 */
	public List<String> getAllGameTitles() {
		List<String> list = new ArrayList<String>();
		Cursor cursor = getAllGames();;
		if (cursor.moveToFirst()) {
			do {
				list.add(cursor.getString(0));
			} while (cursor.moveToNext());
		}
		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
		}
		return list;
	}

	/**
	 * gets a game name from a row id;
	 * @param id
	 * @return cursor containing the note
	 * @author ricky barrette
	 */
	public Cursor getGameFromId(long id) {
		return this.mDb.query(GAME_NAME_TABLE, new String[]{ KEY_NAME }, "id = "+id, null, null, null, null);
	}
	
	/**
	 * gets roll log from the games's history info from the supplied game name and roll id
	 * @param gameName
	 * @param rollId
	 * @return
	 * @author ricky barrette
	 */
	public ContentValues getGameHistoryInfo(String gameName, int rollId){
    	return parseCursor(this.mDb.query(GAME_HISTORY_TABLE, new String[]{ KEY, KEY_VALUE }, KEY_NAME +" = "+ DatabaseUtils.sqlEscapeString(gameName) +" AND "+ KEY_ROLL_ID+" = "+rollId, null, null, null, null));
	}
	
	/**
	 * Retrieves the game's name form the game name table
	 * @param id
	 * @return game's name
	 * @author ricky barrette
	 */
	public String getGameName(long id) {
		Cursor cursor = this.mDb.query(GAME_NAME_TABLE, new String[]{ KEY_NAME }, "id = "+id, null, null, null, null);; 
		if (cursor.moveToFirst()) {
			return cursor.getString(0);
		}
		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
		}
		return null;
	}
	
	/**
	 * Retrieves the game options from the database
	 * @param gameId
	 * @return a ContentValues object containing the game's key/value pairs
	 * @author ricky barrette
	 */
	public ContentValues getGameOptions(long gameId){
		return parseCursor(mDb.query(GAME_OPTIONS_TABLE, new String[]{ KEY, KEY_VALUE }, KEY_NAME +" = "+ DatabaseUtils.sqlEscapeString(getGameName(gameId)), null, null, null, null));
	}
	
	/**
	 * Retrieves the number of rolls for this game
	 * @param id
	 * @return roll count
	 * @author ricky barrette
	 */
	public int getGameRollCount(long id) {
		int rolls  = -1;
		Cursor cursor = this.mDb.query(GAME_NAME_TABLE, new String[]{ KEY_ROLL_ID }, "id = "+id, null, null, null, null);; 
		if (cursor.moveToFirst()) {
			rolls = Integer.parseInt(cursor.getString(0));
		}
		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
		}
		return rolls;
	}

	/**
	 * Inserts a new game into the database
	 * @param game values
	 * @param gameHistory values
	 * @author ricky barrette
	 * @return 
	 */
	public long insertGame(String gameName){
		ContentValues game = new ContentValues();		
		game.put(Database.KEY_NAME, checkName(gameName));
		game.put(Database.KEY_ROLL_ID, 0);
		return mDb.insert(GAME_NAME_TABLE, null, game);
	}
	
	/**
	 * @return true if the database is open
	 * @author ricky barrette
	 */
	public boolean isOpen(){
		return mDb.isOpen();
	}

	/**
	 * Parses a string boolean from the database
	 * @param bool
	 * @return true or false
	 * @author ricky barrette
	 */
	public static boolean parseBoolean(String bool){
		try {
			return bool == null ? false : Integer.parseInt(bool) == 1 ? true : false;
		} catch (NumberFormatException e) {
			return false;
		}
	}
	
	/**
	 * Parses a cursor containing key value pairs
	 * @param query
	 * @return
	 * @author ricky barrette
	 */
	private ContentValues parseCursor(Cursor query) {
		ContentValues values = new ContentValues();
		if (query.moveToFirst()) {
			do {
				values.put(query.getString(query.getColumnIndex(KEY)), query.getString(query.getColumnIndex(KEY_VALUE)));
			} while (query.moveToNext());
		}
		if (query != null && !query.isClosed()) {
			query.close();
		}
		return values;
	}
	
	/**
	 * Restores the database from external storage
	 * @return true if successful
	 * @author ricky barrette
	 */
	public void restore(){
		File dbFile = new File(Environment.getDataDirectory() + "/data/"+mContext.getPackageName()+"/databases/"+DATABASE_NAME);

		File exportDir = new File(Environment.getExternalStorageDirectory(), "/"+this.mContext.getString(R.string.app_name));
		if (!exportDir.exists()) {
			exportDir.mkdirs();
		}
		File file = new File(exportDir, dbFile.getName());

		try {
			file.createNewFile();
			this.copyFile(file, dbFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
		/*
		 * close and reopen the database to upgrade it.
		 */
		this.mDb.close();
		this.mDb = new OpenHelper(this.mContext).getWritableDatabase();
		if(this.mDb.isOpen() && ! this.isUpgrading)
			if(this.mListener != null)
				this.mListener.onRestoreComplete();
	}

	/**
	 * updates a game's history by it's id
	 * @param id
	 * @param gameName
	 * @param gameHistory
	 * @param rollId
	 * @throws NullPointerException
	 * @author ricky barrette
	 */
	public void updateGame(long id, String gameName, ContentValues gameHistory, int rollId) throws NullPointerException{
		
		ContentValues game= new ContentValues();
		
		//store the current roll
		game.put(KEY_ROLL_ID, rollId);
		
		if(gameName == null || gameHistory == null)
			throw new NullPointerException("game content was null");
		
		/*
		 * insert the history information values in the info table
		 */
		for(Entry<String, Object> item : gameHistory.valueSet()){
			ContentValues values = generateContentValues(gameName, item.getKey(), item.getValue());
			values.put(KEY_ROLL_ID, rollId);
			mDb.insert(GAME_HISTORY_TABLE, null, values);
		}
		
		/*
		 * update the game table
		 */
		mDb.update(GAME_NAME_TABLE, game, "id" + "= "+ id, null);
		
		if(mListener != null)
			mListener.onDatabaseInsertComplete();
	}
	
	/**
	 * updates a game's options by it's id
	 * @param id
	 * @param options values
	 * @author ricky barrette
	 */
	public void updateOptions(long id, ContentValues options) throws NullPointerException{
		String gameName = getGameName(id);
		
		if(gameName == null || options == null)
			throw new NullPointerException("game content was null");
		
		/*
		 * update the information values in the info table
		 */
		for(Entry<String, Object> item : options.valueSet()){
			ContentValues values = generateContentValues(gameName, item.getKey(), item.getValue());			
			
			/*
			 * try to update existing information, if not, insert the changes
			 */
			if(!(mDb.update(GAME_OPTIONS_TABLE, values, KEY_NAME + "="+ DatabaseUtils.sqlEscapeString(gameName) +" AND " + KEY +"='"+ item.getKey()+"'", null) > 0))
				mDb.insert(GAME_OPTIONS_TABLE, null, values);
		}
		
		if(mListener != null)
			mListener.onDatabaseInsertComplete();
	}

	/**
	 * Updates the row ids after a row is deleted
	 * @param id of the row to start with
	 * @author ricky barrette
	 */
	private void updateRowIds(long id) {
		long currentRow;
		ContentValues values = new ContentValues();
		Cursor cursor = this.mDb.query(GAME_NAME_TABLE, new String[] { "id" },null, null, null, null, null);
		if (cursor.moveToFirst()) {
			do {
				currentRow = cursor.getLong(0);
				if(currentRow == id){
					id++;
					values.clear();
					values.put("id", currentRow -1);
					mDb.update(GAME_NAME_TABLE, values, "id" + "= "+ currentRow, null);
				}
			} while (cursor.moveToNext());
		}
		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
		}
	}
}