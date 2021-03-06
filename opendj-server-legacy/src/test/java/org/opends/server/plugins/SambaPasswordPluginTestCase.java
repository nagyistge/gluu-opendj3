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
 * Copyright 2011-2012 profiq s.r.o.
 * Portions Copyright 2011-2016 ForgeRock AS.
 */
package org.opends.server.plugins;

import static org.assertj.core.api.Assertions.*;
import static org.forgerock.opendj.ldap.ModificationType.*;
import static org.opends.server.protocols.internal.InternalClientConnection.*;
import static org.opends.server.util.CollectionUtils.*;
import static org.opends.server.util.StaticUtils.*;
import static org.testng.Assert.*;

import java.util.LinkedList;

import org.forgerock.opendj.io.ASN1;
import org.forgerock.opendj.io.ASN1Writer;
import org.forgerock.opendj.ldap.ByteString;
import org.forgerock.opendj.ldap.ByteStringBuilder;
import org.forgerock.opendj.ldap.ModificationType;
import org.forgerock.opendj.ldap.ResultCode;
import org.forgerock.opendj.ldap.requests.ModifyRequest;
import org.forgerock.opendj.ldap.requests.Requests;
import org.opends.server.TestCaseUtils;
import org.opends.server.core.DirectoryServer;
import org.opends.server.core.ExtendedOperation;
import org.opends.server.core.ModifyOperation;
import org.opends.server.extensions.ExtensionsConstants;
import org.opends.server.plugins.SambaPasswordPlugin.MD4MessageDigest;
import org.opends.server.plugins.SambaPasswordPlugin.TimeStampProvider;
import org.opends.server.protocols.internal.InternalClientConnection;
import org.opends.server.types.Attribute;
import org.opends.server.types.Attributes;
import org.opends.server.types.AuthenticationInfo;
import org.forgerock.opendj.ldap.DN;
import org.opends.server.types.DirectoryException;
import org.opends.server.types.Entry;
import org.opends.server.types.Modification;
import org.opends.server.util.ServerConstants;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Unit tests for the Samba password synchronization plugin.
 */
public class SambaPasswordPluginTestCase extends PluginTestCase
{

  /**
   * Initializes the synchronization plugin before running this test suite.
   *
   * @throws Exception
   *           If an unexpected exception occurred.
   */
  @BeforeClass
  public void beforeClass() throws Exception
  {
    // Start the server
    TestCaseUtils.startServer();

    /*
     * Configuration for the plugin which would be used to execute tests.
     */
    Entry configEntry = TestCaseUtils
        .makeEntry(
            "dn: cn=samba password,cn=Plugins,cn=config",
            "objectClass: ds-cfg-samba-password-plugin",
            "objectClass: ds-cfg-plugin",
            "objectClass: top",
            "ds-cfg-plugin-type: postoperationextended",
            "ds-cfg-plugin-type: preoperationmodify",
            "ds-cfg-pwd-sync-policy: sync-nt-password",
            "ds-cfg-pwd-sync-policy: sync-lm-password",
            "ds-cfg-samba-administrator-dn: cn=samba admin,o=test",
            "cn: samba password", "ds-cfg-enabled: true",
            "ds-cfg-java-class: org.opends.server.plugins.SambaPasswordPlugin");

    // Add the configuration to the running directory instance.
    TestCaseUtils.addEntry(configEntry);
  }



  /**
   * Cleans up after running this test suite.
   *
   * @throws Exception
   *           If an unexpected exception occurred.
   */
  @AfterClass
  public void afterClass() throws Exception
  {
    TestCaseUtils.deleteEntry(DN
        .valueOf("cn=samba password,cn=Plugins,cn=config"));
  }



