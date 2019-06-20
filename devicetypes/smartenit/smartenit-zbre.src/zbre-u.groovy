/****************************************************************************
 * DRIVER NAME:		ZBRE (Smartenit Zigbee Range Extender)
 * DESCRIPTION:		Device handler for ZBRE
 * 					
 * $Rev:            $: 1
 * $Author:         $: Dhawal Doshi
 * $Date:			$: 6/19/2019
 * $HeadURL:		$
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
 @Field final BasicCluster = 0x0000
 @Field final ApplicationVersionAttr = 0x0001
 
metadata {
    definition (name: "Zigbee Repeater", namespace: "smartenit", author: "Dhawal Doshi") {
    	capability "Refresh"
        capability "Health Check"
		fingerprint profileId: "0104", deviceId: "0008", inClusters: "0000 0003", outClusters: "0019", manufacturer: "Smartenit, Inc", model: "ZB3RE", deviceJoinName: "Smartenit Zigbee Repeater"
	}
    // Contributors

    // simulator metadata
    simulator {
    }

    // UI tile definitions
	tiles(scale: 1) {
		standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 1, height: 1) {
			state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
		}
        valueTile("online", "device.online", width: 2, height: 1) {
            state "val", label:'${currentValue}', defaultState: "---"
        }
        
        details(["online","refresh"])
	}
}

// Parse incoming device messages to generate events
def parse(String description) {
    log.debug "Parse description $description"
    def mapDescription = zigbee.parseDescriptionAsMap(description)
    if((mapDescription.cluster == "0000") && (mapDescription.result == "success")) {
        return sendEvent(name:"online", value: "Online")
    }
	
    return createEvent([:])
}

/**
 * PING is used by Device-Watch in attempt to reach the Device
 * */
def ping() {
	return refresh()
}

def refresh() {
	zigbee.readAttribute(BasicCluster, ApplicationVersionAttr)
}

def configure() {
	// Device-Watch allows 2 check-in misses from device + ping (plus 2 min lag time)
	sendEvent(name: "checkInterval", value: 2 * 10 * 60 + 2 * 60, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID])
}