package com.prezi.gradle.gradle

import com.prezi.haxe.gradle.EmbeddedResourceEncoding
import org.junit.Test

import static org.hamcrest.CoreMatchers.hasItem
import static org.hamcrest.CoreMatchers.hasItems
import static org.junit.Assert.assertThat

class EmbeddedResourceEncodingTest {

	@Test
	public void testEncodeEmpty()
	{
		def encoded = EmbeddedResourceEncoding.encode([:])
		assertThat encoded, is("")
	}

	@Test
	public void testDecodeEmpty()
	{
		assertThat EmbeddedResourceEncoding.decode(null, new File(".")).size(), is(0)
		assertThat EmbeddedResourceEncoding.decode("", new File(".")).size(), is(0)
	}

	@Test
	public void testEncodeSingle()
	{
		def encoded = EmbeddedResourceEncoding.encode([
			tibor: new File("/some/folder/somewhere/tibor.gif")
		])
		assertThat encoded, is("tibor.gif@tibor")
	}

	@Test
	public void testDecodeSingle()
	{
		def decoded = EmbeddedResourceEncoding.decode("tibor.gif@tibor", new File("."))
		assertThat decoded.size(), is(1)
		assertThat decoded.keySet(), hasItem("tibor")
		assertThat decoded["tibor"], is(new File("./tibor.gif"))
	}

	@Test
	public void testEncodeSingleWithMatchingName()
	{
		def encoded = EmbeddedResourceEncoding.encode([
			"tibor.gif": new File("/some/folder/somewhere/tibor.gif")
		])
		assertThat encoded, is("tibor.gif")
	}

	@Test
	public void testDecodeSingleWithMatchingName()
	{
		def decoded = EmbeddedResourceEncoding.decode("tibor.gif@tibor.gif", new File("."))
		assertThat decoded.size(), is(1)
		assertThat decoded.keySet(), hasItem("tibor.gif")
		assertThat decoded["tibor.gif"], is(new File("./tibor.gif"))
	}

	@Test
	public void testEncodeMultiple()
	{
		def encoded = EmbeddedResourceEncoding.encode([
			tibor: new File("/some/folder/somewhere/tibor.gif"),
			geza: new File("/another/folder/somewhere/geza.gif"),
		])
		assertThat encoded, is("tibor.gif@tibor geza.gif@geza")
	}

	@Test
	public void testDecodeMultiple()
	{
		def decoded = EmbeddedResourceEncoding.decode("tibor.gif@tibor geza.gif@geza", new File("."))
		assertThat decoded.size(), is(2)
		assertThat decoded.keySet(), hasItems("tibor", "geza")
		assertThat decoded["tibor"], is(new File("./tibor.gif"))
		assertThat decoded["geza"], is(new File("./geza.gif"))
	}

	@Test
	public void testEncodeAndDecodeTricky()
	{
		def encoded = EmbeddedResourceEncoding.encode([
			"t ibor": new File("tibor.gif"),
			"lajos": new File("l ajos.gif")
		])
		assertThat encoded, is("tibor.gif@t+ibor l+ajos.gif@lajos")

		def decoded = EmbeddedResourceEncoding.decode(encoded, new File("."))
		assertThat decoded.size(), is(2)
		assertThat decoded.keySet(), hasItems("t ibor", "lajos")
		assertThat decoded["t ibor"], is(new File("./tibor.gif"))
		assertThat decoded["lajos"], is(new File("./l ajos.gif"))
	}

	@Test(expected = IllegalArgumentException)
	public void testEncodeWithAtSignInName()
	{
		EmbeddedResourceEncoding.encode([
			"tibor@geza": new File("tibor-geza.gif")
		])
	}

	@Test(expected = IllegalArgumentException)
	public void testEncodeWithAtSignInFileName()
	{
		EmbeddedResourceEncoding.encode([
			"tibor-geza": new File("tibor@geza.gif")
		])
	}
}
