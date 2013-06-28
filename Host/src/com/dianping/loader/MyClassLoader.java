package com.dianping.loader;

import java.io.File;
import java.util.HashMap;

import android.text.TextUtils;

import com.dianping.app.MyApplication;
import com.dianping.loader.model.FileSpec;
import com.dianping.loader.model.SiteSpec;

import dalvik.system.DexClassLoader;

public class MyClassLoader extends DexClassLoader {
	FileSpec file;
	MyClassLoader[] deps;

	MyClassLoader(FileSpec file, String dexPath, String optimizedDirectory,
			String libraryPath, ClassLoader parent, MyClassLoader[] deps) {
		super(dexPath, optimizedDirectory, libraryPath, parent);
		this.file = file;
		this.deps = deps;
	}

	@Override
	protected Class<?> loadClass(String className, boolean resolve)
			throws ClassNotFoundException {
		Class<?> clazz = findLoadedClass(className);
		if (clazz != null)
			return clazz;
		try {
			clazz = getParent().loadClass(className);
		} catch (ClassNotFoundException e) {
		}
		if (clazz != null)
			return clazz;
		if (deps != null) {
			for (MyClassLoader c : deps) {
				try {
					clazz = c.findClass(className);
					break;
				} catch (ClassNotFoundException e) {
				}
			}
		}
		if (clazz != null)
			return clazz;
		clazz = findClass(className);
		return clazz;
	}

	static final HashMap<String, MyClassLoader> loaders = new HashMap<String, MyClassLoader>();

	/**
	 * return null if not available on the disk
	 */
	public static MyClassLoader getClassLoader(SiteSpec site, FileSpec file) {
		MyClassLoader cl = loaders.get(file.id());
		if (cl != null)
			return cl;
		String[] deps = file.deps();
		MyClassLoader[] ps = null;
		if (deps != null) {
			ps = new MyClassLoader[deps.length];
			for (int i = 0; i < deps.length; i++) {
				FileSpec pf = site.getFile(deps[i]);
				if (pf == null)
					return null;
				MyClassLoader l = getClassLoader(site, pf);
				if (l == null)
					return null;
				ps[i] = l;
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
		File outdir = new File(dir, "dexout");
		outdir.mkdir();
		cl = new MyClassLoader(file, path.getAbsolutePath(),
				outdir.getAbsolutePath(), null, MyApplication.instance()
						.getClassLoader(), ps);
		loaders.put(file.id(), cl);
		return cl;
	}
}
