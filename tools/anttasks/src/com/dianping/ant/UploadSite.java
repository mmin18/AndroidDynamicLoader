package com.dianping.ant;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.security.MessageDigest;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.json.JSONArray;
import org.json.JSONObject;

public class UploadSite extends Task {

	private File src = null;
	private String name = null;

	/**
	 * the project dir
	 */
	public void setSrc(File f) {
		src = f;
	}

	/**
	 * the project name, like dev.home
	 */
	public void setName(String str) {
		name = str;
	}

	@Override
	public void execute() throws BuildException {
		if (src == null) {
			throw new BuildException("src is missing");
		}
		if (!src.isDirectory()) {
			throw new BuildException(src + " is not a directory");
		}
		if (name == null || name.length() == 0) {
			throw new BuildException("name is missing");
		}

		JSONObject site;
		{
			File f = new File(src, "bin");
			f = new File(f, "site.txt");
			if (f.length() == 0) {
				throw new BuildException(f + " missing");
			}
			try {
				FileInputStream fis = new FileInputStream(f);
				byte[] bytes = new byte[fis.available()];
				fis.read(bytes);
				fis.close();

				String str = new String(bytes, "ASCII");
				site = new JSONObject(str);
			} catch (Exception e) {
				throw new BuildException("unable to read " + f);
			}
		}

		try {
			JSONArray files = site.getJSONArray("files");
			for (int i = 0; i < files.length(); i++) {
				JSONObject file = files.getJSONObject(i);
				String id = file.getString("id");
				String url = file.getString("url");
				if (!url.startsWith("file://"))
					continue;
				File path = new File(src, url.substring("file://".length()));
				if (path.length() == 0) {
					throw new BuildException(id + " missing: " + path);
				}

				FileInputStream fis = new FileInputStream(path);
				byte[] bytes = new byte[fis.available()];
				fis.read(bytes);
				fis.close();

				MessageDigest md = MessageDigest.getInstance("MD5");
				byte[] md5b = md.digest(bytes);
				String md5 = byteArrayToHexString(md5b);
				if (!md5.equals(file.getString("md5"))) {
					throw new BuildException("md5 not match, " + md5 + "!="
							+ file.getString("md5") + "\n" + path);
				}

				upload("/repo/" + id + "/" + md5 + ".apk", bytes);
			}
		} catch (Exception e) {
			throw new BuildException("fail to upload " + name, e);
		}

		try {
			String crlf = "\r\n";
			Socket s = new Socket(InetAddress.getByName("127.0.0.1"), 5036);
			OutputStream os = s.getOutputStream();
			StringBuilder sb = new StringBuilder();
			sb.append("PUT /site  HTTP/1.0").append(crlf);
			byte[] bytes = site.toString(2).getBytes("ASCII");
			sb.append("Content-Length: ").append(bytes.length).append(crlf);
			sb.append(crlf);
			os.write(sb.toString().getBytes("ASCII"));
			os.write(bytes);

			os.close();
			s.close();
		} catch (Exception e) {
			throw new BuildException("fail to upload site.txt");
		}
	}

	public static void upload(String path, byte[] bytes) {
		try {
			final String crlf = "\r\n";
			Socket s = new Socket(InetAddress.getByName("127.0.0.1"), 5036);
			OutputStream os = s.getOutputStream();
			StringBuilder sb = new StringBuilder();
			sb.append("PUT ").append(path).append(" HTTP/1.0").append(crlf);
			sb.append("Content-Length: ").append(bytes.length).append(crlf);
			sb.append(crlf);
			os.write(sb.toString().getBytes("ASCII"));
			os.write(bytes);

			os.close();
			s.close();
		} catch (Exception e) {
			throw new BuildException("fail to upload " + path, e);
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
