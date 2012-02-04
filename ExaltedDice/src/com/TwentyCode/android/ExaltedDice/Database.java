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
					KEY_NAME+" TEXT)");
			db.execSQL("CREATE TABLE " + GAME_HISTORY_TABLE + 
					"(id INTEGER PRIMARY KEY, " +
					KEY_NAME+" TEXT, " +
					KEY+" TEXT, " +
					KEY_VALUE+" TEXT)");
		}
		
		/**
		 * called when the database is created for the first time. this will create our Ringer database
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
							//upgrade from 4 to 4
					}
					handler.sendEmptyMessage(0);					
					Database.this.isUpgrading = false;
				}
			 }).start();
		}
	}
	private static final String TAG = "Database";
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
	private Context mContext;
	private SQLiteDatabase mDb;
	
	public boolean isUpgrading = false;	

	private DatabaseListener mListener;
	
	/**
	 * database version. If this is increased, the database will be upgraded the next time it connects
	 */
	private final int DATABASE_VERSION = 1;

	/**
	 * database file name 
	 */
	private final String DATABASE_NAME = "history.db";

	/**
	 * database table for games
	 */
	private final String GAME_NAME_TABLE = "game_name";
	/**
	 * Database table of history
	 */
	private final String GAME_HISTORY_TABLE = "game_history";
	
	/*
	 * Database keys 
	 */
	private static final String KEY = "key";
	private static final String KEY_VALUE = "value";
	/*
	 * database value keys
	 */
	public final static String KEY_NAME = "name";
	public final static String KEY_D_TYPE = "d_type";
	public final static String KEY_NUMBER = "number";
		
	
	public final static String KEY_LOG = "log";

	public final static String KEY_ROLL_ID = "log_number";
	
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
		
		final Handler handler =  new Handler(){
			@Override
		    public void handleMessage(Message msg) {
				if(Database.this.mListener != null)
					Database.this.mListener.onDeletionComplete();
					progress.dismiss();
		    }
		};
    	
    	//ringer deleting thread
		 new Thread( new Runnable(){
			 @Override
			 public void run(){
				 Looper.prepare();
		
				/*
				 * get the game name from the id, and then delete all its information from the game histroy table
				 */
				Database.this.mDb.delete(GAME_HISTORY_TABLE, KEY_NAME +" = "+ DatabaseUtils.sqlEscapeString(Database.this.getGameName(id)), null);
				
				/*
				 * finally delete the ringer from the ringer table
				 */
				Database.this.mDb.delete(GAME_NAME_TABLE, "id = "+ id, null);
				updateRowIds(id +1);
				handler.sendEmptyMessage(0);
			 }
		 }).start();
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
		Cursor cursor = this.mDb.query(GAME_NAME_TABLE, new String[] { KEY_NAME }, null, null, null, null, null);
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
	 * gets a games's histrory info from the supplied ringer name
	 * @param gameName
	 * @return
	 * @author ricky barrette
	 */
	public ContentValues getGameHistoryInfo(String gameName){
		ContentValues values = new ContentValues();
    	Cursor info = this.mDb.query(GAME_HISTORY_TABLE, new String[]{ KEY, KEY_VALUE }, KEY_NAME +" = "+ DatabaseUtils.sqlEscapeString(gameName), null, null, null, null);
		if (info.moveToFirst()) {
			do {
				values.put(info.getString(0), info.getString(1));
			} while (info.moveToNext());
		}
		if (info != null && !info.isClosed()) {
			info.close();
		}
		return values;
	}
	
	/**
	 * Retrieves the game's name form the game name table
	 * @param id
	 * @return game's name
	 * @author ricky barrette
	 */
	public String getGameName(long id) {
		String name  = null;
		Cursor cursor = this.mDb.query(GAME_NAME_TABLE, new String[]{ KEY_NAME }, "id = "+id, null, null, null, null);; 
		if (cursor.moveToFirst()) {
			name = cursor.getString(0);
		}
		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
		}
		return name;
	}
	
	/**
	 * Inserts a new game into the database
	 * @param game values
	 * @param gameHistory values
	 * @author ricky barrette
	 */
	public void insertGame(String gameName, ContentValues gameHistory){
		ContentValues game = new ContentValues();		
		game.put(Database.KEY_NAME, checkName(gameName));
		mDb.insert(GAME_NAME_TABLE, null, game);
		
		if(gameHistory != null)
			//insert the information values
			for(Entry<String, Object> item : gameHistory.valueSet()){
				ContentValues values = new ContentValues();
				values.put(KEY_NAME, gameName);
				values.put(KEY, item.getKey());
				/*
				 * Try get the value.
				 * If there is a class cast exception, try casting to the next object type.
				 * 
				 * The following types are tried:
				 * String
				 * Integer
				 * Boolean
				 */
				try {
					values.put(KEY_VALUE, (String) item.getValue());
				} catch (ClassCastException e) {
					try {
						values.put(KEY_VALUE, (Boolean) item.getValue() ? 1 : 0);
					} catch (ClassCastException e1) {
						values.put(KEY_VALUE, (Integer) item.getValue());
					}
				}
				mDb.insert(GAME_HISTORY_TABLE, null, values);
			}
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
	 * updates a ringer by it's id
	 * @param id
	 * @param ringer values
	 * @param gameHistory values
	 * @author ricky barrette
	 */
	public void updateGame(long id, String gameName, ContentValues gameHistory) throws NullPointerException{
		
		ContentValues game= new ContentValues();
		
		if(gameName == null || gameHistory == null)
			throw new NullPointerException("game content was null");
		
		String ringer_name = getGameName(id);
		if(!ringer_name.equals(gameName))
			game.put(Database.KEY_NAME, checkName(gameName));
		
		/*
		 * update the information values in the info table
		 */
		for(Entry<String, Object> item : gameHistory.valueSet()){
			ContentValues values = new ContentValues();
			values.put(KEY_NAME, game.getAsString(KEY_NAME));
			values.put(KEY, item.getKey());
			try {
				values.put(KEY_VALUE, (String) item.getValue());
			} catch (ClassCastException e) {
				try {
					values.put(KEY_VALUE, (Boolean) item.getValue() ? 1 : 0);
				} catch (ClassCastException e1) {
					values.put(KEY_VALUE, (Integer) item.getValue());
				}
			}
			/*
			 * try to update, if update fails insert
			 */
			if(!(mDb.update(GAME_HISTORY_TABLE, values, KEY_NAME + "="+ DatabaseUtils.sqlEscapeString(ringer_name) +" AND " + KEY +"='"+ item.getKey()+"'", null) > 0))
				mDb.insert(GAME_HISTORY_TABLE, null, values);
		}
		
		/*
		 * update the ringer table
		 */
		mDb.update(GAME_NAME_TABLE, game, "id" + "= "+ id, null);
	}
	
	/**
	 * Updates all the roll ids after a row is deleted
	 * @param gameName
	 * @param id of the roll to start with
	 * @author ricky barrette
	 */
	private void updateRollIds(String gameName, int rollId) {
		long currentRow;
		ContentValues values = new ContentValues();
		Cursor cursor = this.mDb.query(GAME_HISTORY_TABLE, new String[] { KEY_ROLL_ID },KEY_NAME+" = "+ gameName, null, null, null, null);
		if (cursor.moveToFirst()) {
			do {
				currentRow = cursor.getLong(0);
				if(currentRow == rollId){
					rollId++;
					values.clear();
					values.put(KEY_ROLL_ID, currentRow -1);
					mDb.update(GAME_NAME_TABLE, values, KEY_ROLL_ID + "= "+ currentRow, null);
				}
			} while (cursor.moveToNext());
		}
		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
		}
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