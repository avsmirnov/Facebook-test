package com.smirnov.facebook_test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;

import android.util.Log;

import com.facebook.android.AsyncFacebookRunner.RequestListener;
import com.facebook.android.FacebookError;


public abstract class ProtoRequestListener implements RequestListener {

	public void onFacebookError(FacebookError e, final Object state) {
		Log.e(Utility.LOG_ID, e.getMessage());
		e.printStackTrace();
	}

	public void onFileNotFoundException(FileNotFoundException e, final Object state) {
		Log.e(Utility.LOG_ID, e.getMessage());
		e.printStackTrace();
	}

	public void onIOException(IOException e, final Object state) {
		Log.e(Utility.LOG_ID, e.getMessage());
		e.printStackTrace();
	}

	public void onMalformedURLException(MalformedURLException e, final Object state) {
		Log.e(Utility.LOG_ID, e.getMessage());
		e.printStackTrace();
	}
}
