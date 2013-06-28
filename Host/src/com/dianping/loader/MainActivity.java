package com.dianping.loader;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.dianping.app.MyActivity;
import com.dianping.app.MyApplication;
import com.dianping.loader.model.FileSpec;
import com.dianping.loader.model.SiteSpec;

/**
 * 主Activity容器，负责启动并装载Fragment
 * <p>
 * 启动前所有依赖的资源必须加载完毕（由urlMapping和LoaderActivity负责）
 * <p>
 * Intent参数：<br>
 * _site:SiteSpec，指定的site地图<br>
 * _code:String，ClassLoader所需要载入的FileID，如果为空则使用APK自带ClassLoader<br>
 * _fragment:String，Fragment的类名<br>
 * 
 * @author Yimin
 * 
 */
public class MainActivity extends MyActivity {
	private SiteSpec site;
	private FileSpec file;
	private String fragmentName;
	private boolean loaded;
	private MyClassLoader classLoader;
	private MyResources myResources;
	private AssetManager assetManager;
	private Resources resources;
	private Theme theme;
	private FrameLayout rootView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Intent intent = getIntent();

		int error = 0;
		// must be load at the first start
		do {
			site = intent.getParcelableExtra("_site");
			if (site == null) {
				error = 201; // #201
				break;
			}
			fragmentName = intent.getStringExtra("_fragment");
			if (TextUtils.isEmpty(fragmentName)) {
				error = 202; // #202
				break;
			}
			String code = intent.getStringExtra("_code");
			if (TextUtils.isEmpty(code)) {
				loaded = true;
				break;
			}
			file = site.getFile(code);
			if (file == null) {
				error = 205; // #205
				break;
			}
			classLoader = MyClassLoader.getClassLoader(site, file);
			loaded = classLoader != null;
			if (!loaded) {
				error = 210; // #210
				break;
			}
		} while (false);

		super.onCreate(savedInstanceState);

		rootView = new FrameLayout(this);
		rootView.setLayoutParams(new ViewGroup.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.MATCH_PARENT));
		rootView.setId(android.R.id.primary);
		setContentView(rootView);

		if (!loaded) {
			TextView text = new TextView(this);
			text.setText("无法载入页面" + (error == 0 ? "" : " #" + error));
			text.setLayoutParams(new FrameLayout.LayoutParams(
					ViewGroup.LayoutParams.WRAP_CONTENT,
					ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
			rootView.addView(text);

			return;
		}

		// the fragment will be restored by framework
		if (savedInstanceState != null)
			return;

		Fragment fragment = null;
		try {
			fragment = (Fragment) getClassLoader().loadClass(fragmentName)
					.newInstance();
		} catch (Exception e) {
			loaded = false;
			classLoader = null;
			error = 211; // #211
			TextView text = new TextView(this);
			text.setText("无法载入页面 #" + error);
			text.append("\n" + e);
			text.setLayoutParams(new FrameLayout.LayoutParams(
					ViewGroup.LayoutParams.WRAP_CONTENT,
					ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
			rootView.addView(text);

			return;
		}

		FragmentManager fm = getFragmentManager();
		FragmentTransaction ft = fm.beginTransaction();
		ft.replace(android.R.id.primary, fragment);
		ft.commit();
	}

	@Override
	public ClassLoader getClassLoader() {
		return classLoader == null ? super.getClassLoader() : classLoader;
	}

	public SiteSpec getSite() {
		return site;
	}

	public FileSpec getFile() {
		return file;
	}

	public String getFragmentName() {
		return fragmentName;
	}

	@Override
	public Intent urlMap(Intent intent) {
		do {
			// only process my scheme uri
			Uri uri = intent.getData();
			if (uri == null)
				break;
			if (uri.getScheme() == null)
				break;
			if (!(MyApplication.PRIMARY_SCHEME
					.equalsIgnoreCase(uri.getScheme())))
				break;

			if (!intent.hasExtra("_site")) {
				intent.putExtra("_site", site);
			}
		} while (false);
		return super.urlMap(intent);
	}

	@Override
	public AssetManager getAssets() {
		return assetManager == null ? super.getAssets() : assetManager;
	}

	@Override
	public Resources getResources() {
		return resources == null ? super.getResources() : resources;
	}

	@Override
	public Theme getTheme() {
		return theme == null ? super.getTheme() : theme;
	}

	public MyResources getOverrideResources() {
		return myResources;
	}

	void setOverrideResources(MyResources myres) {
		if (myres == null) {
			this.myResources = null;
			this.resources = null;
			this.assetManager = null;
			this.theme = null;
		} else {
			this.myResources = myres;
			this.resources = myres.getResources();
			this.assetManager = myres.getAssets();
			Theme t = myres.getResources().newTheme();
			t.setTo(getTheme());
			this.theme = t;
		}
	}
}
