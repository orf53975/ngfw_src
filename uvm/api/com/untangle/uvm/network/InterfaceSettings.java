/**
 * $Id: InterfaceSettings.java 32043 2012-05-31 21:31:47Z dmorris $
 */
package com.untangle.uvm.network;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.net.InetAddress;

import org.json.JSONObject;
import org.json.JSONString;

import com.untangle.uvm.node.IPMaskedAddress;

/**
 * Interface settings.
 */
@SuppressWarnings("serial")
public class InterfaceSettings implements Serializable, JSONString
{
    public Integer interfaceId; /* the ID of the physical interface (1-254) */
    public String name; /* human name: ie External, Internal, Wireless */
    public String physicalDev; /* physical interface name: eth0, etc */
    public String iptablesDev; /* iptables interface name: eth0, eth0:0, eth0.1, etc */
    public String symbolicDev; /* symbolic interface name: eth0, eth0:0, eth0.1, etc */
    public boolean isWan; /* is a WAN interface? */
    public String config; /* config type: addressed, bridged, disabled */

    public Integer bridgedTo; /* device to bridge to in "bridged" case */
    
    public String v4ConfigType; /* config type: static, auto, pppoe */
    
    public InetAddress v4StaticAddress; /* the address  of this interface if configured static, or dhcp override */ 
    public InetAddress v4StaticNetmask; /* the netmask  of this interface if configured static, or dhcp override */
    public InetAddress v4StaticGateway; /* the gateway  of this interface if configured static, or dhcp override */
    public InetAddress v4StaticDns1; /* the dns1  of this interface if configured static */
    public InetAddress v4StaticDns2; /* the dns2  of this interface if configured static */
    public InetAddress v4AutoAddressOverride; /* the dhcp override address (null means don't override) */ 
    public InetAddress v4AutoNetmaskOverride; /* the dhcp override netmask (null means don't override) */ 
    public InetAddress v4AutoGatewayOverride; /* the dhcp override gateway (null means don't override) */ 
    public InetAddress v4AutoDns1Override; /* the dhcp override dns1 (null means don't override) */
    public InetAddress v4AutoDns2Override; /* the dhcp override dns2 (null means don't override) */

    public String v6ConfigType; /* config type: static, auto */
    
    public InetAddress v6StaticAddress; /* the address  of this interface if configured static, or dhcp override */ 
    public Integer     v6StaticPrefixLength; /* the netmask  of this interface if configured static, or dhcp override */
    public InetAddress v6StaticGateway; /* the gateway  of this interface if configured static, or dhcp override */
    public InetAddress v6StaticDns1; /* the dns1  of this interface if configured static, or dhcp override */
    public InetAddress v6StaticDns2; /* the dns2  of this interface if configured static, or dhcp override */
    
    public List<IPMaskedAddress> aliases; /* alias addresses for static & dhcp */
    
    public InterfaceSettings() { }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

    public Integer getInterfaceId( ) { return this.interfaceId; }
    public void setInterfaceId( Integer newValue ) { this.interfaceId = newValue; }

    public String getName( ) { return this.name; }
    public void setName( String newValue ) { this.name = newValue; }

    public String getPhysicalDev( ) { return this.physicalDev; }
    public void setPhysicalDev( String newValue ) { this.physicalDev = newValue; }

    public String getIptablesDev( ) { return this.iptablesDev; }
    public void setIptablesDev( String newValue ) { this.iptablesDev = newValue; }

    public String getSymbolicDev( ) { return this.symbolicDev; }
    public void setSymbolicDev( String newValue ) { this.symbolicDev = newValue; }

    public boolean getIsWan( ) { return this.isWan; }
    public void setIsWan( boolean newValue ) { this.isWan = newValue; }

    public String getConfig( ) { return this.config; }
    public void setConfig( String newValue ) { this.config = newValue; }

    public Integer getBridgedTo( ) { return this.bridgedTo; }
    public void setBridgedTo( Integer newValue ) { this.bridgedTo = newValue; }
    
    public String getV4ConfigType( ) { return this.v4ConfigType; }
    public void setV4ConfigType( String newValue ) { this.v4ConfigType = newValue; }

    public InetAddress getV4StaticAddress( ) { return this.v4StaticAddress; }
    public void setV4StaticAddress( InetAddress newValue ) { this.v4StaticAddress = newValue; }

    public InetAddress getV4StaticNetmask( ) { return this.v4StaticNetmask; }
    public void setV4StaticNetmask( InetAddress newValue ) { this.v4StaticNetmask = newValue; }
    
    public InetAddress getV4StaticGateway( ) { return this.v4StaticGateway; }
    public void setV4StaticGateway( InetAddress newValue ) { this.v4StaticGateway = newValue; }
    
    public InetAddress getV4StaticDns1( ) { return this.v4StaticDns1; }
    public void setV4StaticDns1( InetAddress newValue ) { this.v4StaticDns1 = newValue; }

    public InetAddress getV4StaticDns2( ) { return this.v4StaticDns2; }
    public void setV4StaticDns2( InetAddress newValue ) { this.v4StaticDns2 = newValue; }

    public InetAddress getV4AutoAddressOverride( ) { return this.v4AutoAddressOverride; }
    public void setV4AutoAddressOverride( InetAddress newValue ) { this.v4AutoAddressOverride = newValue; }
    
    public InetAddress getV4AutoNetmaskOverride( ) { return this.v4AutoNetmaskOverride; }
    public void setV4AutoNetmaskOverride( InetAddress newValue ) { this.v4AutoNetmaskOverride = newValue; }
    
    public InetAddress getV4AutoGatewayOverride( ) { return this.v4AutoGatewayOverride; }
    public void setV4AutoGatewayOverride( InetAddress newValue ) { this.v4AutoGatewayOverride = newValue; }

    public InetAddress getV4AutoDns1Override( ) { return this.v4AutoDns1Override; }
    public void setV4AutoDns1Override( InetAddress newValue ) { this.v4AutoDns1Override = newValue; }

    public InetAddress getV4AutoDns2Override( ) { return this.v4AutoDns2Override; }
    public void setV4AutoDns2Override( InetAddress newValue ) { this.v4AutoDns2Override = newValue; }
    
    public String getV6ConfigType( ) { return this.v6ConfigType; }
    public void setV6ConfigType( String newValue ) { this.v6ConfigType = newValue; }

    public InetAddress getV6StaticAddress( ) { return this.v6StaticAddress; }
    public void setV6StaticAddress( InetAddress newValue ) { this.v6StaticAddress = newValue; }

    public Integer getV6StaticPrefixLength( ) { return this.v6StaticPrefixLength; }
    public void setV6StaticPrefixLength( Integer newValue ) { this.v6StaticPrefixLength = newValue; }

    public InetAddress getV6StaticGateway( ) { return this.v6StaticGateway; }
    public void setV6StaticGateway( InetAddress newValue ) { this.v6StaticGateway = newValue; }

    public InetAddress getV6StaticDns1( ) { return this.v6StaticDns1; }
    public void setV6StaticDns1( InetAddress newValue ) { this.v6StaticDns1 = newValue; }

    public InetAddress getV6StaticDns2( ) { return this.v6StaticDns2; }
    public void setV6StaticDns2( InetAddress newValue ) { this.v6StaticDns2 = newValue; }
    
    public List<IPMaskedAddress> getAliases( ) { return this.aliases; }
    public void setAliases( List<IPMaskedAddress> newValue ) { this.aliases = newValue; }
    
}
