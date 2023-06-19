/*
 * Copyright 2005-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
#!groovy

def spotbugsHome = System.getenv("SPOTBUGS_HOME")
def antBuilder = new AntBuilder()

def cli = new CliBuilder(usage:'fb2local -f spotbugs.home -version version')
cli.h(longOpt: 'help', 'usage information')
cli.f(argName: 'home',  longOpt: 'home', required: false, args: 1, type:GString, 'Spotbugs home directory')
cli.v(argName: 'version',  longOpt: 'version', required: true, args: 1, type:GString, 'Spotbugs version')

def opt = cli.parse(args)
if (!opt) { return }
if (opt.h) opt.usage()
if (opt.f) spotbugsHome = opt.f
def spotbugsVersion = opt.v

println "spotbugsHome is ${spotbugsHome}"
println "spotbugsVersion is ${spotbugsVersion}"
println "Done parsing"

def cmdPrefix = """"""

println "os.name is " + System.getProperty("os.name")

if (System.getProperty("os.name").toLowerCase().contains("windows")) cmdPrefix = """cmd /c """

def modules = ["spotbugs-annotations", "spotbugs", "spotbugs-ant", "jFormatString", "jsr305" ]

modules.each(){ module ->
  antBuilder.copy(file: new File("${module}.pom"), toFile: new File("${module}.xml"), overwrite: true ) {
    filterset() {
      filter(token: "spotbugs.version", value: "${spotbugsVersion}")
    }
  }

  cmd = cmdPrefix + """mvn install:install-file -DpomFile=${module}.xml -Dfile=${spotbugsHome}/lib/${module}.jar -DgroupId=com.github.spotbugs -DartifactId=${module} -Dversion=${spotbugsVersion} -Dpackaging=jar"""
  proc = cmd.execute()
  println proc.text
}

antBuilder.delete(file: "pom.xml")
