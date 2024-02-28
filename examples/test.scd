(
// 1. connect
~circuit = Circuit.new.connect("Circuit");
// ~circuit = Circuit.new.connect("M-Track");
)

(
// 2. receive MIDI values from Circuit
~circuit.cc({ |value, i|
	("syn0: " + i + " " + value).postln;
}, \syn0); // SYNTH 1 knobs
~circuit.cc({ |value, i|
	("syn1: " + i + " " + value).postln;
}, \syn1); // SYNTH 2 knobs
~circuit.cc({ |value, i|
	("drum0: " + i + " " + value).postln;
}, \drum0); // DRUM 1-2 knobs
~circuit.cc({ |value, i|
	("drum1: " + i + " " + value).postln;
}, \drum1); // DRUM 3-4 knobs
~circuit.cc({ |value, i|
	("fltr: " + i + " " + value).postln;
}, \fltr); // FILTER knob
~circuit.cc({ |value, i|
	("mix: " + i + " " + value).postln;
}, \mix); // MIXER knobs
~circuit.cc({ |value, i|
	("fx0: " + i + " " + value).postln;
}, \fx0); // FX 1 REVERB knobs
~circuit.cc({ |value, i|
	("fx1: " + i + " " + value).postln;
}, \fx1); // FX 2 DELAY knobs
~circuit.noteOn({ |value, note|
	("syn0 note on: " + note + " " + value).postln;
}, \syn0); // SYNTH 1 note on
~circuit.noteOff({ |value, note|
	("syn0 note off: " + note + " " + value).postln;
}, \syn0); // SYNTH 1 note off
~circuit.noteOn({ |value, note|
	("syn1 note on: " + note + " " + value).postln;
}, \syn1); // SYNTH 2 note on
~circuit.noteOff({ |value, note|
	("syn1 note off: " + note + " " + value).postln;
}, \syn1); // SYNTH 2 note off
~circuit.noteOn({ |value, note|
	("drum0 note on: " + note + " " + value).postln;
}, \drum0); // DRUM 1-2 note on
~circuit.noteOff({ |value, note|
	("drum0 note off: " + note + " " + value).postln;
}, \drum0); // DRUM 1-2 note off
~circuit.noteOn({ |value, note|
	("drum1 note on: " + note + " " + value).postln;
}, \drum1); // DRUM 3-4 note on
~circuit.noteOff({ |value, note|
	("drum1 note off: " + note + " " + value).postln;
}, \drum1); // DRUM 3-4 note off
)

(
// 3. send MIDI values
// change knob value
~circuit.knob(\syn0, 0, 0);
)
