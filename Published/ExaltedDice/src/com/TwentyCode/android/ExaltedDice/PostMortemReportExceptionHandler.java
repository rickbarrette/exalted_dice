package com.TwentyCode.android.ExaltedDice;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;

/**
 * dont forget the manifest tag
 * <uses-permission android:name="android.permission.READ_LOGS" />
 * @author ricky
 */
public class PostMortemReportExceptionHandler implements UncaughtExceptionHandler, Runnable {
	public static final String ExceptionReportFilename = "postmortem.trace";
	
	private static final String MSG_SUBJECT_TAG = "Exception Report"; //"app title + this tag" = email subject
	private static final String MSG_SENDTO = "twentycodes@gmail.com";    //email will be sent to this account
	//the following may be something you wish to consider localizing
	private static final String MSG_BODY = "Just click send to help make this application better. "+
		"No personal information is being sent (you can check by reading the rest of the email).";
	
	private Thread.UncaughtExceptionHandler mDefaultUEH;
	private Activity mApp = null;
	
	public PostMortemReportExceptionHandler(Activity aApp) {
		mDefaultUEH = Thread.getDefaultUncaughtExceptionHandler();
	    mApp = aApp;
	 }

	public String getDebugReport(Throwable aException) {
		
//		NumberFormat theFormatter = new DecimalFormat("#0.");
		//stack trace
		StackTraceElement[] theStackTrace = aException.getStackTrace();
		
		StringBuffer report = new StringBuffer();
		
		report.append("--------- Application ---------\n\n");
		
		report.append(mApp.getPackageName()+" generated the following exception:\n\n");
		
		report.append(aException.toString() + "\n\n");
		
		report.append("-------------------------------\n\n");
		
		report.append("--------- Stack trace ---------\n\n");
		for (int i = 0; i < theStackTrace.length; i++) {
			report.append("    " + theStackTrace[i].toString() + "\n");
		}
		report.append("-------------------------------\n\n");
			
		//app environment
		PackageManager pm = mApp.getPackageManager();
		PackageInfo pi;
		try {
			pi = pm.getPackageInfo(mApp.getPackageName(), 0);
		} catch (NameNotFoundException eNnf) {
			//doubt this will ever run since we want info about our own package
			pi = new PackageInfo();
			pi.versionName = "unknown";
			pi.versionCode = 69;
		}
		
		Date theDate = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd_HH.mm.ss_zzz");
		report.append("-------- Environment --------\n");
		report.append("Time\t="+sdf.format(theDate)+"\n");
		report.append("Device\t="+Build.FINGERPRINT+"\n");
		try {
			Field theMfrField = Build.class.getField("MANUFACTURER");
			report.append("Make\t="+theMfrField.get(null)+"\n");
		} catch (SecurityException e) {
		} catch (NoSuchFieldException e) {
		} catch (IllegalArgumentException e) {
		} catch (IllegalAccessException e) {
		}
		report.append("Device: " + Build.DEVICE + "\n");
		report.append("Brand: " + Build.BRAND + "\n");
		report.append("Model: "+Build.MODEL+"\n");
		report.append("Product: "+Build.PRODUCT+"\n");
		report.append("App:\t "+mApp.getPackageName()+", version "+pi.versionName+" (build "+pi.versionCode+")\n");
		report.append("Locale: "+mApp.getResources().getConfiguration().locale.getDisplayName()+"\n");
		report.append("-----------------------------\n\n");
		
		report.append("--------- Firmware ---------\n\n");
		report.append("SDK: " + Build.VERSION.SDK + "\n");
		report.append("Release: " + Build.VERSION.RELEASE + "\n");
		report.append("Incremental: " + Build.VERSION.INCREMENTAL + "\n");
		report.append("Build Id: " + Build.ID + "\n");
		report.append("-------------------------------\n\n");

		// If the exception was thrown in a background thread inside
		// AsyncTask, then the actual exception can be found with getCause
		report.append("--------- Cause ---------\n\n");
		Throwable cause = aException.getCause();
		if (cause != null) {
			report.append(cause.toString() + "\n\n");
			theStackTrace = cause.getStackTrace();
			for (int i = 0; i < theStackTrace.length; i++) {
				report.append("    " + theStackTrace[i].toString() + "\n");
			}
		}
		report.append("-------------------------------\n\n");

		report.append("--------- Complete Logcat ---------\n\n");
		report.append(getLog().toString());
		report.append("-------------------------------\n\n");
		
		report.append("END REPORT");
		
		return report.toString();
	}
	
	protected StringBuilder getLog(){
        final StringBuilder log = new StringBuilder();
        try{
            Process process = Runtime.getRuntime().exec("logcat -d");
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            
            String line;
            while ((line = bufferedReader.readLine()) != null){ 
                log.append(line);
                log.append("\n"); 
            }
        } 
        catch (IOException e){
        }
        return log;
    }
	
	public void run() {
		sendDebugReportToAuthor();
	}
	
	protected void saveDebugReport(String aReport) {
		//save report to file
		try {
			FileOutputStream theFile = mApp.openFileOutput(ExceptionReportFilename, Context.MODE_PRIVATE);
			theFile.write(aReport.getBytes());
			theFile.close();
		} catch(IOException ioe) {
			//error during error report needs to be ignored, do not wish to start infinite loop
		}		
	}
	
	public void sendDebugReportToAuthor() {
		String theLine = "";
		StringBuffer theTrace = new StringBuffer();
		try {
			BufferedReader theReader = new BufferedReader(
					new InputStreamReader(mApp.openFileInput(ExceptionReportFilename)));
			while ((theLine = theReader.readLine())!=null) {
				theTrace.append(theLine+"\n");
			}
			if (sendDebugReportToAuthor(theTrace.toString())) {
				mApp.deleteFile(ExceptionReportFilename);
			}
		} catch (FileNotFoundException eFnf) {
			// nothing to do
		} catch(IOException eIo) {
			// not going to report
		}		
	}
	
	public Boolean sendDebugReportToAuthor(String aReport) {
		if (aReport!=null) {
			Intent theIntent = new Intent(Intent.ACTION_SEND);
			String theSubject = mApp.getTitle()+" "+MSG_SUBJECT_TAG;
			String theBody = "\n"+MSG_BODY+"\n\n"+aReport+"\n\n";
			theIntent.putExtra(Intent.EXTRA_EMAIL,new String[] {MSG_SENDTO});
			theIntent.putExtra(Intent.EXTRA_TEXT, theBody);
			theIntent.putExtra(Intent.EXTRA_SUBJECT, theSubject);
			theIntent.setType("message/rfc822");
			Boolean hasSendRecipients = (mApp.getPackageManager().queryIntentActivities(theIntent,0).size()>0);
			if (hasSendRecipients) {
				mApp.startActivity(theIntent);
				return true;
			} else {
				return false;
			}
		} else {
			return true;
		}
	}
	
	public void submit(Throwable e) {
		String theErrReport = getDebugReport(e);
		saveDebugReport(theErrReport);
		//try to send file contents via email (need to do so via the UI thread)
		mApp.runOnUiThread(this);				
	}

    public void uncaughtException(Thread t, Throwable e) {
		submit(e);
		//do not forget to pass this exception through up the chain
		mDefaultUEH.uncaughtException(t,e);
	}
}