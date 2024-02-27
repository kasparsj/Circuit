CircuitSection {
	var <circuit;
	var <name;
	var <numChannels;
	var <chans;
	var <nums;
	var <bus;

	*new { |circuit, name, numChannels, chans, nums|
		var instance = super.newCopyArgs(circuit, name, numChannels, chans, nums);
		instance.init;
		^instance;
	}

	init {
		bus = Bus.control(circuit.server, numChannels);
	}

	control { |chan, num, value|
		circuit.midiOut.control(chan, num, value);
	}

	knob { |offset=0, value|
		this.setCC(chans[offset], nums[offset], value * 127);
	}
}

