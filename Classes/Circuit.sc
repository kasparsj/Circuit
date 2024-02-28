Circuit {
	classvar <numInstances = 0;
	classvar <defaultChans;
	classvar <midiChans;
	classvar <midiCCs;
	classvar <config;

	var <>server;
	var <buses;
	var <out;
	var <>normalize = false;
	var <noteToggles;

	var <midiIn;
	var <midiOut;


	*initClass {
		defaultChans = (
			\synth1: 0,
			\synth2: 1,
			\drums: 9,
			\fx: 15,
		);
		this.configure;

		Class.initClassTree(Event);
		Event.addEventType(\circuit, { |server|
			if  (~midicmd != nil) {
				if (~midiout == nil, { ~midiout = ~circuit.midiOut; });
				~eventTypes[\midi].value(server);
			};
		});
	}

	*configure { |chans|
		chans = defaultChans ++ (chans ? ());
		config = (
			\synth1: [Array.fill(8, chans[\synth1]), (80..87)],
			\synth2: [Array.fill(8, chans[\synth2]), (80..87)],
			\drum12: [Array.fill(8, chans[\drums]), [14, 34, 15, 40, 16, 42, 17, 43]],
			\drum34: [Array.fill(8, chans[\drums]), [ 46, 55, 47, 57, 48, 61, 49, 76]],
			\mixer: [[chans[\fx], chans[\fx], chans[\drums], chans[\drums], chans[\drums], chans[\drums]], [12, 14, 12, 23, 45, 53]],
			\fx1: [Array.fill(6, chans[\fx]), (111..116)], // REVERB (first row)
			\fx2: [Array.fill(6, chans[\fx]), [88, 89, 90, 106, 109, 110]], // DELAY (second row)
			\filter: [[chans[\fx]], [74]],
		);
		midiChans = chans;
		midiCCs = Dictionary.new;
		config.keysValuesDo { |type, conf|
			conf[0].do { |chan, i|
				var cc = conf[1][i];
				if (midiCCs[chan].isNil, { midiCCs[chan] = Dictionary.new });
				midiCCs[chan][cc] = [type, i];
			};
		};
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
		noteToggles = Dictionary.new;

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
			{midiChans[\synth1]}, { [\synth1] },
			{midiChans[\synth2]}, { [\synth2] },
			{midiChans[\drums]}, {
				if (note == 60 or: {note == 62}) {
					[\drum12];
				} {
					//if (note == 64 or: {note == 65}) {
					[\drum34];
					//};
				};
			},
		);
	}

	prCCInfo { |num, chan|
		^block {|break|
			midiCCs[chan].keysValuesDo { |cc, info|
				if (num == cc) {
					break.value(info);
				};
			};
		};
	}

	noteOn { |func, type, note|
		^MIDIFunc.noteOn({ |vel, num, chan, src|
			var midi = (midiIn ? midiOut);
			if (midi.notNil and: {src == midi.uid} and: {note.isNil or: {note == num}}) {
				var noteInfo = this.prNoteInfo(num, chan);
				if (type.isNil or: { noteInfo.notNil and: {type == noteInfo[0]} }) {
					var value = if (normalize, vel / 127.0, vel);
					var typeInfo = if (noteInfo.notNil, noteInfo[0], chan);
					func.value(value, num, typeInfo);
				};
			};
		});
	}

	noteOff { |func, type, note|
		^MIDIFunc.noteOff({ |vel, num, chan, src|
			var midi = (midiIn ? midiOut);
			if (midi.notNil and: {src == midi.uid} and: {note.isNil or: {note == num}}) {
				var noteInfo = this.prNoteInfo(num, chan);
				if (type.isNil or: { noteInfo.notNil and: {type == noteInfo[0]} }) {
					var value = if (normalize, vel / 127.0, vel);
					var typeInfo = if (noteInfo.notNil, noteInfo[0], chan);
					func.value(value, num, typeInfo);
				};
			};
		});
	}

	noteToggle { |func, type, note|
		if (type.isNil, {
			("Circuit.noteToggle: type is required").throw;
		});
		if (noteToggles[type].isNil, { noteToggles[type] = Dictionary.new; });
		this.noteOff({ |value, num, info|
			if (noteToggles[type][num].isNil, { noteToggles[type][num] = false });
			noteToggles[type][num] = noteToggles[type][num].not;
			func.value(noteToggles[type][num], num, info);
		}, type, note);
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

	setKnob { |type, index, value|
		if (config[type].isNil, {
			("Circuit.knob: unknown type " ++ type).throw;
		});
		this.control(config[type][0][index], config[type][1][index], value * if (normalize, 127.0, 1.0));
	}
}
