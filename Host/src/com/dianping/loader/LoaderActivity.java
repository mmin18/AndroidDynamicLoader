package com.dianping.loader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.dianping.app.MyActivity;
import com.dianping.app.MyApplication;
import com.dianping.loader.model.FileSpec;
import com.dianping.loader.model.FragmentSpec;
import com.dianping.loader.model.SiteSpec;

/**
 * 负责下载Fragment运行所需的资源
 * <p>
 * Intent参数：<br>
 * _site:SiteSpec，指定的site地图<br>
 * 
 * @author Yimin
 * 
 */
public class LoaderActivity extends MyActivity {
	private SiteSpec site;
	private boolean loaded;
	private Loader loader;
	private FrameLayout rootView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		rootView = new FrameLayout(this);
		rootView.setLayoutParams(new ViewGroup.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.MATCH_PARENT));
		rootView.setId(android.R.id.primary);
		setContentView(rootView);

		site = getIntent().getParcelableExtra("_site");

		if (!(getApplication() instanceof MyApplication)) {
			setFail(101, null, false, true); // #101
			return;
		}

		startLoader();
	}

	@Override
	protected void onDestroy() {
		if (loader != null) {
			loader.stop();
		}
		super.onDestroy();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 1) {
			setResult(resultCode, data);
			finish();
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	private void startLoader() {
		if (loader != null) {
			loader.stop();
		}
		loader = new Loader();
		loader.start();
	}

	private void setFail(int errorCode, Exception ex, boolean retryBtn,
			boolean upload) {
		rootView.removeAllViews();
		TextView text = new TextView(this);
		text.setText("无法载入页面 #" + (errorCode > 0 ? errorCode : 100)); // #100
		if (ex != null) {
			text.append("\n");
			text.append(ex.toString());
		}
		if (retryBtn) {
			text.setLayoutParams(new LinearLayout.LayoutParams(
					ViewGroup.LayoutParams.WRAP_CONTENT,
					ViewGroup.LayoutParams.WRAP_CONTENT,
					Gravity.CENTER_HORIZONTAL));

			Button btn = new Button(this);
			btn.setText("重试");
			btn.setLayoutParams(new LinearLayout.LayoutParams(
					ViewGroup.LayoutParams.WRAP_CONTENT,
					ViewGroup.LayoutParams.WRAP_CONTENT,
					Gravity.CENTER_HORIZONTAL));
			btn.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					startLoader();
				}
			});

			LinearLayout ll = new LinearLayout(this);
			ll.setOrientation(LinearLayout.VERTICAL);
			ll.setLayoutParams(new FrameLayout.LayoutParams(
					ViewGroup.LayoutParams.WRAP_CONTENT,
					ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
			ll.addView(text);
			ll.addView(btn);
			rootView.addView(ll);
		} else {
			text.setLayoutParams(new FrameLayout.LayoutParams(
					ViewGroup.LayoutParams.WRAP_CONTENT,
					ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
			rootView.addView(text);
		}
	}

	private class Loader implements RepositoryManager.StatusChangeListener {
		RepositoryManager repoManager;
		FragmentSpec fragment;
		FileSpec[] depsList;
		boolean isDownloading;
		Handler handler;

		public Loader() {
			repoManager = ((MyApplication) getApplication())
					.repositoryManager();
		}

		public void start() {
			handler = new Handler(Looper.getMainLooper());
			setLoading();

			if (site == null) {
				setFail(102, null, false, true); // #102
			} else {
				repoManager.addFiles(site.files());
				doRepo();
			}
		}

		private void doRepo() {
			int error = 0;
			do {
				Uri uri = getIntent().getData();
				if (uri == null) {
					error = 105; // #105
					break;
				}
				String host = uri.getHost();
				if (host == null) {
					error = 106; // #106
					break;
				}
				fragment = site.getFragment(host);
				if (fragment == null) {
					error = 107; // #107
					break;
				}
				if (TextUtils.isEmpty(fragment.code())) {
					setDone();
					return;
				}
				if (repoManager.getStatus(fragment.code()) == null) {
					// maybe repoManager is empty
					repoManager.addFiles(site.files());
				}
				ArrayList<FileSpec> deps = new ArrayList<FileSpec>();
				if (!repoManager.appendDepsList(deps, fragment.code())) {
					error = 108; // #108
					break;
				}
				depsList = deps.toArray(new FileSpec[deps.size()]);
			} while (false);
			if (error > 0) {
				setFail(error, null, false, true);
				return;
			}

			boolean missing = false;
			for (FileSpec file : depsList) {
				if (repoManager.getStatus(file.id()) != RepositoryManager.STATUS_DONE) {
					missing = true;
					break;
				}
			}
			if (missing) {
				isDownloading = true;

				repoManager.addListener(this);
				repoManager.require(depsList);
			} else {
				setDone();
			}
		}

		public void stop() {
			if (isDownloading && depsList != null) {
				repoManager.dismiss(depsList);
				depsList = null;
			}
			isDownloading = false;

			if (LoaderActivity.this.loader == this) {
				LoaderActivity.this.loader = null;
			}
		}

		private void setLoading() {
			if (LoaderActivity.this.loader != this)
				return;

			rootView.removeAllViews();
			ProgressBar pb = new ProgressBar(LoaderActivity.this);
			pb.setIndeterminate(true);
			pb.setLayoutParams(new FrameLayout.LayoutParams(
					ViewGroup.LayoutParams.WRAP_CONTENT,
					ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
			rootView.addView(pb);

			// show file list and status
			TextView tv = new TextView(LoaderActivity.this);
			tv.setTag("FileListAndStatus");
			tv.setLayoutParams(new FrameLayout.LayoutParams(
					ViewGroup.LayoutParams.FILL_PARENT,
					ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP));
			rootView.addView(tv);
			updateFileListAndStatus();
		}

		private void setFail(int errorCode, Exception ex, boolean retryBtn,
				boolean upload) {
			if (LoaderActivity.this.loader != this)
				return;

			LoaderActivity.this.setFail(errorCode, ex, retryBtn, upload);
			stop();
		}

		private void setDone() {
			if (LoaderActivity.this.loader != this)
				return;

			rootView.removeAllViews();

			FileSpec fs = site.getFile(fragment.code());
			MyClassLoader classLoader = MyClassLoader.getClassLoader(site, fs);
			loaded = classLoader != null;

			if (loaded) {
				Intent intent = new Intent(getIntent().getAction(), getIntent()
						.getData());
				if (getIntent().getExtras() != null) {
					intent.putExtras(getIntent().getExtras());
				}
				intent.putExtra("_site", site);
				intent = urlMap(intent);
				try {
					// check if it open myself to avoid infinite loop
					List<ResolveInfo> l = getPackageManager()
							.queryIntentActivities(intent, 0);
					if (l.size() == 1) {
						ResolveInfo ri = l.get(0);
						if (getPackageName()
								.equals(ri.activityInfo.packageName)) {
							if (LoaderActivity.this.getClass().getName()
									.equals(ri.activityInfo.name)) {
								setFail(121, null, false, true); // #121
								return;
							}
						}
					} else if (l.size() > 1) {
						// should not happen, do we allow this?
					}
					startActivityForResult(intent, 1);
					// do a switch without transition animation
					overridePendingTransition(0, 0);
				} catch (Exception e) {
					setFail(122, null, false, true); // #122
					Log.e("loader", "fail to start activity", e);
					return;
				}
			} else {
				setFail(120, null, false, true); // #120
				return;
			}

			stop();
		}

		void updateFileListAndStatus() {
			TextView tv = (TextView) rootView
					.findViewWithTag("FileListAndStatus");
			if (tv == null)
				return;
			if (depsList == null || depsList.length == 0) {
				tv.setText(null);
				return;
			}
			ForegroundColorSpan ready = new ForegroundColorSpan(Color.BLACK);
			ForegroundColorSpan idle = new ForegroundColorSpan(Color.GRAY);
			ForegroundColorSpan running = new ForegroundColorSpan(Color.BLUE);
			SpannableStringBuilder sb = new SpannableStringBuilder();
			for (FileSpec f : depsList) {
				int start = sb.length();
				sb.append(f.url());
				int end = sb.length();
				Object status = repoManager.getStatus(f.id());
				if (status == RepositoryManager.STATUS_DONE) {
					sb.setSpan(ready, start, end, 0);
				} else if (status == RepositoryManager.STATUS_IDLE) {
					sb.setSpan(idle, start, end, 0);
				} else if (status == RepositoryManager.STATUS_RUNNING) {
					sb.setSpan(running, start, end, 0);
				}
				sb.append('\n');
			}
			tv.setText(sb);
		}

		@Override
		public void onStatusChanged(FileSpec file, String newStatus) {
			// in background thread
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					updateFileListAndStatus();
				}
			});

			if (newStatus == RepositoryManager.STATUS_RUNNING)
				return;
			FileSpec[] depsList = this.depsList;
			if (isDownloading && depsList != null
					&& Arrays.asList(depsList).contains(file)) {
				if (newStatus == RepositoryManager.STATUS_IDLE) {
					handler.post(new Runnable() {
						@Override
						public void run() {
							setFail(110, null, true, false); // #110
						}
					});
				} else {
					int doneCount = 0, runningCount = 0;
					for (FileSpec f : depsList) {
						String status = repoManager.getStatus(f.id());
						if (status == RepositoryManager.STATUS_DONE) {
							doneCount++;
						} else if (status == RepositoryManager.STATUS_RUNNING) {
							runningCount++;
						}
					}
					if (doneCount == depsList.length) {
						handler.post(new Runnable() {
							@Override
							public void run() {
								setDone();
							}
						});
					} else if (newStatus != RepositoryManager.STATUS_DONE
							&& runningCount == 0) {
						handler.post(new Runnable() {
							@Override
							public void run() {
								setFail(111, null, true, false); // #111
							}
						});
					} else {
						// not finished, do nothing
					}
				}
			}
		}
	}

}
