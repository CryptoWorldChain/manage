package org.brewchain.manage.util;

import java.io.File;
import java.io.IOException;

public class ShellRunner extends Thread {
	private Process proc;
	private String dir;
	private String shell;
	private File tmpFile;

	public ShellRunner(String dir, String shell) throws IOException {
		super();
		this.proc = null;
		this.dir = dir;
		this.shell = shell;
		this.tmpFile = createTempFile(dir, shell);
	}

	@Override
	public void run() {
		try {
			ProcessBuilder builder = new ProcessBuilder("sh", "-c",
					"strace -o " + tmpFile.getPath() + " -f -e trace=setsid setsid sh " + dir + shell);
			builder.directory(new File(dir));

			proc = builder.start();
			System.out.println("Running ...");
			int exitValue = proc.waitFor();
			System.out.println("Exit Value: " + exitValue);
		} catch (IOException e) {
			e.getLocalizedMessage();
		} catch (InterruptedException e) {
			e.getLocalizedMessage();
		}
	}

	public void kill() {
		if (this.getState() != State.TERMINATED) {
			try {
				ProcessBuilder builder = new ProcessBuilder("sh", "-c",
						"ps -o sid,pgid ax | " + "grep $(grep -e \"setsid()\" -e \"<... setsid resumed>\" "
								+ tmpFile.getPath() + " | awk '{printf \" -e\" $NF}') | awk {'print $NF'} | "
								+ "sort | uniq | sed 's/^/-/g' | xargs kill -9 2>/dev/null");
				builder.directory(new File(dir));
				Process proc = builder.start();
				proc.waitFor();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private File createTempFile(String dir, String prefix) throws IOException {
		String name = "." + prefix + "-" + System.currentTimeMillis();
		File tempFile = new File(dir, name);
		if (tempFile.createNewFile()) {
			return tempFile;
		}
		throw new IOException("Failed to create file " + tempFile.getPath() + ".");
	}
}
