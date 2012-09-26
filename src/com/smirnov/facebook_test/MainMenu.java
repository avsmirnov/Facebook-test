package com.smirnov.facebook_test;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.facebook.android.AsyncFacebookRunner;
import com.facebook.android.DialogError;
import com.facebook.android.Facebook;
import com.facebook.android.Facebook.DialogListener;
import com.facebook.android.FacebookError;
import com.facebook.android.R;

public class MainMenu extends Activity {
	private Handler h = new Handler();
	private TextView mText;
	private ImageView mImage;
	public Button loginButton;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_main_menu);

		/**
		 * Get existing access_token if any
		 */
		Utility.fb = new Facebook("157934057680956");
		Utility.ar = new AsyncFacebookRunner(Utility.fb);
		Utility.al = new AuthListener();
		mText = (TextView) findViewById(R.id.textView);
		mImage = (ImageView) findViewById(R.id.test_pic);
		loginButton = (Button) findViewById(R.id.login);

		SessionStore.restore(Utility.fb, this);
	}

	@Override
	protected void onStart() {
		super.onStart();

		if (Utility.fb.isSessionValid()) {

			requestUserData();
			loginButton.setText(R.string.logout);

		} else {
			loginButton.setText(R.string.login);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		Utility.fb.extendAccessTokenIfNeeded(this, null);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		Utility.fb.authorizeCallback(requestCode, resultCode, data);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main_menu, menu);
		return true;
	}

	public void login(View view) {
		if (Utility.fb.isSessionValid()) {
			Utility.al.onLogoutBegin();
			Utility.ar.logout(this, new LogoutRL());
		} else {
			Utility.fb.authorize(this, Utility.permissions, new LoginDL());
		}

	}

	public class LoadRL extends ProtoRequestListener {

		public void onComplete(String response, Object state) {
			Log.d(Utility.LOG_ID, response);
			JSONObject json;
			try {
				json = new JSONObject(response);

				JSONObject picture = json.getJSONObject("picture");
				JSONObject pData = picture.getJSONObject("data");

				final String picURL = pData.getString("url");
				final String name = json.getString("name");
				Utility.userUID = json.getString("id");

				final Bitmap b = Utility.getBitmap(picURL);

				new Thread(new Runnable() {
					public void run() {
						h.post(new Runnable() {
							public void run() {
								mText.setText("Welcome " + name + "!");
								// if
								mImage.setImageBitmap(b);
							}
						});
					}
				}).start();

			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

	}

	private final class LoginDL implements DialogListener {

		public void onComplete(Bundle values) {
			Utility.al.onLoginSuccess();
		}

		public void onFacebookError(FacebookError e) {
			Utility.al.onLoginError(e.getMessage());

		}

		public void onError(DialogError e) {
			Utility.al.onLoginError(e.getMessage());

		}

		public void onCancel() {
			Utility.al.onLoginError("Action Canceled");

		}
	}

	private class LogoutRL extends ProtoRequestListener {

		public void onComplete(String response, Object state) {

			SessionStore.clear(getBaseContext());
			h.post(new Runnable() {
				public void run() {
					Utility.al.onLogoutFinish();
				}
			});
		}

	}

	public class AuthListener {
		public void onLoginSuccess() {
			loginButton.setText(R.string.logout);
			SessionStore.save(Utility.fb, getBaseContext());
			requestUserData();
		}

		public void onLoginError(String error) {
			mText.setText("Login Failed: " + error);
		}

		public void onLogoutBegin() {
			mText.setText("Logging out...");
		}

		public void onLogoutFinish() {
			mText.setText("You have logged out! ");
			loginButton.setText(R.string.login);
			mImage.setImageBitmap(null);
		}
	}

	public void requestUserData() {
		mText.setText("Fetching user name, profile pic...");
		Bundle params = new Bundle();
		params.putString("fields", "name, picture");
		Utility.ar.request("me", params, new LoadRL());
	}

}
