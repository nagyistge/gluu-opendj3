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
package org.opends.server.config;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.management.AttributeList;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanParameterInfo;

import org.forgerock.i18n.LocalizableMessage;
import org.forgerock.i18n.slf4j.LocalizedLogger;
import org.forgerock.opendj.ldap.ByteString;
import org.forgerock.opendj.ldap.schema.Syntax;
import org.opends.server.core.DirectoryServer;
import org.opends.server.types.Attribute;
import org.opends.server.util.CollectionUtils;

import static org.opends.server.config.ConfigConstants.*;
import static org.opends.messages.ConfigMessages.*;

/**
 * This class defines a multi-choice configuration attribute, which can hold
 * zero or more string values.  A user-defined set of allowed values will be
 * enforced.
 */
@org.opends.server.types.PublicAPI(
     stability=org.opends.server.types.StabilityLevel.VOLATILE,
     mayInstantiate=true,
     mayExtend=false,
     mayInvoke=true)
public final class MultiChoiceConfigAttribute
       extends ConfigAttribute
{
  private static final LocalizedLogger logger = LocalizedLogger.getLoggerForThisClass();

  /** The set of active values for this attribute. */
  private List<String> activeValues;

  /** The set of pending values for this attribute. */
  private List<String> pendingValues;

  /** The set of allowed values for this attribute. */
  private Set<String> allowedValues;



  /**
   * Creates a new multi-choice configuration attribute stub with the provided
   * information but no values.  The values will be set using the
   * <CODE>setInitialValue</CODE> method.  No validation will be performed on
   * the set of allowed values.
   *
   * @param  name                 The name for this configuration attribute.
   * @param  description          The description for this configuration
   *                              attribute.
   * @param  isRequired           Indicates whether this configuration attribute
   *                              is required to have at least one value.
   * @param  isMultiValued        Indicates whether this configuration attribute
   *                              may have multiple values.
   * @param  requiresAdminAction  Indicates whether changes to this
   *                              configuration attribute require administrative
   *                              action before they will take effect.
   * @param  allowedValues        The set of allowed values for this attribute.
   *                              All values in this set should be represented
   *                              entirely in lowercase characters.
   */
  public MultiChoiceConfigAttribute(String name, LocalizableMessage description,
                                    boolean isRequired, boolean isMultiValued,
                                    boolean requiresAdminAction,
                                    Set<String> allowedValues)
  {
    super(name, description, isRequired, isMultiValued, requiresAdminAction);


    this.allowedValues = allowedValues;

    activeValues  = new ArrayList<>();
    pendingValues = activeValues;
  }



  /**
   * Creates a new multi-choice configuration attribute with the provided
   * information.  No validation will be performed on the provided value or the
   * set of allowed values.
   *
   * @param  name                 The name for this configuration attribute.
   * @param  description          The description for this configuration
   *                              attribute.
   * @param  isRequired           Indicates whether this configuration attribute
   *                              is required to have at least one value.
   * @param  isMultiValued        Indicates whether this configuration attribute
   *                              may have multiple values.
   * @param  requiresAdminAction  Indicates whether changes to this
   *                              configuration attribute require administrative
   *                              action before they will take effect.
   * @param  allowedValues        The set of allowed values for this attribute.
   *                              All values in this set should be represented
   *                              entirely in lowercase characters.
   * @param  value                The value for this string configuration
   *                              attribute.
   */
  public MultiChoiceConfigAttribute(String name, LocalizableMessage description,
                                    boolean isRequired, boolean isMultiValued,
                                    boolean requiresAdminAction,
                                    Set<String> allowedValues, String value)
  {
    super(name, description, isRequired, isMultiValued, requiresAdminAction,
          getValueSet(value));


    this.allowedValues = allowedValues;

    if (value == null)
    {
      activeValues = new ArrayList<>();
    }
    else
    {
      activeValues = CollectionUtils.newArrayList(value);
    }

    pendingValues = activeValues;
  }



  /**
   * Creates a new multi-choice configuration attribute with the provided
   * information.  No validation will be performed on the provided values or the
   * set of allowed values.
   *
   * @param  name                 The name for this configuration attribute.
   * @param  description          The description for this configuration
   *                              attribute.
   * @param  isRequired           Indicates whether this configuration attribute
   *                              is required to have at least one value.
   * @param  isMultiValued        Indicates whether this configuration attribute
   *                              may have multiple values.
   * @param  requiresAdminAction  Indicates whether changes to this
   *                              configuration attribute require administrative
   *                              action before they will take effect.
   * @param  allowedValues        The set of allowed values for this attribute.
   *                              All values in this set should be represented
   *                              entirely in lowercase characters.
   * @param  values               The set of values for this configuration
   *                              attribute.
   */
  public MultiChoiceConfigAttribute(String name, LocalizableMessage description,
                                    boolean isRequired, boolean isMultiValued,
                                    boolean requiresAdminAction,
                                    Set<String> allowedValues,
                                    List<String> values)
  {
    super(name, description, isRequired, isMultiValued, requiresAdminAction,
          getValueSet(values));


    this.allowedValues = allowedValues;

    activeValues  = values != null ? values : new ArrayList<String>();
    pendingValues = activeValues;
  }



  /**
   * Creates a new multi-choice configuration attribute with the provided
   * information.  No validation will be performed on the provided values or the
   * set of allowed values.
   *
   * @param  name                 The name for this configuration attribute.
   * @param  description          The description for this configuration
   *                              attribute.
   * @param  isRequired           Indicates whether this configuration attribute
   *                              is required to have at least one value.
   * @param  isMultiValued        Indicates whether this configuration attribute
   *                              may have multiple values.
   * @param  requiresAdminAction  Indicates whether changes to this
   *                              configuration attribute require administrative
   *                              action before they will take effect.
   * @param  allowedValues        The set of allowed values for this attribute.
   *                              All values in this set should be represented
   *                              entirely in lowercase characters.
   * @param  activeValues         The set of active values for this
   *                              configuration attribute.
   * @param  pendingValues        The set of pending values for this
   *                              configuration attribute.
   */
  public MultiChoiceConfigAttribute(String name, LocalizableMessage description,
                                    boolean isRequired, boolean isMultiValued,
                                    boolean requiresAdminAction,
                                    Set<String> allowedValues,
                                    List<String> activeValues,
                                    List<String> pendingValues)
  {
    super(name, description, isRequired, isMultiValued, requiresAdminAction,
          getValueSet(activeValues), (pendingValues != null),
          getValueSet(pendingValues));


    this.allowedValues = allowedValues;

    if (activeValues == null)
    {
      this.activeValues = new ArrayList<>();
    }
    else
    {
      this.activeValues = activeValues;
    }

    if (pendingValues == null)
    {
      this.pendingValues = this.activeValues;
    }
    else
    {
      this.pendingValues = pendingValues;
    }
  }



  /**
   * Retrieves the name of the data type for this configuration attribute.  This
   * is for informational purposes (e.g., inclusion in method signatures and
   * other kinds of descriptions) and does not necessarily need to map to an
   * actual Java type.
   *
   * @return  The name of the data type for this configuration attribute.
   */
  @Override
  public String getDataType()
  {
    return "MultiChoice";
  }



  /**
   * Retrieves the attribute syntax for this configuration attribute.
   *
   * @return  The attribute syntax for this configuration attribute.
   */
  @Override
  public Syntax getSyntax()
  {
    return DirectoryServer.getDefaultStringSyntax();
  }



  /**
   * Retrieves the active value for this configuration attribute as a string.
   * This is only valid for single-valued attributes that have a value.
   *
   * @return  The active value for this configuration attribute as a string.
   *
   * @throws  ConfigException  If this attribute does not have exactly one
   *                           active value.
   */
  public String activeValue() throws ConfigException
  {
    if (activeValues == null || activeValues.isEmpty())
    {
      throw new ConfigException(ERR_CONFIG_ATTR_NO_STRING_VALUE.get(getName()));
    }
    if (activeValues.size() > 1)
    {
      throw new ConfigException(ERR_CONFIG_ATTR_MULTIPLE_STRING_VALUES.get(getName()));
    }

    return activeValues.get(0);
  }



  /**
   * Retrieves the set of active values for this configuration attribute.
   *
   * @return  The set of active values for this configuration attribute.
   */
  public List<String> activeValues()
  {
    return activeValues;
  }



  /**
   * Retrieves the pending value for this configuration attribute as a string.
   * This is only valid for single-valued attributes that have a value.  If this
   * attribute does not have any pending values, then the active value will be
   * returned.
   *
   * @return  The pending value for this configuration attribute as a string.
   *
   * @throws  ConfigException  If this attribute does not have exactly one
   *                           pending value.
   */
  public String pendingValue()
         throws ConfigException
  {
    if (! hasPendingValues())
    {
      return activeValue();
    }

    if (pendingValues == null || pendingValues.isEmpty())
    {
      throw new ConfigException(ERR_CONFIG_ATTR_NO_STRING_VALUE.get(getName()));
    }
    if (pendingValues.size() > 1)
    {
      throw new ConfigException(ERR_CONFIG_ATTR_MULTIPLE_STRING_VALUES.get(getName()));
    }

    return pendingValues.get(0);
  }



  /**
   * Retrieves the set of pending values for this configuration attribute.  If
   * there are no pending values, then the set of active values will be
   * returned.
   *
   * @return  The set of pending values for this configuration attribute.
   */
  public List<String> pendingValues()
  {
    if (! hasPendingValues())
    {
      return activeValues;
    }

    return pendingValues;
  }



  /**
   * Retrieves the set of allowed values that may be used for this configuration
   * attribute.  The set of allowed values may be modified by the caller.
   *
   * @return  The set of allowed values that may be used for this configuration
   *          attribute.
   */
  public Set<String> allowedValues()
  {
    return allowedValues;
  }



  /**
   * Sets the value for this string configuration attribute.
   *
   * @param  value  The value for this string configuration attribute.
   *
   * @throws  ConfigException  If the provided value is not acceptable.
   */
  public void setValue(String value)
         throws ConfigException
  {
    if (value == null || value.length() == 0)
    {
      LocalizableMessage message = ERR_CONFIG_ATTR_EMPTY_STRING_VALUE.get(getName());
      throw new ConfigException(message);
    }

    if (! allowedValues.contains(value.toLowerCase()))
    {
      LocalizableMessage message = ERR_CONFIG_ATTR_VALUE_NOT_ALLOWED.get(value, getName());
      throw new ConfigException(message);
    }

    if (requiresAdminAction())
    {
      pendingValues = CollectionUtils.newArrayList(value);
      setPendingValues(getValueSet(value));
    }
    else
    {
      activeValues.clear();
      activeValues.add(value);
      pendingValues = activeValues;
      setActiveValues(getValueSet(value));
    }
  }



  /**
   * Sets the values for this string configuration attribute.
   *
   * @param  values  The set of values for this string configuration attribute.
   *
   * @throws  ConfigException  If the provided value set or any of the
   *                           individual values are not acceptable.
   */
  public void setValues(List<String> values)
         throws ConfigException
  {
    // First check if the set is empty and if that is allowed.
    if (values == null || values.isEmpty())
    {
      if (isRequired())
      {
        throw new ConfigException(ERR_CONFIG_ATTR_IS_REQUIRED.get(getName()));
      }

      if (requiresAdminAction())
      {
        setPendingValues(new LinkedHashSet<ByteString>(0));
        pendingValues = new ArrayList<>();
      }
      else
      {
        setActiveValues(new LinkedHashSet<ByteString>(0));
        activeValues.clear();
      }
    }


    // Next check if the set contains multiple values and if that is allowed.
    int numValues = values.size();
    if (!isMultiValued() && numValues > 1)
    {
      throw new ConfigException(ERR_CONFIG_ATTR_SET_VALUES_IS_SINGLE_VALUED.get(getName()));
    }


    // Iterate through all the provided values, make sure that they are
    // acceptable, and build the value set.
    LinkedHashSet<ByteString> valueSet = new LinkedHashSet<>(numValues);
    for (String value : values)
    {
      if (value == null || value.length() == 0)
      {
        throw new ConfigException(ERR_CONFIG_ATTR_EMPTY_STRING_VALUE.get(getName()));
      }
      if (!allowedValues.contains(value.toLowerCase()))
      {
        throw new ConfigException(ERR_CONFIG_ATTR_VALUE_NOT_ALLOWED.get(value, getName()));
      }

      ByteString attrValue = ByteString.valueOfUtf8(value);
      if (valueSet.contains(attrValue))
      {
        throw new ConfigException(ERR_CONFIG_ATTR_ADD_VALUES_ALREADY_EXISTS.get(getName(), value));
      }

      valueSet.add(attrValue);
    }


    // Apply this value set to the new active or pending value set.
    if (requiresAdminAction())
    {
      pendingValues = values;
      setPendingValues(valueSet);
    }
    else
    {
      activeValues  = values;
      pendingValues = activeValues;
      setActiveValues(valueSet);
    }
  }

  /**
   * Applies the set of pending values, making them the active values for this
   * configuration attribute.  This will not take any action if there are no
   * pending values.
   */
  @Override
  public void applyPendingValues()
  {
    if (! hasPendingValues())
    {
      return;
    }

    super.applyPendingValues();
    activeValues = pendingValues;
  }



  /**
   * Indicates whether the provided value is acceptable for use in this
   * attribute.  If it is not acceptable, then the reason should be written into
   * the provided buffer.
   *
   * @param  value         The value for which to make the determination.
   * @param  rejectReason  A buffer into which a human-readable reason for the
   *                       reject may be written.
   *
   * @return  <CODE>true</CODE> if the provided value is acceptable for use in
   *          this attribute, or <CODE>false</CODE> if not.
   */
  @Override
  public boolean valueIsAcceptable(ByteString value,
                                   StringBuilder rejectReason)
  {
    // Make sure that the value is non-empty.
    String stringValue;
    if (value == null || (stringValue = value.toString()).length() == 0)
    {
      rejectReason.append(ERR_CONFIG_ATTR_EMPTY_STRING_VALUE.get(getName()));
      return false;
    }


    // Make sure that the value is in the allowed value set.
    if (! allowedValues.contains(stringValue.toLowerCase()))
    {
      rejectReason.append(ERR_CONFIG_ATTR_VALUE_NOT_ALLOWED.get(stringValue, getName()));
      return false;
    }


    return true;
  }



  /**
   * Converts the provided set of strings to a corresponding set of attribute
   * values.
   *
   * @param  valueStrings   The set of strings to be converted into attribute
   *                        values.
   * @param  allowFailures  Indicates whether the decoding process should allow
   *                        any failures in which one or more values could be
   *                        decoded but at least one could not.  If this is
   *                        <CODE>true</CODE> and such a condition is acceptable
   *                        for the underlying attribute type, then the returned
   *                        set of values should simply not include those
   *                        undecodable values.
   *
   * @return  The set of attribute values converted from the provided strings.
   *
   * @throws  ConfigException  If an unrecoverable problem occurs while
   *                           performing the conversion.
   */
  @Override
  public LinkedHashSet<ByteString>
              stringsToValues(List<String> valueStrings, boolean allowFailures)
         throws ConfigException
  {
    if (valueStrings == null || valueStrings.isEmpty())
    {
      if (isRequired())
      {
        throw new ConfigException(ERR_CONFIG_ATTR_IS_REQUIRED.get(getName()));
      }
      return new LinkedHashSet<>();
    }

    int numValues = valueStrings.size();
    if (!isMultiValued() && numValues > 1)
    {
      throw new ConfigException(ERR_CONFIG_ATTR_SET_VALUES_IS_SINGLE_VALUED.get(getName()));
    }

    LinkedHashSet<ByteString> valueSet = new LinkedHashSet<>(numValues);
    for (String valueString : valueStrings)
    {
      if (valueString == null || valueString.length() == 0)
      {
        reportError(allowFailures, ERR_CONFIG_ATTR_EMPTY_STRING_VALUE.get(getName()));
        continue;
      }
      if (! allowedValues.contains(valueString.toLowerCase()))
      {
        reportError(allowFailures, ERR_CONFIG_ATTR_VALUE_NOT_ALLOWED.get(valueString, getName()));
        continue;
      }

      valueSet.add(ByteString.valueOfUtf8(valueString));
    }

    // If this method was configured to continue on error, then it is possible
    // that we ended up with an empty list.  Check to see if this is a required
    // attribute and if so deal with it accordingly.
    if (isRequired() && valueSet.isEmpty())
    {
      LocalizableMessage message = ERR_CONFIG_ATTR_IS_REQUIRED.get(getName());
      throw new ConfigException(message);
    }

    return valueSet;
  }

  private void reportError(boolean allowFailures, LocalizableMessage message) throws ConfigException
  {
    if (!allowFailures)
    {
      throw new ConfigException(message);
    }
    logger.error(message);
  }

  /**
   * Converts the set of active values for this configuration attribute into a
   * set of strings that may be stored in the configuration or represented over
   * protocol.  The string representation used by this method should be
   * compatible with the decoding used by the <CODE>stringsToValues</CODE>
   * method.
   *
   * @return The string representations of the set of active values for this configuration attribute.
   */
  @Override
  public List<String> activeValuesToStrings()
  {
    return activeValues;
  }



  /**
   * Converts the set of pending values for this configuration attribute into a
   * set of strings that may be stored in the configuration or represented over
   * protocol.  The string representation used by this method should be
   * compatible with the decoding used by the <CODE>stringsToValues</CODE>
   * method.
   *
   * @return  The string representations of the set of pending values for this
   *          configuration attribute, or <CODE>null</CODE> if there are no
   *          pending values.
   */
  @Override
  public List<String> pendingValuesToStrings()
  {
    if (hasPendingValues())
    {
      return pendingValues;
    }
    return null;
  }



  /**
   * Retrieves a new configuration attribute of this type that will contain the
   * values from the provided attribute.
   *
   * @param  attributeList  The list of attributes to use to create the config
   *                        attribute.  The list must contain either one or two
   *                        elements, with both attributes having the same base
   *                        name and the only option allowed is ";pending" and
   *                        only if this attribute is one that requires admin
   *                        action before a change may take effect.
   *
   * @return  The generated configuration attribute.
   *
   * @throws  ConfigException  If the provided attribute cannot be treated as a
   *                           configuration attribute of this type (e.g., if
   *                           one or more of the values of the provided
   *                           attribute are not suitable for an attribute of
   *                           this type, or if this configuration attribute is
   *                           single-valued and the provided attribute has
   *                           multiple values).
   */
  @Override
  public ConfigAttribute getConfigAttribute(List<Attribute> attributeList)
         throws ConfigException
  {
    ArrayList<String> activeValues  = null;
    ArrayList<String> pendingValues = null;

    for (Attribute a : attributeList)
    {
      if (a.hasOptions())
      {
        // This must be the pending value.
        if (a.hasOption(OPTION_PENDING_VALUES))
        {
          if (pendingValues != null)
          {
            // We cannot have multiple pending value sets.
            LocalizableMessage message =
                ERR_CONFIG_ATTR_MULTIPLE_PENDING_VALUE_SETS.get(a.getName());
            throw new ConfigException(message);
          }


          if (a.isEmpty())
          {
            if (isRequired())
            {
              // This is illegal -- it must have a value.
              throw new ConfigException(ERR_CONFIG_ATTR_IS_REQUIRED.get(a.getName()));
            }
            // This is fine. The pending value set can be empty.
            pendingValues = new ArrayList<>(0);
          }
          else
          {
            int numValues = a.size();
            if (numValues > 1 && !isMultiValued())
            {
              // This is illegal -- the attribute is single-valued.
              LocalizableMessage message =
                  ERR_CONFIG_ATTR_SET_VALUES_IS_SINGLE_VALUED.get(a.getName());
              throw new ConfigException(message);
            }

            pendingValues = new ArrayList<>(numValues);
            for (ByteString v : a)
            {
              String lowerValue = v.toString().toLowerCase();
              if (! allowedValues.contains(lowerValue))
              {
                // This is illegal -- the value is not allowed.
                throw new ConfigException(ERR_CONFIG_ATTR_VALUE_NOT_ALLOWED.get(v, a.getName()));
              }

              pendingValues.add(v.toString());
            }
          }
        }
        else
        {
          // This is illegal -- only the pending option is allowed for
          // configuration attributes.
          LocalizableMessage message =
              ERR_CONFIG_ATTR_OPTIONS_NOT_ALLOWED.get(a.getName());
          throw new ConfigException(message);
        }
      }
      else
      {
        // This must be the active value.
        if (activeValues!= null)
        {
          // We cannot have multiple active value sets.
          LocalizableMessage message =
              ERR_CONFIG_ATTR_MULTIPLE_ACTIVE_VALUE_SETS.get(a.getName());
          throw new ConfigException(message);
        }


        if (a.isEmpty())
        {
          if (isRequired())
          {
            // This is illegal -- it must have a value.
            LocalizableMessage message = ERR_CONFIG_ATTR_IS_REQUIRED.get(a.getName());
            throw new ConfigException(message);
          }
          // This is fine. The active value set can be empty.
          activeValues = new ArrayList<>(0);
        }
        else
        {
          int numValues = a.size();
          if (numValues > 1 && ! isMultiValued())
          {
            // This is illegal -- the attribute is single-valued.
            LocalizableMessage message =
                ERR_CONFIG_ATTR_SET_VALUES_IS_SINGLE_VALUED.get(a.getName());
            throw new ConfigException(message);
          }

          activeValues = new ArrayList<>(numValues);
          for (ByteString v : a)
          {
            String lowerValue = v.toString().toLowerCase();
            if (! allowedValues.contains(lowerValue))
            {
              // This is illegal -- the value is not allowed.
              throw new ConfigException(ERR_CONFIG_ATTR_VALUE_NOT_ALLOWED.get(v, a.getName()));
            }

            activeValues.add(v.toString());
          }
        }
      }
    }

    if (activeValues == null)
    {
      // This is not OK.  The value set must contain an active value.
      LocalizableMessage message = ERR_CONFIG_ATTR_NO_ACTIVE_VALUE_SET.get(getName());
      throw new ConfigException(message);
    }

    if (pendingValues == null)
    {
      // This is OK.  We'll just use the active value set.
      pendingValues = activeValues;
    }

    return new MultiChoiceConfigAttribute(getName(), getDescription(),
                                          isRequired(), isMultiValued(),
                                          requiresAdminAction(), allowedValues,
                                          activeValues, pendingValues);
  }



  /**
   * Retrieves a JMX attribute containing the active value set for this
   * configuration attribute (active or pending).
   *
   * @param pending indicates if pending or active  values are required.
   *
   * @return  A JMX attribute containing the active value set for this
   *          configuration attribute, or <CODE>null</CODE> if it does not have
   *          any active values.
   */
  private javax.management.Attribute _toJMXAttribute(boolean pending)
  {
    List<String> requestedValues ;
    String name ;
    if (pending)
    {
        requestedValues = pendingValues ;
        name = getName() + ";" + OPTION_PENDING_VALUES ;
    }
    else
    {
        requestedValues = activeValues ;
        name = getName() ;
    }

    if (isMultiValued())
    {
      String[] values = new String[requestedValues.size()];
      requestedValues.toArray(values);

      return new javax.management.Attribute(name, values);
    }
    else if (!requestedValues.isEmpty())
    {
      return new javax.management.Attribute(name, requestedValues.get(0));
    }
    return null;
  }

  /**
   * Retrieves a JMX attribute containing the active value set for this
   * configuration attribute.
   *
   * @return  A JMX attribute containing the active value set for this
   *          configuration attribute, or <CODE>null</CODE> if it does not have
   *          any active values.
   */
  @Override
  public javax.management.Attribute toJMXAttribute()
  {
      return _toJMXAttribute(false) ;
  }

  /**
   * Retrieves a JMX attribute containing the pending value set for this
   * configuration attribute.
   *
   * @return  A JMX attribute containing the pending value set for this
   *          configuration attribute, or <CODE>null</CODE> if it does not have
   *          any active values.
   */
  @Override
  public javax.management.Attribute toJMXAttributePending()
  {
    return _toJMXAttribute(true) ;
  }


  /**
   * Adds information about this configuration attribute to the provided JMX
   * attribute list.  If this configuration attribute requires administrative
   * action before changes take effect and it has a set of pending values, then
   * two attributes should be added to the list -- one for the active value
   * and one for the pending value.  The pending value should be named with
   * the pending option.
   *
   * @param  attributeList  The attribute list to which the JMX attribute(s)
   *                        should be added.
   */
  @Override
  public void toJMXAttribute(AttributeList attributeList)
  {
    if (!activeValues.isEmpty())
    {
      if (isMultiValued())
      {
        String[] values = new String[activeValues.size()];
        activeValues.toArray(values);

        attributeList.add(new javax.management.Attribute(getName(), values));
      }
      else
      {
        attributeList.add(new javax.management.Attribute(getName(),
                                                         activeValues.get(0)));
      }
    }
    else
    {
      if (isMultiValued())
      {
        attributeList.add(new javax.management.Attribute(getName(),
                                                         new String[0]));
      }
      else
      {
        attributeList.add(new javax.management.Attribute(getName(), null));
      }
    }


    if (requiresAdminAction() && pendingValues != null && pendingValues != activeValues)
    {
      String name = getName() + ";" + OPTION_PENDING_VALUES;

      if (isMultiValued())
      {
        String[] values = new String[pendingValues.size()];
        pendingValues.toArray(values);

        attributeList.add(new javax.management.Attribute(name, values));
      }
      else if (! pendingValues.isEmpty())
      {
        attributeList.add(new javax.management.Attribute(name, pendingValues.get(0)));
      }
    }
  }



  /**
   * Adds information about this configuration attribute to the provided list in
   * the form of a JMX <CODE>MBeanAttributeInfo</CODE> object.  If this
   * configuration attribute requires administrative action before changes take
   * effect and it has a set of pending values, then two attribute info objects
   * should be added to the list -- one for the active value (which should be
   * read-write) and one for the pending value (which should be read-only).  The
   * pending value should be named with the pending option.
   *
   * @param  attributeInfoList  The list to which the attribute information
   *                            should be added.
   */
  @Override
  public void toJMXAttributeInfo(List<MBeanAttributeInfo> attributeInfoList)
  {
    attributeInfoList.add(new MBeanAttributeInfo(getName(), getType(),
        String.valueOf(getDescription()), true, true, false));

    if (requiresAdminAction())
    {
      String name = getName() + ";" + OPTION_PENDING_VALUES;
      attributeInfoList.add(new MBeanAttributeInfo(name, getType(),
          String.valueOf(getDescription()), true, false, false));
    }
  }



  /**
   * Retrieves a JMX <CODE>MBeanParameterInfo</CODE> object that describes this
   * configuration attribute.
   *
   * @return  A JMX <CODE>MBeanParameterInfo</CODE> object that describes this
   *          configuration attribute.
   */
  @Override
  public MBeanParameterInfo toJMXParameterInfo()
  {
    return new MBeanParameterInfo(getName(), getType(), String.valueOf(getDescription()));
  }

  private String getType()
  {
    return isMultiValued() ? JMX_TYPE_STRING_ARRAY : String.class.getName();
  }

  /**
   * Attempts to set the value of this configuration attribute based on the
   * information in the provided JMX attribute.
   *
   * @param  jmxAttribute  The JMX attribute to use to attempt to set the value
   *                       of this configuration attribute.
   *
   * @throws  ConfigException  If the provided JMX attribute does not have an
   *                           acceptable value for this configuration
   *                           attribute.
   */
  @Override
  public void setValue(javax.management.Attribute jmxAttribute)
         throws ConfigException
  {
    Object value = jmxAttribute.getValue();
    if (value instanceof String)
    {
      setValue((String) value);
    }
    else if (value.getClass().isArray())
    {
      String componentType = value.getClass().getComponentType().getName();
      int length = Array.getLength(value);

      if (componentType.equals(String.class.getName()))
      {
        try
        {
          ArrayList<String> values = new ArrayList<>(length);

          for (int i=0; i < length; i++)
          {
            values.add((String) Array.get(value, i));
          }

          setValues(values);
        }
        catch (ConfigException ce)
        {
          logger.traceException(ce);

          throw ce;
        }
        catch (Exception e)
        {
          logger.traceException(e);

          throw new ConfigException(ERR_CONFIG_ATTR_INVALID_STRING_VALUE.get(getName(), value, e), e);
        }
      }
      else
      {
        LocalizableMessage message =
            ERR_CONFIG_ATTR_STRING_INVALID_ARRAY_TYPE.get(
                    getName(), componentType);
        throw new ConfigException(message);
      }
    }
    else
    {
      throw new ConfigException(ERR_CONFIG_ATTR_STRING_INVALID_TYPE.get(value, getName(), value.getClass().getName()));
    }
  }



  /**
   * Creates a duplicate of this configuration attribute.
   *
   * @return  A duplicate of this configuration attribute.
   */
  @Override
  public ConfigAttribute duplicate()
  {
    return new MultiChoiceConfigAttribute(getName(), getDescription(),
                                          isRequired(), isMultiValued(),
                                          requiresAdminAction(), allowedValues,
                                          activeValues, pendingValues);
  }
}