  /**
   * Initializes the test backend before each test.
   *
   * @throws Exception
   *           If an unexpected exception occurred.
   */
  @BeforeMethod
  public void beforeMethod() throws Exception
  {
    // Create an empty test backend 'o=test'
    TestCaseUtils.initializeTestBackend(true);

    /*
     * For the Samba administrative user parameter to be set correctly the
     * backend needs to have an existing user with the 'password-reset'
     * privilege.
     */
    TestCaseUtils.addEntries("dn: cn=Samba Admin,o=test", "objectClass: top",
        "objectClass: person", "userPassword: password", "sn: Admin",
        "cn: Samba Admin", "ds-privilege-name: password-reset");

    /*
     * Create the administrative user without privileges which will be used for
     * negative configuration tests.
     */
    TestCaseUtils.addEntries("dn: cn=Samba Admin NP,o=test",
        "objectClass: top", "objectClass: person", "userPassword: password",
        "sn: Admin NP", "cn: Samba Admin NP");

    /*
     * Samba administrative user needs a permission to manipulate user accounts.
     * Hence, we add a very permissive ACI.
     */
    ModifyRequest modifyRequest = Requests.newModifyRequest("o=test")
        .addModification(ADD, "aci",
            "(target=\"ldap:///uid=*,o=test\")(targetattr=\"*\")"
                + "(version 3.0; acl \"Samba admin\"; allow (all) "
                + "userdn=\"ldap:///cn=samba admin,o=test\";)");
    ModifyOperation modOp = getRootConnection().processModify(modifyRequest);
    assertEquals(modOp.getResultCode(), ResultCode.SUCCESS);
  }



  /**
   * Returns test data for testMD4Digest taken from RFC 1320.
   *
   * @return The test data.
   */
  @DataProvider
  public Object[][] md4DigestData()
  {
    return new Object[][] {
        { "", "31d6cfe0d16ae931b73c59d7e0c089c0" },
        { "a", "bde52cb31de33e46245e05fbdbd6fb24" },
        { "abc", "a448017aaf21d8525fc10ae87aa6729d" },
        { "message digest", "d9130a8164549fe818874806e1c7014b" },
        { "abcdefghijklmnopqrstuvwxyz", "d79e1c308aa5bbcdeea8ed63df412da9" },
        { "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789",
            "043f8582f241db351ce627e153e7f0e4" },
        {
            "12345678901234567890123456789012345678901234567890123456789012345678901234567890",
            "e33b4ddc9c38f2199c3e7b164fcc0536" }, };
  }



  /**
   * Tests the MD4 implementation ported from Bouncy Castle using the test data
   * defined in RFC 1320.
   *
   * @param inputString
   *          Input data.
   * @param expectedHash
   *          Expected hash in hex.
   * @throws Exception
   *           If an unexpected error occurred.
   */
  @Test(dataProvider = "md4DigestData")
  public void testMD4Digest(String inputString, String expectedHash)
      throws Exception
  {
    MD4MessageDigest digest = new MD4MessageDigest();
    byte[] result = digest.digest(inputString.getBytes("UTF-8"));
    assertEquals(toLowerCase(bytesToHexNoSpace(result)), expectedHash);
  }



  /**
   * Change the user password using REPLACE modify operation on 'userPassword'
   * attribute as ROOT user.
   *
   * @throws Exception
   *           if the password hash does not match.
   */
  @Test
  public void testModifyOperationAsRoot() throws Exception
  {
    // Test entry
    Entry testEntry = TestCaseUtils.makeEntry("dn: uid=test.user1,o=test",
        "objectClass: top", "objectClass: person",
        "objectClass: organizationalPerson", "objectClass: inetOrgPerson",
        "objectClass: sambaSAMAccount", "uid: test.user", "cn: Test User",
        "givenName: Test", "sn: User", "sambaSID: 123",
        "userPassword: password");

    TestCaseUtils.addEntry(testEntry);

    // Perform the modify operation
    ModifyRequest modifyRequest = Requests.newModifyRequest(testEntry.getName())
        .addModification(REPLACE, "userPassword", "password");
    ModifyOperation modOp = getRootConnection().processModify(modifyRequest);
    assertEquals(modOp.getResultCode(), ResultCode.SUCCESS);

    // Verification of the change
    assertThatPasswordsAreEqualTo(testEntry,
        "8846f7eaee8fb117ad06bdd830b7586c", "e52cac67419a9a224a3b108f3fa6cb6d");
    TestCaseUtils.deleteEntry(testEntry);
  }



