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



import org.opends.server.DirectoryServerTestCase;
import org.opends.server.TestCaseUtils;
import org.forgerock.opendj.ldap.DN;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;



/**
 * AggregationPropertyDefinition Tester.
 */
@Test(sequential = true)
public class AggregationPropertyDefinitionTest extends DirectoryServerTestCase {

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
    TestCfg.setUp();
  }



  /**
   * Tears down test environment.
   */
  @AfterClass
  public void tearDown() {
    TestCfg.cleanup();
  }



  /**
   * Tests that the
   * {@link AggregationPropertyDefinition#normalizeValue(String)}
   * works.
   *
   * @throws Exception
   *           If the test unexpectedly fails.
   */
  @Test
  public void testNormalizeValue() throws Exception {
    TestChildCfgDefn d = TestChildCfgDefn.getInstance();
    AggregationPropertyDefinition<?, ?> pd = d
        .getAggregationPropertyPropertyDefinition();
    String nvalue = pd.normalizeValue("  LDAP   connection    handler  ");
    Assert.assertEquals(nvalue, "ldap connection handler");
  }



  /**
   * Tests that the
   * {@link AggregationPropertyDefinition#getChildDN(String)} works.
   *
   * @throws Exception
   *           If the test unexpectedly fails.
   */
  @Test
  public void testGetChildDN() throws Exception {
    TestChildCfgDefn d = TestChildCfgDefn.getInstance();
    AggregationPropertyDefinition<?, ?> pd = d
        .getAggregationPropertyPropertyDefinition();
    DN expected = DN
        .valueOf("cn=ldap connection handler, cn=connection handlers, cn=config");
    DN actual = pd.getChildDN("  LDAP  connection handler  ");
    Assert.assertEquals(actual, expected);
  }



  /**
   * Tests that the
   * {@link AggregationPropertyDefinition#getChildPath(String)} works.
   *
   * @throws Exception
   *           If the test unexpectedly fails.
   */
  @Test
  public void testGetChildPath() throws Exception {
    TestChildCfgDefn d = TestChildCfgDefn.getInstance();
    AggregationPropertyDefinition<?, ?> pd = d
        .getAggregationPropertyPropertyDefinition();
    ManagedObjectPath<?, ?> path = pd.getChildPath("LDAP connection handler");

    Assert.assertSame(path.getManagedObjectDefinition(), pd
        .getRelationDefinition().getChildDefinition());
    Assert.assertSame(path.getRelationDefinition(), pd.getRelationDefinition());
  }

}
