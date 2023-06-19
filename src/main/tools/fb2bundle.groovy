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
