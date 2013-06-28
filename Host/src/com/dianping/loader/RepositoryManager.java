package com.dianping.loader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.SystemClock;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.dianping.loader.model.FileSpec;

public class RepositoryManager {
	/**
	 * 已经下载并校验完毕，可以直接加载
	 */
	static final String STATUS_DONE = "DONE";
	/**
	 * 未开始下载
	 */
	static final String STATUS_IDLE = "IDLE";
	/**
	 * 当前下载
	 */
	static final String STATUS_RUNNING = "RUNNING";

	/**
	 * call in the background thread
	 */
	static interface StatusChangeListener {
		void onStatusChanged(FileSpec file, String newStatus);
	}

	private final Context context;
	private final ConnectivityManager connManager;

	// ./repo/<id>/<md5 or 1>.apk
	// ./repo/<id>/dexout
	private final File repoDir;
	// ./repo/tmp/<id>.<random.4>
	private final File tmpDir;
	private final LinkedList<FileSpec> order = new LinkedList<FileSpec>();
	private final HashMap<String, FileSpec> map = new HashMap<String, FileSpec>();
	private final HashMap<String, String> status = new HashMap<String, String>();
	private final HashMap<String, Integer> require = new HashMap<String, Integer>();
	private final ArrayList<StatusChangeListener> listeners = new ArrayList<StatusChangeListener>();
	private Worker running;

	public RepositoryManager(Context context) {
		this.context = context;
		ConnectivityManager cm = null;
		try {
			cm = (ConnectivityManager) context
					.getSystemService(Context.CONNECTIVITY_SERVICE);
		} catch (Exception e) {
			Log.w("loader",
					"repository manager start without connectivity manager", e);
		}
		connManager = cm;

		repoDir = new File(context.getFilesDir(), "repo");
		repoDir.mkdir();
		tmpDir = new File(repoDir, "tmp");
		tmpDir.mkdir();
		File[] tmps = tmpDir.listFiles();
		if (tmps != null) {
			for (File tmpf : tmps) {
				tmpf.delete();
			}
		}

		disableConnectionReuseIfNecessary();
	}

	//
	// internal
	//

	void addListener(StatusChangeListener listener) {
		listeners.add(listener);
	}

	void removeListener(StatusChangeListener listener) {
		listeners.remove(listener);
	}

	synchronized int runningCount() {
		int count = 0;
		for (String str : status.values()) {
			if (STATUS_RUNNING == str)
				count++;
		}
		return count;
	}

	synchronized int totalCount() {
		return map.size();
	}

	synchronized String getStatus(String fileId) {
		if (status.get(fileId) == null) {
			FileSpec file = map.get(fileId);
			if (file != null) {
				File path = getPath(file);
				status.put(fileId, path.isFile() ? STATUS_DONE : STATUS_IDLE);
			} else {
				return null;
			}
		}
		return status.get(fileId);
	}

	File getDir() {
		return repoDir;
	}

	public File getPath(FileSpec file) {
		File dir = new File(repoDir, file.id());
		File path = new File(dir, TextUtils.isEmpty(file.md5()) ? "1.apk"
				: file.md5() + ".apk");
		return path;
	}

	public synchronized void addFiles(FileSpec[] files) {
		for (FileSpec f : files) {
			map.put(f.id(), f);
		}
		for (FileSpec f : files) {
			appendDepsList(map, order, f.id());
		}
	}

	// return false if missing file or loop deps
	public boolean appendDepsList(List<FileSpec> list, String fileId) {
		return appendDepsList(map, list, fileId);
	}

	// return false if missing file or loop deps
	public static boolean appendDepsList(HashMap<String, FileSpec> map,
			List<FileSpec> list, String fileId) {
		FileSpec f = map.get(fileId);
		if (f == null)
			return false;
		if (list.contains(f))
			return true;
		if (f.deps() != null) {
			for (String dep : f.deps()) {
				if (!appendDepsList(map, list, dep))
					return false;
			}
		}
		if (list.contains(f))
			return false;
		list.add(f);
		return true;
	}

	public synchronized void notifyConnectivityChanged() {
		if (running != null)
			return;
		if (pickFromQueue() == null)
			return;
		start();
	}

	synchronized void start() {
		if (running == null) {
			if (status.size() == map.size()) {
				int idleCount = 0;
				for (String str : status.values()) {
					if (STATUS_IDLE == str)
						idleCount++;
				}
				if (idleCount == 0)
					return;
			}
			running = new Worker();
			running.start();
		}
	}

	public synchronized void require(FileSpec... files) {
		order.removeAll(Arrays.asList(files));
		for (int i = files.length - 1; i >= 0; i--) {
			FileSpec file = files[i];
			order.addFirst(file);
			Integer rc = require.get(file.id());
			if (rc == null) {
				require.put(file.id(), 1);
			} else {
				require.put(file.id(), rc + 1);
			}
		}
		running = new Worker();
		running.start();
	}

	public synchronized void dismiss(FileSpec... files) {
		for (FileSpec file : files) {
			Integer rc = require.get(file.id());
			if (rc != null && rc > 0) {
				require.put(file.id(), rc - 1);
			}
		}
	}

	//
	// worker
	//

	private synchronized FileSpec pickFromQueue() {
		int networkType = -1;
		for (FileSpec file : order) {
			if (getStatus(file.id()) == STATUS_IDLE) {
				Integer rc = require.get(file.id());
				if (rc != null && rc > 0) {
					return file;
				}
				if (file.down() >= FileSpec.DOWN_ALWAYS)
					return file;
				if (file.down() <= FileSpec.DOWN_NONE)
					continue;
				if (networkType < 0) {
					networkType = getNetworkType();
				}
				switch (file.down()) {
				case FileSpec.DOWN_WIFI:
					if (networkType > 3)
						return file;
					break;
				case FileSpec.DOWN_3G:
					if (networkType > 2)
						return file;
				}
			}
		}
		return null;
	}

