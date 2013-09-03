package com.dianping.ant;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.json.JSONArray;
import org.json.JSONObject;

public class LaunchSite extends Task {
	private File src = null;
	private String name = null;
	private String defaultHost;
	private boolean debug;

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

	public void setDefaultHost(String d) {
		defaultHost = d;
	}

	public void setDebug(boolean d) {
		debug = d;
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

		File f = new File(src, "fragment.properties");
		if (f.length() == 0) {
			throw new BuildException("fragment.properties missing");
		}
		Properties ps = new Properties();
		try {
			FileInputStream fis = new FileInputStream(f);
			ps.load(fis);
			fis.close();
		} catch (Exception e) {
			throw new BuildException("fail to load " + f, e);
		}
		if (defaultHost == null || defaultHost.length() == 0) {
			for (Entry<Object, Object> e : ps.entrySet()) {
				String key = String.valueOf(e.getKey()).trim();
				String val = String.valueOf(e.getValue()).trim();
				if ("default".equals(key)) {
					defaultHost = val;
				}
			}

			if (defaultHost != null && defaultHost.length() > 0) {
				System.out.println("defaultHost = " + defaultHost);
			}
		}

		if (defaultHost == null || defaultHost.length() == 0) {
			File site = new File(src, "bin");
			site = new File(site, "site.txt");
			String[] hosts;
			try {
				FileInputStream fis = new FileInputStream(site);
				byte[] bytes = new byte[fis.available()];
				fis.read(bytes);
				fis.close();
				String str = new String(bytes, "UTF-8");
				JSONObject json = new JSONObject(str);
				JSONArray fragments = json.getJSONArray("fragments");
				hosts = new String[fragments.length()];
				for (int i = 0; i < hosts.length; i++) {
					JSONObject fr = fragments.getJSONObject(i);
					String host = fr.getString("host");
					hosts[i] = host;
				}
			} catch (Exception e) {
				throw new BuildException("fail to read site.txt", e);
			}

			SelectFrame sf = new SelectFrame(hosts);
			defaultHost = sf.doModel();
		}

		if (defaultHost == null || defaultHost.length() == 0) {
			throw new BuildException("no host to launch");
		}

		try {
			String crlf = "\r\n";
			Socket s = new Socket(InetAddress.getByName("127.0.0.1"), 5036);
			OutputStream os = s.getOutputStream();
			StringBuilder sb = new StringBuilder();
			sb.append(debug ? "GET /debug/" : "GET /go/").append(defaultHost)
					.append(" HTTP/1.0").append(crlf);
			sb.append(crlf);
			os.write(sb.toString().getBytes("ASCII"));

			os.close();
			s.close();
		} catch (Exception e) {
			throw new BuildException("fail to start " + defaultHost, e);
		}
	}
}
