package com.dianping.app;

import android.app.Activity;
import android.app.Application;
import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;

public class MyActivity extends Activity {

	public Intent urlMap(Intent intent) {
		Application app = getApplication();
		if (app instanceof MyApplication) {
			return ((MyApplication) app).urlMap(intent);
		} else {
			return intent;
		}
	}

	@Override
	public void startActivityForResult(Intent intent, int requestCode) {
		intent = urlMap(intent);
		super.startActivityForResult(intent, requestCode);
	}

	@Override
	public void startActivityFromFragment(Fragment fragment, Intent intent,
			int requestCode) {
		intent = urlMap(intent);
		super.startActivityFromFragment(fragment, intent, requestCode);
	}

	public void startActivity(String urlSchema) {
		startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(urlSchema)));
	}

	public void startActivityForResult(String urlSchema, int requestCode) {
		startActivityForResult(
				new Intent(Intent.ACTION_VIEW, Uri.parse(urlSchema)),
				requestCode);
	}
}