	private class Worker extends Thread {
		private int failCounter = 0;

		@Override
		public void run() {
			FileSpec current;
			while (running == this && (current = pickFromQueue()) != null) {
				final FileSpec fCurrent = current;
				Log.i("loader", "start download " + current.id() + " from "
						+ current.url());
				long startMs = SystemClock.elapsedRealtime();

				status.put(current.id(), STATUS_RUNNING);
				for (StatusChangeListener l : listeners) {
					l.onStatusChanged(fCurrent, STATUS_RUNNING);
				}

				String rnd = Integer.toHexString(new Random(System
						.currentTimeMillis()).nextInt(0xf000) + 0x1000);
				File f = new File(tmpDir, current.id() + "." + rnd);
				boolean succeed = false;
				try {
					URL url = new URL(current.url());
					HttpURLConnection conn = (HttpURLConnection) url
							.openConnection();
					conn.setConnectTimeout(15000);
					// conn.setRequestProperty("User-Agent",
					// Environment.mapiUserAgent());
					InputStream ins = conn.getInputStream();
					FileOutputStream fos = new FileOutputStream(f);
					byte[] buf = new byte[1024 * 4]; // 4k buffer
					int l;
					while ((l = ins.read(buf, 0, buf.length)) != -1) {
						fos.write(buf, 0, l);
					}
					fos.close();
					ins.close();
					conn.disconnect();
					succeed = true;
				} catch (Exception e) {
					Log.w("loader", "fail to download " + current.id()
							+ " from " + current.url(), e);
				}

				if (f.length() > 0 && !TextUtils.isEmpty(current.md5())) {
					succeed = false;
					try {
						MessageDigest m = MessageDigest.getInstance("MD5");
						m.reset();
						FileInputStream fis = new FileInputStream(f);
						byte[] buf = new byte[1024 * 4]; // 4k buffer
						int l;
						while ((l = fis.read(buf, 0, buf.length)) != -1) {
							m.update(buf, 0, l);
						}
						fis.close();
						String md5 = byteArrayToHexString(m.digest());
						succeed = current.md5().equals(md5);
						if (!succeed) {
							Log.e("loader", "fail to match " + current.id()
									+ " md5, " + md5 + " / " + current.md5());
						}
					} catch (Exception e) {
						Log.e("loader",
								"fail to verify file " + f.getAbsolutePath(), e);
					}
				}

				File path = getPath(current);
				if (succeed) {
					path.getParentFile().mkdir();
					succeed = f.renameTo(path);
					if (!succeed) {
						Log.e("loader", "fail to move " + current.id()
								+ " from " + f.getAbsolutePath() + " to "
								+ path.getAbsolutePath());
					}
				}

				if (!succeed) {
					// delete tmp file if not succeed
					f.delete();
				}

				succeed = path.isFile();
				final String newStatus = succeed ? STATUS_DONE : STATUS_IDLE;
				status.put(current.id(), newStatus);
				if (succeed) {
					long elapse = SystemClock.elapsedRealtime() - startMs;
					Log.i("loader", current.id() + " (" + path.length()
							+ " bytes) finished in " + elapse + "ms");
					if (failCounter > 0)
						failCounter--;
				} else {
					order.remove(current);
					order.addLast(current);
					failCounter++;
				}

				for (StatusChangeListener l : listeners) {
					l.onStatusChanged(fCurrent, newStatus);
				}

				if (failCounter >= 3) {
					Log.w("loader", "download fail 3 times, abort");
					break;
				}
			}

			synchronized (RepositoryManager.this) {
				if (running == this) {
					running = null;
				}
			}
		}
	}

	//
	// utils
	//

	/**
	 * 0: Unknown<br>
	 * 1: 2G<br>
	 * 2: 3G or faster<br>
	 * 3: Wifi<br>
	 */
	int getNetworkType() {
		try {
			NetworkInfo info = connManager.getActiveNetworkInfo();
			if (info.getType() == ConnectivityManager.TYPE_MOBILE) {
				switch (info.getSubtype()) {
				case TelephonyManager.NETWORK_TYPE_1xRTT:// ~ 50-100 kbps
				case TelephonyManager.NETWORK_TYPE_CDMA:// ~ 14-64 kbps
				case TelephonyManager.NETWORK_TYPE_EDGE:// ~ 50-100 kbps
				case TelephonyManager.NETWORK_TYPE_GPRS:// ~ 100 kbps
				case TelephonyManager.NETWORK_TYPE_IDEN:// ~25 kbps
					return 1;
				default:
					return 2;
				}
			} else if (info.getType() == ConnectivityManager.TYPE_WIFI) {
				return 3;
			}
		} catch (Exception e) {
		}
		return 0;
	}

	private void disableConnectionReuseIfNecessary() {
		// HTTP connection reuse which was buggy pre-froyo
		if (Integer.parseInt(Build.VERSION.SDK) < 8) {
			System.setProperty("http.keepAlive", "false");
		}
	}

	private final static String[] hexDigits = { "0", "1", "2", "3", "4", "5",
			"6", "7", "8", "9", "a", "b", "c", "d", "e", "f" };

	public static String byteArrayToHexString(byte[] b) {
		StringBuilder resultSb = new StringBuilder();
		for (int i = 0; i < b.length; i++) {
			resultSb.append(byteToHexString(b[i]));
		}
		return resultSb.toString();
	}

	private static String byteToHexString(byte b) {
		int n = b;
		if (n < 0)
			n = 0x100 + n;
		int d1 = n >> 4;
		int d2 = n & 0xF;
		return hexDigits[d1] + hexDigits[d2];
	}
}
