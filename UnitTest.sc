

UnitTest {

	var currentMethod;
	classvar  <failures,<passes,routine,<>reportPasses = true;

	// called before each test
	setUp {}
	// called after each test
	tearDown {}

	// all methods named test_ will be run
	//test_writeYourTests {}

	// UnitTest.gui
	*gui {
		// UnitTest GUI written by Dan Stowell 2009.
		var w, allclasses, classlist, methodlist;
		w = Window.new("[UnitTest GUI]", Rect(100, 100, 400, 600));
		allclasses = UnitTest.allSubclasses.collectAs({|c| 
				c.asString[4..] -> c.findTestMethods.collectAs({|m| 
					m.name.asString -> {c.new.runTestMethod(m)}
					}, Dictionary).add(" run all in this class" -> {c.run} ) 
			}, Dictionary);
		allclasses = allclasses.reject{|d| d.size==1}; // err there may be some empty classes hanging around
		// Hmmmmm, not working?    allclasses.add("...All..." -> Dictionary["Run all" -> {UnitTest.runAll}]); // Contains (runs) UnitTest.runAll
		
		StaticText(w, Rect(0,0, 400, 40)).string_("Select a class, then a test method, and press Enter");
		
		classlist = ListView(w, Rect(0,40, 200, 600-40)).items_(allclasses.asSortedArray.collect(_[0])).action_{|widg| 
			methodlist.items_(allclasses.asSortedArray[widg.value][1].asSortedArray.collect(_[0])) 
		};
		//nowork: classlist.enterKeyAction_{|widg|  methodlist.valueAction_(0)};
		
		methodlist = ListView(w, Rect(200,40, 200, 600-40));
		methodlist.enterKeyAction_{|widg|  allclasses.asSortedArray[classlist.value][1].asSortedArray[widg.value][1].value   };
		
		classlist.doAction; // fills in the right-hand column
		^w.front;
	}
	

	// use YourClass.test of TestYourClass.run
	*run { | reset=true,report=true|
		this.new.run(reset,report);
	}
	// run all UnitTest subclasses
	*runAll {
		routine = Routine({
			this.reset;
			this.allSubclasses.do ({ |testClass|
				testClass.run(false,false);
				0.1.wait;
			});
			this.report;
		}).play(AppClock)
	}
	*runTest { arg methodName;
		// "TestPolyPlayerPool:test_prepareChildrenToBundle"
		var class,method,unitTest;
		# class,method = methodName.split($:);
		class = class.asSymbol.asClass;
		method.asSymbol;
		method = class.findMethod(method.asSymbol);
		if(method.isNil,{ Error("Test method not found "+methodName).throw });
		class.new.runTestMethod(method);
	}
	runTestMethod { arg method;
		var function;
		("RUNNING UNIT TEST" + this.class.name ++ ":" ++ method.name).inform;
		function = {
			this.setUp;
			currentMethod = method;
			this.perform(method.name);
			this.tearDown;
			this.class.report;
			nil
		};
		routine = Routine(function);
		routine.play(AppClock)
	}

	// call these in your test_ methods to check conditions
	// and pass or fail
	assert { | boolean,message, report=true,onFailure|
		if(boolean.not,{
			this.failed(currentMethod,message, report);
			if(onFailure.notNil,{
				{ onFailure.value }.defer;
				Error("UnitTest halted with onFailure handler.").throw;
			});
		},{
			this.passed(currentMethod,message, report)
		});
		^boolean
	}
	assertEquals { |a,b,message="", report=true,onFailure|
		this.assert( a == b, message + "\nIs:\n\t" + a + "\nShould be:\n\t" + b + "\n", report,onFailure)
	}
	assertFloatEquals { |a,b,message="",within=0.0001, report=true,onFailure|
		this.assert( (a - b).abs < within, message + "\nIs:\n\t" + a + "\nShould be:\n\t" + b + "\n", report,onFailure);
	}
	assertArrayFloatEquals { |a,b,message="",within=0.0001, report=true,onFailure|
		this.assert( ((a - b).abs < within).every(_==true), message + "\nIs:\n\t" + a + "\nShould be:\n\t" + b + "\n", report,onFailure);
	}
	// make a further assertion only if it passed, or only if it failed
	ifAsserts { | boolean,message, ifPassedFunc, ifFailedFunc report=true|
		if(boolean.not,{
			this.failed(currentMethod,message, report);
			ifFailedFunc.value;
		},{
			this.passed(currentMethod,message, report);
			ifPassedFunc.value;
		});
		^boolean
	}
	// waits for condition with a maxTime limit
		// if time expires, the test is a failure
	wait { |condition,failureMessage,maxTime = 10.0|
		var limit;
		limit = maxTime / 0.05;
		while({
			condition.value.not and:
			 {(limit = limit - 1) > 0}
		},{
			0.05.wait;
		});
		if(limit == 0 and: failureMessage.notNil,{
			this.failed(currentMethod,failureMessage)
		})
	}

	// wait is better
	asynchAssert { |waitConditionBlock, testBlock,timeoutMessage="",timeout = 10|
		var limit;
		limit = timeout / 0.1;
		while({
			waitConditionBlock.value.not and:
			 {(limit = limit - 1) > 0}
		},{
			0.1.wait;
		});
		if(limit == 0,{
			this.failed(currentMethod,"Timeout:" + timeoutMessage)
		},{
			testBlock.value
		});
	}

	// if already booted, then freeAll and create new allocators
	bootServer { arg server;
		server = server ? Server.default;
		if(server.serverRunning.not,{
			server.bootSync(Condition.new)
		},{
			server.freeAll;
		});
		server.newAllocators; // new nodes, busses regardless
	}

	failed { arg method,message, report=true;
		var r;
		failures = failures.add(r = UnitTestResult(this,method,message));
		if(report){
			Post << Char.nl << "FAIL:";
			r.report;
			Post << Char.nl;
		};
	}
	passed { arg method,message, report=true;
		var r;
		passes = passes.add(r = UnitTestResult(this,method,message));
		if(report and: reportPasses){
			Post << "PASS:";
			r.report;
		};
	}

	// these are mostly private
	// don't use this directly,
	// use Class.test or TestClass.run
	*runTestClassForClass { arg class, reset=true, report=true;
		var testClass;
		if(class.isNil,{
			"No class supplied for testing".die;
		});
		testClass = ("Test" ++ class.name.asString).asSymbol.asClass;
		if(testClass.isNil,{
			("No test class found for " + class).inform;
			^this
		});
		if(testClass.respondsTo(\run).not,{
			("Attempting to run UnitTests on class that is not a subclass of UnitTest"
				+ testClass).error;
			^this
		});
		testClass.run(reset,report)
	}

	*findTestClass { arg forClass;
		^("Test" ++ forClass.name.asString).asSymbol.asClass
	}

	*report {
		Post.nl;
		"UNIT TEST.............".inform;
		if(failures.size > 0,{
			"There were failures:".inform;
			failures.do({ arg results;

				results.report
			});
		},{
			"There were no failures".inform;
		})
	}
	*initClass {
		CmdPeriod.add(this);
	}
	*cmdPeriod {
		if(routine.notNil,{
			routine.stop;
			routine = nil;
		});
	}
	// private.  use YourClass.test or TestYourClass.run
	run { | reset=true,report=true|
		var function;
		if(reset,{ this.class.reset });
		if(report,{ ("RUNNING UNIT TEST" + this).inform });
		function = {
			this.findTestMethods.do({ arg method;
				this.setUp;
				currentMethod = method;
				//{
					this.perform(method.name);
				// unfortunately this removes the interesting part of the call stack
				//}.try({ |err|
				//	("ERROR during test"+method).postln;
				//	err.throw;
				//});

				this.tearDown;
			});
			if(report,{ this.class.report });
			nil
		};
		if(routine.notNil,{ // we are inside the Routine already
			// can we prove it ?
			function.value
		},{
			routine = Routine(function);
			routine.play(AppClock)
		})
	}
	// returns the methods named test_
	findTestMethods {
		^this.class.findTestMethods
	}
	*findTestMethods {
		^methods.select({ arg m;
			m.name.asString.copyRange(0,4) == "test_"
		})
	}
	*classesWithTests { arg package='Common';
		^Quarks.classesInPackage(package).select({ |c| UnitTest.findTestClass(c).notNil })
	}
	*classesWithoutTests { arg package='Common';
		^Quarks.classesInPackage(package).difference( UnitTest.classesWithTests(package) );
	}

	// whom I am testing
	*findTestedClass {
		^this.name.asString.copyToEnd(4).asSymbol.asClass
	}
	// methods in the tested class that do not have test_ methods written
	*untestedMethods {
		var testedClass,testMethods,testedMethods,untestedMethods;
		testedClass = this.findTestedClass;
		// what methods in the target class do not have tests written for them ?
		testMethods = this.findTestMethods;
		testedMethods = testMethods.collect({ |meth|
			testedClass.findMethod(meth.name.asString.copyToEnd(5).asSymbol)
		}).reject(_.isNil);
		if(testedMethods.isNil or: {testedMethods.isEmpty},{
			untestedMethods = testedClass.methods;
		},{
			untestedMethods =
				testedClass.methods.select({ |meth| testedMethods.includes(meth).not });
		});
		// reject getters,setters, empty methods
		untestedMethods = untestedMethods.reject({ |meth| meth.code.isNil });
		^untestedMethods
	}
	*listUntestedMethods { arg forClass;
		this.findTestClass(forClass).untestedMethods.do({|m| m.name.postln })
	}
	// private
	*reset {
		failures = [];
		if(routine.notNil,{
			routine.stop;
			routine = nil;
		});
	}
}





UnitTestResult {
	var <testClass,<testMethod,<message;
	*new { arg testClass,testMethod="",message="";
		^super.newCopyArgs(testClass,testMethod,message)
	}
	report {
		Post << testClass.asString << ":" << testMethod.name << " " << message << Char.nl;
	}
}


