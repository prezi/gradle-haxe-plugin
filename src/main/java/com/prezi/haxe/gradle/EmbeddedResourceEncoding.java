package com.prezi.haxe.gradle;

import com.google.common.collect.Iterators;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class EmbeddedResourceEncoding {
	public static String encode(Map<String, File> resources) {
		StringBuilder encoded = new StringBuilder();
		for (Map.Entry<String, File> entry : resources.entrySet()) {
			String name = entry.getKey();
			File resource = entry.getValue();
			if (name.contains("@")) {
				throw new IllegalArgumentException("Resource name must not contain '@': " + name);
			}

			if (resource.getName().contains("@")) {
				throw new IllegalArgumentException("Resource file name must not contain '@': " + name);
			}

			if (encoded.length() > 0) {
				encoded.append(" ");
			}

			try {
				encoded.append(URLEncoder.encode(resource.getName(), "utf-8"));
				if (!name.equals(resource.getName())) {
					encoded.append("@");
					encoded.append(URLEncoder.encode(name, "utf-8"));
				}
			} catch (UnsupportedEncodingException ex) {
				throw new AssertionError(ex);
			}
		}

		return encoded.toString();
	}

	public static Map<String, File> decode(String encoded, File baseDir) {
		LinkedHashMap<String, File> resources = new LinkedHashMap<String, File>();
		if (encoded != null && !encoded.isEmpty()) {
			for (String res : encoded.split(" ")) {
				String fileName;
				String name;
				try {
					if (res.indexOf('@') >= 0) {
						Iterator<String> iterator = Iterators.forArray(res.split("@", 2));
						String fileNameEnc = iterator.hasNext() ? iterator.next() : null;
						String nameEnc = iterator.hasNext() ? iterator.next() : null;

						fileName = URLDecoder.decode(fileNameEnc, "utf-8");
						name = URLDecoder.decode(nameEnc, "utf-8");
					} else {
						fileName = URLDecoder.decode(res, "utf-8");
						name = fileName;
					}
				} catch (UnsupportedEncodingException ex) {
					throw new AssertionError(ex);
				}

				resources.put(name, new File(baseDir, fileName));
			}
		}

		return resources;
	}
}
