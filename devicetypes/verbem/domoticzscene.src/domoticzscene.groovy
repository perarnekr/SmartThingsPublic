/**
 *  Domoticz Scene SubType Switch.
 *
 *  SmartDevice type for domoticz scenes and groups
 *  
 *
 *  Copyright (c) 2016 Martin Verbeek
 *
 *  This program is free software: you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation, either version 3 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *  Revision History
 *  ----------------
 *  2.1 2016-11-21 Rework of setColor 
 *	2.2 2016-12-01 added calibration of the closing time, now you can use setlevel or ask alexa to dim to a percentage
 *	3.0 2016-12-24 cleanup of DTH statuses
 *	3.1 2016-12-24 Implement PUSH as the button for a Scene
 */

metadata {
    definition (name:"domoticzScene", namespace:"verbem", author:"Martin Verbeek") {
        capability "Actuator"
        capability "Sensor"
        capability "Switch"
        capability "Refresh"
        capability "Polling"
        
        // custom commands
        command "parse"     // (String "<attribute>:<value>[,<attribute>:<value>]")
    }

    tiles(scale:2) {
    	multiAttributeTile(name:"richDomoticzOnOff", type:"lighting",  width:6, height:4, canChangeIcon: true, canChangeBackground: true) {
        	tileAttribute("device.switch", key: "PRIMARY_CONTROL") {
                attributeState "off", label:'Off', icon:"st.lights.philips.hue-single", backgroundColor:"#ffffff", action:"on", nextState:"Turning On"
                attributeState "Off", label:'Off', icon:"st.lights.philips.hue-single", backgroundColor:"#ffffff", action:"on", nextState:"Turning On"
                attributeState "OFF", label:'Off',icon:"st.lights.philips.hue-single", backgroundColor:"#ffffff", action:"on", nextState:"Turning On"
                attributeState "Turning Off", label:'Turning Off', icon:"st.lights.philips.hue-single", backgroundColor:"#FE9A2E", nextState:"Turning On"
                
                attributeState "on", label:'On', icon:"st.lights.philips.hue-single", backgroundColor:"#79b821", action:"off", nextState:"Turning Off"
                attributeState "On", label:'On', icon:"st.lights.philips.hue-single", backgroundColor:"#79b821", action:"off", nextState:"Turning Off"
                attributeState "ON", label:'On', icon:"st.lights.philips.hue-single", backgroundColor:"#79b821", action:"off", nextState:"Turning Off"
                attributeState "Turning On", label:'Turning On', icon:"st.lights.philips.hue-single", backgroundColor:"#FE9A2E", nextState:"Turning Off"
            }
        }
        
        standardTile("debug", "device.motion", inactiveLabel: false, decoration: "flat", width:2, height:2) {
            state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
        }

        main(["richDomoticzOnOff"])
        
        details(["richDomoticzOnOff", "debug"])

        simulator {
            // status messages
            status "Switch On": "switch:1"
            status "Switch Off": "switch:0"
        }
    }
}

def parse(String message) {
    TRACE("parse(${message})")

    Map msg = stringToMap(message)
    if (msg?.size() == 0) {
        log.error "Invalid message: ${message}"
        return null
    }

    if (msg.containsKey("switch")) {
        def value = msg.switch.toInteger()
        switch (value) {
        case 0: off(); break
        case 1: on(); break
        }
    }

    STATE()
    return null
}

// switch.poll() command handler
def poll() {

    if (parent) {
        TRACE("poll() ${device.deviceNetworkId}")
        parent.domoticz_scenepoll(getIDXAddress())
    }
}

// switch.poll() command handler
def refresh() {

    if (parent) {
        parent.domoticz_scenepoll(getIDXAddress())
    }
}

// switch.on() command handler
def on() {

    if (parent) {
        parent.domoticz_sceneon(getIDXAddress())
        parent.domoticz_scenepoll(getIDXAddress())
    }
}

// switch.off() command handler
def off() {

    if (parent) {
        parent.domoticz_sceneoff(getIDXAddress())
        parent.domoticz_scenepoll(getIDXAddress())
    }
}

private def TRACE(message) {
    log.debug message
}

private def STATE() {
    log.debug "switch is ${device.currentValue("switch")}"
    log.debug "deviceNetworkId: ${device.deviceNetworkId}"
}

private String makeNetworkId(ipaddr, port) {

    String hexIp = ipaddr.tokenize('.').collect {
        String.format('%02X', it.toInteger())
    }.join()

    String hexPort = String.format('%04X', port)
    return "${hexIp}:${hexPort}"
}

// gets the address of the device
private getHostAddress() {
	
    def ip = getDataValue("ip")
    def port = getDataValue("port")
    
    if (!ip || !port) {
        def parts = device.deviceNetworkId.split(":")
        if (parts.length == 3) {
            ip = parts[0]
            port = parts[1]
        } else {
            log.warn "Can't figure out ip and port for device: ${device.id}"
        }
    }

    //log.debug "Using ip: $ip and port: $port for device: ${device.id}"
    return ip + ":" + port

}

// gets the IDX address of the device
private getIDXAddress() {
	
    def idx = getDataValue("idx")
        
    if (!idx) {
        def parts = device.deviceNetworkId.split(":")
        if (parts.length == 3) {
            idx = parts[2]
        } else {
            log.warn "Can't figure out idx for device: ${device.id}"
        }
    }

    //log.debug "Using IDX: $idx for device: ${device.id}"
    return idx
}

private Integer convertHexToInt(hex) {
    return Integer.parseInt(hex,16)
}

private String convertHexToIP(hex) {
    return [convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}

// gets the address of the hub
private getCallBackAddress() {
    return device.hub.getDataValue("localIP") + ":" + device.hub.getDataValue("localSrvPortTCP")
}

/*----------------------------------------------------*/
/*			execute event can be called from the service manager!!!
/*----------------------------------------------------*/
def generateEvent (Map results) {
    results.each { name, value ->
        log.info "generateEvent " + name + " " + value
        sendEvent(name:"${name}", value:"${value}")
        }
        return null
}