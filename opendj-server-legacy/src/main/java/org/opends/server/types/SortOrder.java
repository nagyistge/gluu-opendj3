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
 * Portions Copyright 2014 ForgeRock AS.
 */
package org.opends.server.types;

/**
 * This class defines a data structure that defines a set of sort
 * criteria that may be used to order entries in a set of search
 * results.  The sort order object is comprised of one or more sort
 * keys, which indicate which attribute types should be used to
 * perform the sort and information about the ordering to use for
 * those attributes.  If the sort order has multiple sort keys, then
 * the first sort key will be used as the primary sort criteria, and
 * the second will only be used in cases where the values of the
 * attribute associated with the first sort key are equal, the third
 * will only be used if the first and second values are equal, etc.
 * If all of the sort key attributes for two entries are identical,
 * then the relative order for those entries is undefined.
 * <p>
 * FIXME: replace with the SDK's SortKey?
 */
@org.opends.server.types.PublicAPI(
     stability=org.opends.server.types.StabilityLevel.VOLATILE,
     mayInstantiate=true,
     mayExtend=false,
     mayInvoke=true)
public final class SortOrder
{
  /** The set of sort keys in this sort order. */
  private SortKey[] sortKeys;

  /**
   * Creates a new sort order with a single key.
   *
   * @param  sortKey  The sort key to use in this sort order.
   */
  public SortOrder(SortKey sortKey)
  {
    this.sortKeys = new SortKey[] { sortKey };
  }



  /**
   * Creates a new sort order with the provided set of sort keys.
   *
   * @param  sortKeys  The set of sort keys to use for this sort
   *                   order.
   */
  public SortOrder(SortKey... sortKeys)
  {
    this.sortKeys = new SortKey[sortKeys.length];
    System.arraycopy(sortKeys, 0, this.sortKeys, 0, sortKeys.length);
  }



  /**
   * Retrieves the sort keys for this sort order.
   *
   * @return  The sort keys for this sort order.
   */
  public SortKey[] getSortKeys()
  {
    return sortKeys;
  }



  /**
   * Retrieves a string representation of this sort order.
   *
   * @return  A string representation of this sort order.
   */
  @Override
  public String toString()
  {
    StringBuilder buffer = new StringBuilder();
    toString(buffer);
    return buffer.toString();
  }



  /**
   * Appends a string representation of this sort order to the
   * provided buffer.
   *
   * @param  buffer  The buffer to which the information should be
   *                 appended.
   */
  public void toString(StringBuilder buffer)
  {
    buffer.append("SortOrder(");

    if (sortKeys.length > 0)
    {
      sortKeys[0].toString(buffer);

      for (int i=1; i < sortKeys.length; i++)
      {
        buffer.append(",");
        sortKeys[i].toString(buffer);
      }
    }

    buffer.append(")");
  }

  /**
   * Retrieves the hash code for this sort order.
   *
   * @return  The hash code for this sort order.
   */
  @Override
  public int hashCode()
  {
    int hashCode = 0;
    for(SortKey sortKey : sortKeys)
    {
      hashCode += sortKey.hashCode();
    }
    return hashCode;
  }

  /**
   * Indicates whether this sort order is equal to the provided
   * object.
   *
   * @param  o  The object for which to make the determination.
   *
   * @return  <CODE>true</CODE> if the provide object is equal to this
   *          sort order, or <CODE>false</CODE> if it is not.
   */
  @Override
  public boolean equals(Object o)
  {
    if(o == null)
    {
      return false;
    }
    if (o == this)
    {
      return true;
    }
    if (! (o instanceof SortOrder))
    {
      return false;
    }

    SortOrder s = (SortOrder) o;
    if(sortKeys.length != s.sortKeys.length)
    {
      return false;
    }

    for(int i = 0; i < sortKeys.length; i++)
    {
      if(!sortKeys[i].equals(s.sortKeys[i]))
      {
        return false;
      }
    }
    return true;
  }
}

