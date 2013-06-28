package com.dianping.example.fragmentloader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;
import dalvik.system.DexClassLoader;

public class FragmentLoader extends Activity {
	private AssetManager asm;
	private Resources res;
	private Theme thm;
	private ClassLoader cl;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		if ("com.dianping.intent.action.LOAD_FRAGMENT".equals(getIntent()
				.getAction())) {
			// we need to setup environment before super.onCreate
			try {
				String path = getIntent().getStringExtra("path");
				InputStream ins = MyApplication.instance().getAssets()
						.open(path);
				byte[] bytes = new byte[ins.available()];
				ins.read(bytes);
				ins.close();

				File f = new File(MyApplication.instance().getFilesDir(), "dex");
				f.mkdir();
				f = new File(f, "FL_" + Integer.toHexString(path.hashCode())
						+ ".apk");
				FileOutputStream fos = new FileOutputStream(f);
				fos.write(bytes);
				fos.close();

				File fo = new File(MyApplication.instance().getFilesDir(),
						"dexout");
				fo.mkdir();

				DexClassLoader dcl = new DexClassLoader(f.getAbsolutePath(),
						fo.getAbsolutePath(), null, super.getClassLoader());
				cl = dcl;

				try {
					AssetManager am = (AssetManager) AssetManager.class
							.newInstance();
					am.getClass().getMethod("addAssetPath", String.class)
							.invoke(am, f.getAbsolutePath());
					asm = am;
				} catch (Exception e) {
					throw new RuntimeException(e);
				}

				Resources superRes = super.getResources();

				res = new Resources(asm, superRes.getDisplayMetrics(),
						superRes.getConfiguration());

				thm = res.newTheme();
				thm.setTo(super.getTheme());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		super.onCreate(savedInstanceState);

		FrameLayout rootView = new FrameLayout(this);
		rootView.setLayoutParams(new ViewGroup.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.MATCH_PARENT));
		rootView.setId(android.R.id.primary);
		setContentView(rootView);

		if (savedInstanceState != null)
			return;

		if ("com.dianping.intent.action.LOAD_FRAGMENT".equals(getIntent()
				.getAction())) {
			try {
				String fragmentClass = getIntent().getStringExtra("class");
				Fragment f = (Fragment) getClassLoader().loadClass(
						fragmentClass).newInstance();
				FragmentManager fm = getFragmentManager();
				FragmentTransaction ft = fm.beginTransaction();
				ft.add(android.R.id.primary, f);
				ft.commit();
			} catch (Exception e) {
				Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
			}
		} else {
			Fragment f = new ListApkFragment();
			FragmentManager fm = getFragmentManager();
			FragmentTransaction ft = fm.beginTransaction();
			ft.add(android.R.id.primary, f);
			ft.commit();
		}
	}

	@Override
	public AssetManager getAssets() {
		return asm == null ? super.getAssets() : asm;
	}

	@Override
	public Resources getResources() {
		return res == null ? super.getResources() : res;
	}

	@Override
	public Theme getTheme() {
		return thm == null ? super.getTheme() : thm;
	}

	@Override
	public ClassLoader getClassLoader() {
		return cl == null ? super.getClassLoader() : cl;
	}
}
