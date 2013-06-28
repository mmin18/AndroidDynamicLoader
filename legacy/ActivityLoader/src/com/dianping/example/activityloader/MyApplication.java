package com.dianping.example.activityloader;

import android.app.Application;
import android.content.Context;
import android.util.Log;

public class MyApplication extends Application {
	public static ClassLoader ORIGINAL_LOADER;
	public static ClassLoader CUSTOM_LOADER = null;

	@Override
	public void onCreate() {
		super.onCreate();
		try {
			Context mBase = new Smith<Context>(this, "mBase").get();

			Object mPackageInfo = new Smith<Object>(mBase, "mPackageInfo")
					.get();

			Smith<ClassLoader> sClassLoader = new Smith<ClassLoader>(
					mPackageInfo, "mClassLoader");
			ClassLoader mClassLoader = sClassLoader.get();
			ORIGINAL_LOADER = mClassLoader;

			MyClassLoader cl = new MyClassLoader(mClassLoader);
			sClassLoader.set(cl);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	class MyClassLoader extends ClassLoader {
		public MyClassLoader(ClassLoader parent) {
			super(parent);
		}

		@Override
		public Class<?> loadClass(String className)
				throws ClassNotFoundException {
			if (CUSTOM_LOADER != null) {
				if (className.startsWith("com.dianping.")) {
					Log.i("classloader", "loadClass( " + className + " )");
				}
				try {
					Class<?> c = CUSTOM_LOADER.loadClass(className);
					if (c != null)
						return c;
				} catch (ClassNotFoundException e) {
				}
			}
			return super.loadClass(className);
		}
	}
}