  private void assertThatPasswordsAreEqualTo(Entry testEntry,
      String ntPassword, String lmPassword) throws DirectoryException
  {
    Entry entry = DirectoryServer.getEntry(testEntry.getName());
    assertNotNull(entry);

    assertTrue(contains(entry, "sambantpassword", ntPassword), "NT password not found in test entry");
    assertTrue(contains(entry, "sambalmpassword", lmPassword), "LanMan password not found in test entry");
  }

  private boolean contains(Entry entry, String attrName, String password)
  {
    boolean foundPwd = false;
    for (Attribute a : entry.getAttribute(attrName))
    {
      for (ByteString val : a)
      {
        foundPwd = true;
        assertEquals(val.toString(), password);
      }
    }
    return foundPwd;
  }



  /**
   * Use modify operation to replace the userPassword attribute as a normal
   * user.
   *
   * @throws Exception
   *           if the test fails.
   */
  @Test
  public void testModifyOperationAsUser() throws Exception
  {
    // Test entry
    Entry testEntry = TestCaseUtils.makeEntry("dn: uid=test.user3,o=test",
        "objectClass: top", "objectClass: person",
        "objectClass: organizationalPerson", "objectClass: inetOrgPerson",
        "objectClass: sambaSAMAccount", "uid: test.user", "cn: Test User",
        "givenName: Test", "sn: User", "sambaSID: 123",
        "userPassword: password");

    TestCaseUtils.addEntry(testEntry);

    // Perform the modify operation
    AuthenticationInfo authInfo = new AuthenticationInfo(testEntry, false);

    InternalClientConnection conn = new InternalClientConnection(authInfo);

    LinkedList<Modification> mods = newLinkedList(
        new Modification(ModificationType.REPLACE, Attributes.create("userPassword", "password")));
    ModifyOperation modOp = conn.processModify(testEntry.getName(), mods);
    assertEquals(modOp.getResultCode(), ResultCode.SUCCESS);

    // Verification of the result
    assertThatPasswordsAreEqualTo(testEntry,
        "8846f7eaee8fb117ad06bdd830b7586c", "e52cac67419a9a224a3b108f3fa6cb6d");
    TestCaseUtils.deleteEntry(testEntry);
  }



  /**
   * Use modify operation to replace the userPassword attribute as Samba
   * administrative user. This operation should be skipped, that is, the hash
   * should be unchanged if it existed.
   *
   * @throws Exception
   *           if the test fails.
   */
  @Test
  public void testModifyOperationAsSambaAdmin() throws Exception
  {
    // Test entry
    Entry testEntry = TestCaseUtils.makeEntry("dn: uid=test.user2,o=test",
        "objectClass: top", "objectClass: person",
        "objectClass: organizationalPerson", "objectClass: inetOrgPerson",
        "objectClass: sambaSAMAccount", "uid: test.user", "cn: Test User",
        "givenName: Test", "sn: User", "sambaSID: 123",
        "userPassword: password");

    TestCaseUtils.addEntry(testEntry);

    // Perform the modify operation
    AuthenticationInfo authInfo = new AuthenticationInfo(
        TestCaseUtils.makeEntry("dn: cn=Samba Admin,o=test",
            "objectClass: top", "objectClass: person",
            "userPassword: password", "sn: Admin", "cn: Samba Admin",
            "ds-privilege-name: password-reset"), false);

    InternalClientConnection conn = new InternalClientConnection(authInfo);

    LinkedList<Modification> mods = newLinkedList(
        new Modification(ModificationType.REPLACE, Attributes.create("userPassword", "password1")));

    ModifyOperation modOp = conn.processModify(testEntry.getName(), mods);
    assertEquals(modOp.getResultCode(), ResultCode.SUCCESS);

    // Verification of the result

    Entry entry = DirectoryServer.getEntry(testEntry.getName());
    assertNotNull(entry);
    assertThat(entry.getAttribute("sambantpassword")).isEmpty();
    assertThat(entry.getAttribute("sambalmpassword")).isEmpty();

    TestCaseUtils.deleteEntry(entry);
  }



