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
package com.metavize.tran.airgap;

import java.io.Serializable;

import java.util.Date;

public class ShieldRejectionLogEntry implements Serializable
{
    private final Date   createDate;
    private final String client;
    private final String clientIntf;
    private final double reputation;
    private final int    limited;
    private final int    dropped;
    private final int    rejected;

    ShieldRejectionLogEntry( Date createDate, String client, String clientIntf, 
                             double reputation, int limited, int dropped, int rejected )
                             
    {
        this.createDate = createDate;
        this.client     = client;
        this.clientIntf = clientIntf;
        this.reputation = reputation;
        this.limited    = limited;
        this.dropped    = dropped;
        this.rejected   = rejected;
    }

    // util -------------------------------------------------------------------
    public String getReputationString()
    {
        return String.format( "%.5g", this.reputation );
    }
    
    
    // accessors --------------------------------------------------------------
    public Date getCreateDate()
    {
        return this.createDate;
    }

    public String getClient()
    {
        return this.client;
    }

    public String getClientIntf()
    {
        return this.clientIntf;
    }

    public double getReputation()
    {
        return this.reputation;
    }

    public int getLimited()
    {
        return this.limited;
    }

    public int getDropped()
    {
        return this.dropped;
    }
    
    public int getRejected()
    {
        return this.rejected;
    }
}
