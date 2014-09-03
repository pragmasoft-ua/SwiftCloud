#!/bin/bash
//usr/bin/env groovy -classpath .:scripts/groovy "$0" $@; exit $?

package swift.deployment
import static swift.deployment.Topology.*;

// NODES
EuropeEC2 = [
    // DC only
    'ec2-54-72-3-142.eu-west-1.compute.amazonaws.com',
'ec2-54-76-94-5.eu-west-1.compute.amazonaws.com',
'ec2-54-77-200-3.eu-west-1.compute.amazonaws.com',
]

NVirginiaEC2 = [
    // first node is a DC, followed by two groups of 10 scouts
'ec2-54-165-22-172.compute-1.amazonaws.com',

'ec2-54-165-23-103.compute-1.amazonaws.com',
'ec2-54-165-26-186.compute-1.amazonaws.com',
'ec2-54-165-28-56.compute-1.amazonaws.com',
'ec2-54-165-22-141.compute-1.amazonaws.com',
'ec2-54-165-29-38.compute-1.amazonaws.com',
'ec2-54-165-22-147.compute-1.amazonaws.com',
'ec2-54-165-23-2.compute-1.amazonaws.com',
'ec2-54-165-29-154.compute-1.amazonaws.com',
'ec2-54-165-22-4.compute-1.amazonaws.com',
'ec2-54-165-28-209.compute-1.amazonaws.com',

'ec2-54-165-22-168.compute-1.amazonaws.com',
'ec2-54-165-26-255.compute-1.amazonaws.com',
'ec2-54-165-25-232.compute-1.amazonaws.com',
'ec2-54-165-22-125.compute-1.amazonaws.com',
'ec2-54-165-29-12.compute-1.amazonaws.com',
'ec2-54-165-29-33.compute-1.amazonaws.com',
'ec2-54-165-27-40.compute-1.amazonaws.com',
'ec2-54-165-30-105.compute-1.amazonaws.com',
'ec2-54-165-26-63.compute-1.amazonaws.com',
'ec2-54-165-31-209.compute-1.amazonaws.com',

'ec2-54-165-83-13.compute-1.amazonaws.com',
'ec2-54-165-86-237.compute-1.amazonaws.com',
]

OregonEC2 = [
    // first node is a DC, followed by 10 scouts
    'ec2-54-187-132-242.us-west-2.compute.amazonaws.com',

    'ec2-54-213-143-202.us-west-2.compute.amazonaws.com',
    'ec2-54-218-50-173.us-west-2.compute.amazonaws.com',
    'ec2-54-218-56-145.us-west-2.compute.amazonaws.com',
    'ec2-54-218-60-104.us-west-2.compute.amazonaws.com',
    'ec2-54-218-61-19.us-west-2.compute.amazonaws.com',
    'ec2-54-218-61-75.us-west-2.compute.amazonaws.com',
    'ec2-54-218-62-162.us-west-2.compute.amazonaws.com',
    'ec2-54-218-64-244.us-west-2.compute.amazonaws.com',
    'ec2-54-218-67-211.us-west-2.compute.amazonaws.com',
    'ec2-54-218-68-40.us-west-2.compute.amazonaws.com',

'ec2-54-68-104-0.us-west-2.compute.amazonaws.com',
'ec2-54-68-105-152.us-west-2.compute.amazonaws.com',
]

// TOPOLOGY
Topology.clear()

// DC_NV as a first DC - used to initialize the DB 
DC_NV = DC([NVirginiaEC2[0]], [NVirginiaEC2[0]])
DC_EU = DC([EuropeEC2[0]], [EuropeEC2[0]])
DC_OR = DC([OregonEC2[0]], [OregonEC2[0]])
DC_NV2 = DC([NVirginiaEC2[21]], [NVirginiaEC2[21]])
DC_EU2 = DC([EuropeEC2[1]], [EuropeEC2[1]])
DC_OR2 = DC([OregonEC2[11]], [OregonEC2[11]])

ScoutsToEU = SGroup(NVirginiaEC2[1..5], DC_EU)
ScoutsToEU2 = SGroup(NVirginiaEC2[6..10], DC_EU2)
ScoutsToNV = SGroup(OregonEC2[1..5], DC_NV)
ScoutsToNV2 = SGroup(OregonEC2[6..10], DC_NV2)
ScoutsToOR = SGroup(NVirginiaEC2[11..15], DC_OR)
ScoutsToOR2 = SGroup(NVirginiaEC2[16..20], DC_OR2)

