/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.tran;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents the runtime state of a transform instance.
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 */
public class TransformState implements Serializable
{
    private static final long serialVersionUID = 2291079542779127610L;

    /**
     * The transform instance has not been created yet.
     */
    public static final TransformState NOT_LOADED = new TransformState("not-loaded");

    /**
     * Instantiated, but not initialized. This is a transient state, just
     * after the main transform class has been instantiated, but before
     * init has been called.
     */
    public static final TransformState LOADED = new TransformState("loaded");

    /**
     * Initialized, but not running. The transform instance enters this state
     * after it has been initialized, or when it is stopped.
     */
    public static final TransformState INITIALIZED = new TransformState("initialized");

    /**
     * Running.
     */
    public static final TransformState RUNNING = new TransformState("running");

    /**
     * Disabled. Initialized, but cannot be put into RUNNING state.
     */
    public static final TransformState DISABLED = new TransformState("disabled");

    /**
     * Destroyed, this instance should not be used.
     */
    public static final TransformState DESTROYED = new TransformState("destroyed");

    private static final Map INSTANCES = new HashMap();

    static {
        INSTANCES.put(LOADED.toString(), LOADED);
        INSTANCES.put(DISABLED.toString(), DISABLED);
        INSTANCES.put(INITIALIZED.toString(), INITIALIZED);
        INSTANCES.put(RUNNING.toString(), RUNNING);
        INSTANCES.put(DESTROYED.toString(), DESTROYED);
    }

    private String state;

    public static TransformState getInstance(String state)
    {
        return (TransformState)INSTANCES.get(state);
    }

    private TransformState(String state) { this.state = state; }

    public String toString() { return state; }

    // Serialization ----------------------------------------------------------
    Object readResolve()
    {
        return getInstance(state);
    }
}
