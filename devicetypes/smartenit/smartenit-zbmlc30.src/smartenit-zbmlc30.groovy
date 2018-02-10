/****************************************************************************
 * DRIVER NAME:	Smartenit Metering Dual Load Controller
 * DESCRIPTION:	Device handler for Smartenit Metering Dual Load Controller (#4040B)
 * 					
 * $Rev:          $: 2
 * $Author:       $: Dhawal Doshi
 * $Date:	  $: 02/09/2018
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
 /* This device handler should be able to show Metering values and control both Load 1 and 2.
 * Please Note: Currently the Load 2 will not show under a Smart App which is default by ST. 
 * Please email feedback to support@smartenit.com
 */

 import groovy.transform.Field

 @Field final Endpoint2 = 0x02
 def MeteringEP = 0x02
 @Field final MeteringCluster = 0x0702
 @Field final MeteringInstantDemand = 0x0400
 @Field final MeteringInstantDemandDivisor = 0x0304
 @Field final MeteringCurrentSummation = 0x0000

 @Field final OnOffCluster = 0x0006
 @Field final OnOffAttr = 0x0000
 @Field final OffCommand = 0x0000
 @Field final OnCommand  = 0x0001

 @Field final BasicCluster = 0x0000
 @Field final ModelIdAttr = 0x0005
 def ModelName = ""
 
