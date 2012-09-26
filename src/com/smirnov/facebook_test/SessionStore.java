package com.smirnov.facebook_test;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.facebook.android.Facebook;

public class SessionStore {
	private static final String TOKEN = "access_token";
	private static final String EXPIRES = "access_expires";
	private static final String LAST_UPDATE = "last_update";
	private static final String KEY = "fb-smirnov-session";

	public static boolean save(Facebook session, Context ctx) {
		Editor editor = ctx.getSharedPreferences(KEY, Context.MODE_PRIVATE)
				.edit();
		editor.putString(TOKEN, session.getAccessToken());
		editor.putLong(EXPIRES, session.getAccessExpires());
		editor.putLong(LAST_UPDATE, session.getLastAccessUpdate());
		return editor.commit();
	}

	public static boolean restore(Facebook session, Context ctx) {
		SharedPreferences savedSession = ctx.getSharedPreferences(KEY,
				Context.MODE_PRIVATE);
		session.setTokenFromCache(savedSession.getString(TOKEN, null),
				savedSession.getLong(EXPIRES, 0),
				savedSession.getLong(LAST_UPDATE, 0));
		return session.isSessionValid();
	}
	
	public static void clear(Context ctx) {
		Editor editor = ctx.getSharedPreferences(KEY, Context.MODE_PRIVATE).edit();
		editor.clear();
		editor.commit();
	}
}
