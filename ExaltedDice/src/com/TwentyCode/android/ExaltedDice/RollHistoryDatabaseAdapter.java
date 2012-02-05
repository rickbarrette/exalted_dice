/**
 * RollHistoryDatabaseAdapter.java
 * @date Feb 5, 2012
 * @author ricky barrette
 * @author Twenty Codes, LLC
 */
package com.TwentyCode.android.ExaltedDice;

import android.content.ContentValues;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

/**
 * This adapter will be used to display the roll history from a specific game from the database
 * @author ricky barrette
 */
public class RollHistoryDatabaseAdapter extends BaseAdapter {

	private static final String TAG = "RollHistoryDatabaseAdapter";
	private long mGameId;
	private Database mDb;
	private String mGameName;
	private LayoutInflater mInflater;

	/**
	 * Creates a new RollHistoryDatabaseAdapter
	 * @author ricky barrette
	 */
	public RollHistoryDatabaseAdapter(long gameId, Database db, Context context) {
		mGameId = gameId;
		mGameName = db.getGameName(gameId);
		mDb = db;
		mInflater = LayoutInflater.from(context);
	}

	/**
	 * (non-Javadoc)
	 * @see android.widget.Adapter#getCount()
	 */
	@Override
	public int getCount() {
		Log.v(TAG, "getCount() "+mDb.getGameRollCount(mGameId));
		return mDb.getGameRollCount(mGameId);
	}

	/**
	 * (non-Javadoc)
	 * @see android.widget.Adapter#getItem(int)
	 */
	@Override
	public ContentValues getItem(int position) {
		return mDb.getGameHistoryInfo(mGameName, position+1);
	}

	/**
	 * (non-Javadoc)
	 * @see android.widget.Adapter#getItemId(int)
	 */
	@Override
	public long getItemId(int position) {
		return position;
	}

	/**
	 * (non-Javadoc)
	 * @see android.widget.Adapter#getView(int, android.view.View, android.view.ViewGroup)
	 */
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
        	convertView = mInflater.inflate(R.layout.list_row, null);

            holder = new ViewHolder();
            holder.text = (TextView) convertView.findViewById(R.id.text1);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        
        ContentValues roll = getItem(position);
        
        StringBuffer s = new StringBuffer();
        s.append("Rolled: "+roll.getAsInteger(Database.KEY_NUMBER));
        s.append(" "+(String)roll.getAsString(Database.KEY_D_TYPE));
        s.append("\n"+roll.getAsString(Database.KEY_LOG));
        
        holder.text.setText(s.toString());
        return convertView;
	}
	
	private class ViewHolder {
        TextView text;
    }

}