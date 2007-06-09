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

import com.untangle.node.ips.IPSRuleSignature;
import org.apache.log4j.Logger;

public class DistanceOption extends IPSOption {

    private final Logger logger = Logger.getLogger(getClass());

    public DistanceOption(IPSRuleSignature signature, String params) {
        super(signature, params);
        int distance = Integer.parseInt(params);
        IPSOption option = signature.getOption("ContentOption",this);
        if(option == null) {
            logger.warn("Unable to find content option to set distance for sig: " + signature);
            return;
        }

        ContentOption content = (ContentOption) option;
        content.setDistance(distance);
    }
}
