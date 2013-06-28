package com.dianping.loader;

import java.util.List;

import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.dianping.app.MyActivity;
import com.dianping.app.MyApplication;

/**
 * 根据URL Map处理外部链接打开URL Scheme的跳转逻辑
 * <p>
 * 在AndroidManifest.xml中注册应用的host为ForwardActivity<br>
 * 
 * @author Yimin
 * 
 */
public class ForwardActivity extends MyActivity {
	private FrameLayout rootView;
	private boolean launched;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		rootView = new FrameLayout(this);
		rootView.setLayoutParams(new ViewGroup.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.MATCH_PARENT));
		rootView.setId(android.R.id.primary);
		setContentView(rootView);

		launched = savedInstanceState == null ? false : savedInstanceState
				.getBoolean("launched");
		if (!(getApplication() instanceof MyApplication)) {
			TextView text = new TextView(this);
			text.setText("无法载入页面 #401"); // #401
			text.setLayoutParams(new FrameLayout.LayoutParams(
					ViewGroup.LayoutParams.WRAP_CONTENT,
					ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
			rootView.addView(text);
			return;
		}

		doForward();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean("launched", launched);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 1) {
			setResult(resultCode, data);
			finish();
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	protected void doForward() {
		if (launched)
			return;
		Intent intent = getIntent();
		Intent i = new Intent(intent.getAction(), intent.getData());
		if (intent.getExtras() != null) {
			i.putExtras(intent.getExtras());
		}
		intent = urlMap(i);
		try {
			// check if it open myself to avoid infinite loop
			List<ResolveInfo> l = getPackageManager().queryIntentActivities(
					intent, 0);
			if (l.size() == 1) {
				ResolveInfo ri = l.get(0);
				if (getPackageName().equals(ri.activityInfo.packageName)) {
					if (getClass().getName().equals(ri.activityInfo.name)) {
						throw new Exception("infinite loop");
					}
				}
			} else if (l.size() > 1) {
				// should not happen, do we allow this?
			}
			startActivityForResult(intent, 1);
			launched = true;
		} catch (Exception e) {
			TextView text = new TextView(this);
			text.setText("无法载入页面 #402"); // #402
			text.append("\n" + e.toString());
			text.setLayoutParams(new FrameLayout.LayoutParams(
					ViewGroup.LayoutParams.WRAP_CONTENT,
					ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
			rootView.addView(text);
			Log.e("loader", "unable to forward " + getIntent(), e);
		}
	}
}
