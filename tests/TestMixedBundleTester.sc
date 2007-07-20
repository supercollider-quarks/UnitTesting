
TestMixedBundleTester : UnitTest {
	var t;
	setUp {
		t = MixedBundleTester.new;
	}
	test_findMessage {
		t.add( [ "/n_free",1001]);

		t.send(Server.default);
		Server.default.latency.wait;

		this.assert( MixedBundleTester.findMessage( [ "/n_free",1001]),
		 		"should find the message in its sent messages")
	}

	test_findPreparationMessage {
		var d;

		this.bootServer;

		d = SynthDef("help-Synth-get", { arg freq = 440;
			Out.ar(0, SinOsc.ar(freq, 0, 0.1));
		});

		// bad.
		// this is really testng UnitTest setUp
		// and is assuming the implementation of MixedBundle
		//this.assert( t.messages.isNil,"bundle should have no messages to start with");
		//this.assert( t.preparationMessages.isNil,"bundle should have no preparationMessages to start with");
		// anyway, they passed

		t.addPrepare( ["/d_recv", d.asBytes]);
		this.assert( t.preparationMessages.size == 1,"bundle should have 1 preparationMessages");

		t.send(Server.default);

		(Server.default.latency * 2).wait;

		this.assert( MixedBundleTester.findPreparationMessage(  ["/d_recv", d.asBytes] ),
		 		"should find the synth def message in its preparation messages" );
	}

	test_prepare {
		this.makeDefs(100)
	}
	
// crashes the language
//	test_largePrepare {
//		this.makeDefs(1000);
//	}

	makeDefs { |numDefs|
		
		var sd, b,functionFired = false, sent;

		MixedBundleTester.reset;

		b = MixedBundleTester.new;

		sd = Array.fill(numDefs,{ |i|
				var d;
				d = SynthDef( "TestOSCBundle" ++ i,{
						SinOsc.ar([400,403] + i)
				});

				b.addPrepare( ["/d_recv", d.asBytes] )

			});
		b.addFunction({ functionFired = true });


		this.bootServer;


		b.send(Server.default);
		this.asynchAssert( {functionFired}, {
			this.assert( functionFired, "functionFired should be set");
		},"waiting for send to finish prepare and run prSend",numDefs / 4);

		// should be 100 in preparationMessages
		this.assert( MixedBundleTester.bundlesSent.size == 1, "should be 1 bundle sent");
		// this alone is a victory
		this.assert( functionFired, "functionFired should be set when the bundle did its prSend / doFunctions");

		sent = MixedBundleTester.bundlesSent.first;
		this.assert( sent === b, "it should be our bundle");
		this.assert( sent.preparationMessages.size == numDefs,"should be " + numDefs + " in preparationMessages");

	}

}

