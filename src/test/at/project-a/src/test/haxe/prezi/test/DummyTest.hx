package prezi.test;

import massive.munit.Assert;

class DummyTest
{
	@Test
	public function testSomething()
	{
		trace("Running normal test");
		Assert.isTrue(Util.yes());
	}

	@Test
	@TestDebug
	public function testDebugStuff()
	{
		trace("Running debug test");
		Assert.isTrue(true);
	}
}
