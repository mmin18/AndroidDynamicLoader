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

public class LaunchWorkspace extends Task {
	private File dir;
	private String defaultHost;
	private boolean debug;

	/**
	 * the workspace dir
	 */
	public void setSrc(File f) {
		dir = f;
	}

	public void setDefaultHost(String d) {
		defaultHost = d;
	}

	public void setDebug(boolean d) {
		debug = d;
	}

	@Override
	public void execute() throws BuildException {
		if (dir == null) {
			throw new BuildException("src is missing");
		}
		if (!dir.isDirectory()) {
			throw new BuildException(dir + " is not a directory");
		}

		File f = new File(dir, "workspace.properties");
		if (f.length() > 0) {
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
		}

		if (defaultHost == null || defaultHost.length() == 0) {
			File site = new File(dir, "bin");
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
