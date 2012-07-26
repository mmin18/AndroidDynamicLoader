package com.dianping.example.fragmentloader;

import android.app.Application;

public class MyApplication extends Application {
	private static MyApplication instance;

	@Override
	public void onCreate() {
		instance = this;
		super.onCreate();
	}

	public static Application instance() {
		return instance;
	}
}
