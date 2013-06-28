package com.dianping.app;

import java.io.File;
import java.io.FileInputStream;
import java.util.Locale;

import org.json.JSONObject;

import android.app.Application;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.dianping.loader.LoaderActivity;
import com.dianping.loader.MainActivity;
import com.dianping.loader.MyClassLoader;
import com.dianping.loader.RepositoryManager;
import com.dianping.loader.model.FileSpec;
import com.dianping.loader.model.FragmentSpec;
import com.dianping.loader.model.SiteSpec;

public class MyApplication extends Application {
	public static final String PRIMARY_SCHEME = "app";

	private static MyApplication instance;
	private RepositoryManager repoManager;

	public static MyApplication instance() {
		if (instance == null) {
			throw new IllegalStateException("Application has not been created");
		}

		return instance;
	}

	public MyApplication() {
		instance = this;
	}

	public RepositoryManager repositoryManager() {
		if (repoManager == null) {
			repoManager = new RepositoryManager(this);
		}
		return repoManager;
	}

	public SiteSpec readSite() {
		File dir = new File(getFilesDir(), "repo");
		File local = new File(dir, "site.txt");
		if (local.length() > 0) {
			try {
				FileInputStream fis = new FileInputStream(local);
				byte[] bytes = new byte[fis.available()];
				int l = fis.read(bytes);
				fis.close();
				String str = new String(bytes, 0, l, "UTF-8");
				JSONObject json = new JSONObject(str);
				return new SiteSpec(json);
			} catch (Exception e) {
				Log.w("loader", "fail to load site.txt from " + local, e);
			}
		}
		return new SiteSpec("empty.0", "0", new FileSpec[0],
				new FragmentSpec[0]);
	}

	public Intent urlMap(Intent intent) {
		do {
			// already specify a class, no need to map url
			if (intent.getComponent() != null)
				break;

			// only process my scheme uri
			Uri uri = intent.getData();
			if (uri == null)
				break;
			if (uri.getScheme() == null)
				break;
			if (!(PRIMARY_SCHEME.equalsIgnoreCase(uri.getScheme())))
				break;

			SiteSpec site = null;
			if (intent.hasExtra("_site")) {
				site = intent.getParcelableExtra("_site");
			}
			if (site == null) {
				site = readSite();
				intent.putExtra("_site", site);
			}

			// i'm responsible
			intent.setClass(this, LoaderActivity.class);

			String host = uri.getHost();
			if (TextUtils.isEmpty(host))
				break;
			host = host.toLowerCase(Locale.US);
			FragmentSpec fragment = site.getFragment(host);
			if (fragment == null)
				break;
			intent.putExtra("_fragment", fragment.name());

			// class loader
			ClassLoader classLoader;
			if (TextUtils.isEmpty(fragment.code())) {
				classLoader = getClassLoader();
			} else {
				intent.putExtra("_code", fragment.code());
				FileSpec fs = site.getFile(fragment.code());
				if (fs == null)
					break;
				classLoader = MyClassLoader.getClassLoader(site, fs);
				if (classLoader == null)
					break;
			}

			intent.setClass(this, MainActivity.class);
		} while (false);

		return intent;
	}

	@Override
	public void startActivity(Intent intent) {
		intent = urlMap(intent);
		super.startActivity(intent);
	}

}
