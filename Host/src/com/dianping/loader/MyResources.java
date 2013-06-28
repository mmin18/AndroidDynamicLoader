package com.dianping.loader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.HashMap;

import org.xmlpull.v1.XmlPullParser;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.dianping.app.MyApplication;
import com.dianping.loader.model.FileSpec;
import com.dianping.loader.model.SiteSpec;

public class MyResources {

	FileSpec file;
	String packageName;
	Resources res;
	AssetManager asset;
	MyResources[] deps;

	MyResources(FileSpec file, String packageName, Resources res,
			AssetManager asset, MyResources[] deps) {
		this.file = file;
		this.packageName = packageName;
		this.res = res;
		this.asset = asset;
		this.deps = deps;
	}

	/**
	 * Resources.getDrawable(id)
	 */
	public Drawable getDrawable(int id) {
		return res.getDrawable(id);
	}

	/**
	 * Resources.getText(id)
	 */
	public CharSequence getText(int id) {
		return res.getText(id);
	}

	/**
	 * Resources.getString(id)
	 */
	public String getString(int id) {
		return res.getString(id);
	}

	/**
	 * Resources.getStringArray(id)
	 */
	public String[] getStringArray(int id) {
		return res.getStringArray(id);
	}

	/**
	 * Resources.getColor(id)
	 */
	public int getColor(int id) {
		return res.getColor(id);
	}

	/**
	 * Resources.getColorStateList(id)
	 */
	public ColorStateList getColorStateList(int id) {
		return res.getColorStateList(id);
	}

	/**
	 * Resources.getDimension(id)
	 */
	public float getDimension(int id) {
		return res.getDimension(id);
	}

	/**
	 * Resources.getDimensionPixelSize(id)
	 */
	public int getDimensionPixelSize(int id) {
		return res.getDimensionPixelSize(id);
	}

	/**
	 * Resources.getDimensionPixelOffset(id)
	 */
	public int getDimensionPixelOffset(int id) {
		return res.getDimensionPixelOffset(id);
	}

	/**
	 * Resources.openRawResource(id)
	 */
	public InputStream openRawResource(int id) {
		return res.openRawResource(id);
	}

	public byte[] getRawResource(int id) {
		InputStream ins = openRawResource(id);
		try {
			int n = ins.available();
			ByteArrayOutputStream bos = new ByteArrayOutputStream(n > 0 ? n
					: 4096);
			byte[] buf = new byte[4096];
			int l;
			while ((l = ins.read(buf)) != -1) {
				bos.write(buf, 0, l);
			}
			ins.close();
			return bos.toByteArray();
		} catch (Exception e) {
			return new byte[0];
		}
	}

	/**
	 * 返回独立的Resources
	 * <p>
	 * 对Resources进行操作时不会处理依赖关系，所有依赖包的内容均不会出现在该Resources中。
	 * 
	 * @return
	 */
	public Resources getResources() {
		return res;
	}

	/**
	 * 返回独立的AssetManager
	 * <p>
	 * 对AssetManager进行操作时不会处理依赖关系，所有依赖包的内容均不会出现在该AssetManager中。
	 * 
	 * @return
	 */
	public AssetManager getAssets() {
		return asset;
	}

	/**
	 * 同LayoutInflater.inflate(id, parent, attachToRoot)
	 * <p>
	 * 不会处理依赖关系，请确保id对应的layout在当前包内
	 * 
	 * @param name
	 * @return
	 * @throws Resources.NotFoundException
	 */
	public View inflate(Context context, int id, ViewGroup parent,
			boolean attachToRoot) {
		if (!(context instanceof MainActivity)) {
			throw new RuntimeException(
					"unable to inflate without MainActivity context");
		}
		MainActivity ma = (MainActivity) context;
		MyResources old = ma.getOverrideResources();
		ma.setOverrideResources(this);
		try {
			View v = LayoutInflater.from(context).inflate(id, parent,
					attachToRoot);
			return v;
		} finally {
			ma.setOverrideResources(old);
		}
	}

	static final HashMap<String, MyResources> loaders = new HashMap<String, MyResources>();

