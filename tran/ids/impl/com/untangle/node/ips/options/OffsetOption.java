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

import java.util.regex.*;

import com.untangle.uvm.tapi.event.*;
import com.untangle.uvm.node.ParseException;
import com.untangle.node.ips.IPSRuleSignature;
import org.apache.log4j.Logger;

public class OffsetOption extends IPSOption {

    private final Logger logger = Logger.getLogger(getClass());

    public OffsetOption(IPSRuleSignature signature, String params) throws ParseException {
        super(signature, params);
        ContentOption option = (ContentOption) signature.getOption("ContentOption",this);
        if(option == null) {
            logger.warn("Unable to find content option to set offset for sig: " + signature);
            return;
        }

        int offset = 0;
        try {
            offset = Integer.parseInt(params);
        } catch (Exception e) {
            throw new ParseException("Not a valid Offset argument: " + params);
        }
        option.setOffset(offset);
    }
}
