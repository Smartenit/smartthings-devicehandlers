/****************************************************************************
 * DRIVER NAME: Smartenit ZBLC15 Dual Load Controller
 * DESCRIPTION: Device handler for Smartenit ZBLC15 Dual Load Controller (#4033A)
 * Author:     Dhawal Doshi
 * Revision:   1
 * Date:       07/17/2018
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

 @Field final OnOffCluster = 0x0006
 @Field final OnOffAttr = 0x0000
 @Field final OffCommand = 0x00
 @Field final OnCommand  = 0x01

metadata {
    // Automatically generated. Make future change here.
    definition (name: "Smartenit ZBLC15", namespace: "smartenit", author: "Dhawal Doshi") {
        capability "Switch"
        capability "Configuration"
        capability "Refresh"

        command "onRelay2"
                command "offRelay2"
        
        // indicates that device keeps track of heartbeat (in state.heartbeat)
        attribute "heartbeat", "string"
        attribute "switch2", "ENUM",["on","off"]

        fingerprint profileId: "0104", inClusters: "0000,0003,0006", outClusters: "0019", model: "ZBLC15", deviceJoinName: "Smartenit ZBLC15"
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

        main (["switch", "switch2"])
        details(["switch", "switch2","refresh"])
    }
}

def getFPoint(String FPointHex){
    return (Float)Long.parseLong(FPointHex, 16)
}

/*
* Parse incoming device messages to generate events
*/
def parse(String description) {
    def attrName = null
    def attrValue = null

    log.debug "parse... description: ${description}"
    
    def mapDescription = zigbee.parseDescriptionAsMap(description)
    log.debug "parse... mapDescription: ${mapDescription}"
    
    def event = zigbee.getEvent(description)
    log.debug "parse... event: ${event}"
    
    if(event.name == "switch") {
        if(mapDescription.sourceEndpoint == "01") {
            attrName = "switch"
        }else if(mapDescription.sourceEndpoint == "02") {
            attrName = "switch2"
        }else{
            return
        }
        log.debug "parsed in event: attrName: ${attrName}....attrValue: ${event.value}"
        return sendEvent(name: attrName, value: event.value)
    }
    else if(mapDescription.clusterInt == 6)
    {
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
    else
    {
        if(description.contains("on/off")) {
            log.debug "must be a report, but don't know which Endpoint"
        }else {
            log.warn "Did not parse message: $description"
        }
    }

    return createEvent([:])
}

def off() {
    log.info 'turn Off'
    zigbee.off()
}

def on() {
    log.info 'turn On'
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
    if(!state.configured == 1) {
        log.warn "Device not configured, configuring now.."
        return configure()
    }
    sendEvent(name: "heartbeat", value: "alive", displayed:false)
    return (
        zigbee.onOffRefresh() + 
        zigbee.readAttribute(OnOffCluster, OnOffAttr, [destEndpoint:Endpoint2])
    )
}

def configure() {
    log.debug "Configuring Reporting and Bindings."
    runEvery15Minutes(refresh)
    state.configured = 1
    def configCmds = [
        "zdo bind 0x${device.deviceNetworkId} ${Endpoint2} 0x01 ${OnOffCluster} {${device.zigbeeId}} {}"
    ]
    return  + configCmds + zigbee.onOffConfig()
}