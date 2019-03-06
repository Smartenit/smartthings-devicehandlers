/****************************************************************************
 * DRIVER NAME:  Smartenit ZBMSKT
 * DESCRIPTION:	 Device handler for Smartenit Metering Socket (Model # 4035A)
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
 
 @Field final ElecMeasCluster = 0x0B04
 @Field final RMSVoltage = 0x0505
 @Field final RMSCurrent = 0x0508
 @Field final ActivePower = 0x050B
 @Field final PowerFactor = 0x0510
 @Field final MeteringCluster = 0x0702
 @Field final MeteringCurrentSummation = 0x0000
 @Field final EnergyDivisor = 1000

metadata {
    definition (name: "Smartenit ZBMSKT", namespace: "smartenit", author: "Smartenit") {
        capability "Actuator"
        capability "Configuration"
        capability "Refresh"
        capability "Power Meter"
        capability "Sensor"
        capability "Switch"
        capability "Health Check"

        fingerprint profileId: "0104", inClusters: "0000, 0003, 0004, 0005, 0006, 0B04, 0702", model: "ZBMSKT1 (4035A)"
    }

    tiles(scale: 2) {
        multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true){
            tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
                attributeState "on", label:'${name}', action:"switch.off", icon:"st.switches.switch.on", backgroundColor:"#79b821", nextState:"turningOff"
                attributeState "off", label:'${name}', action:"switch.on", icon:"st.switches.switch.off", backgroundColor:"#ffffff", nextState:"turningOn"
                attributeState "turningOn", label:'${name}', action:"switch.off", icon:"st.switches.switch.on", backgroundColor:"#79b821", nextState:"turningOff"
                attributeState "turningOff", label:'${name}', action:"switch.on", icon:"st.switches.switch.off", backgroundColor:"#ffffff", nextState:"turningOn"
            }
            tileAttribute ("power", key: "SECONDARY_CONTROL") {
                attributeState "power", label:'${currentValue} W'
            }
        }
        
        valueTile("energy", "device.energy", width: 4, height: 2) {
            state "val", label:'0${currentValue} kWh', /*backgroundColor: "#e86d13",*/ defaultState: true
        }
        
        standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
        }

        main "switch"
        details(["switch", "refresh", "energy"])
    }
}

def getFPoint(String FPointHex){
    return (Float)Long.parseLong(FPointHex, 16)
}

// Parse incoming device messages to generate events
def parse(String description) {
    //log.debug "parse... description: ${description}"    
	def event = zigbee.getEvent(description)
	if (event) {
    	//sendEvent(name: "parse", value: "success: ${event.name}")
        if (event.name == "power") {
            def powerValue
            powerValue = (event.value as Integer)
            sendEvent(name: "power", value: powerValue)
        }
        else {
            sendEvent(event)
        }
    }
    else {
    	def mapDescription = zigbee.parseDescriptionAsMap(description)
        if(mapDescription) {
            if(mapDescription.cluster == "0702") {
            	//sendEvent(name: "parse", value: "cluster: ${mapDescription.cluster}")
                if(mapDescription.attrId == "0000") {
                    return sendEvent(name:"energy", value: getFPoint(mapDescription.value)/EnergyDivisor)
                }
            }
            else if(mapDescription.cluster == "0B04") {
            	//sendEvent(name: "parse", value: "cluster: ${mapDescription.cluster} data: ${mapDescription}")
                if(mapDescription.attrId == "050B") {
                    return sendEvent(name:"power", value: getFPoint(mapDescription.value))
                }
            }
            else if(mapDescription.clusterInt == 6) {
                def attrName = "switch"
                def attrValue = null

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
        }
    }
}

def off() {
    zigbee.off()
}

def on() {
    zigbee.on()
}

def refresh() {
    zigbee.onOffRefresh() + zigbee.readAttribute(MeteringCluster, MeteringCurrentSummation) + zigbee.electricMeasurementPowerRefresh()
}

def configure() {
    log.debug "in configure()"
    Integer reportIntervalMinutes = 5
    return (configureHealthCheck() +
        zigbee.onOffConfig(0,reportIntervalMinutes * 60) + 
        zigbee.electricMeasurementPowerConfig() +
        zigbee.configureReporting(MeteringCluster, MeteringCurrentSummation, 0x25, 0, 600, 50) +
        zigbee.configureReporting(ElecMeasCluster, RMSVoltage, 0x21, 0xFFFF, 0xFFFF,0xFF) +
        zigbee.configureReporting(ElecMeasCluster, RMSCurrent, 0x21, 0xFFFF, 0xFFFF,0xFF) +
        zigbee.configureReporting(ElecMeasCluster, PowerFactor, 0x28, 0xFFFF, 0xFFFF,0xFF)
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