package com.prezi.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class BufferedStdOutputStreams {

	private List<Entry> entries = new ArrayList<Entry>();

	private enum Type {
		STDOUT, STDERR
	}

	private void write(Type type, byte[] bytes) throws IOException {
		if (!entries.isEmpty() && entries.get(entries.size()-1).type.equals(type)) {
			Entry last = entries.get(entries.size()-1);
			last.bos.write(bytes);
		} else {
			entries.add(new Entry(type, bytes));
		}
	}

	public OutputStream getStdOutOutputStream() {
		return new RedirectedOS(Type.STDOUT);
	}

	public OutputStream getStdErrOutputStream() {
		return new RedirectedOS(Type.STDERR);
	}

	public void printOutput() {
		for (Entry e : entries) {
			if (e.type.equals(Type.STDOUT)) {
				System.out.print(new String(e.bos.toByteArray()));
			} else if (e.type.equals(Type.STDERR)) {
				System.err.print(new String(e.bos.toByteArray()));
			} else {
				throw new RuntimeException("Unknown type: " + e.type);
			}
		}
	}

	public String getErrorOut() {
		StringBuilder sb = new StringBuilder();
		for (Entry e : entries) {
			 if (e.type.equals(Type.STDERR)) {
				 sb.append(new String(e.bos.toByteArray()));
			}
		}
		return sb.toString();
	}

	private class Entry {
		private Type type;
		private ByteArrayOutputStream bos = new ByteArrayOutputStream();

		public Entry(Type type, byte[] bytes) throws IOException {
			this.type = type;
			this.bos.write(bytes);
		}
	}

	private class RedirectedOS extends OutputStream {

		private Type type;

		public RedirectedOS(Type type) {
			this.type = type;
		}

		@Override
		public void write(int b) throws IOException {
			write(new byte[]{(byte)b});
		}

		public void write(byte b[]) throws IOException {
			BufferedStdOutputStreams.this.write(type, b);
		}
	}

}
