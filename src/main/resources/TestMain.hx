import massive.munit.client.RichPrintClient;
import massive.munit.client.JUnitReportClient;
import massive.munit.TestRunner;

class TestMain
{
	static function main(){	new TestMain(); }

	var junitReportClient = new JUnitReportClient();
	public function new()
	{
		patchSetIntervalForMunit();

		var runner:TestRunner = new TestRunner(new RichPrintClient());
		runner.addResultClient(junitReportClient);
		runner.completionHandler = completionHandler;
		runner.run([TestSuite]);
	}

	function completionHandler(successful:Bool):Void
	{
		untyped testComplete(junitReportClient.xml.b, successful);
	}

	function patchSetIntervalForMunit(){

		//munit is passing a string as the first parameter of window.setInterval, which is not supported by nodejs
		untyped __js__('window.setInterval= (function(){

			var oldSetInterval = window.setInterval;

			return function(){
				if (typeof arguments[0] == typeof "")
				{
					var arg0 = arguments[0];
					arguments[0] = function (){
						eval(arg0);
					};
				}
				return oldSetInterval.apply(this, arguments);
			}

		})();
		');

	}

}