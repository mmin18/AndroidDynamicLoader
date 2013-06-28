package com.dianping.ant;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.json.JSONArray;
import org.json.JSONObject;

public class GenWorkspace extends Task {
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

		HashMap<String, JSONObject> files = new HashMap<String, JSONObject>();
		HashMap<String, JSONObject> fragments = new HashMap<String, JSONObject>();

		for (File proj : dir.listFiles()) {
			File bin = new File(proj, "bin");
			File site = new File(bin, "site.txt");
			if (!site.isFile())
				continue;

			System.out.println(proj.getName());
			try {
				FileInputStream fis = new FileInputStream(site);
				byte[] bytes = new byte[fis.available()];
				fis.read(bytes);
				fis.close();

				String str = new String(bytes, "utf-8");
				JSONObject j = new JSONObject(str);
				JSONArray siteFiles = j.getJSONArray("files");
				for (int i = 0; i < siteFiles.length(); i++) {
					JSONObject file = siteFiles.getJSONObject(i);
					String id = file.getString("id");
					JSONObject old = files.get(id);
					if (old == null) {
						// project path -> workspace path
						String url = file.getString("url");
						if (url.startsWith("file://")) {
							File path = new File(proj, url.substring("file://"
									.length()));
							File rel = GenSite.getRelativeFile(path, dir);
							file.put("url", "file://" + rel);
						}
						files.put(id, file);
					} else {
						String md5a = file.getString("md5");
						String md5b = old.getString("md5");
						if (!md5a.equals(md5b)) {
							// break
							throw new BuildException("md5 conflict for file "
									+ id + " in " + proj.getName());
						}
					}
				}
				JSONArray siteFragments = j.getJSONArray("fragments");
				for (int i = 0; i < siteFragments.length(); i++) {
					JSONObject fragment = siteFragments.getJSONObject(i);
					String host = fragment.getString("host");
					JSONObject old = fragments.get(host);
					if (old == null) {
						fragments.put(host, fragment);
					} else {
						// continue;
						System.out.println("override " + host + " as "
								+ proj.getName() + "/"
								+ fragment.getString("name"));
						fragments.put(host, fragment);
					}
				}
			} catch (BuildException e) {
				throw e;
			} catch (Exception e) {
				System.out.println("fail to open " + site);
				e.printStackTrace(System.err);
			}
		}
		try {
			JSONObject json = new JSONObject();
			SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd");
			String today = fmt.format(new Date());
			json.put("id", "all." + today + ".0");
			json.put("version", today + ".0");
			System.out.println("site.txt id=all." + today + ".0");

			JSONArray siteFiles = new JSONArray();
			for (JSONObject file : files.values()) {
				siteFiles.put(file);
			}
			json.put("files", siteFiles);

			JSONArray siteFragments = new JSONArray();
			for (JSONObject fragment : fragments.values()) {
				siteFragments.put(fragment);
			}
			json.put("fragments", siteFragments);

			File output = new File(dir, "bin");
			output.mkdirs();
			output = new File(output, "site.txt");
			FileOutputStream fos = new FileOutputStream(output);
			fos.write(json.toString(2).getBytes("ASCII"));
			fos.close();

			System.out.println(output);
		} catch (Exception e) {
			throw new BuildException("unable to build site.txt");
		}
	}
}
