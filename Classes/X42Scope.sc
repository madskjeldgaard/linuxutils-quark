JACKClient_Base { 
	var maxClientInputs;
	var clientBaseName, processName;

	*new{
		^super.new.init()
	}

	init{
		Server.local.doWhenBooted{
			clientBaseName = this.getBaseName();
			maxClientInputs = this.getMaxClientIns();
			processName = this.getProcessName();

			if(this.scopeIsRunning().not, { 
				"% is not runnning, starting it now... ".format(processName).postln;
				fork{
					this.startProcess();

					// @FIXME: This wait is not cool (but works)
					1.wait;

					this.connect();
				}
			}, {
				this.connect();
			});
		}
	}

	connect{
		var hardwareOuts = Server.local.options.numOutputBusChannels;
		var numberConnections = if(hardwareOuts >= maxClientInputs, { 
			maxClientInputs
		}, {
			hardwareOuts
		});

		fork{
			numberConnections.do{|conNum|
				this.doConnection(conNum)
			}
		}
	}

	doConnection{|conNum|
		var c = Condition.new;
		var command;
		"Connecting SuperCollider output num % to % input".format(conNum, clientBaseName).postln;
		conNum = conNum + 1;
		command = "jack_connect SuperCollider:out_%  '%%'".format(conNum, clientBaseName, conNum);

		command.unixCmd(action: { c.unhang });
		c.hang;

	}

	scopeIsRunning{
		var conns = "jack_lsp".unixCmdGetStdOutLines;
		^conns.indexOfEqual(
			"%1".format(clientBaseName)
		).isNil.not
	}

	startProcess{
		var processName = this.getProcessName();
		"% </dev/null &>/dev/null &".format(processName).unixCmdGetStdOut();
	}

	getProcessName{
		"getProcessName not implemented".error
	}

	getBaseName{
		"getBaseName not implemented".error
	}

	getMaxClientIns{
		"getMaxClientIns not implemented".error
	}

}

X42Scope : JACKClient_Base{

	getMaxClientIns{
		^4
	}

	getBaseName{
		^"Simple Scope (4 channel):in"
	}

	getProcessName{
		^"x42-scope"
	}
	
}

X42StereoMeter : JACKClient_Base{

	getMaxClientIns{
		^2
	}	

	getBaseName{
		^"Stereo/Frequency Scope:in"
	}

	getProcessName{
		^"x42-meter 16"
	}
	
}

X42PhaseWheel : JACKClient_Base{

	getMaxClientIns{
		^2
	}	

	getBaseName{
		^"Phase/Frequency Wheel:in"
	}

	getProcessName{
		^"x42-meter 14"
	}
	
}

X42StereoPhase : JACKClient_Base{
	getMaxClientIns{
		^2
	}	

	getBaseName{
		^"Stereo Phase-Correlation Meter:in"
	}

	getProcessName{
		^"x42-meter 12"
	}

	doConnection{|conNum|
		var c = Condition.new;
		var command;
		var inSuffices = ["L", "R"];
		"Connecting SuperCollider output num % to % input".format(conNum, clientBaseName).postln;
		command = "jack_connect SuperCollider:out_%  '%%'".format(conNum+1, clientBaseName, inSuffices[conNum]);

		command.unixCmd(action: { c.unhang });
		c.hang;

	}
}

X42GonioMeter : JACKClient_Base{
	getMaxClientIns{
		^2
	}	

	getBaseName{
		^"Goniometer:in"
	}

	getProcessName{
		^"x42-meter 13"
	}

	doConnection{|conNum|
		var c = Condition.new;
		var command;
		var inSuffices = ["L", "R"];
		"Connecting SuperCollider output num % to % input".format(conNum, clientBaseName).postln;
		command = "jack_connect SuperCollider:out_%  '%%'".format(conNum+1, clientBaseName, inSuffices[conNum]);

		command.unixCmd(action: { c.unhang });
		c.hang;

	}
}

Japa : JACKClient_Base{
	getMaxClientIns{
		^4
	}	

	getBaseName{
		^"japa:in_"
	}

	getProcessName{
		^"japa -J"
	}
}

Jaaa : JACKClient_Base{
	getMaxClientIns{
		^8
	}	

	getBaseName{
		^"jaaa:in_"
	}

	getProcessName{
		^"jaaa -J"
	}
}
