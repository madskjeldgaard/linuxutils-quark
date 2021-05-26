# linuxutils

### Assorted linux based utilities

Assorted linux based utilities

### Dependencies

- Linux
- Jack
- [X42 plugins](http://x42-plugins.com/x42/)

#### Included

Convenience functions for starting up and connecting to the excellent x42 meters and scopes:

##### X42

![x42 meters in action](x42.jpg)

```
x = X42Scope.new;
x = X42StereoMeter.new;
x = X42StereoPhase.new;

Ndef(\1, { Pan2.ar(PinkNoise.ar(), SinOsc.kr(1)) }).mold(2).play
```

### Installation

Open up SuperCollider and evaluate the following line of code:
`Quarks.install("https://github.com/madskjeldgaard/linuxutils-quark")`
