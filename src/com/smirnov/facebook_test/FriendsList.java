package com.smirnov.facebook_test;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.android.R;

public class FriendsList extends Activity implements OnItemClickListener {

	private Handler h;
	private ProgressDialog dialog;
	protected ListView friendsList;
	protected static JSONArray jsonArray;
	public String url; 

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		h = new Handler();
		setContentView(R.layout.friends_list);

		Bundle extras = getIntent().getExtras();
		String apiResponse = extras.getString("API_RESPONSE");

		try {
			jsonArray = new JSONObject(apiResponse).getJSONArray("data");
		} catch (JSONException e) {
			showToast("Error: " + e.getMessage());
			return;
		}

		friendsList = (ListView) findViewById(R.id.friends_list);
		friendsList.setOnItemClickListener(this);
		friendsList.setAdapter(new FriendsLA(this));
	}

	public void onItemClick(AdapterView<?> arg0, View v, int position, long arg3) {
		try {
			dialog = ProgressDialog.show(FriendsList.this, "",
					getString(R.string.please_wait), true, true);
			Long friendId = jsonArray.getJSONObject(position).getLong("id");
			url = friendId + "/feed";
			Utility.ar.request(url, new FeedOtherRL());

		} catch (JSONException e) {
			showToast("Error: " + e.getMessage());
		}
	}

	public class FeedOtherRL extends ProtoRequestListener {
		public void onComplete(String response, Object state) {
			dialog.dismiss();
			
			Intent intent = new Intent(getApplicationContext(),
					FeedsList.class);
			intent.putExtra("API_RESPONSE", response);
			intent.putExtra("URL", url);
			startActivity(intent);
		}

	}

	public void showToast(final String msg) {
		h.post(new Runnable() {
			public void run() {
				Toast.makeText(FriendsList.this, msg, Toast.LENGTH_LONG).show();
			}
		});
	}

	public class FriendsLA extends BaseAdapter {
		private LayoutInflater inflater;
		FriendsList friendsList;

		public FriendsLA(FriendsList friendsList) {
			this.friendsList = friendsList;
			if (Utility.model == null) {
				Utility.model = new GetProfilePics();
			}

			Utility.model.setListener(this);
			inflater = LayoutInflater.from(friendsList.getBaseContext());
		}

		public int getCount() {
			return jsonArray.length();
		}

		public Object getItem(int position) {
			return null;
		}

		public long getItemId(int position) {
			return 0;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			JSONObject jsonObject = null;
			try {
				jsonObject = jsonArray.getJSONObject(position);
			} catch (JSONException e1) {
				e1.printStackTrace();
			}

			View hView = convertView;
			if (convertView == null) {
				hView = inflater.inflate(R.layout.friends_item, null);
				ViewHolder holder = new ViewHolder();
				holder.profile_pic = (ImageView) hView
						.findViewById(R.id.profile_pic);
				holder.name = (TextView) hView.findViewById(R.id.name);
				holder.info = (TextView) hView.findViewById(R.id.info);
				hView.setTag(holder);
			}

			ViewHolder holder = (ViewHolder) hView.getTag();
			try {
				holder.profile_pic.setImageBitmap(Utility.model.getImage(
						jsonObject.getString("id"),
						jsonObject.getJSONObject("picture")
								.getJSONObject("data").getString("url")));
			} catch (JSONException e) {
				holder.name.setText("");
			}
			try {
				holder.name.setText(jsonObject.getString("name"));
			} catch (JSONException e) {
				holder.name.setText("");
			}
			try {
				holder.info.setText(jsonObject.getJSONObject("location")
						.getString("name"));
			} catch (JSONException e) {
				holder.info.setText("");
			}
			return hView;
		}
	}
}
