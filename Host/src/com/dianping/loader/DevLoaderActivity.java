package com.dianping.loader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Locale;

import org.json.JSONObject;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Html;
import android.text.TextUtils;
import android.view.ViewGroup;
import android.widget.TextView;

import com.dianping.app.MyActivity;
import com.dianping.app.MyApplication;
import com.dianping.loader.model.SiteSpec;
import com.dianping.util.EmbedHttpServer;

/**
 * 开发调试启动入口
 * <p>
 * 
 * Intent参数：<br>
 * port:int，指定服务器的监听端口号，默认为5036<br>
 * 
 * @author yimin.tu
 * 
 */
public class DevLoaderActivity extends MyActivity {
	private TextView text;
	private MyHttpServer server;
	private RepositoryManager repoManager;
	private SiteSpec site;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		repoManager = ((MyApplication) getApplication()).repositoryManager();
		text = new TextView(this);
		text.setLayoutParams(new ViewGroup.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.MATCH_PARENT));
		setContentView(text);

		int port = getIntent().getIntExtra("port", 5036);
		server = new MyHttpServer(port);
		try {
			server.start();
			println("server started on port " + port);
		} catch (Exception e) {
			print("unable to start server on port " + port + ": ");
			println(e);
		}
	}

	@Override
	protected void onDestroy() {
		try {
			server.stop();
		} catch (Exception e) {
		}
		super.onDestroy();
	}

	private void print(final Object obj) {
		if (Thread.currentThread().getId() == Looper.getMainLooper()
				.getThread().getId()) {
			text.append(String.valueOf(obj));
		} else {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					print(obj);
				}
			});
		}
	}

	private void println(final Object obj) {
		if (Thread.currentThread().getId() == Looper.getMainLooper()
				.getThread().getId()) {
			text.append(String.valueOf(obj));
			text.append("\n");
		} else {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					println(obj);
				}
			});
		}
	}

	/**
	 * GET /list<br>
	 * return list of files in repo<br>
	 * <p>
	 * 
	 * GET /repo/...<br>
	 * PUT /repo/...<br>
	 * DELETE /repo/...<br>
	 * get or upload or delete file in repo<br>
	 * <p>
	 * 
	 * PUT /site<br>
	 * PUT /site.txt<br>
	 * upload the site.txt file in this session<br>
	 * <p>
	 * 
	 * GET /go/[host]?[params]<br>
	 * launch the host with params<br>
	 * 
	 * GET /debug/[host]?[params]<br>
	 * launch and debug (Waiting For Debugger)<br>
	 * 
	 * @author yimin.tu
	 * 
	 */
	private class MyHttpServer extends EmbedHttpServer {
		public MyHttpServer(int port) {
			super(port);
		}

		@Override
		protected void handle(String method, String path,
				HashMap<String, String> headers, InputStream input,
				ResponseOutputStream response) throws Exception {
			final String origPath = path;
			println(method + " " + origPath);
			path = path.toLowerCase(Locale.US);
			String query = "";
			{
				int i = path.indexOf('?');
				if (i != -1) {
					query = path.substring(i + 1);
					path = path.substring(0, i);
				}
				i = query.indexOf('#');
				if (i != -1) {
					query = query.substring(0, i);
				}
			}
			if ("GET".equalsIgnoreCase(method)
					&& "/list".equalsIgnoreCase(path)) {
				response.setContentTypeText();
				File dir = repoManager.getDir();
				dir.mkdirs();
				int count = 0;
				for (File id : dir.listFiles()) {
					if (id.isDirectory()) {
						for (File file : id.listFiles()) {
							if (file.isFile()
									&& file.getName().endsWith(".apk")) {
								String str = id.getName() + "/"
										+ file.getName() + "\n";
								response.write(str.getBytes("ASCII"));
								count++;
							}
						}
					}
				}
				println(count + " files listed");
			}
			if (path.startsWith("/repo/")) {
				File dir = repoManager.getDir();
				String str = path.substring("/repo/".length());
				str = dir.getAbsolutePath() + "/" + str;
				File file = new File(str);
				if ("PUT".equalsIgnoreCase(method)) {
					file.getParentFile().mkdirs();
					FileOutputStream fos = new FileOutputStream(file);
					byte[] buf = new byte[4096];
					int l;
					while ((l = input.read(buf)) != -1) {
						fos.write(buf, 0, l);
					}
					fos.close();
					println(file.length() + " bytes writed");
				}
				if ("GET".equalsIgnoreCase(method)) {
					if (file.isFile()) {
						response.setContentLength((int) file.length());
						response.setContentTypeBinary();
						FileInputStream fis = new FileInputStream(file);
						byte[] buf = new byte[4096];
						int l;
						while ((l = input.read(buf)) != -1) {
							response.write(buf, 0, l);
						}
						fis.close();
						println(file.length() + " bytes read");
					} else {
						response.setStatusCode(404);
						println("404 not found");
					}
				}
				if ("DELETE".equalsIgnoreCase(method)) {
					int c = delete(file);
					boolean fail = file.exists();
					println(c + " files deleted" + (fail ? " (fail)" : ""));
				}
			}
			if ("PUT".equalsIgnoreCase(method)
					&& ("/site".equals(path) || "/site.txt".equals(path))) {
				byte[] buf = new byte[input.available()];
				int l, n = 0;
				while ((l = input.read(buf, n, buf.length - n)) != -1) {
					n += l;
				}
				String str = new String(buf, 0, n, Charset.forName("UTF-8"));
				try {
					JSONObject json = new JSONObject(str);
					site = new SiteSpec(json);
					File file = new File(getFilesDir(), "repo");
					new File(file, "lastUrl.txt").delete();
					file = new File(file, "site.txt");
					FileOutputStream fos = new FileOutputStream(file);
					fos.write(buf, 0, n);
					fos.close();
					println("site.txt is ready (" + site + ")");
				} catch (Exception e) {
					println("malformed site.txt");
					println(e.toString());
				}
			}
			if ("GET".equalsIgnoreCase(method)
					&& (path.startsWith("/go/") || path.startsWith("/debug/"))) {
				StringBuilder sb = new StringBuilder(
						MyApplication.PRIMARY_SCHEME);
				sb.append("://").append(
						path.substring(path.indexOf('/', 1) + 1));
				if (!TextUtils.isEmpty(query)) {
					sb.append('?').append(query);
				}

				Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(sb
						.toString()));
				if (site != null) {
					i.putExtra("_site", site);
				}

				if (path.startsWith("/debug/")) {
					new WaitForDebugger(i).start();
				} else {
					startActivity(i);
				}

				try {
					File file = new File(getFilesDir(), "repo");
					file = new File(file, "lastUrl.txt");
					FileOutputStream fos = new FileOutputStream(file);
					fos.write(sb.toString().getBytes("UTF-8"));
					fos.close();
				} catch (Exception e) {
				}
			}
		}

		private int delete(File dir) {
			if (dir.isFile()) {
				dir.delete();
				return 1;
			}
			File[] list = dir.listFiles();
			if (list == null)
				return 0;
			int c = 0;
			for (File f : list) {
				c += delete(f);
			}
			return c;
		}
	}

	private class WaitForDebugger extends Handler {
		Intent intent;
		AlertDialog dialog;

		public WaitForDebugger(Intent i) {
			super(Looper.getMainLooper());
			intent = i;
		}

		public void start() {
			sendEmptyMessage(1);
		}

		@Override
		public void handleMessage(Message msg) {
			if (msg.what == 1) {
				AlertDialog.Builder b = new AlertDialog.Builder(
						DevLoaderActivity.this);
				b.setTitle("Waiting For Debugger..");
				b.setIcon(android.R.drawable.ic_dialog_alert);
				b.setMessage(Html
						.fromHtml("Go to eclipse, open DDMS perspective, debug <b>"
								+ getPackageName() + "</b> in Devices panel"));
				b.setNegativeButton("Skip",
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int button) {
								sendEmptyMessage(2);
							}
						});
				b.setCancelable(false);
				dialog = b.create();
				dialog.show();

				new Thread(looper, "wait-for-debugger").start();
			}
			if (msg.what == 2) {
				if (dialog != null) {
					dialog.dismiss();
					dialog = null;
					startActivity(intent);
				}
			}
		}

		private final Runnable looper = new Runnable() {
			@Override
			public void run() {
				while (dialog != null && !Debug.isDebuggerConnected()) {
					try {
						Thread.sleep(200);
					} catch (InterruptedException e) {
					}
				}
				sendEmptyMessage(2);
			}
		};
	}
}
