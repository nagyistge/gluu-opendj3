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

package org.opends.guitools.controlpanel.datamodel;


 /**
  * Policy to follow to choose the protocol to be used.
  *
  * */
public enum ConnectionProtocolPolicy
{
  /**
   * Force to use Start TLS.
   */
  USE_STARTTLS,
  /**
   * Force to use LDAP.
   */
  USE_LDAP,
  /**
   * Force to use LDAPs.
   */
  USE_LDAPS,
  /**
   * Force to use the Administration Connector.
   */
  USE_ADMIN,
  /**
   * Use the most secure available (LDAPs, StartTLS and finally LDAP).
   */
  USE_MOST_SECURE_AVAILABLE,
  /**
   * Use the less secure available (LDAP, and then LDAPs).
   */
  USE_LESS_SECURE_AVAILABLE;

  /**
   * Returns the ConnectionProtocolPolicy to be used with the parameters
   * provided by the user.
   * @param useSSL whether the user asked to use SSL or not.
   * @param useStartTLS whether the user asked to use Start TLS or not.
   * @return the ConnectionProtocolPolicy to be used with the parameters
   * provided by the user.
   */
  public static ConnectionProtocolPolicy getConnectionPolicy(boolean useSSL,
      boolean useStartTLS)
  {
    ConnectionProtocolPolicy policy;
    if (useStartTLS)
    {
      policy = ConnectionProtocolPolicy.USE_STARTTLS;
    }
    else if (useSSL)
    {
      policy = ConnectionProtocolPolicy.USE_LDAPS;
    }
    else
    {
      policy = ConnectionProtocolPolicy.USE_LESS_SECURE_AVAILABLE;
    }
    return policy;
  }
}
