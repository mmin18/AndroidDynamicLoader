package com.dianping.ant;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;

public class UpdateProject {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		System.out
				.println("Prepare your android project to build to a plugin.");
		if (args.length < 2) {
			System.out.println("Useage:");
			System.out.println("  update <sdk> <workspace or project>");
			System.out.println("    - search and update all projects which");
			System.out.println("      contains \"fragment.properties\"");
			return;
		}
		File sdkdir = new File(args[0]);
		if (!sdkdir.isDirectory()) {
			System.out.println(sdkdir + " is not a directory");
			System.exit(1);
		}
		byte[] host;
		{
			File f = new File(sdkdir, "lib");
			f = new File(f, "host.jar");
			if (!f.isFile()) {
				System.out.println(f + " not found");
				System.exit(1);
			}
			FileInputStream fis = new FileInputStream(f);
			host = new byte[fis.available()];
			fis.read(host);
			fis.close();
		}

		File pluginXml = new File(sdkdir, "build-plugin.xml");
		if (!pluginXml.isFile()) {
			System.out.println(pluginXml + " not found");
			System.exit(1);
		}
		File workspaceXml = new File(sdkdir, "build-workspace.xml");
		if (!workspaceXml.isFile()) {
			System.out.println(workspaceXml + " not found");
			System.exit(1);
		}

		File basedir = new File(args[1]);
		if (!basedir.isDirectory()) {
			System.out.println(basedir + " is not a directory");
			System.exit(1);
		}

		ArrayList<File> dirs = new ArrayList<File>();
		search(basedir, dirs);

		// if more than one plugin in a dir, it is a workspace
		HashMap<File, Integer> workspaces = new HashMap<File, Integer>();

		for (File dir : dirs) {
			File am = new File(dir, "AndroidManifest.xml");
			FileInputStream fis = new FileInputStream(am);
			byte[] buf = new byte[4096];
			int l = fis.read(buf);
			fis.close();
			String str = new String(buf, 0, l, "UTF-8");
			int i = str.indexOf("package=\"");
			if (i < 0) {
				System.out.println(" [plugin]       fail to process " + am);
				continue;
			}
			int j = str.indexOf('\"', i + "package=\"".length());
			if (j < 0) {
				System.out.println(" [plugin]       fail to process " + am);
				continue;
			}
			String packageName = str.substring(i + "package=\"".length(), j);
			System.out.println(" [plugin]       " + dir + ", package = "
					+ packageName);

			String androidCmd = System.getProperty("os.name").startsWith(
					"Windows") ? "android.bat" : "android";
			Process p = Runtime.getRuntime().exec(
					new String[] { androidCmd, "update", "project", "-p",
							dir.getAbsolutePath(), "-n", packageName, "-t",
							"android-16" });
			int pcode = p.waitFor();
			if (pcode == 0) {
				System.out.println("                android update success");
			} else {
				System.out.println("                android update fail ("
						+ pcode + ")");
			}

			File bxml = new File(dir, "build.xml");
			fis = new FileInputStream(bxml);
			buf = new byte[fis.available()];
			fis.read(buf);
			fis.close();
			str = new String(buf, "UTF-8");
			str = str.replace("${sdk.dir}/tools/ant/build.xml",
					String.valueOf(getRelativeFile(pluginXml, dir)));
			FileOutputStream fos = new FileOutputStream(bxml);
			fos.write(str.getBytes("UTF-8"));
			fos.close();

			File llib = new File(dir, "libs");
			llib = new File(llib, "host.jar");
			fos = new FileOutputStream(llib);
			fos.write(host);
			fos.close();

			File ws = dir.getParentFile();
			if (workspaces.get(ws) != null) {
				workspaces.put(ws, workspaces.get(ws) + 1);
			} else {
				workspaces.put(ws, 1);
			}
		}

		for (File ws : workspaces.keySet()) {
			if (workspaces.get(ws) > 1) {
				StringBuilder sb = new StringBuilder();
				sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
				sb.append("<project name=\"dev\" default=\"help\">\n");
				sb.append("    <import file=\"")
						.append(getRelativeFile(workspaceXml, ws))
						.append("\" />\n");
				sb.append("</project>\n");

				File buildXml = new File(ws, "build.xml");
				FileOutputStream fos = new FileOutputStream(buildXml);
				fos.write(sb.toString().getBytes("utf-8"));
				fos.close();

				System.out.println(" [workspace]    workspace " + ws + ", "
						+ workspaces.get(ws) + " plugins");
			}
		}
	}

	static boolean isPlugin(File dir) {
		File am = new File(dir, "AndroidManifest.xml");
		if (am.exists()) {
			File f = new File(dir, "fragment.properties");
			if (f.exists()) {
				return true;
			}
		}
		return false;
	}

	static void search(File dir, ArrayList<File> output) {
		if (isPlugin(dir)) {
			output.add(dir);
		} else {
			File[] files = dir.listFiles();
			for (File sub : files) {
				if (sub.isDirectory()) {
					search(sub, output);
				}
			}
		}
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
	static File getRelativeFile(File target, File base) throws IOException {
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
