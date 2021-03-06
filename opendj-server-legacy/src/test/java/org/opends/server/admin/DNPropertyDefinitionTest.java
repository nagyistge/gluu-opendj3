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
 * Portions Copyright 2014-2016 ForgeRock AS.
 */

package org.opends.server.admin;



import static org.testng.Assert.assertEquals;

import org.opends.server.TestCaseUtils;
import org.opends.server.DirectoryServerTestCase;
import org.opends.server.admin.std.meta.RootCfgDefn;
import org.forgerock.opendj.ldap.DN;
import org.opends.server.types.DirectoryException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;



/**
 * ClassPropertyDefinition Tester.
 */
public class DNPropertyDefinitionTest extends DirectoryServerTestCase {

  /**
   * Sets up tests
   *
   * @throws Exception
   *           If the server could not be initialized.
   */
  @BeforeClass
  public void setUp() throws Exception {
    // This test suite depends on having the schema available, so
    // we'll start the server.
    TestCaseUtils.startServer();
  }



  /**
   * @return data for testing
   */
  @DataProvider(name = "testBuilderSetBaseDN")
  public Object[][] createBuilderSetBaseDN() {
    return new Object[][] { { null },
        { "cn=key manager providers, cn=config" } };
  }



  /**
   * Tests builder.setBaseDN with valid data.
   *
   * @param baseDN
   *          The base DN.
   * @throws DirectoryException
   *           If the DN could not be decoded.
   */
  @Test(dataProvider = "testBuilderSetBaseDN")
  public void testBuilderSetBaseDN(String baseDN)
      throws DirectoryException {
    DNPropertyDefinition.Builder localBuilder = DNPropertyDefinition
        .createBuilder(RootCfgDefn.getInstance(), "test-property");
    localBuilder.setBaseDN(baseDN);
    DNPropertyDefinition pd = localBuilder.getInstance();

    DN actual = pd.getBaseDN();
    DN expected = baseDN == null ? null : DN.valueOf(baseDN);

    assertEquals(actual, expected);
  }



  /**
   * @return data for testing
   */
  @DataProvider(name = "testLegalValues")
  public Object[][] createLegalValues() {
    return new Object[][] {
        { null, "cn=config" },
        { null, "dc=example,dc=com" },
        { "", "cn=config" },
        { "cn=config", "cn=key manager providers, cn=config" },
        { "cn=key manager providers, cn=config",
            "cn=my provider, cn=key manager providers, cn=config" }, };
  }



  /**
   * @return data for testing
   */
  @DataProvider(name = "testIllegalValues")
  public Object[][] createIllegalValues() {
    return new Object[][] {
    // Above base DN.
        { "cn=config", "" },

        // Same as base DN.
        { "cn=config", "cn=config" },

        // Same as base DN.
        { "cn=key manager providers, cn=config",
            "cn=key manager providers, cn=config" },

        // Too far beneath base DN.
        { "cn=config",
            "cn=my provider, cn=key manager providers, cn=config" },

        // Unrelated to base DN.
        { "cn=config", "dc=example, dc=com" }, };
  }



  /**
   * Tests validation with valid data.
   *
   * @param baseDN
   *          The base DN.
   * @param value
   *          The value to be validated.
   * @throws DirectoryException
   *           If the DN could not be decoded.
   */
  @Test(dataProvider = "testLegalValues")
  public void testValidateLegalValues(String baseDN, String value)
      throws DirectoryException {
    DNPropertyDefinition.Builder localBuilder = DNPropertyDefinition
        .createBuilder(RootCfgDefn.getInstance(), "test-property");
    localBuilder.setBaseDN(baseDN);
    DNPropertyDefinition pd = localBuilder.getInstance();
    pd.validateValue(DN.valueOf(value));
  }



  /**
   * Tests validation with invalid data.
   *
   * @param baseDN
   *          The base DN.
   * @param value
   *          The value to be validated.
   * @throws DirectoryException
   *           If the DN could not be decoded.
   */
  @Test(dataProvider = "testIllegalValues", expectedExceptions = PropertyException.class)
  public void testValidateIllegalValues(String baseDN, String value)
      throws DirectoryException {
    DNPropertyDefinition.Builder localBuilder = DNPropertyDefinition
        .createBuilder(RootCfgDefn.getInstance(), "test-property");
    localBuilder.setBaseDN(baseDN);
    DNPropertyDefinition pd = localBuilder.getInstance();
    pd.validateValue(DN.valueOf(value));
  }



  /**
   * Tests decoding with valid data.
   *
   * @param baseDN
   *          The base DN.
   * @param value
   *          The value to be validated.
   */
  @Test(dataProvider = "testLegalValues")
  public void testDecodeLegalValues(String baseDN, String value) {
    DNPropertyDefinition.Builder localBuilder = DNPropertyDefinition
        .createBuilder(RootCfgDefn.getInstance(), "test-property");
    localBuilder.setBaseDN(baseDN);
    DNPropertyDefinition pd = localBuilder.getInstance();
    pd.decodeValue(value);
  }



  /**
   * Tests validation with invalid data.
   *
   * @param baseDN
   *          The base DN.
   * @param value
   *          The value to be validated.
   */
  @Test(dataProvider = "testIllegalValues", expectedExceptions = PropertyException.class)
  public void testDecodeIllegalValues(String baseDN, String value) {
    DNPropertyDefinition.Builder localBuilder = DNPropertyDefinition
        .createBuilder(RootCfgDefn.getInstance(), "test-property");
    localBuilder.setBaseDN(baseDN);
    DNPropertyDefinition pd = localBuilder.getInstance();
    pd.decodeValue(value);
  }
}
