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

import static org.opends.server.util.CollectionUtils.*;

import java.util.ArrayList;
import java.util.Set;

import org.forgerock.i18n.LocalizableMessage;
import org.forgerock.opendj.config.server.ConfigException;
import org.opends.server.admin.std.server.PluginCfg;
import org.opends.server.api.plugin.DirectoryServerPlugin;
import org.opends.server.api.plugin.PluginResult;
import org.opends.server.api.plugin.PluginType;
import org.opends.server.types.Attribute;
import org.forgerock.opendj.ldap.schema.AttributeType;
import org.opends.server.types.DirectoryException;
import org.opends.server.types.Modification;
import org.opends.server.types.ObjectClass;
import org.opends.server.types.operation.PreOperationAddOperation;
import org.opends.server.types.operation.PreOperationModifyOperation;

/**
 * This class defines a pre-operation plugin that can be used in add and modify
 * operations to make changes to the target operation during pre-op plugin
 * processing.  For add operations, it can add objectclasses, remove
 * objectclasses, replace attributes, or remove attributes.  For modify
 * operations, it can add modifications.
 */
public class UpdatePreOpPlugin
       extends DirectoryServerPlugin<PluginCfg>
{
  /**
   * The singleton instance of this test password validator.
   */
  private static UpdatePreOpPlugin instance;



  /** The set of attributes to set in the next add operation. */
  private ArrayList<Attribute> setAttributes;

  /**
   * The set of attribute types for attributes to remove from the next add
   * operation.
   */
  private ArrayList<AttributeType> removeAttributes;

  /** The set of objectclasses to add to the next add operation. */
  private ArrayList<ObjectClass> addObjectClasses;

  /** The set of objectclasses to remove from the next add operation. */
  private ArrayList<ObjectClass> removeObjectClasses;

  /** The set of modifications to add to the next modify operation. */
  private ArrayList<Modification> modifications;



  /**
   * Creates a new instance of this Directory Server plugin.  Every
   * plugin must implement a default constructor (it is the only one
   * that will be used to create plugins defined in the
   * configuration), and every plugin constructor must call
   * <CODE>super()</CODE> as its first element.
   */
  public UpdatePreOpPlugin()
  {
    super();
  }



  /** {@inheritDoc} */
  @Override
  public void initializePlugin(Set<PluginType> pluginTypes,
                               PluginCfg configuration)
         throws ConfigException
  {
    // This plugin may only be used as a pre-operation plugin.
    for (PluginType t : pluginTypes)
    {
      switch (t)
      {
        case PRE_OPERATION_ADD:
        case PRE_OPERATION_MODIFY:
          // This is fine.
          break;
        default:
          throw new ConfigException(LocalizableMessage.raw("Invalid plugin type " + t +
                                    " for update pre-op plugin."));
      }
    }

    // We assume that there is only one of these active at a time.
    instance = this;

    setAttributes       = new ArrayList<>();
    removeAttributes    = new ArrayList<>();
    addObjectClasses    = new ArrayList<>();
    removeObjectClasses = new ArrayList<>();
    modifications       = new ArrayList<>();
  }



  /** {@inheritDoc} */
  @Override
  public PluginResult.PreOperation
       doPreOperation(PreOperationAddOperation addOperation)
  {
    for (AttributeType t : removeAttributes)
    {
      addOperation.removeAttribute(t);
    }

    for (Attribute a : setAttributes)
    {
      addOperation.setAttribute(a.getAttributeDescription().getAttributeType(), newArrayList(a));
    }

    for (ObjectClass oc : removeObjectClasses)
    {
      addOperation.removeObjectClass(oc);
    }

    for (ObjectClass oc : addObjectClasses)
    {
      addOperation.addObjectClass(oc, oc.getPrimaryName());
    }

    return PluginResult.PreOperation.continueOperationProcessing();
  }



  /** {@inheritDoc} */
  @Override
  public PluginResult.PreOperation
       doPreOperation(PreOperationModifyOperation modifyOperation)
  {
    for (Modification m : modifications)
    {
      try
      {
        modifyOperation.addModification(m);
      }
      catch (DirectoryException de)
      {
        return PluginResult.PreOperation.stopProcessing(de.getResultCode(),
            de.getMessageObject(), de.getMatchedDN(), de.getReferralURLs());
      }
    }

    return PluginResult.PreOperation.continueOperationProcessing();
  }



  /**
   * Clears all of the updates currently in place.
   */
  public static void reset()
  {
    instance.setAttributes.clear();
    instance.removeAttributes.clear();
    instance.addObjectClasses.clear();
    instance.removeObjectClasses.clear();
    instance.modifications.clear();
  }



  /**
   * Adds the provided attribute to the set of attributes that will be set in
   * the next add operation.
   *
   * @param  attribute  The attribute to be set in the next add operation.
   */
  public static void addAttributeToSet(Attribute attribute)
  {
    instance.setAttributes.add(attribute);
  }



  /**
   * Adds the provided attribute type to the set of attributes that will be
   * removed from the next add operation.
   *
   * @param  attributeType  The attribute type to be removed in the next add
   *                        operation.
   */
  public static void addAttributeToRemove(AttributeType attributeType)
  {
    instance.removeAttributes.add(attributeType);
  }



  /**
   * Adds the provided objectclass to the set of objectclasses that will be
   * added to the next add operation.
   *
   * @param  objectClass  The objectclass to be added.
   */
  public static void addObjectClassToAdd(ObjectClass objectClass)
  {
    instance.addObjectClasses.add(objectClass);
  }



  /**
   * Adds the provided objectclass to the set of objectclasses that will be
   * removed from the next add operation.
   *
   * @param  objectClass  The objectclass to be added.
   */
  public static void addObjectClassToRemove(ObjectClass objectClass)
  {
    instance.removeObjectClasses.add(objectClass);
  }



  /**
   * Adds the provided modification so that it will be included in the next
   * modify operation.
   *
   * @param  modification  The modification to be added.
   */
  public static void addModification(Modification modification)
  {
    instance.modifications.add(modification);
  }
}

