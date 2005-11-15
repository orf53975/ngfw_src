/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: NatStatisticManager.java 1293 2005-07-11 23:21:41Z rbscott $
 */
package com.metavize.tran.ids;

import com.metavize.mvvm.logging.EventLogger;
import com.metavize.mvvm.logging.StatisticEvent;
import com.metavize.mvvm.tran.StatisticManager;
import com.metavize.mvvm.tran.TransformContext;
import com.metavize.mvvm.tran.firewall.IntfMatcher;

class IDSStatisticManager extends StatisticManager {

    /* Interface matcher to determine if the sessions is incoming or outgoing */
    //final IntfMatcher matcherIncoming = IntfMatcher.MATCHER_IN;
    //final IntfMatcher matcherOutgoing = IntfMatcher.MATCHER_OUT;

    private IDSStatisticEvent statisticEvent = new IDSStatisticEvent();

    public IDSStatisticManager(TransformContext tctx) {
        super(new EventLogger(tctx));
    }

    protected StatisticEvent getInitialStatisticEvent() {
        return this.statisticEvent;
    }

    protected StatisticEvent getNewStatisticEvent() {
        return ( this.statisticEvent = new IDSStatisticEvent());
    }

    void incrScanned() {
        this.statisticEvent.incrScanned();
    }

    void incrPassed() {
        this.statisticEvent.incrPassed();
    }

    void incrBlocked() {
        this.statisticEvent.incrBlocked();
    }
}
