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
 * Portions Copyright 2014-2015 ForgeRock AS.
 */

package org.opends.server.admin;
import org.forgerock.i18n.LocalizableMessage;



import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;



/**
 * A class for retrieving internationalized resource properties
 * associated with a managed object definition.
 * <p>
 * I18N resource properties are not available for the
 * {@link TopCfgDefn}.
 */
public final class ManagedObjectDefinitionI18NResource {

  /** Application-wide set of instances. */
  private static final Map<String, ManagedObjectDefinitionI18NResource> INSTANCES = new HashMap<>();

  /**
   * Gets the internationalized resource instance which can be used to
   * retrieve the localized descriptions for the managed objects and
   * their associated properties and relations.
   *
   * @return Returns the I18N resource instance.
   */
  public static ManagedObjectDefinitionI18NResource getInstance() {
    return getInstance("admin.messages");
  }



  /**
   * Gets the internationalized resource instance for the named
   * profile.
   *
   * @param profile
   *          The name of the profile.
   * @return Returns the I18N resource instance for the named profile.
   */
  public static ManagedObjectDefinitionI18NResource getInstanceForProfile(
      String profile) {
    return getInstance("admin.profiles." + profile);
  }



  /** Get a resource instance creating it if necessary. */
  private static synchronized ManagedObjectDefinitionI18NResource getInstance(
      String prefix) {
    ManagedObjectDefinitionI18NResource instance = INSTANCES.get(prefix);

    if (instance == null) {
      instance = new ManagedObjectDefinitionI18NResource(prefix);
      INSTANCES.put(prefix, instance);
    }

    return instance;
  }



  /** Mapping from definition to locale-based resource bundle. */
  private final Map<AbstractManagedObjectDefinition<?, ?>,
    Map<Locale, ResourceBundle>> resources;



  /** The resource name prefix. */
  private final String prefix;



  /** Private constructor. */
  private ManagedObjectDefinitionI18NResource(String prefix) {
    this.resources = new HashMap<>();
    this.prefix = prefix;
  }



  /**
   * Get the internationalized message associated with the specified
   * key in the default locale.
   *
   * @param d
   *          The managed object definition.
   * @param key
   *          The resource key.
   * @return Returns the internationalized message associated with the
   *         specified key in the default locale.
   * @throws MissingResourceException
   *           If the key was not found.
   * @throws UnsupportedOperationException
   *           If the provided managed object definition was the
   *           {@link TopCfgDefn}.
   */
  public LocalizableMessage getMessage(AbstractManagedObjectDefinition<?, ?> d, String key)
      throws MissingResourceException, UnsupportedOperationException {
    return getMessage(d, key, Locale.getDefault(), (String[]) null);
  }



  /**
   * Get the internationalized message associated with the specified
   * key and locale.
   *
   * @param d
   *          The managed object definition.
   * @param key
   *          The resource key.
   * @param locale
   *          The locale.
   * @return Returns the internationalized message associated with the
   *         specified key and locale.
   * @throws MissingResourceException
   *           If the key was not found.
   * @throws UnsupportedOperationException
   *           If the provided managed object definition was the
   *           {@link TopCfgDefn}.
   */
  public LocalizableMessage getMessage(AbstractManagedObjectDefinition<?, ?> d,
      String key, Locale locale) throws MissingResourceException,
      UnsupportedOperationException {
    return getMessage(d, key, locale, (String[]) null);
  }



  /**
   * Get the parameterized internationalized message associated with
   * the specified key and locale.
   *
   * @param d
   *          The managed object definition.
   * @param key
   *          The resource key.
   * @param locale
   *          The locale.
   * @param args
   *          Arguments that should be inserted into the retrieved
   *          message.
   * @return Returns the internationalized message associated with the
   *         specified key and locale.
   * @throws MissingResourceException
   *           If the key was not found.
   * @throws UnsupportedOperationException
   *           If the provided managed object definition was the
   *           {@link TopCfgDefn}.
   */
  public LocalizableMessage getMessage(AbstractManagedObjectDefinition<?, ?> d,
      String key, Locale locale, String... args)
      throws MissingResourceException, UnsupportedOperationException {
    ResourceBundle resource = getResourceBundle(d, locale);

    // TODO: use message framework directly
    if (args == null) {
      return LocalizableMessage.raw(resource.getString(key));
    } else {
      MessageFormat mf = new MessageFormat(resource.getString(key));
      return LocalizableMessage.raw(mf.format(args));
    }
  }