  /**
   * If multiple passwords are set, make sure only last one is synchronized.
   *
   * @throws Exception
   *           if the test fails.
   */
  @Test
  public void testModifyOperationMultiplePasswords() throws Exception
  {
    // Test entry
    Entry testEntry = TestCaseUtils.makeEntry("dn: uid=test.user4,o=test",
        "objectClass: top", "objectClass: person",
        "objectClass: organizationalPerson", "objectClass: inetOrgPerson",
        "objectClass: sambaSAMAccount", "uid: test.user", "cn: Test User",
        "givenName: Test", "sn: User", "sambaSID: 123",
        "userPassword: password");

    TestCaseUtils.addEntry(testEntry);

    // Perform the modify operation
    LinkedList<Modification> mods = new LinkedList<>();
    mods.add(new Modification(ModificationType.ADD, Attributes.create("userPassword", "password1")));
    mods.add(new Modification(ModificationType.ADD, Attributes.create("userPassword", "password2")));
    mods.add(new Modification(ModificationType.ADD, Attributes.create("userPassword", "password3")));

    ModifyOperation modOp = getRootConnection().processModify(testEntry.getName(), mods);
    assertEquals(modOp.getResultCode(), ResultCode.SUCCESS);

    // Verification of the result
    assertThatPasswordsAreEqualTo(testEntry,
        "bd7dfbf29a93f93c63cb84790da00e63", "e52cac67419a9a221b087c18752bdbee");
    TestCaseUtils.deleteEntry(testEntry);
  }



  /**
   * Return test authz IDs for password modify extended operations.
   *
   * @return The test authz IDs.
   */
  @DataProvider
  public Object[][] authzID()
  {
    return new Object[][] { { "uid=test.user,o=test" },
        { "dn:uid=test.user,o=test" }, { "u:test.user" } };
  }



  /**
   * Test the Password Modify Extended Operation as ROOT.
   *
   * @param authzID The authz ID.
   * @throws Exception
   *           if the test fails.
   */
  @Test(dataProvider="authzID")
  public void testPWEOAsRoot(String authzID) throws Exception
  {
    // Test entry
    Entry testEntry = TestCaseUtils.makeEntry("dn: uid=test.user,o=test",
        "objectClass: top", "objectClass: person",
        "objectClass: organizationalPerson", "objectClass: inetOrgPerson",
        "objectClass: sambaSAMAccount", "uid: test.user", "cn: Test User",
        "givenName: Test", "sn: User", "sambaSID: 123",
        "userPassword: password");

    TestCaseUtils.addEntry(testEntry);

    // Perform the extended operation
    InternalClientConnection conn = InternalClientConnection
        .getRootConnection();

    /*
     * Create the Password Modify Extended Operation request. It has the
     * following format: passwdModifyOID OBJECT IDENTIFIER ::=
     * 1.3.6.1.4.1.4203.1.11.1 PasswdModifyRequestValue ::= SEQUENCE {
     * userIdentity [0] OCTET STRING OPTIONAL oldPasswd [1] OCTET STRING
     * OPTIONAL newPasswd [2] OCTET STRING OPTIONAL }
     */
    ByteStringBuilder bsBuilder = new ByteStringBuilder();
    ASN1Writer writer = ASN1.getWriter(bsBuilder);

    // Start the sequence

    writer.writeStartSequence();

    // Write the authzID of the entry we are changing.
    writer.writeOctetString(ExtensionsConstants.TYPE_PASSWORD_MODIFY_USER_ID,
        authzID);

    /*
     * Since we perform the operation as ROOT, we don't have to put the old
     * password writer.writeOctetString(
     * ExtensionsConstants.TYPE_PASSWORD_MODIFY_OLD_PASSWORD, "");
     */

    // Write the new password
    writer.writeOctetString(
        ExtensionsConstants.TYPE_PASSWORD_MODIFY_NEW_PASSWORD, "password");

    // End the sequence

    writer.writeEndSequence();

    ExtendedOperation extOp = conn.processExtendedOperation(
        ServerConstants.OID_PASSWORD_MODIFY_REQUEST, bsBuilder.toByteString());

    assertSame(extOp.getResultCode(), ResultCode.SUCCESS);

    // Verification of the result
    assertThatPasswordsAreEqualTo(testEntry,
        "8846f7eaee8fb117ad06bdd830b7586c", "e52cac67419a9a224a3b108f3fa6cb6d");
    TestCaseUtils.deleteEntry(testEntry);
  }



