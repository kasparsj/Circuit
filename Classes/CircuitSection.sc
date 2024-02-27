CircuitSection {
	var <circuit;
	var <name;
	var <numChannels;
	var <chans;
	var <nums;
	var <bus;

	var <>cc;
	var <>noteOn;
	var <>noteOff;

	*new { |circuit, name, numChannels, chans, nums|
		var instance = super.newCopyArgs(circuit, name, numChannels, chans, nums);
		instance.init;
		^instance;
	}

	init { |s|
		bus = Bus.control(circuit.server, numChannels);
	}

	midiCC { |value, num|
		var from = nums.first;
		if (num >= from and: { num <= nums.last }, {
			this.setAt(value, num-nums.first);
		});
	}

	midiCCIndex { |value, idx|
		bus.setAt(idx, value);
		if (cc.notNil, { cc.value(idx, value) });
	}

	midiNoteOn { |value, note|
		if (noteOn.notNil, { noteOn.value(note, value) });
	}

	midiNoteOff { |value, note|
		if (noteOff.notNil, { noteOff.value(note, value) });
	}

	setCC { |chan, num, value|
		circuit.midiOut.control(chan, num, value);
	}

	setKnob { |offset=0, value|
		this.setCC(chans[offset], nums[offset], value * 127);
	}
}

