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
 * Copyright 2006-2008 Sun Microsystems, Inc.
 * Portions Copyright 2011-2015 ForgeRock AS.
 */
package org.opends.server.types;

import org.forgerock.i18n.LocalizableMessage;



import static org.opends.messages.CoreMessages.*;



/**
 * This enumeration defines the set of possible reasons for the
 * closure of a connection between a client and the Directory Server.
 */
@org.opends.server.types.PublicAPI(
     stability=org.opends.server.types.StabilityLevel.VOLATILE,
     mayInstantiate=false,
     mayExtend=false,
     mayInvoke=true)
public enum DisconnectReason
{
  /**
   * The disconnect reason that indicates that the client connection
   * was closed because the client unbind from the server.
   */
  UNBIND(
          INFO_DISCONNECT_DUE_TO_UNBIND.get()),



  /**
   * The disconnect reason that indicates that the client connection
   * was closed because the client disconnected without unbinding.
   */
  CLIENT_DISCONNECT(
          INFO_DISCONNECT_DUE_TO_CLIENT_CLOSURE.get()),



  /**
   * The disconnect reason that indicates that the client connection
   * was closed because the client connection was rejected.
   */
  CONNECTION_REJECTED(
          INFO_DISCONNECT_DUE_TO_REJECTED_CLIENT.get()),



  /**
   * The disconnect reason that indicates that the client connection
   * was closed because of an I/O error.
   */
  IO_ERROR(
          INFO_DISCONNECT_DUE_TO_IO_ERROR.get()),



  /**
   * The disconnect reason that indicates that the client connection
   * was closed because of a protocol error.
   */
  PROTOCOL_ERROR(
          INFO_DISCONNECT_DUE_TO_PROTOCOL_ERROR.get()),



  /**
   * The disconnect reason that indicates that the client connection
   * was closed because the Directory Server shut down.
   */
  SERVER_SHUTDOWN(
          INFO_DISCONNECT_DUE_TO_SERVER_SHUTDOWN.get()),



  /**
   * The disconnect reason that indicates that the client connection
   * was closed because an administrator terminated the connection.
   */
  ADMIN_DISCONNECT(
          INFO_DISCONNECT_BY_ADMINISTRATOR.get()),



  /**
   * The disconnect reason that indicates that the client connection
   * was closed because of a security problem.
   */
  SECURITY_PROBLEM(
          INFO_DISCONNECT_DUE_TO_SECURITY_PROBLEM.get()),



  /**
   * The disconnect reason that indicates that the client connection was closed
   * because the bound user's entry is no longer accessible.
   */
  INVALID_CREDENTIALS(
          INFO_DISCONNECT_DUE_TO_INVALID_CREDENTIALS.get()),



  /**
   * The disconnect reason that indicates that the client connection
   * was closed because the maximum allowed request size was exceeded.
   */
  MAX_REQUEST_SIZE_EXCEEDED(
          INFO_DISCONNECT_DUE_TO_MAX_REQUEST_SIZE.get()),



  /**
   * The disconnect reason that indicates that the client connection
   * was closed because an administrative limit was exceeded.
   */
  ADMIN_LIMIT_EXCEEDED(
          INFO_DISCONNECT_DUE_TO_ADMIN_LIMIT.get()),



  /**
   * The disconnect reason that indicates that the client connection
   * was closed because the idle time limit was exceeded.
   */
  IDLE_TIME_LIMIT_EXCEEDED(
          INFO_DISCONNECT_DUE_TO_IDLE_TIME_LIMIT.get()),



  /**
   * The disconnect reason that indicates that the client connection
   * was closed because of an I/O timeout.
   */
  IO_TIMEOUT(
          INFO_DISCONNECT_DUE_TO_IO_TIMEOUT.get()),



  /**
   * The disconnect reason that indicates that the client connection
   * was closed because of an internal error within the server.
   */
  SERVER_ERROR(
          INFO_DISCONNECT_DUE_TO_SERVER_ERROR.get()),



  /**
   * The disconnect reason that indicates that the client connection
   * was closed by a plugin.
   */
  CLOSED_BY_PLUGIN(
          INFO_DISCONNECT_BY_PLUGIN.get()),



  /**
   * The disconnect reason that indicates that the client connection
   * was closed for some other reason.
   */
  OTHER(
          INFO_DISCONNECT_OTHER.get());



  /** The disconnect reason. */
  private LocalizableMessage message;


  /**
   * Creates a new disconnect reason element with the provided closure
   * message.
   *
   * @param  message  The message for this disconnect reason.
   */
  private DisconnectReason(LocalizableMessage message)
  {
    this.message = message;
  }



  /**
   * Retrieves the human-readable disconnect reason.
   *
   * @return  The human-readable disconnect reason.
   */
  public LocalizableMessage getClosureMessage()
  {
    return message;
  }



  /**
   * Retrieves a string representation of this disconnect reason.
   *
   * @return  A string representation of this disconnect reason.
   */
  public String toString()
  {
    return message.toString();
  }
}

