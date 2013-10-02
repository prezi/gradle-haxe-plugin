package com.prezi.gradle.haxe

class EmbeddedResourceEncoding {
	public static String encode(Map<String, File> resources)
	{
		def encoded = new StringBuilder()
		resources.each { String name, File resource ->
			if (name.contains('@'))
			{
				throw new IllegalArgumentException("Resource name must not contain '@': " + name)
			}
			if (encoded.length() > 0)
			{
				encoded.append ' '
			}
			encoded.append URLEncoder.encode(name, "utf-8")
			if (name != resource.name)
			{
				encoded.append '@'
				encoded.append URLEncoder.encode(resource.name, "utf-8")
			}
		}
		return encoded.toString()
	}

	public static Map<String, File> decode(String encoded, File baseDir)
	{
		LinkedHashMap<String, File> resources = [:]
		if (encoded != null && !encoded.empty)
		{
			encoded.split(' ').each { String res ->
				def (nameEnc, fileNameEnc) = res.split('@', 2)
				def name = URLDecoder.decode(nameEnc, "utf-8")
				def fileName = fileNameEnc ? URLDecoder.decode(fileNameEnc, "utf-8") : name
				resources.put name, new File(baseDir, fileName)
			}
		}
		return resources
	}
}
