/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions Copyright [year] [name of copyright owner]".
 *
 * Copyright 2008 Sun Microsystems, Inc.
 */

package org.forgerock.opendj.config;

/**
 * This enumeration contains various options that can be associated with
 * relation definitions.
 */
public enum RelationOption {
    /**
     * Use this option to identify relations which should be considered as
     * advanced and should not be exposed by default in client applications.
     */
    ADVANCED,

    /**
     * Use this option to identify relations which must not be directly exposed
     * in client applications.
     */
    HIDDEN;
}
