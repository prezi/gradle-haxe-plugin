package com.prezi.io

import spock.lang.Specification


class BufferedStdOutputStreamsTest extends Specification {

	def oldStdOut
	def oldStdErr

	ByteArrayOutputStream stdOut
	ByteArrayOutputStream stdErr

	def setup() {
		oldStdOut = System.out
		oldStdErr = System.err
		stdOut = new ByteArrayOutputStream()
		System.out = new PrintStream(false, stdOut)
		stdErr = new ByteArrayOutputStream()
		System.err = new PrintStream(false, stdErr)
	}

	def cleanup() {
		System.out = oldStdOut
		System.err = oldStdErr
	}

	def "plays back stdout"() {
		def os = new BufferedStdOutputStreams()
		os.getStdOutOutputStream().write("Hello".bytes)
		os.getStdOutOutputStream().write(" world".bytes)

		os.printOutput()
		expect:
		stdOut.toString() == "Hello world"
	}

	def "plays back stderr"() {
		def os = new BufferedStdOutputStreams()
		os.getStdErrOutputStream().write("Hello".bytes)
		os.getStdErrOutputStream().write(" world".bytes)

		os.printOutput()
		expect:
		stdErr.toString() == "Hello world"
	}

	def "plays back stdout & stderr"() {
		def os = new BufferedStdOutputStreams()
		os.getStdOutOutputStream().write("Hello".bytes)
		os.getStdErrOutputStream().write("Hello".bytes)
		os.getStdOutOutputStream().write(" stdout".bytes)
		os.getStdErrOutputStream().write(" stderr".bytes)

		os.printOutput()
		expect:
		stdOut.toString() == "Hello stdout"
		stdErr.toString() == "Hello stderr"
	}

	def "plays back stdout & stderr in the right order"() {
		def os = new BufferedStdOutputStreams()
		System.err = System.out
		os.getStdOutOutputStream().write("1".bytes)
		os.getStdErrOutputStream().write("2".bytes)
		os.getStdOutOutputStream().write("3".bytes)
		os.getStdErrOutputStream().write("4".bytes)

		os.printOutput()
		expect:
		stdOut.toString() == "1234"
	}

	def "can get error output"() {
		def os = new BufferedStdOutputStreams()
		System.err = System.out
		os.getStdOutOutputStream().write("Hello".bytes)
		os.getStdErrOutputStream().write("Hello".bytes)
		os.getStdOutOutputStream().write(" stdout".bytes)
		os.getStdErrOutputStream().write(" stderr".bytes)

		os.printOutput()
		expect:
		os.errorOut.toString() == "Hello stderr"
	}

}
