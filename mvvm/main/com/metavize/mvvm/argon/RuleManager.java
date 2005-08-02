/*
 * Copyright (c) 2003, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id$
 */

package com.metavize.mvvm.argon;

import java.io.File;
import java.io.FileWriter;
import java.io.FileNotFoundException;
import java.io.BufferedWriter;

import org.apache.log4j.Logger;

import com.metavize.jnetcap.Netcap;
import com.metavize.jnetcap.JNetcapException;
import com.metavize.jnetcap.PortRange;

public class RuleManager
{
    private static final String HEADER = "##AUTOGENERATED BY METAVIZE DO NOT MODIFY MANUALLY\n\n";

    private static final String UDP_DIVERT_PORT_FLAG         = "UDP_DIVERT_PORT";
    private static final String TCP_REDIRECT_PORT_FLAG       = "TCP_REDIRECT_PORTS";
    private static final String ANTISUBSCRIBE_LOCAL_IN_FLAG  = "ANTISUBSCRIBE_LOCAL_INSIDE";
    private static final String ANTISUBSCRIBE_LOCAL_OUT_FLAG = "ANTISUBSCRIBE_LOCAL_OUTSIDE";
    private static final String DHCP_BLOCK_FORWARD_FLAG      = "DHCP_BLOCK_FORWARDING";

    private static RuleManager INSTANCE = null;

    private final String BUNNICULA_BASE = System.getProperty( "bunnicula.home" );
    private final String BUNNICULA_CONF = System.getProperty( "bunnicula.conf.dir" );
    private final String MVVM_TMP_FILE  = BUNNICULA_CONF + "/tmp_params";

    private final String RULE_GENERATOR_SCRIPT = BUNNICULA_BASE + "/networking/rule-generator";
    private final String RULE_DESTROYER_SCRIPT = BUNNICULA_BASE + "/networking/rule-destroyer";

    private final Logger logger = Logger.getLogger( RuleManager.class );

    private boolean subscribeLocalInside  = false;
    private boolean subscribeLocalOutside = false;
    private boolean dhcpEnableForwarding  = true;

    private boolean isShutdown = false;

    /* Call the script to generate all of the iptables rules */
    synchronized void generateIptablesRules() throws ArgonException
    {
        if ( isShutdown ) {
            logger.warn( "MVVM is already shutting down, no longer able to generate rules" );
            return;
        }

        int ret = 0;
        try {
            writeConfig();

            /* Call the rule generator */
            Process p = Runtime.getRuntime().exec( "sh " + RULE_GENERATOR_SCRIPT );
            
            ret = p.waitFor();
        } catch ( Exception e ) {
            logger.error( "Error while generating iptables rules", e );
            throw new ArgonException( "Unable to generate iptables rules", e );
        }
        
        if ( ret != 0 ) throw new ArgonException( "Error while generating iptables rules: " + ret );
    }

    synchronized void destroyIptablesRules() throws ArgonException
    {
        int ret = 0;
        try {
            /* Call the rule generator */
            /* XXXXXXX Make the scripts executable */
            Process p = Runtime.getRuntime().exec( "sh " + RULE_DESTROYER_SCRIPT );
            
            ret = p.waitFor();
        } catch ( Exception e ) {
            logger.error( "Error while removing iptables rules", e );
            throw new ArgonException( "Unable to remove iptables rules", e );
        }
        
        if ( ret != 0 ) throw new ArgonException( "Error while removing iptables rules: " + ret );
    }

    void subscribeLocalInside( boolean subscribeLocalInside )
    {
        this.subscribeLocalInside = subscribeLocalInside;
    }

    void subscribeLocalOutside( boolean subscribeLocalOutside )
    {
        this.subscribeLocalOutside = subscribeLocalOutside;
    }
    
    void dhcpEnableForwarding( boolean dhcpEnableForwarding )
    {
        this.dhcpEnableForwarding = dhcpEnableForwarding;
    }

    synchronized void isShutdown()
    {
        this.isShutdown = true;
    }

    private void writeConfig() throws ArgonException
    {
        try {
            StringBuilder sb = new StringBuilder();
            
            Netcap netcap = Netcap.getInstance();
            
            sb.append( HEADER );
            
            PortRange tcp = netcap.tcpRedirectPortRange();
            int divertPort = netcap.udpDivertPort();
            
            sb.append( TCP_REDIRECT_PORT_FLAG       + "=" + tcp.low() + ":" + tcp.high() + "\n" );
            sb.append( UDP_DIVERT_PORT_FLAG         + "=" + divertPort + "\n" );
            sb.append( ANTISUBSCRIBE_LOCAL_IN_FLAG  + "=" + !subscribeLocalInside + "\n" );
            sb.append( ANTISUBSCRIBE_LOCAL_OUT_FLAG + "=" + !subscribeLocalOutside + "\n" );
            sb.append( DHCP_BLOCK_FORWARD_FLAG      + "=" + !dhcpEnableForwarding  + "\n\n" );
            
            writeFile( sb, MVVM_TMP_FILE );
        } catch ( JNetcapException e ) {
            logger.error( "Unable to write rule manager configuration", e );
        }
    }

    private void writeFile( StringBuilder sb, String fileName ) throws ArgonException
    {
        BufferedWriter out = null;
        
        /* Open up the interfaces file */
        try {
            String data = sb.toString();
            
            out = new BufferedWriter(new FileWriter( fileName ));
            out.write( data, 0, data.length());
        } catch ( Exception ex ) {
            /* XXX May need to catch this exception, restore defaults
             * then try again */
            logger.error( "Error writing file " + fileName + ":", ex );
            throw new ArgonException( "Error writing file " + fileName, ex );
        } finally {
            try {
                if ( out != null ) out.close();
            } catch ( Exception e ) {
                logger.error( "Unable to close file", e );
            }
        }
    }

    static synchronized RuleManager getInstance()
    {
        if ( INSTANCE == null ) {
            INSTANCE = new RuleManager();
        }
        
        return INSTANCE;
    }


}
