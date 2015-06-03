import massive.munit.client.RichPrintClient;
import massive.munit.client.HTTPClient;
import massive.munit.client.JUnitReportClient;
import massive.munit.client.SummaryReportClient;
import massive.munit.TestRunner;

class TestMain
{
	static function main(){	new TestMain(); }

	public function new()
	{
		var suites = new Array<Class<massive.munit.TestSuite>>();
		suites.push(TestSuite);

		var client = new RichPrintClient();

		var runner:TestRunner = new TestRunner(client);
//		runner.addResultClient(new HTTPClient(new JUnitReportClient()));

		runner.completionHandler = completionHandler;
		runner.run(suites);
	}

	function completionHandler(successful:Bool):Void
	{
		try
		{
			trace('hello');
		}
		catch (e:Dynamic)
		{
		}
	}
}