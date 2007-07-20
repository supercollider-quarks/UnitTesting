
/*
	saves all sent messages so that UnitTests can query it later

	but its not a Mock object : it still sends to the server

	could be implemented as one though

*/

MixedBundleTester : MixedBundle {

	classvar <bundlesSent;

	// private //
	prSend { arg server, latency;
		latency = latency ? Server.default.latency;
		super.prSend(server,latency);
		SystemClock.sched(latency,{
			bundlesSent = bundlesSent.add( this );
		})
	}
	clumpedSendNow { arg server;
		if(messages.notNil,{
			messages.clump(10).do({ arg bundle,i;
				server.listSendBundle(i * 0.001,bundle);
			});
			SystemClock.sched(messages.size * 0.001,{
				this.doFunctions;
				bundlesSent = bundlesSent.add(this);
				nil
			});
			^messages.size
		},{
			this.doFunctions;
			bundlesSent = bundlesSent.add(this);
			^0
		})
	}
	*reset {
		bundlesSent = [];
	}
	*findMessage { arg message;
		if(bundlesSent.isNil,{ ^false });
		^bundlesSent.any({ |b|
			if(b.messages.isNil,{
				false
			},{
				b.messages.any({ |m| m == message })
			})
		})
	}
	*findPreparationMessage { arg message;
		if(bundlesSent.isNil,{ ^false });
		^bundlesSent.any({ |b|
			if(b.preparationMessages.isNil,{
				false
			},{
				b.preparationMessages.any({ |pm| pm == message })
			})
		})
	}
}

