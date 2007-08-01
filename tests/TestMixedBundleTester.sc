
TestMixedBundleTester : UnitTest {

	var t;

	setUp {
		t = MixedBundleTester.new;
		MixedBundleTester.reset;

	}
	test_findMessage {
		t.add( [ "/n_free",1001]);

		t.send(Server.default);
		Server.default.latency.wait;
		0.01.wait;
		this.assert( MixedBundleTester.findMessage( [ "/n_free",1001]),
		 		"should find the message in its sent messages");

		this.assert( MixedBundleTester.findMessage( [ "/n_free"]),
		 		"should match any /n_free message")
	}

	test_findPreparationMessage {
		var d;

		this.bootServer;

		d = SynthDef("TestOSCBundle", { arg freq = 440;
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

		this.assert( MixedBundleTester.findPreparationMessage(  ["/d_recv"] ),
		 		"should match any synth def /d_recv" );
	}

	// test that after sending that the bundle gets put in bundlesSent
	test_send { arg numDefs = 100;
		var functionFired = false, sent;

		this.makeDefs(numDefs);
		t.addFunction({ functionFired = true });

		this.bootServer;

		t.send(Server.default);
		this.wait( { functionFired }, "wait for functionFired to be set by bundle.doFunction");

		// should be 100 in preparationMessages
		this.assert( MixedBundleTester.bundlesSent.size == 1, "should be 1 bundle sent");

		sent = MixedBundleTester.bundlesSent.first;
		this.assert( sent === t, "it should be our bundle");
		this.assert( sent.preparationMessages.size == numDefs,"should be " + numDefs + " in preparationMessages");
	}

// crashes the language
//	test_largePrepare {
//		this.test_prepare(1000)
//	}

	makeDefs { |numDefs|
		numDefs.do({ |i|
			var d;
			d = SynthDef( "TestOSCBundle" ++ i,{
					SinOsc.ar([400,403] + i)
			});

			t.addPrepare( ["/d_recv", d.asBytes] )

		});
	}

}

