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

package org.opends.server.admin.server;



import org.forgerock.opendj.ldap.DN;
import org.opends.server.admin.LDAPProfile;
import org.opends.server.admin.ManagedObjectPath;
import org.opends.server.admin.RelationDefinition;



/**
 * A factory class for creating <code>DN</code>s from managed
 * object paths.
 */
final class DNBuilder {

  /**
   * Creates a new DN representing the specified managed object path.
   *
   * @param path
   *          The managed object path.
   * @return Returns a new DN representing the specified managed
   *         object path.
   */
  public static DN create(ManagedObjectPath<?, ?> path) {
    return path.toDN();
  }



  /**
   * Creates a new DN representing the specified managed object path
   * and relation.
   *
   * @param path
   *          The managed object path.
   * @param relation
   *          The child relation.
   * @return Returns a new DN representing the specified managed
   *         object path and relation.
   */
  public static DN create(ManagedObjectPath<?, ?> path,
      RelationDefinition<?, ?> relation) {
    DN dn = path.toDN();
    LDAPProfile profile = LDAPProfile.getInstance();
    DN localName = DN.valueOf(profile.getRelationRDNSequence(relation));
    return dn.child(localName);
  }



  /** Prevent instantiation. */
  private DNBuilder() {
    // No implementation required.
  }
}