  /**
   * Test the Password Modify Extended Operation as Samba administrative user.
   * This operation should be skipped.
   *
   * @param authzID The authz ID.
   * @throws Exception
   *           if the test fails.
   */
  @Test(dataProvider="authzID")
  public void testPWEOAsSambaAdmin(String authzID) throws Exception
  {
    // Test entry
    Entry testEntry = TestCaseUtils.makeEntry("dn: uid=test.user,o=test",
        "objectClass: top", "objectClass: person",
        "objectClass: organizationalPerson", "objectClass: inetOrgPerson",
        "objectClass: sambaSAMAccount", "uid: test.user", "cn: Test User",
        "givenName: Test", "sn: User", "sambaSID: 123",
        "userPassword: password");

    TestCaseUtils.addEntry(testEntry);

    // Perform the extended operation
    AuthenticationInfo authInfo = new AuthenticationInfo(
        TestCaseUtils.makeEntry("dn: cn=Samba Admin,o=test",
            "objectClass: top", "objectClass: person",
            "userPassword: password", "sn: Admin", "cn: Samba Admin",
            "ds-privilege-name: password-reset"), false);

    InternalClientConnection conn = new InternalClientConnection(authInfo);

    /*
     * Create the Password Modify Extended Operation request. It has the
     * following format: passwdModifyOID OBJECT IDENTIFIER ::=
     * 1.3.6.1.4.1.4203.1.11.1 PasswdModifyRequestValue ::= SEQUENCE {
     * userIdentity [0] OCTET STRING OPTIONAL oldPasswd [1] OCTET STRING
     * OPTIONAL newPasswd [2] OCTET STRING OPTIONAL }
     */
    ByteStringBuilder bsBuilder = new ByteStringBuilder();
    ASN1Writer writer = ASN1.getWriter(bsBuilder);

    // Start the sequence

    writer.writeStartSequence();

    // Write the DN of the entry we are changing.

    writer.writeOctetString(ExtensionsConstants.TYPE_PASSWORD_MODIFY_USER_ID,
        authzID);

    /*
     * Since we perform the operation as Samba admininistrative user, we don't
     * have to put the old password. writer.writeOctetString(
     * ExtensionsConstants.TYPE_PASSWORD_MODIFY_OLD_PASSWORD, "");
     */

    // Write the new password

    writer.writeOctetString(
        ExtensionsConstants.TYPE_PASSWORD_MODIFY_NEW_PASSWORD, "password");

    // End the sequence

    writer.writeEndSequence();

    ExtendedOperation extOp = conn.processExtendedOperation(
        ServerConstants.OID_PASSWORD_MODIFY_REQUEST, bsBuilder.toByteString());

    assertSame(extOp.getResultCode(), ResultCode.SUCCESS);

    // Verification of the result

    Entry entry = DirectoryServer.getEntry(testEntry.getName());
    assertNotNull(entry);
    assertThat(entry.getAttribute("sambantpassword")).isEmpty();
    assertThat(entry.getAttribute("sambalmpassword")).isEmpty();

    TestCaseUtils.deleteEntry(entry);
  }



