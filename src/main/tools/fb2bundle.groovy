#!/usr/bin/env groovy

def cli = new CliBuilder(usage:'fb2bundle -f spotbugs.home -version version')
cli.h(longOpt: 'help', 'usage information')
cli.v(argName: 'version',  longOpt: 'version', required: true, args: 1, type:GString, 'Spotbugs version')

def opt = cli.parse(args)
if (!opt) { return }
if (opt.h) opt.usage()
def spotbugsVersion = opt.v

println "spotbugsVersion is ${spotbugsVersion}"
println "Done parsing"

def cmdPrefix = """"""

println "os.name is " + System.getProperty("os.name")

if (System.getProperty("os.name").toLowerCase().contains("windows")) cmdPrefix = """cmd /c """

def modules = ["spotbugs-annotations", "spotbugs", "spotbugs-ant", "jFormatString", "jsr305" ]

modules.each(){ module ->
    println "Processing ${module}........"
    cmd = cmdPrefix + """mvn repository:bundle-pack -B -DgroupId=com.github.spotbugs -DartifactId=${module} -Dversion=${spotbugsVersion}"""
    proc = cmd.execute()
    println proc.text
}

