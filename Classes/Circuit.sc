Circuit {
	classvar <numInstances = 0;
	classvar <defaultChans;
	classvar <midiChans;
	classvar <midiCCs;
	classvar <config;

	var <>server;
	var <>midiIn;
	var <>midiOut;
	var <buses;
	var <out;

	var <>checkSrc = true;
	var <>normalize = false;
	var <noteOnFn;
	var <noteOffFn;
	var <ccFn;
	var <programFn;
	var <noteOnListeners;
	var <noteOffListeners;
	var <ccListeners;
	var <programListeners;
	var <programChange = false;
	var <programChangeType = false;
	var <programChangeNotes;
	var <toggleModeNotes;


	*initClass {
		defaultChans = (
			\synth1: 0,
			\synth2: 1,
			\drums: 9,
			\sessions: 15,
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

	*findMIDIIn { |deviceName="Circuit", portName=nil|
		^block { |break|
			MIDIClient.sources.do { |endpoint, i|
				// todo: allow partial match
				if (endpoint.device == deviceName and: { portName.isNil or: { portName == endpoint.name } }) {
					break.value(MIDIClient.sources.at(i));
				};
			};
		};
	}

	*findMIDIOut { |deviceName="Circuit", portName=nil|
		^block { |break|
			MIDIClient.destinations.do {|endpoint, i|
				// todo: allow partial match
				if (endpoint.device == deviceName and: { portName.isNil or: { portName == endpoint.name } }) {
					break.value(MIDIOut.newByName(deviceName, endpoint.name));
				};
			};
		};
	}

	*configure { |chans|
		chans = defaultChans ++ (chans ? ());
		config = (
			\synth1: [Array.fill(8, chans[\synth1]), (80..87)],
			\synth2: [Array.fill(8, chans[\synth2]), (80..87)],
			\drum12: [Array.fill(8, chans[\drums]), [14, 34, 15, 40, 16, 42, 17, 43]],
			\drum34: [Array.fill(8, chans[\drums]), [ 46, 55, 47, 57, 48, 61, 49, 76]],
			\mixer: [[chans[\sessions], chans[\sessions], chans[\drums], chans[\drums], chans[\drums], chans[\drums]], [12, 14, 12, 23, 45, 53]],
			\fx1: [Array.fill(6, chans[\sessions]), (111..116)], // REVERB (first row)
			\fx2: [Array.fill(6, chans[\sessions]), [88, 89, 90, 106, 109, 110]], // DELAY (second row)
			\filter: [[chans[\sessions]], [74]],
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

	*new { |deviceName="Circuit", portName=nil, s=nil|
		var instance;
		instance = super.newCopyArgs(s ? Server.default, this.findMIDIIn(deviceName, portName), this.findMIDIOut(deviceName, portName));
		numInstances = numInstances + 1;
		instance.init;
		^instance;
	}

	init {
		buses = Dictionary.new;
		config.keysValuesDo { |key, value|
			buses[key] = Bus.control(server, value[0].size);
		};
		out = Bus.audio(server, 16);

		noteOnListeners = [];
		noteOffListeners = [];
		ccListeners = [];
		programListeners = [];
		programChangeNotes = [];
		toggleModeNotes = Dictionary.new;

		this.initNdef;

		if (Circuit.numInstances == 1, {
			this.makeDefault();
		});
		CmdPeriod.add { this.deinit(); };

		this.initMidiFns;
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
		/*if (noteOnFn.notNil, {
			noteOnFn.free;
			noteOnFn = nil;
			noteOffFn.free;
			noteOffFn = nil;
			ccFn.free;
			ccFn = nil;
			programFn.free;
			programFn = nil;
		});*/
		programChange = false;
		programChangeType = false;
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

	initMidiFns {
		noteOnFn = MIDIFunc.noteOn({ |vel, num, chan, src|
			// todo: can't use midiOut here because it does not have uid property
			var midi = (midiIn ? midiOut);
			if (checkSrc.not or: {midi.notNil and: {src == midi.uid}}) {
				this.prOnNoteOn(vel, num, chan);
			};
		});
		noteOffFn = MIDIFunc.noteOff({ |vel, num, chan, src|
			var midi = (midiIn ? midiOut);
			if (checkSrc.not or: {midi.notNil and: {src == midi.uid}}) {
				this.prOnNoteOff(vel, num, chan);
			};
		});
		ccFn = MIDIFunc.cc({ |value, num, chan, src|
			var midi = (midiIn ? midiOut);
			if (checkSrc.not or: {midi.notNil and: {src == midi.uid}}) {
				this.prOnControl(value, num, chan);
			};
		});
		programFn = MIDIFunc.program({ |value, chan, src|
			var midi = (midiIn ? midiOut);
			if (checkSrc.not or: {midi.notNil and: {src == midi.uid}}) {
				this.prOnProgram(value, chan);
			};
		});
	}

	prOnNoteOn { |vel, num, chan|
		var noteType = this.prNoteType(num, chan);
		if (noteType.notNil and: { programChangeType == noteType }) {
			programChangeNotes = programChangeNotes.add(num);
			if (programChangeNotes.size == 1) {
				this.prFilterListeners(programListeners, num, noteType).do { |listener|
					{ |func, type|
						func.value(programChange % 32, noteType);
					}.valueArray(listener);
				};
			};
		} {
			block { |break|
				if (noteType.notNil and: { toggleModeNotes[noteType].notNil and: { toggleModeNotes[noteType][num].notNil } }) {
					break.value;
				};
				this.prFilterListeners(noteOnListeners, num, noteType).do { |listener|
					{ |func, type, note|
						var value = if (normalize, vel / 127.0, vel);
						func.value(value, num, noteType ? chan);
					}.valueArray(listener);
				};
			};
		};
	}

	prOnNoteOff { |vel, num, chan|
		var noteType = this.prNoteType(num, chan);
		if (noteType.notNil and: { programChangeType == noteType }) {
			programChangeNotes.removeAt(programChangeNotes.indexOf(num));
			if (programChangeNotes.size == 0) {
				programChange = false;
				programChangeType = false;
			};
		} {
			block { |break|
				if (noteType.notNil and: { toggleModeNotes[noteType].notNil }) {
					if (toggleModeNotes[noteType][num].isNil) {
						toggleModeNotes[noteType][num] = 127;
						this.noteOn(noteType, num, 127, false);
						break.value;
					} {
						toggleModeNotes[noteType].removeAt(num);
						this.noteOff(noteType, num, 0, false);
					};
				};
				this.prFilterListeners(noteOffListeners, num, noteType).do { |listener|
					{ |func, type, note|
						var value = if (normalize, vel / 127.0, vel);
						func.value(value, num, noteType ? chan);
					}.valueArray(listener);
				};
			};
		};
	}

	prOnControl { |value, num, chan|
		var ccInfo = this.prCCInfo(num, chan);
		this.prFilterListeners(ccListeners, num, if (ccInfo.notNil, { ccInfo[0] })).do { |listener|
			{ |func, type, note|
				value = if (normalize, value / 127.0, value);
				if (ccInfo.notNil, {
					func.value(value, ccInfo[1], ccInfo[0]);
				}, {
					func.value(value, num, chan);
				});
			}.valueArray(listener);
		};
	}

	prOnProgram { |value, chan|
		var chanInfo = this.prChanInfo(chan);
		if (chanInfo.notNil) {
			block { |break|
				programListeners.do { |listener|
					{ |func, type|
						if (type.isNil or: { chanInfo == type }) {
							if (chanInfo == \sessions) {
								{ |func, type|
									func.value(value % 32, chanInfo);
								}.valueArray(listener);
							} {
								programChange = value;
								programChangeType = chanInfo;
								break.value;
							};
						};
					}.valueArray(listener);
				};
			};
		};
	}

	prFilterListeners { |listeners, num, noteType|
		^listeners.select { |listener|
			{ |func, type, note|
				if (note.isNil or: {note == num}) {
					if (type.isNil or: { noteType.notNil and: {type == noteType} }) {
						true
					};
				};
			}.valueArray(listener) ? false;
		};
	}

	prChanInfo { |chan|
		^switch (chan,
			{midiChans[\synth1]}, { \synth1 },
			{midiChans[\synth2]}, { \synth2 },
			{midiChans[\sessions]}, { \sessions },
		);
	}

	prNoteType { |note, chan|
		^switch (chan,
			{midiChans[\synth1]}, { \synth1 },
			{midiChans[\synth2]}, { \synth2 },
			{midiChans[\drums]}, {
				if (note == 60 or: {note == 62}) {
					\drum12;
				} {
					//if (note == 64 or: {note == 65}) {
					\drum34;
					//};
				};
			},
		);
	}

	prCCInfo { |num, chan|
		^block { |break|
			midiCCs[chan].keysValuesDo { |cc, info|
				if (num == cc) {
					break.value(info);
				};
			};
		};
	}

	onNoteOn { |func, type, note|
		noteOnListeners = noteOnListeners.add([func, type, note]);
	}

	onNoteOff { |func, type, note|
		noteOffListeners = noteOffListeners.add([func, type, note]);
	}

	onControl { |func, type, num|
		ccListeners = ccListeners.add([func, type, num]);
	}

	onProgram { |func, type|
		programListeners = programListeners.add([func, type, nil]);
	}

	control { |chan, num, value|
		if (midiOut.isNil, {
			("Circuit.control: midiOut not connected").throw;
		});
		midiOut.control(chan, num, value);
	}

	knob { |type, index, value|
		if (config[type].isNil, {
			("Circuit.knob: unknown type " ++ type).throw;
		});
		this.control(config[type][0][index], config[type][1][index], value * if (normalize, 127.0, 1.0));
	}

	program { |type, num|
		if (midiChans[type].isNil, {
			("Circuit.program: unknown type " ++ type).throw;
		});
		if (type == \drums, {
			("Circuit.program: cannot do program change for drums").throw;
		});
		if (midiOut.isNil, {
			("Circuit.program: midiOut not connected").throw;
		});
		midiOut.program(midiChans[type], if (type == \synth2, { 32+(num%32) }, { (num%32) }));
	}

	noteOn { |type, note = 60, vel = 64, callListeners = true|
		var chan;
		if (type == \drum12 or: { type == \drum34 }, { type = \drums; });
		if (midiChans[type].isNil, {
			("Circuit.noteOn: unknown type " ++ type).throw;
		});
		if (midiOut.isNil, {
			("Circuit.noteOn: midiOut not connected").throw;
		});
		chan = midiChans[type];
		vel = if (normalize, { vel * 127 }, { vel });
		midiOut.noteOn(chan, note, vel);
		if (callListeners) {
			this.prOnNoteOn(vel, note, chan);
		};
	}

	noteOff { |type, note = 60, vel = 64, callListeners = true|
		var chan;
		if (type == \drum12 or: { type == \drum34 }, { type = \drums; });
		if (midiChans[type].isNil, {
			("Circuit.noteOff: unknown type " ++ type).throw;
		});
		if (midiOut.isNil, {
			("Circuit.noteOff: midiOut not connected").throw;
		});
		chan = midiChans[type];
		vel = if (normalize, { vel * 127 }, { vel });
		midiOut.noteOff(chan, note, vel);
		if (callListeners) {
			this.prOnNoteOff(vel, note, chan);
		};
	}

	toggleMode { |type, enable = true|
		if (midiChans[type].isNil, {
			("Circuit.toggleMode: unknown type " ++ type).throw;
		});
		if (type == \drums, {
			("Circuit.toggleMode: cannot do toggleMode for drums").throw;
		});
		if (midiOut.isNil, {
			("Circuit.toggleMode: midiOut not connected").throw;
		});
		if (enable) {
			if (toggleModeNotes[type].isNil, {
				toggleModeNotes[type] = Dictionary.new;
			});
		} {
			if (toggleModeNotes[type].notNil, {
				toggleModeNotes[type].keys.do { |note|
					this.noteOff(type, note, 0);
				};
				toggleModeNotes.removeAt(type);
			});
		};
	}
}
