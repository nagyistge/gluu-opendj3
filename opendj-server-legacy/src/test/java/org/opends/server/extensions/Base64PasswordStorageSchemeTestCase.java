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
 */
package org.opends.server.extensions;



import org.opends.server.admin.server.AdminTestCaseUtils;
import org.opends.server.admin.std.meta.Base64PasswordStorageSchemeCfgDefn;
import org.opends.server.admin.std.server.Base64PasswordStorageSchemeCfg;
import org.opends.server.api.PasswordStorageScheme;



/**
 * A set of test cases for the base64 password storage scheme.
 */
public class Base64PasswordStorageSchemeTestCase
       extends PasswordStorageSchemeTestCase
{
  /**
   * Creates a new instance of this storage scheme test case.
   */
  public Base64PasswordStorageSchemeTestCase()
  {
    super("cn=Base64,cn=Password Storage Schemes,cn=config");
  }



  /**
   * Retrieves an initialized instance of this password storage scheme.
   *
   * @return  An initialized instance of this password storage scheme.
   *
   * @throws  Exception  If an unexpected problem occurs.
   */
  protected PasswordStorageScheme getScheme()
         throws Exception
  {
    Base64PasswordStorageScheme scheme = new Base64PasswordStorageScheme();

    Base64PasswordStorageSchemeCfg configuration =
      AdminTestCaseUtils.getConfiguration(
          Base64PasswordStorageSchemeCfgDefn.getInstance(),
          configEntry.getEntry()
          );

    scheme.initializePasswordStorageScheme(configuration);
    return scheme;
  }
}

