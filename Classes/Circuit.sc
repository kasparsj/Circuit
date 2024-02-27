Circuit {
	classvar <numInstances = 0;
	classvar <sections;

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

	*new { |s=nil|
		var instance = super.newCopyArgs(s ? Server.default);
		numInstances = numInstances + 1;
		instance.init(s ? Server.default);
		^instance;
	}

	init {
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

	prNoteInfo { |note = 60, chan = 0|
		switch (chan,
			0, {
				[\syn0];
			},
			1, {
				[\syn1];
			},
		);
	}

	prCCInfo { |num, chan|
		^switch (chan,
			0, {
				if (num >= syn0.nums.first and: { num <= syn0.nums.last }) {
					[\syn0, num-syn0.nums.first];
				};
			},

			1, {
				if (num >= syn1.nums.first and: { num <= syn1.nums.last }) {
					[\syn1, num-syn1.nums.first];
				};
			},

			9, {
				if (num == 12, {
					[\mix, 2];
				});
				if (num >= 14 and: { num <= 21 }, {
					[\drum0, num-14];
				});
				if (num == 23, {
					[\mix, 3];
				});
				if (num == 45, {
					[\mix, 4];
				});
				if (num >= 46 and: { num <= 53 }, {
					[\drum1, num-46];
				});
				if (num == 53, {
					[\mix, 5];
				});
			},

			15, {
				if (num == 12, {
					[\mix, 0];
				});
				if (num == 14, {
					[\mix, 1];
				});
				if (num == 74, {
					[\fltr, 0];
				});
				if (num >= 88 and: { num <= 90 }, {
					[\fx1, num-88];
				});
				if (num == 106, {
					[\fx1, 3];
				});
				if (num >= 109 and: { num <= 110 }, {
					[\fx1, num-109+4]
				});
				if (num >= 111 and: { num <= 116 }, {
					[\fx0, num-111];
				});
			},
		);
	}

	noteOn { |func, type|
		^MIDIFunc.noteOn({ |vel, num, chan, src|
			if (src == midiOut.uid) {
				var noteInfo = this.prNoteInfo(num, chan);
				if (noteInfo.notNil and: {type.isNil or: { type == noteInfo[0] }}) {
					var value = vel / 127.0;
					func.value(value, num);
				};
			};
		});
	}

	noteOff { |func, type|
		^MIDIFunc.noteOff({ |vel, num, chan, src|
			if (src == midiOut.uid) {
				var noteInfo = this.prNoteInfo(num, chan);
				if (noteInfo.notNil and: {type.isNil or: { type == noteInfo[0] }}) {
					var value = vel / 127.0;
					func.value(value, num);
				};
			};
		});
	}

	cc { |func, type|
		^MIDIFunc.cc({ |value, num, chan, src|
			src.postln;
			midiOut.uid.postln;
			if (src == midiOut.uid) {
				var ccInfo = this.prCCInfo(num, chan);
				if ( ccInfo.notNil and: {type.isNil or: { type == ccInfo[0] }}) {
					value = value / 127.0;
					func.value(value, ccInfo[1]);
				};
			};
		});

	}

}
