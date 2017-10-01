#!/usr/bin/env groovy

def spotbugsHome = System.getenv("SPOTBUGS_HOME")
def antBuilder = new AntBuilder()

def cli = new CliBuilder(usage:'fb2repo -f spotbugs.home -version version -u repositoryURL')
cli.h(longOpt: 'help', 'usage information')
cli.f(argName: 'home',  longOpt: 'home', required: false, args: 1, type:GString, 'Spotbugs home directory')
cli.v(argName: 'version',  longOpt: 'version', required: true, args: 1, type:GString, 'Spotbugs version')
cli.u(argName: 'url',  longOpt: 'url', required: true, args: 1, type:GString, 'Repository URL')

def opt = cli.parse(args)
if (!opt) { return }
if (opt.h) opt.usage()
if (opt.f) spotbugsHome = opt.f
def spotbugsVersion = opt.v
def repoUrl = opt.u

println "spotbugsHome is ${spotbugsHome}"
println "spotbugsVersion is ${spotbugsVersion}"
println "Done parsing"

def cmdPrefix = """"""

println "os.name is " + System.getProperty("os.name")

if (System.getProperty("os.name").toLowerCase().contains("windows")) cmdPrefix = """cmd /c """

def modules = ["annotations", "bcel", "spotbugs", "spotbugs-ant", "jFormatString", "jsr305" ]

modules.each(){ module ->
    antBuilder.copy(file: new File("${module}.pom"), toFile: new File("${module}.xml"), overwrite: true ) {
        filterset() {
            filter(token: "spotbugs.version", value: "${spotbugsVersion}")
        }
    }

    cmd = cmdPrefix + """mvn deploy:deploy-file -DpomFile=${module}.xml -Dfile=${spotbugsHome}/lib/${module}.jar -DgroupId=com.github.spotbugs -DartifactId=${module} -Dversion=${spotbugsVersion} -Durl=${repoUrl} -Dpackaging=jar"""
    proc = cmd.execute()
    println proc.text
    antBuilder.delete(file: "pom.xml")
}

