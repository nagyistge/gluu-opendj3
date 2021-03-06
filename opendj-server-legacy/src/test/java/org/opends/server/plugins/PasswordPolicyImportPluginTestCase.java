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
 * Portions Copyright 2014-2016 ForgeRock AS.
 */
package org.opends.server.plugins;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.opends.server.TestCaseUtils;
import org.opends.server.admin.server.AdminTestCaseUtils;
import org.opends.server.admin.std.meta.PasswordPolicyImportPluginCfgDefn;
import org.opends.server.admin.std.server.PasswordPolicyImportPluginCfg;
import org.opends.server.api.plugin.PluginType;
import org.forgerock.opendj.config.server.ConfigException;
import org.opends.server.core.DirectoryServer;
import org.forgerock.opendj.ldap.DN;
import org.opends.server.types.Entry;
import org.opends.server.types.LDIFImportConfig;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * This class defines a set of tests for the
 * org.opends.server.plugins.PasswordPolicyImportPluginTestCase class.
 */
public class PasswordPolicyImportPluginTestCase
       extends PluginTestCase
{
  /**
   * Ensures that the Directory Server is running.
   *
   * @throws  Exception  If an unexpected problem occurs.
   */
  @BeforeClass
  public void startServer()
         throws Exception
  {
    TestCaseUtils.startServer();
  }



  /**
   * Retrieves a set of valid configuration entries that may be used to
   * initialize the plugin.
   *
   * @throws  Exception  If an unexpected problem occurs.
   */
  @DataProvider(name = "validConfigs")
  public Object[][] getValidConfigs()
         throws Exception
  {
    List<Entry> entries = TestCaseUtils.makeEntries(
         "dn: cn=Password Policy Import,cn=Plugins,cn=config",
         "objectClass: top",
         "objectClass: ds-cfg-plugin",
         "objectClass: ds-cfg-password-policy-import-plugin",
         "cn: Password Policy Import",
         "ds-cfg-java-class: org.opends.server.plugins." +
              "PasswordPolicyImportPlugin",
         "ds-cfg-enabled: true",
         "ds-cfg-plugin-type: ldifImport",
         "",
         "dn: cn=Password Policy Import,cn=Plugins,cn=config",
         "objectClass: top",
         "objectClass: ds-cfg-plugin",
         "objectClass: ds-cfg-password-policy-import-plugin",
         "cn: Password Policy Import",
         "ds-cfg-java-class: org.opends.server.plugins." +
              "PasswordPolicyImportPlugin",
         "ds-cfg-enabled: true",
         "ds-cfg-plugin-type: ldifImport",
         "ds-cfg-default-user-password-storage-scheme: " +
              "cn=Salted SHA-1,cn=Password Storage Schemes,cn=config",
         "",
         "dn: cn=Password Policy Import,cn=Plugins,cn=config",
         "objectClass: top",
         "objectClass: ds-cfg-plugin",
         "objectClass: ds-cfg-password-policy-import-plugin",
         "cn: Password Policy Import",
         "ds-cfg-java-class: org.opends.server.plugins." +
              "PasswordPolicyImportPlugin",
         "ds-cfg-enabled: true",
         "ds-cfg-plugin-type: ldifImport",
         "ds-cfg-default-user-password-storage-scheme: " +
              "cn=CRYPT,cn=Password Storage Schemes,cn=config",
         "ds-cfg-default-user-password-storage-scheme: " +
              "cn=Salted SHA-1,cn=Password Storage Schemes,cn=config",
         "",
         "dn: cn=Password Policy Import,cn=Plugins,cn=config",
         "objectClass: top",
         "objectClass: ds-cfg-plugin",
         "objectClass: ds-cfg-password-policy-import-plugin",
         "cn: Password Policy Import",
         "ds-cfg-java-class: org.opends.server.plugins." +
              "PasswordPolicyImportPlugin",
         "ds-cfg-enabled: true",
         "ds-cfg-plugin-type: ldifImport",
         "ds-cfg-default-auth-password-storage-scheme: " +
              "cn=Salted SHA-1,cn=Password Storage Schemes,cn=config",
         "",
         "dn: cn=Password Policy Import,cn=Plugins,cn=config",
         "objectClass: top",
         "objectClass: ds-cfg-plugin",
         "objectClass: ds-cfg-password-policy-import-plugin",
         "cn: Password Policy Import",
         "ds-cfg-java-class: org.opends.server.plugins." +
              "PasswordPolicyImportPlugin",
         "ds-cfg-enabled: true",
         "ds-cfg-plugin-type: ldifImport",
         "ds-cfg-default-user-password-storage-scheme: " +
              "cn=Salted SHA-1,cn=Password Storage Schemes,cn=config",
         "ds-cfg-default-auth-password-storage-scheme: " +
              "cn=Salted SHA-1,cn=Password Storage Schemes,cn=config"
    );

    Object[][] array = new Object[entries.size()][1];
    for (int i=0; i < array.length; i++)
    {
      array[i] = new Object[] { entries.get(i) };
    }

    return array;
  }



  /**
   * Tests the process of initializing the server with valid configurations.
   *
   * @param  e  The configuration entry to use for the initialization.
   *
   * @throws  Exception  If an unexpected problem occurs.
   */
  @Test(dataProvider = "validConfigs")
  public void testInitializeWithValidConfigs(Entry e)
         throws Exception
  {
    HashSet<PluginType> pluginTypes = TestCaseUtils.getPluginTypes(e);

    PasswordPolicyImportPluginCfg configuration =
         AdminTestCaseUtils.getConfiguration(
              PasswordPolicyImportPluginCfgDefn.getInstance(), e);

    PasswordPolicyImportPlugin plugin = new PasswordPolicyImportPlugin();
    plugin.initializePlugin(pluginTypes, configuration);
    plugin.finalizePlugin();
  }



  /**
   * Retrieves a set of invalid configuration entries.
   *
   * @throws  Exception  If an unexpected problem occurs.
   */
  @DataProvider(name = "invalidConfigs")
  public Object[][] getInvalidConfigs()
         throws Exception
  {
    ArrayList<Entry> entries = new ArrayList<>();
    for (String s : PluginType.getPluginTypeNames())
    {
      if (s.equalsIgnoreCase("ldifimport"))
      {
        continue;
      }

      Entry e = TestCaseUtils.makeEntry(
           "dn: cn=Password Policy Import,cn=Plugins,cn=config",
           "objectClass: top",
           "objectClass: ds-cfg-plugin",
           "objectClass: ds-cfg-password-policy-import-plugin",
           "cn: Password Policy Import",
           "ds-cfg-java-class: org.opends.server.plugins." +
                "PasswordPolicyImportPlugin",
           "ds-cfg-enabled: true",
           "ds-cfg-plugin-type: " + s);
      entries.add(e);
    }

    Object[][] array = new Object[entries.size()][1];
    for (int i=0; i < array.length; i++)
    {
      array[i] = new Object[] { entries.get(i) };
    }

    return array;
  }



  /**
   * Tests the process of initializing the server with valid configurations.
   *
   * @param  entry  The configuration entry to use for the initialization.
   *
   * @throws  Exception  If an unexpected problem occurs.
   */
  @Test(dataProvider = "invalidConfigs",
        expectedExceptions = { ConfigException.class })
  public void testInitializeWithInvalidConfigs(Entry e)
         throws Exception
  {
    HashSet<PluginType> pluginTypes = TestCaseUtils.getPluginTypes(e);

    PasswordPolicyImportPluginCfg configuration =
         AdminTestCaseUtils.getConfiguration(
              PasswordPolicyImportPluginCfgDefn.getInstance(), e);

    PasswordPolicyImportPlugin plugin = new PasswordPolicyImportPlugin();
    plugin.initializePlugin(pluginTypes, configuration);
    plugin.finalizePlugin();
  }



  /**
   * Tests the <CODE>doLDIFImport</CODE> method.
   *
   * @throws  Exception  If an unexpected problem occurs.
   */
  @Test
  public void testDoLDIFImport()
         throws Exception
  {
    TestCaseUtils.initializeTestBackend(true);

    DN dn = DN.valueOf("cn=Password Policy Import,cn=plugins,cn=config");
    PasswordPolicyImportPlugin plugin =
         (PasswordPolicyImportPlugin)
         DirectoryServer.getPluginConfigManager().getRegisteredPlugin(dn);

    String[] entryLines =
    {
      "dn: o=test",
      "objectClass: top",
      "objectClass: organization",
      "o: test",
      "",
      "dn: uid=test.user1,o=test",
      "objectClass: top",
      "objectClass: person",
      "objectClass: organizationalPerson",
      "objectClass: inetOrgPerson",
      "uid: test.user1",
      "givenName: Test",
      "sn: User1",
      "cn: Test User1",
      "userPassword: password",
      "",
      "dn: uid=test.user2,o=test",
      "objectClass: top",
      "objectClass: person",
      "objectClass: organizationalPerson",
      "objectClass: inetOrgPerson",
      "uid: test.user2",
      "givenName: Test",
      "sn: User2",
      "cn: Test User2",
      "userPassword: password",
      "ds-pwp-password-policy-dn: cn=SSHA512 UserPassword Policy," +
           "cn=Password Policies,cn=config",
      "",
      "dn: uid=test.user3,o=test",
      "objectClass: top",
      "objectClass: person",
      "objectClass: organizationalPerson",
      "objectClass: inetOrgPerson",
      "objectClass: authPasswordObject",
      "uid: test.user3",
      "givenName: Test",
      "sn: User3",
      "cn: Test User3",
      "authPassword: password",
      "ds-pwp-password-policy-dn: cn=SHA1 AuthPassword Policy," +
           "cn=Password Policies,cn=config"
    };

    String ldifString = TestCaseUtils.makeLdif(entryLines);
    ByteArrayInputStream bais =
         new ByteArrayInputStream(ldifString.getBytes("UTF-8"));
    LDIFImportConfig importConfig = new LDIFImportConfig(bais);

    for (Entry e : TestCaseUtils.makeEntries(entryLines))
    {
      plugin.doLDIFImport(importConfig, e);
    }
  }
}

