(
~circuit = Circuit.new().connect("Circuit", "Circuit MIDI 1");
~circuit.syn0.cc = { |i, value|
	("syn0: " + i + " " + value).postln;
};
~circuit.syn1.cc = { |i, value|
	("syn1: " + i + " " + value).postln;
};
~circuit.drum0.cc = { |i, value|
	("drum0: " + i + " " + value).postln;
};
~circuit.drum1.cc = { |i, value|
	("drum1: " + i + " " + value).postln;
};
~circuit.fltr.cc = { |i, value|
	("fltr: " + i + " " + value).postln;
};
~circuit.mix.cc = { |i, value|
	("mix: " + i + " " + value).postln;
};
~circuit.fx0.cc = { |i, value|
	("fx0: " + i + " " + value).postln;
};
~circuit.fx1.cc = { |i, value|
	("fx1: " + i + " " + value).postln;
};
)