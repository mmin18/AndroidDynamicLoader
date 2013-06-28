package com.dianping.ant;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.json.JSONArray;
import org.json.JSONObject;

public class GenSite extends Task {

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

		File myApk = new File(src, "bin");
		myApk = new File(myApk, name + ".apk");
		if (myApk.length() == 0) {
			System.out.println(myApk + " is missing");
			File bin = new File(src, "bin");
			File site = new File(bin, "site.txt");
			site.delete();
			return;
		}

		final ArrayList<File> directLibrary = new ArrayList<File>();
		File propProj = new File(src, "project.properties");
		if (propProj.length() > 0) {
			Properties p = new Properties();
			try {
				FileInputStream fis = new FileInputStream(propProj);
				p.load(fis);
				fis.close();
			} catch (Exception e) {
			}

			int f = 0, i = 0;
			while (f < 10) {
				Object v = p.get("android.library.reference." + (++i));
				if (v == null) {
					f++;
					continue;
				}
				File dir = new File(src, String.valueOf(v));
				directLibrary.add(dir);
			}
		}

		final ArrayList<JSONObject> sites = new ArrayList<JSONObject>();
		for (File lib : directLibrary) {
			File bin = new File(lib, "bin");
			File site = new File(bin, "site.txt");
			if (site.length() == 0) {
				throw new BuildException("site.txt is missing under " + bin);
			}
			JSONObject json;
			try {
				FileInputStream fis = new FileInputStream(site);
				byte[] bytes = new byte[fis.available()];
				fis.read(bytes);
				fis.close();
				String str = new String(bytes, "ASCII");
				json = new JSONObject(str);
			} catch (Exception e) {
				throw new BuildException("unable to read " + site);
			}

			sites.add(json);
		}

		try {
			JSONObject json = new JSONObject();
			SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd");
			String today = fmt.format(new Date());
			json.put("id", name + "." + today + ".0");
			json.put("version", today + ".0");
			System.out.println("site.txt id=" + name + "." + today + ".0");

			//
			// files
			// last is myself
			//
			JSONArray files = new JSONArray();
			final HashMap<String, JSONObject> fileMap = new HashMap<String, JSONObject>();
			for (int i = 0; i < directLibrary.size(); i++) {
				File dir = directLibrary.get(i);
				JSONObject site = sites.get(i);
				JSONArray siteFiles = site.getJSONArray("files");
				for (int j = 0; j < siteFiles.length(); j++) {
					JSONObject f = siteFiles.getJSONObject(j);
					JSONObject exists = fileMap.get(f.getString("id"));
					if (exists != null) {
						if (!exists.getString("md5").equals(f.get("md5"))) {
							throw new BuildException("md5 conflict on "
									+ f.getString("id") + "\n" + dir);
						}
					} else {
						String url = f.getString("url");
						if (url.startsWith("file://")) {
							File apk = new File(dir, url.substring("file://"
									.length()));
							File rel = getRelativeFile(apk, src);
							f.put("url", "file://" + rel);
						}
						files.put(f);
						fileMap.put(f.getString("id"), f);
					}
				}
			}

			// last is myself
			String myFileId = name + "." + today + ".0";
			JSONObject myFile = new JSONObject();
			myFile.put("id", myFileId);
			myFile.put("url", "file://bin/" + name + ".apk");
			myFile.put("md5", md5(myApk));
			if (directLibrary.size() > 0) {
				JSONArray deps = new JSONArray();
				for (JSONObject site : sites) {
					JSONArray siteFiles = site.getJSONArray("files");
					String str = siteFiles
							.getJSONObject(siteFiles.length() - 1).getString(
									"id");
					deps.put(str);
				}
				myFile.put("deps", deps);
			}
			files.put(myFile);

			json.put("files", files);

			//
			// fragments
			//
			ArrayList<JSONObject> fragments = new ArrayList<JSONObject>();
			HashMap<String, JSONObject> fragmentMap = new HashMap<String, JSONObject>();
			for (JSONObject site : sites) {
				JSONArray arr = site.getJSONArray("fragments");
				for (int i = 0; i < arr.length(); i++) {
					JSONObject fragment = arr.getJSONObject(i);
					String host = fragment.getString("host");
					if (fragmentMap.containsKey(host)) {
						System.out.println("dianping://" + host
								+ " is override by " + site.get("id"));
						fragments.remove(fragmentMap.get(host));
					}
					fragments.add(fragment);
					fragmentMap.put(host, fragment);
				}
			}

			// my fragments
			Properties ps = new Properties();
			{
				File f = new File(src, "fragment.properties");
				if (f.length() == 0) {
					System.out.println("fragment.properties is missing");
				} else {
					try {
						FileInputStream fis = new FileInputStream(f);
						ps.load(fis);
						fis.close();
					} catch (Exception e) {
						throw new BuildException("fail to load " + f, e);
					}
				}
			}

			for (Entry<Object, Object> e : ps.entrySet()) {
				String key = String.valueOf(e.getKey()).trim();
				String val = String.valueOf(e.getValue()).trim();
				if (!"default".equals(key)) {
					if (fragmentMap.containsKey(key)) {
						System.out.println("dianping://" + key
								+ " is override by " + myFileId);
						fragments.remove(fragmentMap.get(key));
					}

					JSONObject fragment = new JSONObject();
					fragment.put("host", key);
					fragment.put("code", myFileId);
					fragment.put("name", val);
					fragments.add(fragment);
				}
			}

			JSONArray arr = new JSONArray();
			for (JSONObject fragment : fragments) {
				arr.put(fragment);
			}
			json.put("fragments", arr);

			File output = new File(src, "bin");
			output = new File(output, "site.txt");
			FileOutputStream fos = new FileOutputStream(output);
			fos.write(json.toString(2).getBytes("ASCII"));
			fos.close();

			System.out.println(output);
		} catch (Exception e) {
			throw new BuildException("unable to build site.txt");
		}
	}

	private static String md5(File file) throws Exception {
		FileInputStream fis = new FileInputStream(file);
		byte[] bytes = new byte[fis.available()];
		fis.read(bytes);
		fis.close();

		MessageDigest md = MessageDigest.getInstance("MD5");
		byte[] md5b = md.digest(bytes);
		String md5 = byteArrayToHexString(md5b);
		return md5;
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

	/**
	 * Returns the path of one File relative to another.
	 * 
	 * @param target
	 *            the target directory
	 * @param base
	 *            the base directory
	 * @return target's path relative to the base directory
	 * @throws IOException
	 *             if an error occurs while resolving the files' canonical names
	 */
	public static File getRelativeFile(File target, File base)
			throws IOException {
		String[] baseComponents = base.getCanonicalPath().split(
				Pattern.quote(File.separator));
		String[] targetComponents = target.getCanonicalPath().split(
				Pattern.quote(File.separator));

		// skip common components
		int index = 0;
		for (; index < targetComponents.length && index < baseComponents.length; ++index) {
			if (!targetComponents[index].equals(baseComponents[index]))
				break;
		}

		StringBuilder result = new StringBuilder();
		if (index != baseComponents.length) {
			// backtrack to base directory
			for (int i = index; i < baseComponents.length; ++i)
				result.append(".." + File.separator);
		}
		for (; index < targetComponents.length; ++index)
			result.append(targetComponents[index] + File.separator);
		if (!target.getPath().endsWith("/") && !target.getPath().endsWith("\\")) {
			// remove final path separator
			result.delete(result.length() - File.separator.length(),
					result.length());
		}
		return new File(result.toString());
	}
}
