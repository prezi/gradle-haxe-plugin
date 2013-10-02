package com.prezi.gradle.haxe

import org.junit.Test

import static org.hamcrest.CoreMatchers.hasItem
import static org.hamcrest.CoreMatchers.hasItems
import static org.hamcrest.CoreMatchers.is
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
		assertThat encoded, is("tibor@tibor.gif")
	}

	@Test
	public void testDecodeSingle()
	{
		def decoded = EmbeddedResourceEncoding.decode("tibor@tibor.gif", new File("."))
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
		assertThat encoded, is("tibor@tibor.gif geza@geza.gif")
	}

	@Test
	public void testDecodeMultiple()
	{
		def decoded = EmbeddedResourceEncoding.decode("tibor@tibor.gif geza@geza.gif", new File("."))
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
			"lajos": new File("l ajos.gif"),
			"geza": new File("g@za.gif")
		])
		assertThat encoded, is("t+ibor@tibor.gif lajos@l+ajos.gif geza@g%40za.gif")

		def decoded = EmbeddedResourceEncoding.decode(encoded, new File("."))
		assertThat decoded.size(), is(3)
		assertThat decoded.keySet(), hasItems("t ibor", "lajos", "geza")
		assertThat decoded["t ibor"], is(new File("./tibor.gif"))
		assertThat decoded["lajos"], is(new File("./l ajos.gif"))
		assertThat decoded["geza"], is(new File("./g@za.gif"))
	}

	@Test(expected = IllegalArgumentException)
	public void testEncodeWithAtSignInName()
	{
		EmbeddedResourceEncoding.encode([
			"tibor@geza": new File("tibor-geza.gif")
		])
	}
}
