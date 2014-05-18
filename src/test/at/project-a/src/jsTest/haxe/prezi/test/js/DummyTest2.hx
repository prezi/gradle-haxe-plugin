package prezi.test.js;

import massive.munit.Assert;

class DummyTest2
{
	@Test
	public function testSomething()
	{
		Assert.areEqual("Hello", new Dummy().hello());
	}
}
