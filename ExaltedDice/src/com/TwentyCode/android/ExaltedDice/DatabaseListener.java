/**
 * DatabaseListener.java
 * @date Feb 4, 2012
 * @author Twenty Codes, LLC
 * @author ricky barrette
 */
package com.TwentyCode.android.ExaltedDice;

/**
 * This interface will be used to listen to see when the database events are complete
 * @author ricky barrette
 */
public interface DatabaseListener {

	/**
	 * Called when a database upgrade is completed 
	 * @author ricky barrette
	 */
	public void onDatabaseUpgradeComplete();

	/**
	 * Called when a deletion is completed
	 * 
	 * @author ricky barrette
	 */
	public void onDeletionComplete();

	/**
	 * Called when a database restore is completed 
	 * @author ricky barrette
	 */
	public void onRestoreComplete();

	/**
	 * Called when a database is being upgraded 
	 * @author ricky barrette
	 */
	public void onDatabaseUpgrade();

}