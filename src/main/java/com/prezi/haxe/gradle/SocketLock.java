package com.prezi.haxe.gradle;

import com.google.common.base.Throwables;

import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SocketLock {
	private static final int DEFAULT_PORT = 7055;
	private static final int SLEEP_TIME = 2000;

	private static Lock localLock = new ReentrantLock();

	private Socket socket;
	private InetSocketAddress socketAddress;


	public SocketLock() {
		socket = new Socket();
		try {
			socketAddress = new InetSocketAddress(InetAddress.getLocalHost().getHostName(), DEFAULT_PORT);
		} catch (UnknownHostException e) {
			Throwables.propagate(e);
		}
	}

	public void lock(long timeoutMillis) {
		long end = System.currentTimeMillis() + timeoutMillis;
		localLock.lock();
		try {
			while(System.currentTimeMillis() < end) {
				try {
					if (lockFree()) {
						return;
					}
					Thread.sleep((long) (SLEEP_TIME * Math.random()));
				} catch (InterruptedException e) {
					Throwables.propagate(e);
				} catch (IOException e) {
					Throwables.propagate(e);
				}
			}
			throw new RuntimeException(
					new StringBuilder().append("Timout for lock on port:").append(DEFAULT_PORT)
							.append(", timeout after: ").append(SLEEP_TIME).toString());
		} finally {
			localLock.unlock();
		}
	}
	public void unlock() {
		try {
			if (socket.isBound()) {
				socket.close();
			}
		} catch (IOException e) {
			Throwables.propagate(e);
		}
	}

	private boolean lockFree() throws IOException {
		try {
			socket.bind(socketAddress);
			return true;
		} catch (BindException e) {
			return false;
		} catch (SocketException e) {
			return false;
		}
	}
}
