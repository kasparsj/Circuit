Circuit {
	classvar <numInstances = 0;
	classvar <sections;

	var <>deviceID;
	var <>server;
	var <>syn0;
	var <>syn1;
	var <>drum0;
	var <>drum1;
	var <>mix;
	var <>fx0;
	var <>fx1;
	var <>fltr;
	var <out;

	var <midiOut;
	var <midiOn;
	var <midiOff;
	var <midiCC;


	*initClass {
		sections = (
			\syn0: [Array.fill(8, 0), (80..87)],
			\syn1: [Array.fill(8, 1), (80..87)],
			\drum0: [Array.fill(8, 9), (14..21)],
			\drum1: [Array.fill(8, 9), (46..53)],
			\mix: [[15, 15, 9, 9, 9, 9], [12, 14, 12, 23, 45, 53]],
			\fx0: [Array.fill(6, 15), (111..116)],
			\fx1: [Array.fill(6, 15), [88, 89, 90, 106, 109, 110]],
			\fltr: [[15], [74]],
		);
	}

	*new { |deviceID, s=nil|
		var instance = super.newCopyArgs(deviceID);
		numInstances = numInstances + 1;
		instance.init(s ? Server.default);
		^instance;
	}

	init { |s|
		server = s;

		sections.keysValuesDo { |key, value|
			this.perform((key ++ "_").asSymbol, CircuitSection.new(this, key, value[0].size, value[0], value[1]));
		};
		out = Bus.audio(server, 16);

		this.initNdef;

		if (Circuit.numInstances == 1, {
			this.makeDefault();
		});
		CmdPeriod.add { this.deinit(); }
	}

	deinit {
		if (midiOut != nil, {
			midiOut.free;
			midiOut = nil;
		});
		if (midiOn != nil, {
			midiOn.free;
			midiOn = nil;
		});
		if (midiOff != nil, {
			midiOff.free;
			midiOff = nil;
		});
	}

	initNdef {
		Ndef(\circuit, { |inBus, ccBus|
			var val = 0;
			8.do { |i|
				val = val + (In.ar(inBus + (i*2), 2) * In.kr(ccBus + i) * 0.5);
			};
			val;
		});
		{
			server.sync;
			Ndef(\circuit).set(\inBus, out.index);
			Ndef(\circuit).set(\ccBus, mix.bus.index);
			Ndef(\circuit).play;
		}.fork;
	}

	makeDefault {
		if (Main.versionAtLeast( 3, 9 )) {
		    Event.addParentType(\circuit, (circuit: this));
		};
		// todo: do we need to throw an error?
	}

	connect { |deviceName, portName, forceInit = false|
		if (MIDIClient.initialized == false || forceInit, {
			MIDIClient.init;
		});
		MIDIIn.connectAll;
		// todo: should search for matching names
		midiOut = MIDIOut.newByName(deviceName, portName);
	}

	midiFunc {
		midiOn = MIDIFunc.noteOn({ |vel, num, chan, src|
			if (src == deviceID) {
				this.prNoteOn(vel, num, chan);
			};
		});
		midiOff = MIDIFunc.noteOff({ |vel, num, chan, src|
			if (src == deviceID) {
				this.prNoteOff(vel, num, chan);
			};
		});
		midiCC = MIDIFunc.cc({ |value, num, chan, src|
			if (src == deviceID) {
				this.prCC(value, num, chan);
			};
		});
	}

	prNoteOn { |veloc = 64, note = 60, chan = 0|
		veloc = veloc / 127.0;
		switch (chan,
			0, {
				syn0.midiNoteOn(veloc, note, true);
			},
			1, {
				syn1.midiNoteOn(veloc, note, true);
			},
		);
	}

	prNoteOff { |veloc = 64, note = 60, chan = 0|
		veloc = veloc / 127.0;
		switch (chan,
			0, {
				syn0.midiNoteOff(veloc, note, false);
			},
			1, {
				syn1.midiNoteOff(veloc, note, false);
			},
		);
	}

	prCC { |value, num, chan|
		value = value / 127.0;
		switch (chan,
			0, {
				syn0.midiCC(value, num);
			},

			1, {
				syn1.midiCC(value, num);
			},

			9, {
				if (num == 12, {
					mix.midiCCIndex(value, 2);
				});
				if (num >= 14 and: { num <= 21 }, {
					drum0.midiCC(value, num);
				});
				if (num == 23, {
					mix.midiCCIndex(value, 3);
				});
				if (num == 45, {
					mix.midiCCIndex(value, 4);
				});
				if (num >= 46 and: { num <= 53 }, {
					drum1.midiCC(value, num);
				});
				if (num == 53, {
					mix.midiCCIndex(value, 5);
				});
			},

			15, {
				if (num == 12, {
					mix.midiCCIndex(value, 0);
				});
				if (num == 14, {
					mix.midiCCIndex(value, 1);
				});
				if (num == 74, {
					fltr.midiCCIndex(value, 0);
				});
				if (num >= 88 and: { num <= 90 }, {
					fx1.midiCC(value, num);
				});
				if (num == 106, {
					fx1.midiCCIndex(value, 3);
				});
				if (num >= 109 and: { num <= 110 }, {
					fx1.midiCCIndex(value, num-109+4);
				});
				if (num >= 111 and: { num <= 116 }, {
					fx0.midiCCIndex(value, num-111);
				});
			},
		);
	}

}
