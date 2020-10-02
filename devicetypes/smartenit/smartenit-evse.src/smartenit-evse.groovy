/****************************************************************************
 * DRIVER NAME:  Smartenit EVSE
 * DESCRIPTION:	 Device handler for Smartenit SmartElek EVSE
 * 					
 * $Rev:         $: 1
 * $Author:      $: Dhawal Doshi
 * $Date:	 	 $: 09/20/2020
 * $HeadURL:	 $  https://github.com/Smartenit/smartthings-devicehandlers
 
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
 // EVSE Cluster Doc: https://docs.smartenit.io/display/SMAR/EVSE+Processor+Details 
 
 import groovy.transform.Field
 
 @Field final EVSECluster = 0xFF00
 @Field final MeteringCluster = 0x0702
 @Field final MeteringCurrentSummation = 0x0000
 @Field final MeteringInstantDemand = 0x0400
 @Field final EnergyDivisor = 100000
 @Field final ChargingStatus = 0x0000
 @Field final ChargerSessionDuration = 0x0013
 @Field final ChargerSessionSummation = 0x0015
 @Field final ChargerVRMS = 0x0020
 @Field final ChargerIRMS = 0x0021
 @Field final SmartenitMfrCode = 0x1075
 @Field final StartCharging = 0x00
 @Field final StopCharging = 0x02
 
metadata {
    definition (name: "Smartenit EVSE", namespace: "smartenit", author: "Smartenit") {
        capability "Actuator"
        capability "Configuration"
        capability "Refresh"
        capability "Power Meter"
        capability "Switch"
        capability "Health Check"
		command "stopcharging"
        command "startcharging"
        
        fingerprint profileId: "0104", inClusters: "0000, 0003, FF00, 0702", model: "IOTEVSE-Z"
    }

    tiles(scale: 2) {
        multiAttributeTile(name:"charger", type: "generic", width: 6, height: 4, canChangeIcon: true) {
            tileAttribute ("device.chargerstatus", key: "PRIMARY_CONTROL") {
                attributeState "charging", label:'Charging', action:"stopcharging", icon:"st.Transportation.transportation6", backgroundColor:"#00a0dc"
                attributeState "pluggedin", label:'Plugged In', action:"startcharging", icon:"st.Transportation.transportation6", backgroundColor:"#e86d13"
                attributeState "unplugged", label:'Unplugged', icon:"st.Transportation.transportation6", backgroundColor:"#ffffff"
                attributeState "chargingcompleted", label:'Charging Completed', action:"stopcharging", icon:"st.Transportation.transportation6", backgroundColor:"#e86d13"
                attributeState "fault", label:'Fault', icon:"st.Transportation.transportation6", backgroundColor:"#bc2323"
            }
        }
        
		valueTile("energy", "device.energy", width: 4, height: 2) {
            state "val", label:'${currentValue} kWh', defaultState: true
        }
        
		valueTile("power", "device.power", width: 2, height: 2) {
            state "val", label:'${currentValue} kW', defaultState: true
        }
        
        valueTile("voltage", "device.voltage", width: 2, height: 2) {
            state "val", label:'${currentValue} V', defaultState: true
        }
        
		valueTile("current", "device.current", width: 2, height: 2) {
            state "val", label:'${currentValue} A', defaultState: true
        }
        
		valueTile("sessionduration", "device.sessionduration", width: 4, height: 2) {
            state "val", label:'Session Time: ${currentValue} (hh:mm)', defaultState: true
        }
        
		valueTile("sessionenergy", "device.sessionenergy", width: 4, height: 2) {
            state "val", label:'Session energy: ${currentValue} kWh', defaultState: true
        }
        
        standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "default", label:"refresh", action:"refresh.refresh", icon:"st.secondary.refresh"
        }

        main "charger"
        details(["charger", "refresh", "sessionenergy", "sessionduration", "power", "voltage", "current"])
    }
}

def getFPoint(String FPointHex){
    return (Float)Long.parseLong(FPointHex, 16)
}

// Parse incoming device messages to generate events
def parse(String description) {
    log.debug "parse... description: ${description}"
	def event = zigbee.getEvent(description)
	if (event) {
    	log.debug "event: ${event}, ${event.name}, ${event.value}"
    	if (event.name == "power") {
            sendEvent(name: "power", value: (event.value/EnergyDivisor))
        }
        else {
            sendEvent(event)
        }
    }
    else {
    	def mapDescription = zigbee.parseDescriptionAsMap(description)
        log.debug "mapDescription... : ${mapDescription}"
        if(mapDescription) {
            if(mapDescription.cluster == "0702") {
            	log.debug "simple metering cluster"
            	if(mapDescription.attrId == "0000") {
                	log.debug "energy attr, value: ${mapDescription.value}"
                    return sendEvent(name:"energy", value: getFPoint(mapDescription.value)/EnergyDivisor)
                } else if(mapDescription.attrId == "0400") {
                	log.debug "power attr, value: ${mapDescription.value}"
                    return sendEvent(name:"power", value: getFPoint(mapDescription.value/EnergyDivisor))
                }
            }
            else if(mapDescription.cluster == "FF00") {
            	log.debug "EVSE cluster, attrId: ${mapDescription.attrId}, value: ${mapDescription.value}"
				if(mapDescription.attrId == "0000") {
                	def strvalue = parseChargerStatusValue(mapDescription.value)
                	log.debug "charging status attribute: ${mapDescription.value}, ${strvalue}"
                    if (strvalue == "unplugged") {
                    	sendEvent(name:"sessionduration", value: "--")
                        sendEvent(name:"sessionenergy", value: "--")
                    }
                    return sendEvent(name:"chargerstatus", value: strvalue)
                } else if(mapDescription.attrId == "0013") {
                	int time = (int) Long.parseLong(mapDescription.value, 16);
                    log.debug "ChargerSessionDuration attribute: ${mapDescription.value}, time: ${time}"
                    def hours = Math.round(Math.floor(time / 3600))
                    def secs = time % 3600
                    def mins = Math.round(Math.floor(secs / 60))
                    def timestr = "${hours}:${mins}"
                    return sendEvent(name:"sessionduration", value: timestr)
                } else if(mapDescription.attrId == "0015") {
                	log.debug "ChargerSessionSummation attribute: ${mapDescription.value}"
                    return sendEvent(name:"sessionenergy", value: getFPoint(mapDescription.value)/EnergyDivisor)
                } else if(mapDescription.attrId == "0020") {
                	log.debug "ChargerVRMS attribute: ${mapDescription.value}"
                    return sendEvent(name:"voltage", value: getFPoint(mapDescription.value) / 100)
                } else if(mapDescription.attrId == "0021") {
                	log.debug "ChargerIRMS attribute: ${mapDescription.value}"
                    return sendEvent(name:"current", value: getFPoint(mapDescription.value) / 100)
                } else {
                	log.debug "attribute not handled"
                }
            }
        }
    }
}

def parseChargerStatusValue(val) {
	log.debug "parseChargerStatusValue: ${val}"
	switch(val as Integer) {
    	case 0:
        	log.debug "value is Unplugged"
			return "unplugged"
           	break;
        case 1:
        	log.debug "value is Plugged In"
        	return "pluggedin"
            break;
        case 2:
        	return "pluggedin"
            break;
		case 3:
        	log.debug "value is Charging"
        	return "charging"
            break;
        case 4:
        	log.debug "value is Fault"
        	return "fault"
            break;
        case 5:
        	log.debug "value is Charging Completed"
        	return "chargingcompleted"
            break;
        default:
        	return ""
            break;            
    }
}

def stopcharging() {
	log.debug "sending stopcharging command.."
    zigbee.command(EVSECluster, StopCharging, "", [mfgCode: SmartenitMfrCode])
}

def startcharging() {
	log.debug "sending startcharging command.."
    zigbee.command(EVSECluster, StartCharging, "", [mfgCode: SmartenitMfrCode])
}

def refresh() {
    zigbee.readAttribute(MeteringCluster, MeteringCurrentSummation) +
    zigbee.readAttribute(MeteringCluster, MeteringInstantDemand) +
    zigbee.readAttribute(EVSECluster, ChargingStatus, [mfgCode: SmartenitMfrCode]) +
    zigbee.readAttribute(EVSECluster, ChargerVRMS, [mfgCode: SmartenitMfrCode]) +
    zigbee.readAttribute(EVSECluster, ChargerSessionSummation, [mfgCode: SmartenitMfrCode]) +
    zigbee.readAttribute(EVSECluster, ChargerSessionDuration, [mfgCode: SmartenitMfrCode]) +
    zigbee.readAttribute(EVSECluster, ChargerIRMS, [mfgCode: SmartenitMfrCode])
}

def configure() {
    log.debug "in configure()"
    Integer reportIntervalMinutes = 5
    return (configureHealthCheck() +
        zigbee.configureReporting(MeteringCluster, MeteringCurrentSummation, 0x25, 0, 600, 50) +
        zigbee.configureReporting(MeteringCluster, MeteringInstantDemand, 0x2a, 0, 600, 50) +
        zigbee.configureReporting(EVSECluster, ChargingStatus, 0x30, 0x0, 0x0, null, [mfgCode: SmartenitMfrCode])
        )
}

def configureHealthCheck() {
    Integer hcIntervalMinutes = 10
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
    return zigbee.readAttribute(EVSECluster, ChargingStatus, [mfgCode: SmartenitMfrCode])
}