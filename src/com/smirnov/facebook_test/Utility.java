package com.smirnov.facebook_test;

import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.net.http.AndroidHttpClient;
import android.provider.MediaStore;

import com.facebook.android.AsyncFacebookRunner;
import com.facebook.android.Facebook;
import com.smirnov.facebook_test.MainMenu.AuthListener;


public class Utility {
	public static Facebook fb;
	public static AsyncFacebookRunner ar;
	public static String LOG_ID = "FB-Smirnov";
	public static String userUID = null;
	public static String[] permissions = { "publish_checkins" };
	public static AndroidHttpClient httpclient = null;
	public static AuthListener al;
	private static final int MAX_IMAGE_DIMENSION = 720;

	public static Bitmap getBitmap(String url) {
		Bitmap b = null;
		InputStream is = null;
		try {
			URL aURL = new URL(url);
			URLConnection conn = aURL.openConnection();
			conn.connect();
			is = conn.getInputStream();
			b = BitmapFactory.decodeStream(new FlushedInputStream(is));

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (httpclient != null) {
                httpclient.close();
            }

			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		return b;
	}

	public static class FlushedInputStream extends FilterInputStream {
		public FlushedInputStream(InputStream inputStream) {
			super(inputStream);
		}

		@Override
		public long skip(long n) throws IOException {
			long totalBytesSkipped = 0L;
			while (totalBytesSkipped < n) {
				long bytesSkipped = in.skip(n - totalBytesSkipped);
				if (bytesSkipped == 0L) {
					int b = read();
					if (b < 0) {
						break;
					} else {
						bytesSkipped = 1;
					}
				}
				totalBytesSkipped += bytesSkipped;
			}
			return totalBytesSkipped;
		}

		public static byte[] scaleImage(Context context, Uri photoUri)
				throws IOException {
			InputStream in = context.getContentResolver().openInputStream(
					photoUri);
			BitmapFactory.Options dbo = new BitmapFactory.Options();
			dbo.inJustDecodeBounds = true;
			BitmapFactory.decodeStream(in, null, dbo);
			in.close();

			int rotatedWidth, rotatedHeight;
			int orientation = getOrientation(context, photoUri);

			if (orientation == 90 || orientation == 270) {
				rotatedWidth = dbo.outHeight;
				rotatedHeight = dbo.outWidth;
			} else {
				rotatedWidth = dbo.outWidth;
				rotatedHeight = dbo.outHeight;
			}

			Bitmap srcBitmap;
			in = context.getContentResolver().openInputStream(photoUri);
			if (rotatedWidth > MAX_IMAGE_DIMENSION
					|| rotatedHeight > MAX_IMAGE_DIMENSION) {
				float widthRatio = ((float) rotatedWidth)
						/ ((float) MAX_IMAGE_DIMENSION);
				float heightRatio = ((float) rotatedHeight)
						/ ((float) MAX_IMAGE_DIMENSION);
				float maxRatio = Math.max(widthRatio, heightRatio);

				BitmapFactory.Options options = new BitmapFactory.Options();
				options.inSampleSize = (int) maxRatio;
				srcBitmap = BitmapFactory.decodeStream(in, null, options);
			} else {
				srcBitmap = BitmapFactory.decodeStream(in);
			}

			in.close();

			if (orientation > 0) {
				Matrix matrix = new Matrix();
				matrix.postRotate(orientation);

				srcBitmap = Bitmap.createBitmap(srcBitmap, 0, 0,
						srcBitmap.getWidth(), srcBitmap.getHeight(), matrix,
						true);
			}

			String type = context.getContentResolver().getType(photoUri);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			if (type.equals("image/png")) {
				srcBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
			} else if (type.equals("image/jpeg") || type.equals("image/jpg")) {
				srcBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
			}
			byte[] bMapArray = baos.toByteArray();
			baos.close();
			return bMapArray;
		}

		public static int getOrientation(Context context, Uri photoUri) {
			Cursor cursor = context
					.getContentResolver()
					.query(photoUri,
							new String[] { MediaStore.Images.ImageColumns.ORIENTATION },
							null, null, null);
			
			if (cursor.getCount() != 1) {
				return -1;
			}
			
			cursor.moveToFirst();
			return cursor.getInt(0);
		}
	}

}
