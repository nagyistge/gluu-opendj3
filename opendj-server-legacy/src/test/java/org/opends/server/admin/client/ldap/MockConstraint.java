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
package org.opends.server.admin.client.ldap;



import java.util.Collection;
import java.util.Collections;

import org.forgerock.i18n.LocalizableMessage;
import org.opends.server.admin.Constraint;
import org.opends.server.admin.ManagedObjectPath;
import org.opends.server.admin.client.AuthorizationException;
import org.opends.server.admin.client.ClientConstraintHandler;
import org.opends.server.admin.client.CommunicationException;
import org.opends.server.admin.client.ManagedObject;
import org.opends.server.admin.client.ManagementContext;
import org.opends.server.admin.server.ServerConstraintHandler;



/**
 * A mock constraint which can be configured to refuse various types
 * of operation.
 */
public final class MockConstraint extends Constraint {

  /**
   * Mock client constraint handler.
   */
  private class Handler extends ClientConstraintHandler {

    /** {@inheritDoc} */
    @Override
    public boolean isAddAcceptable(ManagementContext context,
        ManagedObject<?> managedObject, Collection<LocalizableMessage> unacceptableReasons)
        throws AuthorizationException, CommunicationException {
      if (!allowAdds) {
        unacceptableReasons.add(LocalizableMessage.raw("Adds not allowed"));
      }

      return allowAdds;
    }



    /** {@inheritDoc} */
    @Override
    public boolean isDeleteAcceptable(ManagementContext context,
        ManagedObjectPath<?, ?> path, Collection<LocalizableMessage> unacceptableReasons)
        throws AuthorizationException, CommunicationException {
      if (!allowDeletes) {
        unacceptableReasons.add(LocalizableMessage.raw("Deletes not allowed"));
      }

      return allowDeletes;
    }



    /** {@inheritDoc} */
    @Override
    public boolean isModifyAcceptable(ManagementContext context,
        ManagedObject<?> managedObject, Collection<LocalizableMessage> unacceptableReasons)
        throws AuthorizationException, CommunicationException {
      if (!allowModifies) {
        unacceptableReasons.add(LocalizableMessage.raw("Modifies not allowed"));
      }

      return allowModifies;
    }

  }

  /** Determines if add operations are allowed. */
  private final boolean allowAdds;

  /** Determines if modify operations are allowed. */
  private final boolean allowModifies;

  /** Determines if delete operations are allowed. */
  private final boolean allowDeletes;



  /**
   * Creates a new mock constraint.
   *
   * @param allowAdds
   *          Determines if add operations are allowed.
   * @param allowModifies
   *          Determines if modify operations are allowed.
   * @param allowDeletes
   *          Determines if delete operations are allowed.
   */
  public MockConstraint(boolean allowAdds, boolean allowModifies,
      boolean allowDeletes) {
    this.allowAdds = allowAdds;
    this.allowModifies = allowModifies;
    this.allowDeletes = allowDeletes;
  }



  /** {@inheritDoc} */
  public Collection<ClientConstraintHandler> getClientConstraintHandlers() {
    return Collections.<ClientConstraintHandler> singleton(new Handler());
  }



  /** {@inheritDoc} */
  public Collection<ServerConstraintHandler> getServerConstraintHandlers() {
    return Collections.emptySet();
  }

}
