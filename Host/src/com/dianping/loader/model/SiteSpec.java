package com.dianping.loader.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Parcel;
import android.os.Parcelable;

public class SiteSpec implements Parcelable {
	private String id;
	private String version;
	private FileSpec[] files;
	private FragmentSpec[] fragments;

	public SiteSpec(String id, String version, FileSpec[] files,
			FragmentSpec[] fragments) {
		this.id = id;
		this.version = version;
		this.files = files;
		this.fragments = fragments;
	}

	public SiteSpec(JSONObject json) throws JSONException {
		id = json.getString("id");
		version = json.getString("version");
		JSONArray arr = json.getJSONArray("files");
		files = new FileSpec[arr.length()];
		for (int i = 0; i < files.length; i++) {
			JSONObject obj = arr.getJSONObject(i);
			FileSpec f = new FileSpec(obj);
			files[i] = f;
		}
		arr = json.getJSONArray("fragments");
		fragments = new FragmentSpec[arr.length()];
		for (int i = 0; i < fragments.length; i++) {
			JSONObject obj = arr.getJSONObject(i);
			FragmentSpec f = new FragmentSpec(obj);
			fragments[i] = f;
		}
	}

	public String id() {
		return id;
	}

	public String version() {
		return version;
	}

	public FileSpec[] files() {
		return files;
	}

	public FragmentSpec[] fragments() {
		return fragments;
	}

	public FragmentSpec getFragment(String host) {
		for (FragmentSpec f : fragments) {
			if (host.equalsIgnoreCase(f.host())) {
				return f;
			}
		}
		return null;
	}

	public FileSpec getFile(String id) {
		for (FileSpec f : files) {
			if (id.equals(f.id())) {
				return f;
			}
		}
		return null;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(id);
		if (!id.contains(version)) {
			sb.append(" v").append(version);
		}
		sb.append(" (").append(files.length).append(" files, ")
				.append(fragments.length).append(" fragments)");
		return sb.toString();
	}

	//
	// Parcelable
	//

	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeString(id);
		out.writeTypedArray(files, 0);
		out.writeTypedArray(fragments, 0);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	public static final Parcelable.Creator<SiteSpec> CREATOR = new Parcelable.Creator<SiteSpec>() {
		public SiteSpec createFromParcel(Parcel in) {
			return new SiteSpec(in);
		}

		public SiteSpec[] newArray(int size) {
			return new SiteSpec[size];
		}
	};

	protected SiteSpec(Parcel in) {
		id = in.readString();
		files = in.createTypedArray(FileSpec.CREATOR);
		fragments = in.createTypedArray(FragmentSpec.CREATOR);
	}
}
