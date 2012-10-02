package com.smirnov.facebook_test;

import java.util.Iterator;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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
	public ProgressDialog dialog;

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
			Utility.ar.request("me/permissions", new PermissionsRL());
		} else {
			checkLoginButton();
		}
	}
	
	@Override
	protected void onPause() {
		super.onPause();
	}

	public void checkLoginButton() {

		h.post(new Runnable() {
			public void run() {
				if (Utility.fb.isSessionValid()) {
					loginButton.setText(R.string.logout);
					requestUserData();
				} else {
					loginButton.setText(R.string.login);
				}
			}
		});

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

	public void friends(View view) {
		dialog = ProgressDialog.show(MainMenu.this, "",
				getString(R.string.please_wait), true, true);

		Bundle params = new Bundle();
		params.putString("fields", "name, picture, location");
		Utility.ar.request("me/friends", params, new FriendsRL());
	}

	public void feed(View view) {
		dialog = ProgressDialog.show(MainMenu.this, "",
				getString(R.string.please_wait), true, true);
		
		Bundle params = new Bundle();
		params.putInt("limit", Utility.limit);

		Utility.ar.request("me/home", params, new FeedRL());
	}

	public class LoadRL extends ProtoRequestListener {

		public void onComplete(String response, Object state) {
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

	public class FriendsRL extends ProtoRequestListener {

		public void onComplete(String response, final Object state) {
			dialog.dismiss();

			Intent intent = new Intent(getApplicationContext(),
					FriendsList.class);
			intent.putExtra("API_RESPONSE", response);
			startActivity(intent);

		}

		public void onFacebookError(FacebookError error) {
			dialog.dismiss();
			Toast.makeText(getApplicationContext(),
					"Facebook error: " + error.getMessage(), Toast.LENGTH_SHORT)
					.show();
		}
	}

	public class FeedRL extends ProtoRequestListener {

		public void onComplete(String response, Object state) {
			dialog.dismiss();
			
			Intent intent = new Intent(getApplicationContext(), FeedsList.class);
			intent.putExtra("API_RESPONSE", response);
			intent.putExtra("URL", "me/home");
			startActivity(intent);
		}

	}

	public class PermissionsRL extends ProtoRequestListener {

		// check permissions
		public void onComplete(String response, Object state) {
			JSONObject jsonArray;
			Iterator<?> jsonKeys;
			int lengthPermissions = Utility.permissions.length, lengthJson = 0;

			try {
				jsonArray = new JSONObject(response).getJSONArray("data")
						.getJSONObject(0);
				jsonKeys = jsonArray.keys();
				String temp;

				while (jsonKeys.hasNext()) {
					temp = (String) jsonKeys.next();

					if (jsonArray.getString(temp).equals("1")
							&& Utility.keyExists(temp)) {
						lengthJson++;
					}
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}

			if (lengthPermissions != lengthJson) {
				h.post(new Runnable() {
					public void run() {
						Toast.makeText(getBaseContext(),
								R.string.check_permission, Toast.LENGTH_SHORT)
								.show();
						SessionStore.clear(getBaseContext());
						SessionStore.restore(Utility.fb, getBaseContext());
					}
				});
			}

			checkLoginButton();
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
