Circuit {
	classvar <numInstances = 0;
	classvar <types;
	classvar <config;

	var <>server;
	var <buses;
	var <out;

	var <midiIn;
	var <midiOut;


	*initClass {
		config = (
			\syn0: [Array.fill(8, 0), (80..87)],
			\syn1: [Array.fill(8, 1), (80..87)],
			\drum0: [Array.fill(8, 9), (14..21)],
			\drum1: [Array.fill(8, 9), (46..53)],
			\mix: [[15, 15, 9, 9, 9, 9], [12, 14, 12, 23, 45, 53]],
			\fx0: [Array.fill(6, 15), (111..116)],
			\fx1: [Array.fill(6, 15), [88, 89, 90, 106, 109, 110]],
			\fltr: [[15], [74]],
		);
		types = config.keys;
	}

	*new { |s=nil|
		var instance = super.newCopyArgs(s ? Server.default);
		numInstances = numInstances + 1;
		instance.init(s ? Server.default);
		^instance;
	}

	init {
		buses = Dictionary.new;
		config.keysValuesDo { |key, value|
			buses[key] = Bus.control(server, value[0].size);
		};
		out = Bus.audio(server, 16);

		this.initNdef;

		if (Circuit.numInstances == 1, {
			this.makeDefault();
		});
		CmdPeriod.add { this.deinit(); }
	}

	deinit {
		if (midiIn != nil, {
			midiIn.free;
			midiIn = nil;
		});
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
			Ndef(\circuit).set(\ccBus, buses[\mix].index);
			Ndef(\circuit).play;
		}.fork;
	}

	makeDefault {
		if (Main.versionAtLeast( 3, 9 )) {
		    Event.addParentType(\circuit, (circuit: this));
		};
		// todo: do we need to throw an error?
	}

	connect { |deviceName, portName|
		this.connectIn(deviceName, portName);
		this.connectOut(deviceName, portName);
	}

	connectIn { |deviceName, portName|
		MIDIClient.sources.do { |endpoint, i|
			// todo: allow partial match
			if (endpoint.device == deviceName and: { portName.isNil or: { portName == endpoint.name } }) {
				try {
					midiIn = MIDIIn.connect(i, MIDIClient.sources.at(i));
				} { |err|
					err.postln;
				};
			};
		};
	}

	connectOut { |deviceName, portName|
		MIDIClient.destinations.do {|endpoint, i|
			// todo: allow partial match
			if (endpoint.device == deviceName and: { portName.isNil or: { portName == endpoint.name } }) {
				midiOut = MIDIOut.newByName(deviceName, endpoint.name);
			};
		};
	}

	prNoteInfo { |note = 60, chan = 0|
		^switch (chan,
			0, {
				[\syn0];
			},
			1, {
				[\syn1];
			},
		);
	}

	prCCInfo { |num, chan|
		var info;
		switch (chan,
			0, {
				if (num >= config[\syn0][1].first and: { num <= config[\syn0][1].last }) {
					info = [\syn0, num-config[\syn0][1].first];
				};
			},

			1, {
				if (num >= config[\syn1][1].first and: { num <= config[\syn1][1].last }) {
					info = [\syn1, num-config[\syn1][1].first];
				};
			},

			9, {
				if (num == 12, {
					info = [\mix, 2];
				});
				if (num >= 14 and: { num <= 17 }, {
					info = [\drum0, (num-14)*2];
				});
				if (num == 23, {
					info = [\mix, 3];
				});
				if (num == 34, {
					info = [\drum0, 1];
				});
				if (num == 40, {
					info = [\drum0, 3];
				});
				if (num == 42, {
					info = [\drum0, 5];
				});
				if (num == 43, {
					info = [\drum0, 7];
				});
				if (num == 45, {
					info = [\mix, 4];
				});
				if (num >= 46 and: { num <= 49 }, {
					info = [\drum1, (num-46)*2];
				});
				if (num == 53, {
					info = [\mix, 5];
				});
				if (num == 55, {
					info = [\drum1, 1];
				});
				if (num == 57, {
					info = [\drum1, 3];
				});
				if (num == 61, {
					info = [\drum1, 5];
				});
				if (num == 76, {
					info = [\drum1, 7];
				});
			},

			15, {
				if (num == 12, {
					info = [\mix, 0];
				});
				if (num == 14, {
					info = [\mix, 1];
				});
				if (num == 74, {
					info = [\fltr, 0];
				});
				if (num >= 88 and: { num <= 90 }, {
					info = [\fx1, num-88];
				});
				if (num == 106, {
					info = [\fx1, 3];
				});
				if (num >= 109 and: { num <= 110 }, {
					info = [\fx1, num-109+4]
				});
				if (num >= 111 and: { num <= 116 }, {
					info = [\fx0, num-111];
				});
			},
		);
		^info;
	}

	noteOn { |func, type|
		^MIDIFunc.noteOn({ |vel, num, chan, src|
			var midi = (midiIn ? midiOut);
			if (midi.notNil and: {src == midi.uid}) {
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
			var midi = (midiIn ? midiOut);
			if (midi.notNil and: {src == midi.uid}) {
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
			var midi = (midiIn ? midiOut);
			if (midi.notNil and: {src == midi.uid}) {
				var ccInfo = this.prCCInfo(num, chan);
				if ( ccInfo.notNil and: {type.isNil or: { type == ccInfo[0] }}) {
					value = value / 127.0;
					func.value(value, ccInfo[1]);
				};
			};
		});
	}

	control { |chan, num, value|
		midiOut.control(chan, num, value);
	}

	knob { |type, offset, value|
		this.control(config[type][0][offset], config[type][1][offset], value * 127);
	}
}