	/**
	 * return null if not available on the disk
	 */
	public static MyResources getResource(SiteSpec site, FileSpec file) {
		MyResources rl = loaders.get(file.id());
		if (rl != null)
			return rl;

		String[] deps = file.deps();
		MyResources[] rs = null;
		if (deps != null) {
			rs = new MyResources[deps.length];
			for (int i = 0; i < deps.length; i++) {
				FileSpec pf = site.getFile(deps[i]);
				if (pf == null)
					return null;
				MyResources r = getResource(site, pf);
				if (r == null)
					return null;
				rs[i] = r;
			}
		}

		File dir = MyApplication.instance().getFilesDir();
		dir = new File(dir, "repo");
		if (!dir.isDirectory())
			return null;
		dir = new File(dir, file.id());
		File path = new File(dir, TextUtils.isEmpty(file.md5()) ? "1.apk"
				: file.md5() + ".apk");
		if (!path.isFile())
			return null;

		try {
			AssetManager am = (AssetManager) AssetManager.class.newInstance();
			am.getClass().getMethod("addAssetPath", String.class)
					.invoke(am, path.getAbsolutePath());

			// parse packageName from AndroidManifest.xml
			String packageName = null;
			XmlResourceParser xml = am
					.openXmlResourceParser("AndroidManifest.xml");
			int eventType = xml.getEventType();
			xmlloop: while (eventType != XmlPullParser.END_DOCUMENT) {
				switch (eventType) {
				case XmlPullParser.START_TAG:
					if ("manifest".equals(xml.getName())) {
						packageName = xml.getAttributeValue(
								"http://schemas.android.com/apk/res/android",
								"package");
						break xmlloop;
					}
				}
				eventType = xml.nextToken();
			}
			xml.close();
			if (packageName == null) {
				throw new RuntimeException(
						"package not found in AndroidManifest.xml [" + path
								+ "]");
			}

			Resources superRes = MyApplication.instance().getResources();
			Resources res = new Resources(am, superRes.getDisplayMetrics(),
					superRes.getConfiguration());

			rl = new MyResources(file, packageName, res, am, rs);
		} catch (Exception e) {
			if (e instanceof RuntimeException)
				throw (RuntimeException) e;
			throw new RuntimeException(e);
		}

		loaders.put(file.id(), rl);
		return rl;
	}

	/**
	 * 从当前类所在的包载入MyResource
	 * 
	 * @param clazz
	 * @return
	 * @throws RuntimeException
	 *             如果当前类不是动态加载包载入的
	 */
	public static MyResources getResource(Class<?> clazz) {
		if (!(clazz.getClassLoader() instanceof MyClassLoader)) {
			throw new RuntimeException(clazz
					+ " is not loaded from dynamic loader");
		}
		return getResource((MyClassLoader) clazz.getClassLoader());
	}

	static MyResources getResource(MyClassLoader mcl) {
		FileSpec file = mcl.file;
		MyResources rl = loaders.get(file.id());
		if (rl != null)
			return rl;

		MyResources[] rs = null;
		if (mcl.deps != null) {
			rs = new MyResources[mcl.deps.length];
			for (int i = 0; i < rs.length; i++) {
				MyResources r = getResource(mcl.deps[i]);
				rs[i] = r;
			}
		}

		File dir = MyApplication.instance().getFilesDir();
		dir = new File(dir, "repo");
		if (!dir.isDirectory())
			throw new RuntimeException(dir + " not exists");
		dir = new File(dir, file.id());
		File path = new File(dir, TextUtils.isEmpty(file.md5()) ? "1.apk"
				: file.md5() + ".apk");
		if (!path.isFile())
			throw new RuntimeException(path + " not exists");

		try {
			AssetManager am = (AssetManager) AssetManager.class.newInstance();
			am.getClass().getMethod("addAssetPath", String.class)
					.invoke(am, path.getAbsolutePath());

			Resources superRes = MyApplication.instance().getResources();
			Resources res = new Resources(am, superRes.getDisplayMetrics(),
					superRes.getConfiguration());

			// parse packageName from AndroidManifest.xml
			String packageName = null;
			XmlResourceParser xml = am
					.openXmlResourceParser("AndroidManifest.xml");
			int eventType = xml.getEventType();
			xmlloop: while (eventType != XmlPullParser.END_DOCUMENT) {
				switch (eventType) {
				case XmlPullParser.START_TAG:
					if ("manifest".equals(xml.getName())) {
						packageName = xml.getAttributeValue(null, "package");
						break xmlloop;
					}
				}
				eventType = xml.nextToken();
			}
			xml.close();
			if (packageName == null) {
				throw new RuntimeException(
						"package not found in AndroidManifest.xml [" + path
								+ "]");
			}

			rl = new MyResources(file, packageName, res, am, rs);
		} catch (Exception e) {
			if (e instanceof RuntimeException)
				throw (RuntimeException) e;
			throw new RuntimeException(e);
		}

		loaders.put(file.id(), rl);
		return rl;
	}
}
