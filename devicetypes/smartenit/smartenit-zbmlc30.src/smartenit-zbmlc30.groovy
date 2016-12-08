/****************************************************************************
 * DRIVER NAME:  Smartenit ZBMLC30
 * DESCRIPTION:	 Device handler for Smartenit Metering Dual Load Controller (Model 4040B)
 * 					
 * $Rev:         $: 1
 * $Author:      $: Dhawal Doshi
 * $Date:	 	 $: 12/06/2016
 * $HeadURL:	 $  https://github.com/thewall7/st-devicehandlers
 ****************************************************************************
 * This software is owned by Compacta and/or its supplier and is protected
 * under applicable copyright laws. All rights are reserved. We grant You,
 * and any third parties, a license to use this software solely and
 * exclusively on Compacta products. You, and any third parties must reproduce
 * the copyright and warranty notice and any other legend of ownership on each
 * copy or partial copy of the software.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS". COMPACTA MAKES NO WARRANTIES, WHETHER
 * EXPRESS, IMPLIED OR STATUTORY, INCLUDING, BUT NOT LIMITED TO, IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE,
 * ACCURACY OR LACK OF NEGLIGENCE. COMPACTA SHALL NOT, UNDERN ANY CIRCUMSTANCES,
 * BE LIABLE FOR ANY DAMAGES, INCLUDING, BUT NOT LIMITED TO, SPECIAL,
 * INCIDENTAL OR CONSEQUENTIAL DAMAGES FOR ANY REASON WHATSOEVER.
 *
 * Copyright Compacta International, Ltd 2016. All rights reserved
 ****************************************************************************/

 import groovy.transform.Field

 @Field final Endpoint2 = 0x02
 @Field final MeteringCluster = 0x0702
 @Field final MeteringInstantDemand = 0x0400
 @Field final MeteringCurrentSummation = 0x0000
 @Field final PowerDivisor = 100
 @Field final EnergyDivisor = 10000
 
metadata {
	// Automatically generated. Make future change here.
	definition (name: "Smartenit ZBMLC30", namespace: "smartenit", author: "Smartenit") {
		capability "Switch"
		capability "Power Meter"
		capability "Configuration"
		capability "Refresh"
		capability "Sensor"
		capability "Energy Meter"
		capability "Actuator"
        capability "Health Check"

		// indicates that device keeps track of heartbeat (in state.heartbeat)
		attribute "heartbeat", "string"

		fingerprint profileId: "0104", inClusters: "0000,0003,0004,0005,0006,0702", model: "ZBMLC30", deviceJoinName: "Smartenit Dual Load Controller"
		fingerprint profileId: "0104", inClusters: "0000,0003,0004,0005,0006,0702", model: "ZBMLC30-1", deviceJoinName: "Smartenit Dual Load Controller"
}

	// simulator metadata
	simulator {
		// status messages
		status "on": "on/off: 1"
		status "off": "on/off: 0"

		// reply messages
		reply "zcl on-off on": "on/off: 1"
		reply "zcl on-off off": "on/off: 0"
	}

	preferences {
		section {
			image(name: 'educationalcontent', multiple: true, images: [
				"http://cdn.device-gse.smartthings.com/Outlet/US/OutletUS1.jpg",
				"http://cdn.device-gse.smartthings.com/Outlet/US/OutletUS2.jpg"
				])
		}
	}

	// UI tile definitions
	tiles(scale: 2) {
		multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true){
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on", label: '${name}', action: "switch.off", icon: "st.switches.switch.on", backgroundColor: "#79b821", nextState: "turningOff"
				attributeState "off", label: '${name}', action: "switch.on", icon: "st.switches.switch.off", backgroundColor: "#ffffff", nextState: "turningOn"
				attributeState "turningOn", label: '${name}', action: "switch.off", icon: "st.switches.switch.on", backgroundColor: "#79b821", nextState: "turningOff"
				attributeState "turningOff", label: '${name}', action: "switch.on", icon: "st.switches.switch.off", backgroundColor: "#ffffff", nextState: "turningOn"
			}
		}

		valueTile("power", "device.power", width: 4, height: 2) {
		        state "val", label:'${currentValue} W', /*backgroundColor: "#e86d13",*/ defaultState: true
		    }

		standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
		}

		valueTile("energy", "device.energy", width: 6, height: 2) {
		        state "val", label:'0${currentValue} kWh', /*backgroundColor: "#e86d13",*/ defaultState: true
		    }

		main "switch"
		details(["switch","power","refresh","energy"])
	}
}

