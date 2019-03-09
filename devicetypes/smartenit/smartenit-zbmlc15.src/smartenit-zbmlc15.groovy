/****************************************************************************
 * DRIVER NAME:  Smartenit ZBMLC15
 * DESCRIPTION:	 Device handler for Smartenit Metering Single Load Controller (#4034A)
 * 				 https://smartenit.com/product/zbmlc15/
 * $Rev:         $: 3
 * $Author:      $: Dhawal Doshi
 * $Date:	 	 $: 07/13/2018
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

 @Field final OnoffCluster = 0x0006
 
 @Field final MeteringEP = 0x0A
 @Field final MeteringCluster = 0x0702
 @Field final MetSummDataType = 0x25
 @Field final MetDemandDataType = 0x2A
 @Field final MeteringDemandAttrID = 0x0400
 @Field final MeteringSummAttrID = 0x0000
 @Field final MeteringDivisor = 1000

 @Field final SumMaxReportTimeSecs = 0
 @Field final SumMinReportTimeSecs = 0
 @Field final DemMaxReportTimeSecs = 0
 @Field final DemMinReportTimeSecs = 0
 @Field final MeteringReportableChange = 1	//watt-hour and watts

 @Field final ReportIntervalsecs = 300	// 5 mins
 @Field final HealthCheckSecs = 600	// 5 mins
 
metadata {
	// Automatically generated. Make future change here.
	definition (name: "Smartenit ZBMLC15", namespace: "smartenit", author: "Smartenit") {
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

		fingerprint profileId: "0104", inClusters: "0000,0003,0006,0702", model: "ZBMLC15", deviceJoinName: "Smartenit ZBMLC15"
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
				attributeState "on", label: '${name}', action: "switch.off", icon: "st.switches.switch.on", backgroundColor: "#00a0dc", nextState: "turningOff"
				attributeState "off", label: '${name}', action: "switch.on", icon: "st.switches.switch.off", backgroundColor: "#ffffff", nextState: "turningOn"
				attributeState "turningOn", label: '${name}', action: "switch.off", icon: "st.switches.switch.on", backgroundColor: "#00a0dc", nextState: "turningOff"
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
	//log.debug "parse... description: ${description}"    
	def event = zigbee.getEvent(description)
	if ((event) && (event.name == "switch")) {
        sendEvent(event)
    }
    else  {
        def mapDescription = zigbee.parseDescriptionAsMap(description)
		// log.debug "parse... mapDescription: ${mapDescription}"
        if(mapDescription.clusterInt == MeteringCluster) {
        	
            if(mapDescription.attrId == "0400") {
            	log.debug "Received Power value: ${mapDescription.value}"
                sendEvent(name:"power", value: getFPoint(mapDescription.value))
            }
            else if(mapDescription.attrId == "0000") {
            	log.debug "Received Energy value: ${mapDescription.value}"
                sendEvent(name:"energy", value: getFPoint(mapDescription.value)/MeteringDivisor)
            }
            
            if (mapDescription.additionalAttrs) {
            	log.debug "Additional attrs found"
                if(mapDescription.additionalAttrs[0].attrId == "0400") {
            		log.debug "Received Power value: ${mapDescription.additionalAttrs[0].value}"
                	sendEvent(name:"power", value: getFPoint(mapDescription.additionalAttrs[0].value))
            	}
            	else if(mapDescription.additionalAttrs[0].attrId == "0000") {
            		log.debug "Received Energy value: ${mapDescription.additionalAttrs[0].value}"
                	sendEvent(name:"energy", value: getFPoint(mapDescription.additionalAttrs[0].value)/MeteringDivisor)
           	 	}
            }
        }
        else if(mapDescription.clusterInt == OnoffCluster) {
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
            return sendEvent(name: attrName, value: attrValue)
        }
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
	if (state.configured != 1) {
    	return configure()
	}
    
    return (
    	zigbee.readAttribute(MeteringCluster, MeteringSummAttrID, [destEndpoint:MeteringEP]) + 
    	zigbee.readAttribute(MeteringCluster, MeteringDemandAttrID, [destEndpoint:MeteringEP]) + 
    	zigbee.onOffRefresh() 
    )
}

def configure() {
	log.debug "in configure()"
    state.configured = 1
    
    //configureHealthCheck()
    
	def meterconfigCmds = ["zdo bind 0x${device.deviceNetworkId} MeteringEP 0x01 MeteringCluster {${device.zigbeeId}} {}"]
    def onoffconfigCmds = ["zdo bind 0x${device.deviceNetworkId} MeteringEP 0x01 OnoffCluster {${device.zigbeeId}} {}"]
    return  (
    	meterconfigCmds + 
        onoffconfigCmds +
    	zigbee.configureReporting(MeteringCluster, MeteringSummAttrID, MetSummDataType, SumMinReportTimeSecs, SumMaxReportTimeSecs, MeteringReportableChange, [destEndpoint:MeteringEP]) + 
    	zigbee.configureReporting(MeteringCluster, MeteringDemandAttrID, MetDemandDataType, DemMinReportTimeSecs, DemMaxReportTimeSecs, MeteringReportableChange, [destEndpoint:MeteringEP]) +
        zigbee.configureReporting(OnoffCluster, 0x0000, 0x10, 0, 0, 0x01, [destEndpoint:MeteringEP])
    	)
}

def configureHealthCheck() {
    sendEvent(name: "checkInterval", value: HealthCheckSecs, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID])
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