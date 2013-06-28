package com.dianping.loader.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Parcel;
import android.os.Parcelable;

public class FileSpec implements Parcelable {
	/**
	 * Download when required<br>
	 * 需要时再下载
	 */
	public static final int DOWN_NONE = 0;
	/**
	 * Try to download in background if Wifi or faster network is available<br>
	 * Wifi网络下尝试后台下载
	 */
	public static final int DOWN_WIFI = 1;
	/**
	 * Try to download in background if 3G or faster network is available<br>
	 * 3G或Wifi网络下尝试后台下载
	 */
	public static final int DOWN_3G = 2;
	/**
	 * Try to download in background<br>
	 * 任何时候都尝试后台下载
	 */
	public static final int DOWN_ALWAYS = 5;

	private String id;
	private String url;
	private String md5;
	private int down;
	private int length;
	private String[] deps;

	public FileSpec(String id, String url, String md5, int down, int length,
			String[] deps) {
		this.id = id;
		this.url = url;
		this.md5 = md5;
		this.down = down;
		this.length = length;
		this.deps = deps;
	}

	public FileSpec(JSONObject json) throws JSONException {
		id = json.getString("id");
		url = json.getString("url");
		md5 = json.optString("md5");
		down = json.optInt("down", 0);
		length = json.optInt("length", 0);
		JSONArray arr = json.optJSONArray("deps");
		if (arr != null) {
			deps = new String[arr.length()];
			for (int i = 0; i < deps.length; i++) {
				deps[i] = arr.getString(i);
			}
		}
	}

	public String id() {
		return id;
	}

	public String url() {
		return url;
	}

	public String md5() {
		return md5;
	}

	public int down() {
		return down;
	}

	public int length() {
		return length;
	}

	public String[] deps() {
		return deps;
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof FileSpec))
			return false;
		return id.equals(((FileSpec) o).id);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(id);
		if (deps != null && deps.length > 0) {
			sb.append(':');
			sb.append(deps[0]);
			for (int i = 1; i < deps.length; i++) {
				sb.append(',').append(deps[i]);
			}
		}
		return sb.toString();
	}

	//
	// Parcelable
	//

	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeString(id);
		out.writeString(url);
		out.writeString(md5);
		out.writeInt(down);
		out.writeInt(length);
		out.writeStringArray(deps);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	public static final Parcelable.Creator<FileSpec> CREATOR = new Parcelable.Creator<FileSpec>() {
		public FileSpec createFromParcel(Parcel in) {
			return new FileSpec(in);
		}

		public FileSpec[] newArray(int size) {
			return new FileSpec[size];
		}
	};

	protected FileSpec(Parcel in) {
		id = in.readString();
		url = in.readString();
		md5 = in.readString();
		down = in.readInt();
		length = in.readInt();
		deps = in.createStringArray();
	}
}