  /**
   * Test the Password Modify Extended Operation as normal user.
   *
   * @param authzID The authz ID.
   * @throws Exception
   *           if the test fails.
   */
  @Test(dataProvider="authzID")
  public void testPWEOAsUser(String authzID) throws Exception
  {
    // Test entry
    Entry testEntry = TestCaseUtils.makeEntry("dn: uid=test.user,o=test",
        "objectClass: top", "objectClass: person",
        "objectClass: organizationalPerson", "objectClass: inetOrgPerson",
        "objectClass: sambaSAMAccount", "uid: test.user", "cn: Test User",
        "givenName: Test", "sn: User", "sambaSID: 123",
        "userPassword: password");

    TestCaseUtils.addEntry(testEntry);

    // Perform the extended operation
    AuthenticationInfo authInfo = new AuthenticationInfo(testEntry, false);

    InternalClientConnection conn = new InternalClientConnection(authInfo);

    /*
     * Create the Password Modify Extended Operation request. It has the
     * following format: passwdModifyOID OBJECT IDENTIFIER ::=
     * 1.3.6.1.4.1.4203.1.11.1 PasswdModifyRequestValue ::= SEQUENCE {
     * userIdentity [0] OCTET STRING OPTIONAL oldPasswd [1] OCTET STRING
     * OPTIONAL newPasswd [2] OCTET STRING OPTIONAL }
     */
    ByteStringBuilder bsBuilder = new ByteStringBuilder();
    ASN1Writer writer = ASN1.getWriter(bsBuilder);

    // Start the sequence

    writer.writeStartSequence();

    // Write the DN of the entry we are changing.

    writer.writeOctetString(ExtensionsConstants.TYPE_PASSWORD_MODIFY_USER_ID,
        authzID);

    // Write the old password

    writer.writeOctetString(
        ExtensionsConstants.TYPE_PASSWORD_MODIFY_OLD_PASSWORD, "password");

    // Write the new password

    writer.writeOctetString(
        ExtensionsConstants.TYPE_PASSWORD_MODIFY_NEW_PASSWORD, "password");

    // End the sequence

    writer.writeEndSequence();

    ExtendedOperation extOp = conn.processExtendedOperation(
        ServerConstants.OID_PASSWORD_MODIFY_REQUEST, bsBuilder.toByteString());

    assertSame(extOp.getResultCode(), ResultCode.SUCCESS);

    // Verification of the result
    assertThatPasswordsAreEqualTo(testEntry,
        "8846f7eaee8fb117ad06bdd830b7586c", "e52cac67419a9a224a3b108f3fa6cb6d");
    TestCaseUtils.deleteEntry(testEntry);
  }

  /**
   * Test if the plugin properly updates the modifications list with the
   * 'sambaPwdLastSetAttr' attribute.
   * @throws Exception
   */
  @Test
  public void testSambaPwdLastSetAttrThroughLDAPModify()
    throws Exception
  {
    SambaPasswordPlugin plugin = (SambaPasswordPlugin)
      DirectoryServer.getPluginConfigManager().getRegisteredPlugin(
        DN.valueOf("cn=samba password,cn=Plugins,cn=config"));

    TimeStampProvider testTimeStampProvider = new TimeStampProvider()
    {
      @Override
      public long getCurrentTime()
      {
        return 1339012789L;
      }

    };

    // Test entry
    Entry testEntry = TestCaseUtils.makeEntry(
      "dn: uid=test.user1,o=test",
        "objectClass: top",
        "objectClass: person",
        "objectClass: organizationalPerson",
        "objectClass: inetOrgPerson",
        "objectClass: sambaSAMAccount",
        "uid: test.user",
        "cn: Test User",
        "givenName: Test",
        "sn: User",
        "sambaSID: 123",
        "userPassword: password");

    try
    {
      plugin.setTimeStampProvider(testTimeStampProvider);

      TestCaseUtils.addEntry(testEntry);

      // Perform the modify operation
      InternalClientConnection conn = InternalClientConnection
          .getRootConnection();

      LinkedList<Modification> mods =
          newLinkedList(new Modification(ModificationType.REPLACE, Attributes.create("userPassword", "password")));

      ModifyOperation modOp = conn.processModify(testEntry.getName(), mods);
      assertEquals(modOp.getResultCode(), ResultCode.SUCCESS);

      Attribute sambaPwdLastSetAttr =
        Attributes.create("sambapwdlastset", String.valueOf(1339012789L));
      assertTrue(containsAttribute(modOp, sambaPwdLastSetAttr));

      TestCaseUtils.deleteEntry(testEntry);
    }
    finally
    {
      plugin.setTimeStampProvider(null);
    }
  }

