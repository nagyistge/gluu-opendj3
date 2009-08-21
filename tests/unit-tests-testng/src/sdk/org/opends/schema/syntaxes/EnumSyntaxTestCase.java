package org.opends.schema.syntaxes;

import org.opends.schema.*;
import org.opends.ldap.DecodeException;
import org.opends.server.types.ByteString;
import static org.opends.server.schema.SchemaConstants.OMR_OID_GENERIC_ENUM;
import org.opends.types.ConditionResult;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.Assert;

import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA.
 * User: boli
 * Date: Aug 21, 2009
 * Time: 3:56:25 PM
 * To change this template use File | Settings | File Templates.
 */
public class EnumSyntaxTestCase extends SyntaxTestCase
{
  /**
   * {@inheritDoc}
   */
  @Override
  protected Syntax getRule() throws SchemaException, DecodeException
  {
    SchemaBuilder builder = new SchemaBuilder();
    builder.addSyntax("3.3.3", "Day Of The Week",
        false, "monday", "tuesday", "wednesday", "thursday", "friday",
        "saturday", "sunday");
    return builder.toSchema().getSyntax("3.3.3");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @DataProvider(name="acceptableValues")
  public Object[][] createAcceptableValues()
  {
    return new Object [][] {
        {"arbit-day", false},
        {"wednesday", true},
    };
  }

  @Test(expectedExceptions=DecodeException.class)
  public void testDuplicateEnum() throws SchemaException, DecodeException
  {
    SchemaBuilder builder = new SchemaBuilder();
    builder.addSyntax("( 3.3.3  DESC 'Day Of The Week' " +
        " X-ENUM  ( 'monday' 'tuesday'   'wednesday'  'thursday'  'friday' " +
        " 'saturday' 'monday') )", true);
  }

  @Test
  public void testDecode() throws SchemaException, DecodeException
  {
    SchemaBuilder builder = new SchemaBuilder();
    builder.addSyntax("( 3.3.3  DESC 'Day Of The Week' " +
        " X-ENUM  ( 'monday' 'tuesday'   'wednesday'  'thursday'  'friday' " +
        " 'saturday' 'sunday') )", true);
    Schema schema = builder.toSchema();
    Syntax syntax = schema.getSyntax("3.3.3");
    OrderingMatchingRule rule = syntax.getOrderingMatchingRule();
    Assert.assertEquals(rule.valuesMatch(ByteString.valueOf("monday"),
        ByteString.valueOf("thursday")), ConditionResult.TRUE);
    Assert.assertEquals(rule.valuesMatch(ByteString.valueOf("tuesday"),
        ByteString.valueOf("monday")), ConditionResult.FALSE);
    Assert.assertNotNull(schema.getMatchingRule(OMR_OID_GENERIC_ENUM +
        ".3.3.3"));
  }
}
