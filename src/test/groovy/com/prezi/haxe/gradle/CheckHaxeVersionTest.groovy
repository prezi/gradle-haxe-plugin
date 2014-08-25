package com.prezi.haxe.gradle

import spock.lang.Specification
import spock.lang.Unroll

class CheckHaxeVersionTest extends Specification {
	@Unroll
	def "check #versions vs #version should be #expected"() {
		expect:
		CheckHaxeVersion.checkVersion(versions.toSet(), version) == expected
		where:
		versions           | version | expected
		["3.1.3"]          | "3.1.3" | true
		["3.1.3"]          | "3.1.4" | false
		["3.1.3", "3.1.4"] | "3.1.4" | true
		[~/3\.1\.\d+/]      | "3.1.3" | true
		[~/3\.1\.\d+/]      | "3.1.4" | true
		[~/3\.1\.\d+/]      | "3.2.0" | false
	}
}
