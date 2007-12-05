

UnitTest {

	var currentMethod;
	classvar  <failures,<passes,routine;

	// called before each test
	setUp {}
	// called after each test
	tearDown {}
	// all methods named test_ will be run
	//test_writeYourTests {}



	// find TestClass for Test and run it
	*testClass { arg class, reset=true, report=true;
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
	// TestClass run
	*run { | reset=true,report=true|
		this.new.run(reset,report);
	}
	// run all UnitTest subclasses
	*runAll {
		routine = Routine({
			this.reset;
			this.allSubclasses.do ({ |testClass|
				testClass.run(false,false)
			});
			this.report;
		}).play(AppClock)
	}
	assert { | boolean,message, report=true|
		if(boolean.not,{
			this.failed(currentMethod,message, report)
		},{
			this.passed(currentMethod,message, report)
		});
		^boolean
	}
	assertEquals { |a,b,message="", report=true|
		this.assert( a == b, message + "\nIs:\n\t" + a + "\nShould be:\n\t" + b + "\n", report)
	}
	assertFloatEquals { |a,b,message="",within=0.0001, report=true|
		this.assert( (a - b).abs < within, message + "\nIs:\n\t" + a + "\nShould be:\n\t" + b + "\n", report);
	}
	// waits for condition with a maxTime limit
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

	// wait is kind of better
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
			server.newAllocators;
		});
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
		if(report){
			Post << "PASS:"; 
			r.report;
		};
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
	//private
	run { | reset=true,report=true|
		var function;
		if(reset,{ this.class.reset });
		if(report,{ ("RUNNING UNIT TEST" + this).inform });
		function = {
			this.testMethods.do({ arg method;
				this.setUp;
				currentMethod = method;
				this.perform(method.name);
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

	testMethods {
		^this.class.methods.select({ arg m;
			m.name.asString.copyRange(0,4) == "test_"
		})
	}
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


