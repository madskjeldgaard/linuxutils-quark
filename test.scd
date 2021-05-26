x = X42Scope.new;
x = X42StereoMeter.new;
x = X42StereoPhase.new;

Ndef(\1, { Pan2.ar(PinkNoise.ar(), SinOsc.kr(1)) }).mold(2).play
