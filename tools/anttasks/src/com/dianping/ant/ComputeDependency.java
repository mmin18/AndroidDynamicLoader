package com.dianping.ant;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Path;

public class ComputeDependency extends Task {

	private File dir;
	private String refid;

	/**
	 * the workspace dir
	 */
	public void setSrc(File f) {
		dir = f;
	}

	public void setRefid(String str) {
		refid = str;
	}

	@Override
	public void execute() throws BuildException {
		if (dir == null) {
			throw new BuildException("dir is missing");
		}
		if (!dir.isDirectory()) {
			throw new BuildException(dir + " not exists");
		}
		if (refid == null || refid.length() == 0) {
			throw new BuildException("refid is missing");
		}

		ArrayList<File> deps = new ArrayList<File>();
		if (new File(dir, "AndroidManifest.xml").isFile()) {
			// it's a project
			appendDependency(dir, deps);
		} else {
			// it's a workspace
			for (File proj : dir.listFiles()) {
				appendDependency(proj, deps);
			}
		}
		Path p = new Path(getProject());
		for (File f : deps) {
			p.createPathElement().setLocation(f);
		}
		getProject().addReference(refid, p);
	}

	private void appendDependency(File proj, List<File> deps) {
		{
			File f = new File(proj, "AndroidManifest.xml");
			if (!f.isFile())
				return;
			f = new File(proj, "fragment.properties");
			if (!f.isFile())
				return;
			f = new File(proj, "build.xml");
			if (!f.isFile())
				return;
		}

		File propProj = new File(proj, "project.properties");
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
				File dir = new File(proj, String.valueOf(v));
				appendDependency(dir, deps);
			}
		}

		if (deps.contains(proj))
			return;
		deps.add(proj);
	}
}
