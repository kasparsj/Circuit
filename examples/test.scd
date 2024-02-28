(
// 1. connect
~circuit = Circuit.new.connect("Circuit");
// ~circuit = Circuit.new.connect("M-Track");
// ~circuit.checkSrc = false;
)

(
// 2. receive knob values from Circuit
~circuit.onControl({ |value, i|
	("synth1 knob " + i + ": " + value).postln;
}, \synth1);
~circuit.onControl({ |value, i|
	("synth2 knob " + i + ": " + value).postln;
}, \synth2);
~circuit.onControl({ |value, i|
	("drum12 knob " + i + ": " + value).postln;
}, \drum12);
~circuit.onControl({ |value, i|
	("drum34 knob " + i + ": " + value).postln;
}, \drum34);
~circuit.onControl({ |value, i|
	("filter knob: " + value).postln;
}, \filter);
~circuit.onControl({ |value, i|
	("mixer knob " + i + ": " + value).postln;
}, \mixer);
~circuit.onControl({ |value, i|
	("fx1 knob " + i + ": " + value).postln;
}, \fx1);
~circuit.onControl({ |value, i|
	("fx2 knob " + i + ": " + value).postln;
}, \fx2);
)

(
// 3. receive note on/off/toggle from Circuit
~circuit.onNoteOn({ |value, note|
	("synth1 note on: " + note + " " + value).postln;
}, \synth1);
~circuit.onNoteOff({ |value, note|
	("synth1 note off: " + note + " " + value).postln;
}, \synth1);

~circuit.onNoteOn({ |value, note|
	("synth2 note on: " + note + " " + value).postln;
}, \synth2);
~circuit.onNoteOff({ |value, note|
	("synth2 note off: " + note + " " + value).postln;
}, \synth2);

~circuit.onNoteOn({ |value, note|
	("drum12 note on: " + note + " " + value).postln;
}, \drum12);
~circuit.onNoteOff({ |value, note|
	("drum12 note off: " + note + " " + value).postln;
}, \drum12);

~circuit.onNoteOn({ |value, note|
	("drum34 note on: " + note + " " + value).postln;
}, \drum34);
~circuit.onNoteOff({ |value, note|
	("drum34 note off: " + note + " " + value).postln;
}, \drum34);
)

(
// 4. receive program changes from Circuit
~circuit.onProgram({ |value|
	("synth1 program change: " + value).postln;
}, \synth1);
~circuit.onProgram({ |value|
	("synth2 program change: " + value).postln;
}, \synth2);
~circuit.onProgram({ |value|
	("sessions program change: " + value).postln;
}, \sessions);
)

(
// 5. send MIDI to Circuit

// set first knob to full on
~circuit.knob(\synth1, 0, 127);

// choose second patch for synth1
~circuit.program(\synth1, 1);

// play a note for 1 second
~circuit.noteOn(\synth1, 60, 127);
{
	1.wait;
	~circuit.noteOff(\synth1, 60, 127);
}.fork;

// emulate toggleMode on synth2
~circuit.toggleMode(\synth2);
)

// turn toggleMode off
~circuit.toggleMode(\synth2, false);

(
// optionally normalize input/output
~circuit.normalize = true;
~circuit.onControl({ |value, i|
	// now value is between 0.0 and 1.0
	("synth1 knob " + i + ": " + value).postln;
}, \synth1);
~circuit.knob(\synth1, 0, 1); // same as above
)

(
// if you've reconfigured midi channels, e.g.:
Circuit.configure((
	\synth1: 2,
	\synth2: 3
	\drums: 4,
));
// restore defaults midi channel config
Circuit.configure(Circuit.defaultChans)
)
