#!/bin/bash
//usr/bin/env groovy -classpath .:scripts/groovy "$0" $@; exit $?

package swift.deployment

import static swift.deployment.SwiftYCSB.*
import static swift.deployment.Tools.*
import static swift.deployment.Topology.*;

if (args.length != 5) {
    System.err.println "usage: runycsbresponsetime.groovy <topology configuration file> <workload> <mode> <local_fraction> <outputdir>"
    System.exit(1)
}

topologyDef = new File(args[0])
println "==== Loading topology definition from file " + topologyDef + "===="
evaluate(topologyDef)

SwiftYCSB ycsb = new SwiftYCSB()
// VARs
def workloadName = args[1]
ycsb.baseWorkload = SwiftYCSB.WORKLOADS[workloadName]
def modeName = args[2]
ycsb.mode = SwiftYCSB.MODES[modeName]
// TODO: update for multi-DC setup
if (workloadName.startsWith("workloada")) {
    ycsb.incomingOpPerSecLimit = 400
    ycsb.mode['swift.cacheSize'] = '64'
    ycsb.localRecordCount = 48
} else {
    ycsb.incomingOpPerSecLimit = 4000
    // TODO: increase?
    ycsb.mode['swift.cacheSize'] = '64'
    ycsb.localRecordCount = 48
}
ycsb.localRequestProportion = args[3]
ycsb.threads = 40
def outputDir = args[4]
ycsb.runExperiment(String.format("%s/%s-mode-%s-locality-%s", outputDir, workloadName, modeName, locality))

System.exit(0)