  /**
   * Get the parameterized internationalized message associated with
   * the specified key in the default locale.
   *
   * @param d
   *          The managed object definition.
   * @param key
   *          The resource key.
   * @param args
   *          Arguments that should be inserted into the retrieved
   *          message.
   * @return Returns the internationalized message associated with the
   *         specified key in the default locale.
   * @throws MissingResourceException
   *           If the key was not found.
   * @throws UnsupportedOperationException
   *           If the provided managed object definition was the
   *           {@link TopCfgDefn}.
   */
  public LocalizableMessage getMessage(AbstractManagedObjectDefinition<?, ?> d,
      String key, String... args) throws MissingResourceException,
      UnsupportedOperationException {
    return getMessage(d, key, Locale.getDefault(), args);
  }



  /**
   * Forcefully removes any resource bundles associated with the
   * provided definition and using the default locale.
   * <p>
   * This method is intended for internal testing only.
   *
   * @param d
   *          The managed object definition.
   */
  synchronized void removeResourceBundle(
      AbstractManagedObjectDefinition<?, ?> d) {
    removeResourceBundle(d, Locale.getDefault());
  }



  /**
   * Forcefully removes any resource bundles associated with the
   * provided definition and locale.
   * <p>
   * This method is intended for internal testing only.
   *
   * @param d
   *          The managed object definition.
   * @param locale
   *          The locale.
   */
  synchronized void removeResourceBundle(
      AbstractManagedObjectDefinition<?, ?> d, Locale locale) {
    // Get the locale resource mapping.
    Map<Locale, ResourceBundle> map = resources.get(d);
    if (map != null) {
      map.remove(locale);
    }
  }



  /**
   * Forcefully adds the provided resource bundle to this I18N
   * resource for the default locale.
   * <p>
   * This method is intended for internal testing only.
   *
   * @param d
   *          The managed object definition.
   * @param resoureBundle
   *          The resource bundle to be used.
   */
  synchronized void setResourceBundle(AbstractManagedObjectDefinition<?, ?> d,
      ResourceBundle resoureBundle) {
    setResourceBundle(d, Locale.getDefault(), resoureBundle);
  }



  /**
   * Forcefully adds the provided resource bundle to this I18N
   * resource.
   * <p>
   * This method is intended for internal testing only.
   *
   * @param d
   *          The managed object definition.
   * @param locale
   *          The locale.
   * @param resoureBundle
   *          The resource bundle to be used.
   */
  synchronized void setResourceBundle(AbstractManagedObjectDefinition<?, ?> d,
      Locale locale, ResourceBundle resoureBundle) {
    // First get the locale-resource mapping, creating it if
    // necessary.
    Map<Locale, ResourceBundle> map = resources.get(d);
    if (map == null) {
      map = new HashMap<>();
      resources.put(d, map);
    }

    // Add the resource bundle.
    map.put(locale, resoureBundle);
  }



  /**
   * Retrieve the resource bundle associated with a managed object and
   * locale, lazily loading it if necessary.
   */
  private synchronized ResourceBundle getResourceBundle(
      AbstractManagedObjectDefinition<?, ?> d, Locale locale)
      throws MissingResourceException, UnsupportedOperationException {
    if (d.isTop()) {
      throw new UnsupportedOperationException(
          "I18n resources are not available for the "
              + "Top configuration definition");
    }

    // First get the locale-resource mapping, creating it if
    // necessary.
    Map<Locale, ResourceBundle> map = resources.get(d);
    if (map == null) {
      map = new HashMap<>();
      resources.put(d, map);
    }

    // Now get the resource based on the locale, loading it if
    // necessary.
    ResourceBundle resourceBundle = map.get(locale);
    if (resourceBundle == null) {
      String baseName = prefix + "." + d.getClass().getName();
      resourceBundle = ResourceBundle.getBundle(baseName, locale,
          ClassLoaderProvider.getInstance().getClassLoader());
      map.put(locale, resourceBundle);
    }

    return resourceBundle;
  }
}