metadata {
	// Automatically generated. Make future change here.
	definition (name: "Smartenit ZBMLC30 Both Loads", namespace: "smartenit", author: "Dhawal Doshi") {
		capability "Switch"
		capability "Power Meter"
		capability "Configuration"
		capability "Refresh"
		capability "Sensor"
		capability "Energy Meter"

		command "onRelay2"
               command "offRelay2"
        
		// indicates that device keeps track of heartbeat (in state.heartbeat)
		attribute "heartbeat", "string"
		attribute "switch2", "ENUM",["on","off"]

		fingerprint profileId: "0104", inClusters: "0000,0003,0004,0005,0006,0702", outClusters: "0019", model: "ZBMLC30", deviceJoinName: "Smartenit Metering Dual Load Controller"
		fingerprint profileId: "0104", inClusters: "0000,0003,0006,0702", outClusters: "0019", model: "ZBMLC30-1", deviceJoinName: "Smartenit Metering Dual Load Controller"
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
	tiles(scale: 1) {
        standardTile("switch", "device.switch", width: 1, height: 1, canChangeIcon: true) {
            state "off", label: '${name}', action: "switch.on", icon: "st.switches.switch.off", backgroundColor: "#ffffff", nextState: "turningOn"
            state "on", label: '${name}', action: "switch.off", icon: "st.switches.switch.on", backgroundColor: "#79b821", nextState: "turningOff"
            state "turningOn", label: '${name}', action: "switch.off", icon: "st.switches.switch.on", backgroundColor: "#79b821", nextState: "turningOff"
            state "turningOff", label: '${name}', action: "switch.on", icon: "st.switches.switch.off", backgroundColor: "#ffffff", nextState: "turningOn"
        }

        standardTile("switch2", "device.switch2", width: 1, height: 1, canChangeIcon: true) {
            state "off", label: '${name}', action: "onRelay2", icon: "st.switches.switch.off", backgroundColor: "#ffffff", nextState: "turningOn"
            state "on", label: '${name}', action: "offRelay2", icon: "st.switches.switch.on", backgroundColor: "#79b821", nextState: "turningOff"
            state "turningOn", label: '${name}', action: "offRelay2", icon: "st.switches.switch.on", backgroundColor: "#79b821", nextState: "turningOff"
            state "turningOff", label: '${name}', action: "onRelay2", icon: "st.switches.switch.off", backgroundColor: "#ffffff", nextState: "turningOn"
        }
        
		standardTile("refresh", "device.power", inactiveLabel: false, decoration: "flat", width: 1, height: 1) {
			state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
		}

		valueTile("power", "device.power", width: 3, height: 1) {
            state "val", label:'${currentValue} Watts', defaultState: true
        }

		valueTile("energy", "device.energy", width: 3, height: 1) {
            state "val", label:'0${currentValue} kWh', defaultState: true
        }
        
		main (["switch", "switch2"])
		details(["switch", "switch2","refresh","power","energy"])
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
    def mapDescription = zigbee.parseDescriptionAsMap(description)

    if(mapDescription.cluster == "0702")
    {
        if(mapDescription.attrId == "0400")
        {
            return sendEvent(name:"power", value: getFPoint(mapDescription.value)/100.0)
        }
        else if(mapDescription.attrId == "0000")
        {
            return sendEvent(name:"energy", value: getFPoint(mapDescription.value)/10000.0)
        }
    }
    else if(mapDescription.clusterInt == 6)
    {
        def attrName = null
        def attrValue = null

        sendEvent(name: "parseSwitch", value: mapDescription)

        if(mapDescription.sourceEndpoint == "01") {
            attrName = "switch"
        }else if(mapDescription.sourceEndpoint == "02") {
            attrName = "switch2"
        }else{
            return
        }

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

        sendEvent(name: attrName, value: attrValue)

        def result = createEvent(name: attrName, value: attrValue)
        return result
    }
    else if(mapDescription.clusterInt == 0)
    {
        def attrName = null
        def attrValue = null

        sendEvent(name: "parseBasic", value: mapDescription)
        if(mapDescription.attrId == "0005")
        {
            attrName = "ModelId"
            attrValue = mapDescription.value.toString()
            ModelName = mapDescription.value
            log.debug "ModelName attr received: ${ModelName}"
            if (ModelName == "5A424D4C4333302D31") 
            {
                MeteringEP = 0x01
            }
        }
        sendEvent(name: attrName, value: attrValue)

        def result = createEvent(name: attrName, value: attrValue)
        return result
    }
    else
    {
        log.warn "Did not parse message for description : $description"
        def result = zigbee.getEvent(description);
    }

    return createEvent([:])
}

def off() {
	log.info 'Turning Off Relay 1'
	zigbee.off()
}

def on() {
	log.info 'Turning On Relay 1'
	zigbee.on()
}

def onRelay2(){
	log.debug "Turning On Relay2"
    //zigbee.command(OnOffCluster, OnCommand, additionalParams=[destEndpoint:Endpoint2])
    def cmds = []
    cmds << "st cmd 0x${device.deviceNetworkId} ${Endpoint2} ${OnOffCluster} ${OnCommand} {}"
	cmds
}

def offRelay2(){
	log.debug "Turning Off Relay2"
	//zigbee.command(OnOffCluster, OffCommand, additionalParams=[destEndpoint:Endpoint2])
    def cmds = []
    cmds << "st cmd 0x${device.deviceNetworkId} ${Endpoint2} ${OnOffCluster} ${OffCommand} {}"
	cmds
}

def refresh() {
	log.debug "Device Model: ${ModelName}"
    if (ModelName == "5A424D4C4333302D31") 
    {
    	MeteringEP = 0x01
    }
    
	sendEvent(name: "heartbeat", value: "alive", displayed:false)
    
    return (
    	zigbee.onOffRefresh() + 
        zigbee.readAttribute(OnOffCluster, OnOffAttr, [destEndpoint:Endpoint2]) +
    	zigbee.readAttribute(MeteringCluster, MeteringCurrentSummation, [destEndpoint:MeteringEP]) +
    	zigbee.readAttribute(MeteringCluster, MeteringInstantDemand, [destEndpoint:MeteringEP])
    )
}

def configure() {
    log.debug "Configuring Reporting and Bindings."
    if (ModelName == "5A424D4C4333302D31") 
    {
    	MeteringEP = 0x01
    }
    
    def retrieveModel = [
    	zigbee.readAttribute(BasicCluster, ModelIdAttr)
    ]
    
	def configCmds = [
        "zdo bind 0x${device.deviceNetworkId} ${MeteringEP} 0x01 ${MeteringCluster} {${device.zigbeeId}} {}",
        "zdo bind 0x${device.deviceNetworkId} ${Endpoint2} 0x01 ${OnOffCluster} {${device.zigbeeId}} {}"
    ]

	return  retrieveModel + configCmds + zigbee.onOffConfig()
}
