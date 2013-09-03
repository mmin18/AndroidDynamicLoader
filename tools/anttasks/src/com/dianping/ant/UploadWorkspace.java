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

public class UploadWorkspace extends Task {
	private File dir;

	/**
	 * the workspace dir
	 */
	public void setSrc(File f) {
		dir = f;
	}

	@Override
	public void execute() throws BuildException {
		if (dir == null) {
			throw new BuildException("src is missing");
		}
		if (!dir.isDirectory()) {
			throw new BuildException(dir + " is not a directory");
		}

		JSONObject site;
		{
			File f = new File(dir, "bin");
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
				File path = new File(dir, url.substring("file://".length()));
				if (path.length() == 0) {
					throw new BuildException(id + " missing: " + path);
				}

				FileInputStream fis = new FileInputStream(path);
				byte[] bytes = new byte[fis.available()];
				fis.read(bytes);
				fis.close();

				MessageDigest md = MessageDigest.getInstance("MD5");
				byte[] md5b = md.digest(bytes);
				String md5 = UploadSite.byteArrayToHexString(md5b);
				if (!md5.equals(file.getString("md5"))) {
					throw new BuildException("md5 not match, " + md5 + "!="
							+ file.getString("md5") + "\n" + path);
				}

				UploadSite.upload("/repo/" + id + "/" + md5 + ".apk", bytes);
			}
		} catch (Exception e) {
			throw new BuildException("fail to upload workspace", e);
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
}
