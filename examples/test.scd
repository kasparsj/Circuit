(
// 1. connect
~circuit = Circuit.new.connect("Circuit");
// ~circuit = Circuit.new.connect("M-Track");
)

(
// 2. receive knob values from Circuit
~circuit.cc({ |value, i|
	("synth1 knob " + i + ": " + value).postln;
}, \synth1);
~circuit.cc({ |value, i|
	("synth2 knob " + i + ": " + value).postln;
}, \synth2);
~circuit.cc({ |value, i|
	("drum12 knob " + i + ": " + value).postln;
}, \drum12);
~circuit.cc({ |value, i|
	("drum34 knob " + i + ": " + value).postln;
}, \drum34);
~circuit.cc({ |value, i|
	("filter knob: " + value).postln;
}, \filter);
~circuit.cc({ |value, i|
	("mixer knob " + i + ": " + value).postln;
}, \mixer);
~circuit.cc({ |value, i|
	("fx1 knob " + i + ": " + value).postln;
}, \fx1);
~circuit.cc({ |value, i|
	("fx2 knob " + i + ": " + value).postln;
}, \fx2);
)

(
// 3. receive note on/off/toggle from Circuit
~circuit.noteOn({ |value, note|
	("synth1 note on: " + note + " " + value).postln;
}, \synth1);
~circuit.noteOff({ |value, note|
	("synth1 note off: " + note + " " + value).postln;
}, \synth1);

~circuit.noteToggle({ |value, note|
	("synth2 note toggle: " + note + " " + value).postln;
}, \synth2);

~circuit.noteOn({ |value, note|
	("drum12 note on: " + note + " " + value).postln;
}, \drum12);
~circuit.noteOff({ |value, note|
	("drum12 note off: " + note + " " + value).postln;
}, \drum12);

~circuit.noteOn({ |value, note|
	("drum34 note on: " + note + " " + value).postln;
}, \drum34);
~circuit.noteOff({ |value, note|
	("drum34 note off: " + note + " " + value).postln;
}, \drum34);
)

(
// 4. receive program changes from Circuit
~circuit.program({ |value|
	("synth1 program change: " + value).postln;
}, \synth1);
~circuit.program({ |value|
	("synth2 program change: " + value).postln;
}, \synth2);
)

(
// 5. send MIDI to Circuit

// set first knob to full on
~circuit.setKnob(\synth1, 0, 127);
)

(
// optionally normalize input/output
~circuit.normalize = true;
~circuit.cc({ |value, i|
	// now value is between 0.0 and 1.0
	("synth1 knob " + i + ": " + value).postln;
}, \synth1);
~circuit.setKnob(\synth1, 0, 1); // same as above
)

(
// if you've reconfigured midi channels, e.g.:
Circuit.configure((
	\synth1: 2,
	\synth2: 3
	\drums: 4,
	\fx: 5,
));
// restore defaults midi channel config
Circuit.configure(Circuit.defaultChans)
)
