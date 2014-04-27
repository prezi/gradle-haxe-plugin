package com.prezi.haxe.gradle

import spock.lang.Specification

class EmbeddedResourceEncodingTest extends Specification {

	def "encode empty"() {
		def encoded = EmbeddedResourceEncoding.encode([:])

		expect:
		encoded == ""
	}

	def "decode null"() {
		def decoded = EmbeddedResourceEncoding.decode(null, new File("."))

		expect:
		decoded.isEmpty()
	}

	def "decode empty"() {
		def decoded = EmbeddedResourceEncoding.decode("", new File("."))

		expect:
		decoded.isEmpty()
	}

	def "encode single"() {
		def encoded = EmbeddedResourceEncoding.encode([
			tibor: new File("/some/folder/somewhere/tibor.gif")
		])

		expect:
		encoded == "tibor.gif@tibor"
	}

	def "decode single"() {
		def decoded = EmbeddedResourceEncoding.decode("tibor.gif@tibor", new File("."))

		expect:
		decoded.size() == 1
		decoded.get("tibor") == new File("./tibor.gif")
	}

	def "encode single with matching name"() {
		def encoded = EmbeddedResourceEncoding.encode([
			"tibor.gif": new File("/some/folder/somewhere/tibor.gif")
		])

		expect:
		encoded == "tibor.gif"
	}

	def "decode single with matching name"() {
		def decoded = EmbeddedResourceEncoding.decode("tibor.gif@tibor.gif", new File("."))

		expect:
		decoded.size() == 1
		decoded.get("tibor.gif") == new File("./tibor.gif")
	}

	def "encode multiple"() {
		def encoded = EmbeddedResourceEncoding.encode([
			tibor: new File("/some/folder/somewhere/tibor.gif"),
			geza: new File("/another/folder/somewhere/geza.gif"),
		])

		expect:
		encoded == "tibor.gif@tibor geza.gif@geza"
	}

	def "decode multiple"() {
		def decoded = EmbeddedResourceEncoding.decode("tibor.gif@tibor geza.gif@geza", new File("."))

		expect:
		decoded.size() == 2
		decoded.get("tibor") == new File("./tibor.gif")
		decoded.get("geza") == new File("./geza.gif")
	}

	def "encode tricky"() {
		def encoded = EmbeddedResourceEncoding.encode([
				"t ibor": new File("tibor.gif"),
				"lajos" : new File("l ajos.gif")
		])

		expect:
		encoded == "tibor.gif@t+ibor l+ajos.gif@lajos"
	}

	def "decode tricky"() {
		def encoded = "tibor.gif@t+ibor l+ajos.gif@lajos"
		def decoded = EmbeddedResourceEncoding.decode(encoded, new File("."))

		expect:
		decoded.size() == 2
		decoded["t ibor"] == new File("./tibor.gif")
		decoded["lajos"] == new File("./l ajos.gif")
	}

	def "encode with '@' in name"() {
		when:
		EmbeddedResourceEncoding.encode([
			"tibor@geza": new File("tibor-geza.gif")
		])

		then:
		thrown(IllegalArgumentException)
	}

	def "encode with '@' in file name"() {
		when:
		EmbeddedResourceEncoding.encode([
			"tibor-geza": new File("tibor@geza.gif")
		])

		then:
		thrown(IllegalArgumentException)
	}
}
