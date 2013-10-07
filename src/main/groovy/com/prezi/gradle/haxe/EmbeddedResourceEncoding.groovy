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
			if (resource.name.contains('@'))
			{
				throw new IllegalArgumentException("Resource file name must not contain '@': " + name)
			}
			if (encoded.length() > 0)
			{
				encoded.append ' '
			}
			encoded.append URLEncoder.encode(resource.name, "utf-8")
			if (name != resource.name)
			{
				encoded.append '@'
				encoded.append URLEncoder.encode(name, "utf-8")
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
				def fileName, name
				if (res.contains('@'))
				{
					def (fileNameEnc, nameEnc) = res.split('@', 2)
					fileName = URLDecoder.decode(fileNameEnc, "utf-8")
					name = URLDecoder.decode(nameEnc, "utf-8")
				}
				else
				{
					fileName = URLDecoder.decode(res, "utf-8")
					name = fileName
				}
				resources.put name, new File(baseDir, fileName)
			}
		}
		return resources
	}
}