def getFPoint(String FPointHex){
    return (Float)Long.parseLong(FPointHex, 16)
}

/*
* Parse incoming device messages to generate events
*/
def parse(String description) {
	log.debug "parse... description: ${description}"
    
	def event = zigbee.getEvent(description)
	if ((event) && (event.name == "switch"))
    {
        sendEvent(event)
    }
    else 
    {
        log.warn "DID NOT PARSE MESSAGE for description : $description"
        def mapDescription = zigbee.parseDescriptionAsMap(description)

        if(mapDescription.cluster == "0702")
        {
            if(mapDescription.attrId == "0400")
            {
                return sendEvent(name:"power", value: getFPoint(mapDescription.value)/100.0)
            }
            else
            {
                return sendEvent(name:"energy", value: getFPoint(mapDescription.value)/10000.0)
            }
        }
        else if(mapDescription.clusterInt == 6)
        {
            def attrName = "switch"
            def attrValue = null

            //sendEvent(name: "parseSwitch", value: mapDescription)

			if(mapDescription.command == "0B") {
                if(mapDescription.data[0] == "00") {
                    attrValue = "off"
                }else if(mapDescription.data[0] == "01") {
                    attrValue = "on"
                }else{
                	return
                }
            }else {
                if(mapDescription.value == "00") {
                    attrValue = "off"
                }else if(mapDescription.value == "01") {
                    attrValue = "on"
                }else{
                	return
                }
            }

            //sendEvent(name: attrName, value: attrValue)

            def result = createEvent(name: attrName, value: attrValue)
            return result
        }
        /*else
        {
            def result = zigbee.getEvent(description);
        }*/

        return createEvent([:])
    }

}

def off() {
	//log.info 'turn Off'
	zigbee.off()
}

def on() {
	//log.info 'turn On'
	zigbee.on()
}

def refresh() {
    Integer reportIntervalMinutes = 5
    return (
    	zigbee.readAttribute(MeteringCluster, MeteringCurrentSummation, [destEndpoint:Endpoint2]) + 
    	zigbee.readAttribute(MeteringCluster, MeteringInstantDemand, [destEndpoint:Endpoint2]) + 
    	zigbee.onOffRefresh() 
    )
}

def configure() {
	//log.debug "in configure()"
    Integer reportIntervalMinutes = 5
	def configCmds = ["zdo bind 0x${device.deviceNetworkId} 0x02 0x01 0x0702 {${device.zigbeeId}} {}"]
    return  (
    	configCmds + 
    	zigbee.configureReporting(MeteringCluster, MeteringCurrentSummation, 0x25, 0, 600, 50, [destEndpoint:Endpoint2]) + 
    	zigbee.configureReporting(MeteringCluster, MeteringInstantDemand, 0x2A, 0, 600, 10, [destEndpoint:Endpoint2]) +
    	zigbee.onOffConfig(0,reportIntervalMinutes * 60) + 
    	zigbee.onOffRefresh() + 
    	configureHealthCheck()
    	)
}

def configureHealthCheck() {
    Integer hcIntervalMinutes = 12
    sendEvent(name: "checkInterval", value: hcIntervalMinutes * 60, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID])
    return refresh()
}

def updated() {
    log.debug "in updated()"
    // updated() doesn't have it's return value processed as hub commands, so we have to send them explicitly
    def cmds = configureHealthCheck()
    cmds.each{ sendHubCommand(new physicalgraph.device.HubAction(it)) }
}

def ping() {
    return zigbee.onOffRefresh()
}