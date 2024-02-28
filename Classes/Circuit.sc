Circuit {
	classvar <numInstances = 0;
	classvar <>config;

	var <>server;
	var <buses;
	var <out;
	var <>normalize = false;

	var <midiIn;
	var <midiOut;


	*initClass {
		config = (
			\synth1: [Array.fill(8, 0), (80..87)], // SYNTH 1
			\synth2: [Array.fill(8, 1), (80..87)], // SYNTH 2
			\drum12: [Array.fill(8, 9), (14..21)], // DRUM 1-2
			\drum34: [Array.fill(8, 9), (46..53)], // DRUM 3-4
			\mixer: [[15, 15, 9, 9, 9, 9], [12, 14, 12, 23, 45, 53]], // mixerER
			\fx1: [Array.fill(6, 15), (111..116)], // REVERB (first row)
			\fx2: [Array.fill(6, 15), [88, 89, 90, 106, 109, 110]], // DELAY (second row)
			\filter: [[15], [74]], // FILTER KNOB
		);

		Class.initClassTree(Event);
		Event.addEventType(\circuit, { |server|
			if  (~midicmd != nil) {
				if (~midiout == nil, { ~midiout = ~circuit.midiOut; });
				~eventTypes[\midi].value(server);
			};
		});
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
			Ndef(\circuit).set(\ccBus, buses[\mixer].index);
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
				[\synth1];
			},
			1, {
				[\synth2];
			},
			9, {
				if (note == 60 or: {note == 62}) {
					[\drum12];
				} {
					//if (note == 64 or: {note == 65}) {
						[\drum34];
					//};
				};
			}
		);
	}

	prCCInfo { |num, chan|
		var info;
		switch (chan,
			0, {
				if (num >= config[\synth1][1].first and: { num <= config[\synth1][1].last }) {
					info = [\synth1, num-config[\synth1][1].first];
				};
			},

			1, {
				if (num >= config[\synth2][1].first and: { num <= config[\synth2][1].last }) {
					info = [\synth2, num-config[\synth2][1].first];
				};
			},

			9, {
				if (num == 12, {
					info = [\mixer, 2];
				});
				if (num >= 14 and: { num <= 17 }, {
					info = [\drum12, (num-14)*2];
				});
				if (num == 23, {
					info = [\mixer, 3];
				});
				if (num == 34, {
					info = [\drum12, 1];
				});
				if (num == 40, {
					info = [\drum12, 3];
				});
				if (num == 42, {
					info = [\drum12, 5];
				});
				if (num == 43, {
					info = [\drum12, 7];
				});
				if (num == 45, {
					info = [\mixer, 4];
				});
				if (num >= 46 and: { num <= 49 }, {
					info = [\drum34, (num-46)*2];
				});
				if (num == 53, {
					info = [\mixer, 5];
				});
				if (num == 55, {
					info = [\drum34, 1];
				});
				if (num == 57, {
					info = [\drum34, 3];
				});
				if (num == 61, {
					info = [\drum34, 5];
				});
				if (num == 76, {
					info = [\drum34, 7];
				});
			},

			15, {
				if (num == 12, {
					info = [\mixer, 0];
				});
				if (num == 14, {
					info = [\mixer, 1];
				});
				if (num == 74, {
					info = [\filter, 0];
				});
				if (num >= 88 and: { num <= 90 }, {
					info = [\fx2, num-88];
				});
				if (num == 106, {
					info = [\fx2, 3];
				});
				if (num >= 109 and: { num <= 110 }, {
					info = [\fx2, num-109+4]
				});
				if (num >= 111 and: { num <= 116 }, {
					info = [\fx1, num-111];
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
				if (type.isNil or: { noteInfo.notNil and: {type == noteInfo[0]} }) {
					var value = if (normalize, vel / 127.0, vel);
					var typeInfo = if (noteInfo.notNil, noteInfo[0], chan);
					func.value(value, num, typeInfo);
				};
			};
		});
	}

	noteOff { |func, type|
		^MIDIFunc.noteOff({ |vel, num, chan, src|
			var midi = (midiIn ? midiOut);
			if (midi.notNil and: {src == midi.uid}) {
				var noteInfo = this.prNoteInfo(num, chan);
				if (type.isNil or: { noteInfo.notNil and: {type == noteInfo[0]} }) {
					var value = if (normalize, vel / 127.0, vel);
					var typeInfo = if (noteInfo.notNil, noteInfo[0], chan);
					func.value(value, num, typeInfo);
				};
			};
		});
	}

	cc { |func, type|
		^MIDIFunc.cc({ |value, num, chan, src|
			var midi = (midiIn ? midiOut);
			if (midi.notNil and: {src == midi.uid}) {
				var ccInfo = this.prCCInfo(num, chan);
				if (type.isNil or: { ccInfo.notNil and: {type == ccInfo[0]} }) {
					value = if (normalize, value / 127.0, value);
					if (ccInfo.notNil, {
						func.value(value, ccInfo[1], ccInfo[0]);
					}, {
						func.value(value, num, chan);
					});
				};
			};
		});
	}

	control { |chan, num, value|
		if (midiOut.isNil, {
			("Circuit.control: midiOut not connected").throw;
		});
		midiOut.control(chan, num, value);
	}

	knob { |type, offset, value|
		if (config[type].isNil, {
			("Circuit.knob: unknown type " ++ type).throw;
		});
		this.control(config[type][0][offset], config[type][1][offset], value * if (normalize, 127.0, 1.0));
	}
}
