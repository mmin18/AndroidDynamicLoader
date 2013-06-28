package com.dianping.ant;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

public class GetAnyBuildTools extends Task {
	private File sdkdir;
	private String name;

	/**
	 * the android sdk dir
	 */
	public void setSdkDir(File f) {
		sdkdir = f;
	}

	/**
	 * output property name
	 */
	public void setName(String s) {
		name = s;
	}

	@Override
	public void execute() throws BuildException {
		if (sdkdir == null || !sdkdir.isDirectory()) {
			throw new BuildException("dir is missing");
		}

		// build-tools
		File buildTools = new File(sdkdir, "build-tools");
		if (buildTools.isDirectory()) {
			if (isBuildToolsDir(buildTools)) {
				getProject().setProperty(name, buildTools.getAbsolutePath());
				return;
			}
			File[] files = buildTools.listFiles();
			// start with the highest version
			Arrays.sort(files, new Comparator<File>() {
				@Override
				public int compare(File f1, File f2) {
					return f2.getName().compareTo(f1.getName());
				}
			});
			for (File f : files) {
				if (f.isDirectory() && isBuildToolsDir(f)) {
					getProject().setProperty(name, f.getAbsolutePath());
					return;
				}
			}
		}

		// platform-tools in old version sdk (<17)
		File platformTools = new File(sdkdir, "platform-tools");
		if (platformTools.isDirectory() && isBuildToolsDir(platformTools)) {
			getProject().setProperty(name, platformTools.getAbsolutePath());
			return;
		}

		throw new BuildException("build tools directory not found");
	}

	private boolean isBuildToolsDir(File dir) {
		if (new File(dir, "aapt").exists() && new File(dir, "dx").exists())
			return true;
		if (new File(dir, "aapt.exe").exists()
				&& new File(dir, "dx.bat").exists())
			return true;
		return false;
	}
}
