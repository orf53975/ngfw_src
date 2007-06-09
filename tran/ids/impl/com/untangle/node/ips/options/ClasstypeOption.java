/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.node.ips.options;

import com.untangle.uvm.tapi.event.*;
import com.untangle.node.ips.IPSDetectionEngine;
import com.untangle.node.ips.IPSRule;
import com.untangle.node.ips.IPSRuleSignature;
import com.untangle.node.ips.RuleClassification;
import org.apache.log4j.Logger;

public class ClasstypeOption extends IPSOption {
    private static final int HIGH_PRIORITY = 1;
    private static final int MEDIUM_PRIORITY = 2;
    private static final int LOW_PRIORITY = 3;
    private static final int INFORMATIONAL_PRIORITY = 4; // Super low priority

    private final Logger logger = Logger.getLogger(getClass());

    public ClasstypeOption(IPSDetectionEngine engine, IPSRuleSignature signature, String params, boolean initializeSettingsTime) {
        super(signature, params);

        RuleClassification rc = null;
        if (engine != null)
            // Allow null for testing.
            rc = engine.getClassification(params);
        if (rc == null) {
            logger.warn("Unable to find rule classification: " + params);
            // use default classification text for signature
        } else {
            signature.setClassification(rc.getDescription());

            if (true == initializeSettingsTime) {
                IPSRule rule = signature.rule();
                int priority = rc.getPriority();
                // logger.debug("Rule Priority for " + rule.getDescription() + " is " + priority);
                switch (priority) {
                case HIGH_PRIORITY:
                    rule.setLive(true);
                    rule.setLog(true);
                    break;
                case MEDIUM_PRIORITY:
                    rule.setLog(true);
                    break;
                default:
                    break;
                }
            }
        }
    }
}
