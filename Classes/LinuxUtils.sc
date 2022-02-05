
PluginServerClient_Base {
	var maxClientInputs;
	var clientBaseName, <processName, hasStarted=false, <pid;
    var <>framework = 'pipewire';

	*new{
		^super.new.init()
	}

	init{
		this.afterInit();
	}

	afterInit{
		Server.local.doWhenBooted{
			clientBaseName = this.getBaseName();
			maxClientInputs = this.getMaxClientIns();
			processName = this.getProcessName();

            framework.isNil.if {
                "PluginServerClient_Base.framework needs to be set to 'pipewire' or 'jack'".warn;
                ^nil
            };

			if(this.scopeIsRunning().not, { 
				"% is not runnning, starting it now... ".format(processName).postln;
				fork{
					var cond = Condition.new;
					var timer; 

					this.startProcess();

					// Wait for plugin's conn client to start up
					// Tak fredrik olofsson!
					timer= Main.elapsedTime;
					"Waiting for % client to start".postf(framework);
					while({cond.test.not}, {
						cond.test= this.scopeIsRunning();
						cond.signal;
						if(cond.test.not, {
                            ".".post;
							if(Main.elapsedTime-timer<7, {1.wait}, {"timeout - trying to connect".warn; cond.test= true})
						});
					});

					cond.wait;

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

// 	connectInserted{
// 		var hardwareOuts = Server.local.options.numOutputBusChannels;
// 		var numberConnections = if(hardwareOuts >= maxClientInputs, { 
// 			maxClientInputs
// 		}, {
// 			hardwareOuts
// 		});

// 		fork{
// 			numberConnections.do{|conNum|
// 				this.doConnection(conNum)
// 			}
// 		}
// 	}

    // here we could look into using pw-link -d [link_id] -> maybe storing them in a dictionary, if one woudl want to have several instances of some util going..
	disconnectSuperColliderOut{|outNum|
		var c = Condition.new;
		var command, plSrv;
		"Disconnecting SuperCollider output num %".format(outNum).postln;
		outNum = outNum + 1;

        plSrv = case
        { framework.asSymbol == \pipewire } { "pw-link -d" }
        { framework.asSymbol == \jack } { "jack_disconnect" }
        { "wrong framework specified".warn; ^nil };

		command = "% SuperCollider:out_%  '%%'".format(plSrv, outNum, clientBaseName, outNum);

		command.unixCmd(action: { c.unhang });
		c.hang;
	}

	doConnection{|conNum|
		var c = Condition.new;
		var command, plSrv;
		"Connecting SuperCollider output num % to % input".format(conNum, clientBaseName).postln;
		conNum = conNum + 1;

        plSrv = case
        { framework.asSymbol == \pipewire } { "pw-link" }
        { framework.asSymbol == \jack } { "jack_connect" }
        { "wrong framework specified".warn; ^nil };

		command = "% SuperCollider:out_%  '%%'".format(plSrv, conNum, clientBaseName, conNum);

		command.unixCmd(action: { c.unhang });
		c.hang;
	}

	scopeIsRunning{
		var conns;
        var plSrv = case
        { framework.asSymbol == \pipewire } { "pw-link -l" }
        { framework.asSymbol == \jack } { "jack_lsp" }
        { "wrong framework specified".warn; ^nil };
        conns = plSrv.unixCmdGetStdOutLines;
		^conns.indexOfEqual(
			"%1".format(clientBaseName)
		).isNil.not
	}

	pidRunning{
		if(pid.isNil, {
			^false
		}, { 
			var result = "ps -p %".format(pid).postln.unixCmdGetStdOutLines;
			^result[1].isNil.not
		})
	}

	startProcess{
		var processName = this.getProcessName();

		pid = "% &".format(processName).unixCmd();

		// if(this.pidRunning.not, {
		// 	// Start process, background it and get the process id (pid) 
		// 	// "% </dev/null &>/dev/null & $!".format(processName).unixCmdGetStdOut();
		// 			}, {
		// 	"% has already been launched...".format(processName).warn
		// })
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

CarlaSingleVST : PluginServerClient_Base{
	var plugName, plugPrefix, plugPath, plugFormat;

	*new{|pluginName, pluginPath, pluginFormat, pluginPrefix="SC_"|
		^super.new.init(pluginName, pluginPath, pluginFormat, pluginPrefix).afterInit()
	}

	init{|pluginName, pluginPath, pluginFormat, pluginPrefix|

		plugName = pluginName;
		plugPrefix = pluginPrefix;
		plugPath = pluginPath;
		plugFormat = pluginFormat;

		// this.afterInit();
	}

	getMaxClientIns{
		^2
	}	

	getBaseName{
		^this.getClientName ++ ":input_"
	}

	getClientName{
		^plugPrefix ++ plugName
	}

	getProcessName{
		^"CARLA_CLIENT_NAME=\"%\" carla-single % '%'".format(this.getClientName, plugFormat, plugPath)
	}
}

X42Scope : PluginServerClient_Base{

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

X42StereoMeter : PluginServerClient_Base{

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

X42PhaseWheel : PluginServerClient_Base{

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

X42StereoPhase : PluginServerClient_Base{
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
		var command, plSrv;
		var inSuffices = ["L", "R"];
        plSrv = case
        { framework.asSymbol == \pipewire } { "pw-link" }
        { framework.asSymbol == \jack } { "jack_connect" }
        { "wrong framework specified".warn; ^nil };
		"Connecting SuperCollider output num % to % input".format(conNum, clientBaseName).postln;
		command = "% SuperCollider:out_%  '%%'".format(plSrv, conNum+1, clientBaseName, inSuffices[conNum]);

		command.unixCmd(action: { c.unhang });
		c.hang;

	}
}

X42GonioMeter : PluginServerClient_Base{
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
		var command, plSrv;
		var inSuffices = ["L", "R"];
        plSrv = case
        { framework.asSymbol == \pipewire } { "pw-link" }
        { framework.asSymbol == \jack } { "jack_connect" }
        { "wrong framework specified".warn; ^nil };
		"Connecting SuperCollider output num % to % input".format(conNum, clientBaseName).postln;
		command = "% SuperCollider:out_%  '%%'".format(plSrv, conNum+1, clientBaseName, inSuffices[conNum]);

		command.unixCmd(action: { c.unhang });
		c.hang;

	}
}

Japa : PluginServerClient_Base{
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

Jaaa : PluginServerClient_Base{
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


