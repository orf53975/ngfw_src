/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.metavize.tran.firewall;


import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.logging.StatisticEvent;
import com.metavize.mvvm.tapi.IPNewSessionRequest;
import com.metavize.mvvm.tapi.Protocol;
import com.metavize.mvvm.tran.StatisticManager;

class FirewallStatisticManager extends StatisticManager
{
    private FirewallStatisticEvent statisticEvent = new FirewallStatisticEvent();

    FirewallStatisticManager()
    {
        super();
    }

    protected StatisticEvent getInitialStatisticEvent()
    {
        return this.statisticEvent;
    }

    protected StatisticEvent getNewStatisticEvent()
    {
        return ( this.statisticEvent = new FirewallStatisticEvent());
    }

    /**
     * Keep the stats on a request
     */
    void incrRequest( Protocol protocol, IPNewSessionRequest request, boolean isBlock, boolean isDefault )
    {
        if ( protocol == Protocol.TCP ) {
            incrTcpSession( isBlock, isDefault );
        } else {
            if (( request.clientPort() == 0 ) && ( request.serverPort() == 0 )) {
                incrIcmpSession( isBlock, isDefault );
            } else {
                incrUdpSession( isBlock, isDefault );
            }
        }
    }

    /* XXX This is a lot of duplicated effort, but the only way to get around this is a class
     * hierarchy which makes hibernate a little more difficult */
    private void incrTcpSession( boolean isBlock, boolean isDefault )
    {
        if ( isBlock ) {
            if ( isDefault ) this.statisticEvent.incrTcpBlockedDefault();
            else             this.statisticEvent.incrTcpBlockedRule();
        } else {
            if ( isDefault ) this.statisticEvent.incrTcpPassedDefault();
            else             this.statisticEvent.incrTcpPassedRule();
        }
    }

    private void incrUdpSession( boolean isBlock, boolean isDefault )
    {
        if ( isBlock ) {
            if ( isDefault ) this.statisticEvent.incrUdpBlockedDefault();
            else             this.statisticEvent.incrUdpBlockedRule();
        } else {
            if ( isDefault ) this.statisticEvent.incrUdpPassedDefault();
            else             this.statisticEvent.incrUdpPassedRule();
        }
    }

    private void incrIcmpSession( boolean isBlock, boolean isDefault )
    {
        if ( isBlock ) {
            if ( isDefault ) this.statisticEvent.incrIcmpBlockedDefault();
            else             this.statisticEvent.incrIcmpBlockedRule();
        } else {
            if ( isDefault ) this.statisticEvent.incrIcmpPassedDefault();
            else             this.statisticEvent.incrIcmpPassedRule();
        }
    }
}