  private boolean containsAttribute(ModifyOperation modOp, Attribute attr)
  {
    for (Modification mod : modOp.getModifications())
    {
      if (mod.getAttribute().equals(attr))
      {
        return true;
      }
    }
    return false;
  }

  /**
   * Test if the plugin properly updates the 'sambaPwdLastSet' attribute when
   * the password is changed through the PMEO.
   * @param authzID
   * @throws Exception
   */
  @Test(dataProvider="authzID")
  public void testSambaPwdLastSetAttrThroughPWEOA(String authzID)
    throws Exception
  {
    SambaPasswordPlugin plugin = (SambaPasswordPlugin)
      DirectoryServer.getPluginConfigManager().getRegisteredPlugin(
        DN.valueOf("cn=samba password,cn=Plugins,cn=config"));

    TimeStampProvider testTimeStampProvider = new TimeStampProvider()
    {
      @Override
      public long getCurrentTime()
      {
        return 1339012789L;
      }

    };

    // Test entry
    Entry testEntry = TestCaseUtils.makeEntry(
      "dn: uid=test.user,o=test",
       "objectClass: top",
       "objectClass: person",
       "objectClass: organizationalPerson",
       "objectClass: inetOrgPerson",
       "objectClass: sambaSAMAccount",
       "uid: test.user",
       "cn: Test User",
       "givenName: Test", "sn: User",
       "sambaSID: 123",
       "userPassword: password");

    try
    {
      plugin.setTimeStampProvider(testTimeStampProvider);

      TestCaseUtils.addEntry(testEntry);

      // Perform the extended operation
      InternalClientConnection conn = InternalClientConnection
          .getRootConnection();

      /*
       * Create the Password Modify Extended Operation request. It has the
       * following format: passwdModifyOID OBJECT IDENTIFIER ::=
       * 1.3.6.1.4.1.4203.1.11.1 PasswdModifyRequestValue ::= SEQUENCE {
       * userIdentity [0] OCTET STRING OPTIONAL oldPasswd [1] OCTET STRING
       * OPTIONAL newPasswd [2] OCTET STRING OPTIONAL }
       */
      ByteStringBuilder bsBuilder = new ByteStringBuilder();
      ASN1Writer writer = ASN1.getWriter(bsBuilder);

      // Start the sequence

      writer.writeStartSequence();

      // Write the authzID of the entry we are changing.
      writer.writeOctetString(ExtensionsConstants.TYPE_PASSWORD_MODIFY_USER_ID,
          authzID);

      /*
       * Since we perform the operation as ROOT, we don't have to put the old
       * password writer.writeOctetString(
       * ExtensionsConstants.TYPE_PASSWORD_MODIFY_OLD_PASSWORD, "");
       */

      // Write the new password
      writer.writeOctetString(
          ExtensionsConstants.TYPE_PASSWORD_MODIFY_NEW_PASSWORD, "password");

      // End the sequence

      writer.writeEndSequence();

      ExtendedOperation extOp = conn.processExtendedOperation(
          ServerConstants.OID_PASSWORD_MODIFY_REQUEST, bsBuilder.toByteString());

      assertSame(extOp.getResultCode(), ResultCode.SUCCESS);

      // Verification of the result

      Entry entry = DirectoryServer.getEntry(testEntry.getName());
      assertNotNull(entry);

      Attribute sambaPwdLastSetAttr =
        Attributes.create("sambapwdlastset", String.valueOf(1339012789L));

      assertThat(entry.getAttribute("sambapwdlastset")).contains(sambaPwdLastSetAttr);
      TestCaseUtils.deleteEntry(testEntry);
    }
    finally
    {
      plugin.setTimeStampProvider(null);
    }
  }
}
