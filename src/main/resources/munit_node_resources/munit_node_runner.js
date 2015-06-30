#!/usr/bin/env node

var jsdom = require("jsdom");
var fs = require('fs');

global.$ = require('jquery');
global.$.ajax = function(){};
global.window = jsdom.jsdom().createWindow();
global.alert = function(){};

for(var p in global.window)
{

    if(!global[p])
        global[p] = global.window[p];
}

process.stdout.write("\n\n");

(function() {
	var oldErr = process.stderr.write;
	process.stderr.write = function (t) {
		//emscripten floods error: https://github.com/kripken/emscripten/blob/667dcd241886fdb878e248d95d8e03abb09c80b7/src/postamble.js
		if (t.indexOf("pre-main") !== 0) {
			oldErr.apply(process.stderr, arguments);
		}
	}
})();

global.addToQueue = (function ()
{
	var stdlog = function(str) {
		process.stdout.write(str);
	};
	var stderr = function(str) {
		process.stderr.write(str);
	};

	var colors = {'pass': 90
		, 'fail': 31
		, 'bright pass': 92
		, 'bright fail': 91
		, 'bright yellow': 93
		, 'pending': 36
		, 'suite': 0
		, 'error title': 0
		, 'error message': 31
		, 'error stack': 90
		, 'checkmark': 32
		, 'fast': 90
		, 'medium': 33
		, 'slow': 31
		, 'green': 32
		, 'light': 90
		, 'diff gutter': 90
		, 'diff added': 32
		, 'diff removed': 31
	};
	var colorize = function(type, str) {
		return '\u001b[' + colors[type] + 'm' + str + '\u001b[0m';
	};

	var tab = function(str, tabNum) {
		var t = new Array(tabNum + 1).join("    ");
		return t + str.replace(/\n/g, "\n" + t);
	};

	var TestCase = function(){
		this.result = 0;
		this.trace = [];
		this.className = '';
		this.summary = '';
	};
	TestCase.prototype.write = function(out) {
		var failure = this.result == "1" || this.result == "2";
		var res = failure ? colorize('fail', "✖") : colorize('checkmark', "✓");

		out(tab(res + " " + this.className + " " + this.summary, 1) + "\n");
		this.trace.forEach(function(t) {
			out(tab(colorize('light', t), 2) + "\n");
		});
	};

	var oldLog = console.log;
	console.log = function(){};

	var currentTestCase;
	var failedTestCases = [];
	var summary;

	function unhtml(st)
	{
		st = st.replace(/<br\/>/g, '\n');
		st = st.replace(/&nbsp;/g, " ");
		st = st.replace(/&amp;/g , "&");
		st = st.replace(/&quot;/g, "\"");
		st = st.replace(/&lt;/g  , "<");
		st = st.replace(/&gt;/g  , ">");
		return st;
	}

	function addToQueue(fnc,arg1,arg2,arg3)
	{
		arg1 = unhtml(arg1);

		if(fnc == "updateTestSummary")
		{
			currentTestCase.summary += arg1;
		}
		else if(fnc == "setTestClassResult")	
		{
			currentTestCase.result = arg1;
			currentTestCase.write(stdlog);
			if(arg1 == "1" || arg1 == "2")
				failedTestCases.push(currentTestCase);
		}
		else if(fnc == "addTestError")
		{
			currentTestCase.trace.push(arg1);
		}
		else if(fnc == "createTestClass")	
		{
			currentTestCase = new TestCase();
			currentTestCase.className = arg1;
		}
		else if(fnc == "addTestIgnore")	
		{

		}
		else if(fnc == "munitTrace")
		{
			currentTestCase.trace.push(arg1);
		}
		else if(fnc == "printSummary")
		{
			summary = arg1;
		}
		else if(fnc == "setResult")
		{
			var resultColor = 'green';
			if (failedTestCases.length) {
				resultColor = 'fail';
			}
			stdlog("\n\n" + tab(colorize(resultColor, summary), 1) + "\n\n");
			failedTestCases.forEach(function (testCase) {
				testCase.write(stderr);
			});
		}
		else
		{
			oldLog("XXXXX", arguments);
		}
	}

	return addToQueue;
})();


global.testComplete = function(resultXml, successful){

    function mkdir(path, root) {

        var dirs = path.split('/'), dir = dirs.shift(), root = (root||'')+dir+'/';

        try { fs.mkdirSync(root); }
        catch (e) {
            //dir wasn't made, something went wrong
            if(!fs.statSync(root).isDirectory()) throw new Error(e);
        }

        return !dirs.length||mkdir(dirs.join('/'), root);
    }


    mkdir('report/test/junit/js/xml');
    fs.writeFileSync("report/test/junit/js/xml/report.xml", resultXml);

    process.exit(successful ? 0 : 1);

}


navigator = {userAgent: {match: function(){}}};


require("./nodejsTest_test.js");