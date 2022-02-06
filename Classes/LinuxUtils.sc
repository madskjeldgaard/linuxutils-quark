LinuxUtils {
    classvar <>framework = 'pipewire';
    classvar <>pluginTimeout = 5; // in reconds
    classvar <plugins;
    var maxClientInputs;
    var clientBaseName, <processName, hasStarted=false, <pid;

    *initClass {
        plugins = Dictionary.new();
        plugins.add(\CarlaSingleVST -> "Use Carla to load Single VST's");
        plugins.add(\X42Scope -> "An audio oscilloscope with variable time scale");
        plugins.add(\X42StereoMeter -> "A Stereo Meter");
        plugins.add(\X42PhaseWheel -> "A Wheel Phase Meter");
        plugins.add(\X42StereoPhase -> "A Phase Meter");
        plugins.add(\X42GonioMeter -> "A Stereo Phase Scope");
        plugins.add(\Japa -> "A 'perceptual' or 'psychoacoustic' audio spectrum analyser");
        plugins.add(\Jaaa -> "An audio signal generator and spectrum analyser designed to make accurate measurements");
    }

    init {
        Server.local.doWhenBooted{
            clientBaseName = this.getBaseName();
            maxClientInputs = this.getMaxClientIns();
            processName = this.getProcessName();

            framework.isNil.if {
                "LinuxUtils.framework needs to be set to 'pipewire' or 'jack'".warn;
                ^nil
            };

            if(this.neededInputExists().not, { 
                "% is not runnning, starting it now...\n".postf(processName);
                fork{
                    var cond = Condition.new;
                    var timer; 

                    this.startProcess();

                    timer= Main.elapsedTime;
                    "Waiting for % client to start\n".postf(framework);
                    while({cond.test.not}, {
                        cond.test = this.neededInputExists();
                        if(cond.test.not, {
                            ".".post;
                            if(Main.elapsedTime-timer<pluginTimeout)
                            {0.5.wait}
                            {"timeout - trying to connect".warn; cond.test= true}
                        });
                        cond.signal;
                    });

                    cond.wait;
                    "\n".post;
                    this.connect();
                }
            }, {
                this.connect();
            });
        }
    }

    connect {
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

    // here we could look into using pw-link -d [link_id] -> maybe storing them in a Dictionary
    // if one would want to have several instances of some util going..
    disconnectSuperColliderOut {|outNum|
        var c = Condition.new;
        var command, plSrv;
        "Disconnecting SuperCollider output num %\n".postf(outNum);
        outNum = outNum + 1;

        plSrv = case
        { framework.asSymbol == \pipewire } { "pw-link -d" }
        { framework.asSymbol == \jack } { "jack_disconnect" }
        { "LinuxUtils: wrong framework specified".warn; ^nil };

        command = "% SuperCollider:out_%  '%%'".format(plSrv, outNum, clientBaseName, outNum);

        command.unixCmd(action: { c.unhang });
        c.hang;
    }

    doConnection {|conNum|
        var c = Condition.new;
        var command, plSrv;
        "Connecting SuperCollider output num % to % input\n".postf(conNum, clientBaseName);
        conNum = conNum + 1;

        plSrv = case
        { framework.asSymbol == \pipewire } { "pw-link" }
        { framework.asSymbol == \jack } { "jack_connect" }
        { "LinuxUtils: wrong framework specified".warn; ^nil };

        command = "% SuperCollider:out_%  '%%'".format(plSrv, conNum, clientBaseName, conNum);

        command.unixCmd(action: { c.unhang });
        c.hang;
    }

    neededInputExists {
        var conns, plSrv = case
        { framework.asSymbol == \pipewire } {"pw-link -i"}
        { framework.asSymbol == \jack } {"jack_lsp"}
        { "LinuxUtils: wrong framework specified".warn; ^nil };
        conns = plSrv.unixCmdGetStdOutLines;
        ^conns.indexOfEqual(
            "%1".format(clientBaseName)
        ).isNil.not
    }

    pidRunning {
        if(pid.isNil, {
            ^false
        }, { 
            var result = "ps -p %".format(pid).postln.unixCmdGetStdOutLines;
            ^result[1].isNil.not
        })
    }

    startProcess {
        var processName = this.getProcessName();

        pid = "% &".format(processName).unixCmd();

        // if(this.pidRunning.not, {
        // 	// Start process, background it and get the process id (pid) 
        // 	// "% </dev/null &>/dev/null & $!".format(processName).unixCmdGetStdOut();
        // 			}, {
        // 	"% has already been launched...".format(processName).warn
        // })
    }

    getProcessName {
        "getProcessName not implemented".error
    }

    getBaseName {
        "getBaseName not implemented".error
    }

    getMaxClientIns {
        "getMaxClientIns not implemented".error
    }
}

CarlaSingleVST : LinuxUtils {
    var plugName, plugPrefix, plugPath, plugFormat;

    *new{|pluginName, pluginPath, pluginFormat, pluginPrefix="SC_"|
        ^super.new.init(pluginName, pluginPath, pluginFormat, pluginPrefix).init()
    }

    init{|pluginName, pluginPath, pluginFormat, pluginPrefix|

        plugName = pluginName;
        plugPrefix = pluginPrefix;
        plugPath = pluginPath;
        plugFormat = pluginFormat;

        // this.init();
    }

    getMaxClientIns {
        ^2
    }	

    getBaseName {
        ^this.getClientName ++ ":input_"
    }

    getClientName {
        ^plugPrefix ++ plugName
    }

    getProcessName {
        ^"CARLA_CLIENT_NAME=\"%\" carla-single % '%'".format(this.getClientName, plugFormat, plugPath)
    }
}

X42Scope : LinuxUtils {

    *new {
        ^super.new.init()
    }

    getMaxClientIns {
        ^4
    }

    getBaseName {
        ^"Simple Scope (4 channel):in"
    }

    getProcessName {
        ^"x42-scope"
    }
}

X42StereoMeter : LinuxUtils {

    *new {
        ^super.new.init()
    }

    getMaxClientIns {
        ^2
    }	

    getBaseName {
        ^"Stereo/Frequency Scope:in"
    }

    getProcessName {
        ^"x42-meter 16"
    }

}

X42PhaseWheel : LinuxUtils {

    *new {
        ^super.new.init()
    }

    getMaxClientIns {
        ^2
    }	

    getBaseName {
        ^"Phase/Frequency Wheel:in"
    }

    getProcessName {
        ^"x42-meter 14"
    }

}

X42StereoPhase : LinuxUtils {

    *new {
        ^super.new.init()
    }

    getMaxClientIns {
        ^2
    }	

    getBaseName {
        ^"Stereo Phase-Correlation Meter:in"
    }

    getProcessName {
        ^"x42-meter 12"
    }

    doConnection {|conNum|
        var c = Condition.new;
        var command, plSrv;
        var inSuffices = ["L", "R"];
        plSrv = case
        { framework.asSymbol == \pipewire } { "pw-link" }
        { framework.asSymbol == \jack } { "jack_connect" }
        { "LinuxUtils: wrong framework specified".warn; ^nil };
        "Connecting SuperCollider output num % to % input".format(conNum, clientBaseName).postln;
        command = "% SuperCollider:out_%  '%%'".format(plSrv, conNum+1, clientBaseName, inSuffices[conNum]);

        command.unixCmd(action: { c.unhang });
        c.hang;
    }
}

X42GonioMeter : LinuxUtils {

    *new {
        ^super.new.init()
    }

    getMaxClientIns {
        ^2
    }	

    getBaseName {
        ^"Goniometer:in"
    }

    getProcessName {
        ^"x42-meter 13"
    }

    doConnection {|conNum|
        var c = Condition.new;
        var command, plSrv;
        var inSuffices = ["L", "R"];
        plSrv = case
        { framework.asSymbol == \pipewire } { "pw-link" }
        { framework.asSymbol == \jack } { "jack_connect" }
        { "LinuxUtils: wrong framework specified".warn; ^nil };
        "Connecting SuperCollider output num % to % input".format(conNum, clientBaseName).postln;
        command = "% SuperCollider:out_%  '%%'".format(plSrv, conNum+1, clientBaseName, inSuffices[conNum]);

        command.unixCmd(action: { c.unhang });
        c.hang;
    }
}

Japa : LinuxUtils {

    *new {
        ^super.new.init()
    }

    getMaxClientIns {
        ^4
    }	

    getBaseName {
        ^"japa:in_"
    }

    getProcessName {
        ^"japa -J"
    }
}

Jaaa : LinuxUtils {

    *new {
        ^super.new.init()
    }

    getMaxClientIns {
        ^8
    }	

    getBaseName {
        ^"jaaa:in_"
    }

    getProcessName {
        ^"jaaa -J"
    }
}

Yass : LinuxUtils {
    classvar yassChanCount, yassColor;
    *new {|numChans, color|
        yassChanCount=numChans;
        yassColor=color;
        (framework.asSymbol==\pipewire).if{"please ignore the alsa_ connection attempts ...".postln};
        yassColor.isInteger.not.if{"Yass scrolling color is set with integers [0-?]"};
        ^super.new.init()
    }

    getMaxClientIns {
        ^32
    }	

    getBaseName {
        ^"yass:in_"
    }

    connect {
        var neededHardwareOuts = yassChanCount;
        var numConnections = if(neededHardwareOuts >= maxClientInputs, { 
            maxClientInputs
        }, {
            neededHardwareOuts
        });

        fork{
            numConnections.do{|conNum|
                this.doConnection(conNum)
            }
        }
    }

    getProcessName {
        var server = server ? Server.default;
        yassChanCount.isNil.if{
            yassChanCount=server.options.numOutputBusChannels;
            if(yassChanCount>this.getMaxClientIns) {yassChanCount=this.getMaxClientIns};
        };
        ^"yass -n % -c %".format(yassChanCount, yassColor)
    }
}
