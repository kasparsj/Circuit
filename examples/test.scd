(
~circuit = Circuit.new.connect("Circuit");
// ~circuit = Circuit.new.connect("M-Track");
~circuit.cc({ |value, i|
	("syn0: " + i + " " + value).postln;
}, \syn0);
~circuit.cc({ |value, i|
	("syn1: " + i + " " + value).postln;
}, \syn1);
~circuit.cc({ |value, i|
	("drum0: " + i + " " + value).postln;
}, \drum0);
~circuit.cc({ |value, i|
	("drum1: " + i + " " + value).postln;
}, \drum1);
~circuit.cc({ |value, i|
	("fltr: " + i + " " + value).postln;
}, \fltr);
~circuit.cc({ |value, i|
	("mix: " + i + " " + value).postln;
}, \mix);
~circuit.cc({ |value, i|
	("fx0: " + i + " " + value).postln;
}, \fx0);
~circuit.cc({ |value, i|
	("fx1: " + i + " " + value).postln;
}, \fx1);
~circuit.noteOn({ |value, note|
	("syn0 note on: " + note + " " + value).postln;
}, \syn0);
~circuit.noteOff({ |value, note|
	("syn0 note off: " + note + " " + value).postln;
}, \syn0);
~circuit.noteOn({ |value, note|
	("syn1 note on: " + note + " " + value).postln;
}, \syn1);
~circuit.noteOff({ |value, note|
	("syn1 note off: " + note + " " + value).postln;
}, \syn1);
~circuit.noteOn({ |value, note|
	("drum0 note on: " + note + " " + value).postln;
}, \drum0);
~circuit.noteOff({ |value, note|
	("drum0 note off: " + note + " " + value).postln;
}, \drum0);
~circuit.noteOn({ |value, note|
	("drum1 note on: " + note + " " + value).postln;
}, \drum1);
~circuit.noteOff({ |value, note|
	("drum1 note off: " + note + " " + value).postln;
}, \drum1);
)
