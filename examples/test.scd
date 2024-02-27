(
// ~circuit = Circuit.new().connect("Circuit", "Circuit MIDI 1");
~circuit = Circuit.new.connect("M-Track", "M-Track MIDI 1");
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
)
