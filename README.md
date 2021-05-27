# LinuxUtils

Assorted linux based utilities and convenience functions for launching and connecting to Jack stuff automatically without leaving SuperCollider.

### Dependencies

- Linux
- Jack
- [X42 plugins](http://x42-plugins.com/x42/) (optional)
- [Jaaa and Japa](https://kokkinizita.linuxaudio.org/linuxaudio/) (optional)
- [Carla](https://github.com/falkTX/Carla) (optional)

### Install dependencies

See helpfile `Overview/LinuxUtils` for more info.

#### Included

Convenience functions for starting up and connecting to the excellent Japa, Jaa and x42 meters and scopes:

![x42 meters in action](x42.jpg)

```
// Open a ton of scopes
(
X42Scope.new;
X42StereoMeter.new;
X42StereoPhase.new;
X42GonioMeter.new;
X42PhaseWheel.new;
Jaaa.new;
Japa.new;
)

Ndef(\1, { Pan2.ar(PinkNoise.ar(), SinOsc.kr(1)) }).mold(2).play
```

Open and connect to a plugin as a jack client using carla:

```
c = CarlaSingleVST.new("FabFilter Pro-C 222", "~/.vst3/yabridge/FabFilter Pro-C 2.vst3", "vst3")
```

### Installation

Open up SuperCollider and evaluate the following line of code:
`Quarks.install("https://github.com/madskjeldgaard/linuxutils-quark")`
