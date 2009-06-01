/*
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License, Version 1.0 only
 * (the "License").  You may not use this file except in compliance
 * with the License.
 *
 * You can obtain a copy of the license at
 * trunk/opends/resource/legal-notices/OpenDS.LICENSE
 * or https://OpenDS.dev.java.net/OpenDS.LICENSE.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each
 * file and include the License file at
 * trunk/opends/resource/legal-notices/OpenDS.LICENSE.  If applicable,
 * add the following below this CDDL HEADER, with the fields enclosed
 * by brackets "[]" replaced with your own identifying information:
 *      Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 *
 *
 *      Copyright 2009 Sun Microsystems, Inc.
 */

package org.opends.common.api.raw.request;


import org.opends.common.api.raw.RawAttribute;
import org.opends.server.core.operations.AddRequest;
import org.opends.server.core.operations.Schema;
import org.opends.server.types.DirectoryException;
import org.opends.server.types.OperationType;
import org.opends.server.util.Validator;

import java.util.ArrayList;
import java.util.List;



/**
 * A raw add request.
 */
public final class RawAddRequest extends RawRequest
{
  // The list of attributes associated with this request.
  private final List<RawAttribute> attributes =
      new ArrayList<RawAttribute>();

  // The DN of the entry to be added.
  private String dn;



  /**
   * Creates a new raw add request using the provided entry DN.
   * <p>
   * The new raw add request will contain an empty list of controls, and
   * an empty list of attributes.
   *
   * @param dn
   *          The raw, unprocessed entry DN for this add request.
   */
  public RawAddRequest(String dn)
  {
    super(OperationType.ADD);
    Validator.ensureNotNull(dn);
    this.dn = dn;
  }



  /**
   * Adds the provided attribute to the set of raw attributes for this
   * add request.
   *
   * @param attribute
   *          The attribute to add to the set of raw attributes for this
   *          add request.
   * @return This raw add request.
   */
  public RawAddRequest addAttribute(RawAttribute attribute)
  {
    Validator.ensureNotNull(attribute);
    attributes.add(attribute);
    return this;
  }



  /**
   * Returns the list of attributes in their raw, unparsed form as read
   * from the client request.
   * <p>
   * Some of these attributes may be invalid as no validation will have
   * been performed on them. Any modifications made to the returned
   * attribute {@code List} will be reflected in this add request.
   *
   * @return The list of attributes in their raw, unparsed form as read
   *         from the client request.
   */
  public Iterable<RawAttribute> getAttributes()
  {
    return attributes;
  }



  /**
   * Returns the raw, unprocessed entry DN as included in the request
   * from the client.
   * <p>
   * This may or may not contain a valid DN, as no validation will have
   * been performed.
   *
   * @return The raw, unprocessed entry DN as included in the request
   *         from the client.
   */
  public String getDN()
  {
    return dn;
  }



  /**
   * Sets the raw, unprocessed entry DN for this add request.
   * <p>
   * This may or may not contain a valid DN.
   *
   * @param dn
   *          The raw, unprocessed entry DN for this add request.
   * @return This raw add request.
   */
  public RawAddRequest setDN(String dn)
  {
    Validator.ensureNotNull(dn);
    this.dn = dn;
    return this;
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public AddRequest toRequest(Schema schema) throws DirectoryException
  {
    // TODO: not yet implemented.
    return null;
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public void toString(StringBuilder buffer)
  {
    buffer.append("AddRequest(entry=");
    buffer.append(dn);
    buffer.append(", attributes=");
    buffer.append(attributes);
    buffer.append(", controls=");
    buffer.append(getControls());
    buffer.append(")");
  }
}
