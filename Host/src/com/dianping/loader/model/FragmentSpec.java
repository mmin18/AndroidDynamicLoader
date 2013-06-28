package com.dianping.loader.model;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.Parcel;
import android.os.Parcelable;

public class FragmentSpec implements Parcelable {
	private String host;
	private String code;
	private String name;

	public FragmentSpec(String host, String code, String name) {
		this.host = host;
		this.code = code;
		this.name = name;
	}

	public FragmentSpec(JSONObject json) throws JSONException {
		host = json.getString("host");
		code = json.optString("code");
		name = json.getString("name");
	}

	public String host() {
		return host;
	}

	public String code() {
		return code;
	}

	public String name() {
		return name;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("xxx://").append(host).append(':');
		sb.append(name);
		sb.append('(');
		if (code == null) {
			sb.append('.');
		} else {
			sb.append(code);
		}
		sb.append(')');
		return sb.toString();
	}

	//
	// Parcelable
	//

	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeString(host);
		out.writeString(code);
		out.writeString(name);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	public static final Parcelable.Creator<FragmentSpec> CREATOR = new Parcelable.Creator<FragmentSpec>() {
		public FragmentSpec createFromParcel(Parcel in) {
			return new FragmentSpec(in);
		}

		public FragmentSpec[] newArray(int size) {
			return new FragmentSpec[size];
		}
	};

	protected FragmentSpec(Parcel in) {
		host = in.readString();
		code = in.readString();
		name = in.readString();
	}
}
