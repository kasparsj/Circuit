# Circuit

SuperCollider quark for using Novation Circuit as a MIDI controller

## Installation

`Quarks.install("https://github.com/kasparsj/Circuit.git");`

## Usage

The library can be used to:

- receive values of 51 knobs
- set values of 51 knobs
- receive note on/off events

The knobs are segregated by section:

- synth1 (8 knobs)
- synth2 (8 knobs)
- drum12 (8 knobs)
- drum34 (8 knobs)
- mixer (6 knobs)
- fx1 (6 knobs)
- fx2 (6 knobs)
- filter (1 knob)

Note on/off events can be received for these sections:

- synth1
- synth2
- drum12 (midinote 60 and 62)
- drum34 (midinote 64 and 65)

Fox example source code see: [examples/test.scd](https://github.com/kasparsj/Circuit/blob/main/examples/test.scd)
