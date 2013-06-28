package com.dianping.loader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONObject;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.dianping.loader.model.SiteSpec;

public class HomeActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_home);
	}

	private class Worker extends Thread {
		private String url;

		public Worker(String url) {
			this.url = url;
		}

		@Override
		public void run() {
			final String siteUrl = url;
			try {
				URL url = new URL(siteUrl);
				HttpURLConnection conn = (HttpURLConnection) url
						.openConnection();
				conn.setConnectTimeout(15000);
				// conn.setRequestProperty("User-Agent",
				// Environment.mapiUserAgent());
				InputStream ins = conn.getInputStream();
				ByteArrayOutputStream bos = new ByteArrayOutputStream(16 * 1024);
				byte[] buf = new byte[1024 * 4]; // 4k buffer
				int l;
				while ((l = ins.read(buf, 0, buf.length)) != -1) {
					bos.write(buf, 0, l);
				}
				bos.close();
				ins.close();
				conn.disconnect();

				byte[] bytes = bos.toByteArray();
				String str = new String(bytes, "UTF-8");

				// TODO:
				// verify signature

				JSONObject json = new JSONObject(str);
				final SiteSpec fSite = new SiteSpec(json);

				File dir = new File(getFilesDir(), "repo");
				dir.mkdir();
				File local = new File(dir, "site.txt");
				File tmp = new File(dir, "site_tmp");
				try {
					FileOutputStream fos = new FileOutputStream(tmp);
					fos.write(bytes);
					fos.close();
					tmp.renameTo(local);
				} catch (Exception e) {
					tmp.delete();
				}

				Log.i("loader", "site.xml updated to " + fSite.id());
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						// TODO: succeed
					}
				});
			} catch (Exception e) {
				Log.w("loader", "fail to download site from " + siteUrl, e);
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						// TODO: failed
					}
				});
			}
		}
	}

}
