/*
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License, Version 1.0 only
 * (the "License").  You may not use this file except in compliance
 * with the License.
 *
 * You can obtain a copy of the license at
 * trunk/opends/resource/legal-notices/OpenDS.LICENSE
 * or https://OpenDS.dev.java.net/OpenDS.LICENSE.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each
 * file and include the License file at
 * trunk/opends/resource/legal-notices/OpenDS.LICENSE.  If applicable,
 * add the following below this CDDL HEADER, with the fields enclosed
 * by brackets "[]" replaced with your own identifying information:
 *      Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 *
 *
 *      Portions Copyright 2007 Sun Microsystems, Inc.
 */

package org.opends.quicksetup;

import org.opends.quicksetup.event.ProgressNotifier;
import org.opends.quicksetup.util.ProgressMessageFormatter;

/**
 * Represents a quick setup CLI application.
 */
public interface CliApplication extends ProgressNotifier, Runnable {

  /**
   * Creates a set of user data from command line arguments and installation
   * status.
   * @param args String[] of arguments passed in from the command line
   * @param status the current installation status
   * @return UserData object populated to reflect the input args and status
   * @throws UserDataException if something is wrong
   */
  UserData createUserData(String[] args, CurrentInstallStatus status)
          throws UserDataException;

  /**
   * Gets the user data this application will use when running.
   * @return UserData to use when running
   */
  UserData getUserData();


  /**
   * Sets the user data this application will use when running.
   * @param userData UserData to use when running
   */
  void setUserData(UserData userData);

  /**
   * Sets the formatter that will be used to format messages.
   * @param formatter ProgressMessageFormatter used to format messages
   */
  void setProgressMessageFormatter(ProgressMessageFormatter formatter);

  /**
   * Indicates whether or not this applicat is finished running.
   * @return boolean where true indicates we are not running
   */
  boolean isFinished();

  /**
   * Gets any exception that happened while this application was running.
   * A null value returned from this method indicates that the execution
   * of the CLI program is not complete or was successful.
   * @return an exception that happened while the CLI was running
   */
  ApplicationException getException();

}
