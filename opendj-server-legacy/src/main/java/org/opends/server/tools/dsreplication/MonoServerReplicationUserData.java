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
 * Copyright 2008-2009 Sun Microsystems, Inc.
 */

package org.opends.server.tools.dsreplication;

/**
 * This is an abstract class used for code refactorization.
 *
 */
abstract class MonoServerReplicationUserData extends ReplicationUserData
{
  private String hostName;
  private int port;
  private boolean useStartTLS;
  private boolean useSSL;

  /**
   * Returns the host name of the server.
   * @return the host name of the server.
   */
  public String getHostName()
  {
    return hostName;
  }

  /**
   * Sets the host name of the server.
   * @param hostName the host name of the server.
   */
  public void setHostName(String hostName)
  {
    this.hostName = hostName;
  }

  /**
   * Returns the port of the server.
   * @return the port of the server.
   */
  public int getPort()
  {
    return port;
  }

  /**
   * Sets the port of the server.
   * @param port the port of the server.
   */
  public void setPort(int port)
  {
    this.port = port;
  }
  /**
   * Returns <CODE>true</CODE> if we must use SSL to connect to the server and
   * <CODE>false</CODE> otherwise.
   * @return <CODE>true</CODE> if we must use SSL to connect to the server and
   * <CODE>false</CODE> otherwise.
   */
  boolean useSSL()
  {
    return useSSL;
  }

  /**
   * Sets whether we must use SSL to connect to the server or not.
   * @param useSSL whether we must use SSL to connect to the server or not.
   */
  void setUseSSL(boolean useSSL)
  {
    this.useSSL = useSSL;
  }

  /**
   * Returns <CODE>true</CODE> if we must use StartTLS to connect to the server
   * and <CODE>false</CODE> otherwise.
   * @return <CODE>true</CODE> if we must use StartTLS to connect to the server
   * and <CODE>false</CODE> otherwise.
   */
  boolean useStartTLS()
  {
    return useStartTLS;
  }

  /**
   * Sets whether we must use StartTLS to connect to the server or not.
   * @param useStartTLS whether we must use SSL to connect to the server or not.
   */
  void setUseStartTLS(boolean useStartTLS)
  {
    this.useStartTLS = useStartTLS;
  }
}


