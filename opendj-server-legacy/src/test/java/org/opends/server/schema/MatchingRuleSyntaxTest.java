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
 * Portions Copyright 2015 ForgeRock AS.
 */
package org.opends.server.schema;

import org.opends.server.api.AttributeSyntax;
import org.opends.server.util.RemoveOnceSDKSchemaIsUsed;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test the MatchingRuleSyntax.
 */
@RemoveOnceSDKSchemaIsUsed
@Test
public class MatchingRuleSyntaxTest extends AttributeSyntaxTest
{

  /** {@inheritDoc} */
  @Override
  protected AttributeSyntax getRule()
  {
    return new MatchingRuleSyntax();
  }

  /** {@inheritDoc} */
  @Override
  @DataProvider(name="acceptableValues")
  public Object[][] createAcceptableValues()
  {
    return new Object [][] {
          {"( 1.2.3.4 NAME 'fullMatchingRule' "
              + " DESC 'description of matching rule' OBSOLETE "
              + " SYNTAX 1.3.6.1.4.1.1466.115.121.1.17 )", true},
          {"( 1.2.3.4 NAME 'missing closing parenthesis' "
                + " DESC 'description of matching rule' "
                + " SYNTAX 1.3.6.1.4.1.1466.115.121.1.17 ", false},
    };
  }

}
