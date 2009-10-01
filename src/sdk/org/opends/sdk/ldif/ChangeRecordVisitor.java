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

package org.opends.sdk.ldif;



import org.opends.sdk.requests.AddRequest;
import org.opends.sdk.requests.DeleteRequest;
import org.opends.sdk.requests.ModifyDNRequest;
import org.opends.sdk.requests.ModifyRequest;



/**
 * A visitor of {@code ChangeRecord}s, in the style of the visitor
 * design pattern.
 * <p>
 * Classes implementing this interface can query change records in a
 * type-safe manner. When a visitor is passed to a change record's
 * accept method, the corresponding visit method most applicable to that
 * change record is invoked.
 *
 * @param <R>
 *          The return type of this visitor's methods. Use
 *          {@link java.lang.Void} for visitors that do not need to
 *          return results.
 * @param <P>
 *          The type of the additional parameter to this visitor's
 *          methods. Use {@link java.lang.Void} for visitors that do not
 *          need an additional parameter.
 */
public interface ChangeRecordVisitor<R, P>
{

  /**
   * Visits an {@code Add} change record.
   * @param p
   *          A visitor specified parameter.
   * @param change
   *          The {@code Add} change record.
   *
   * @return Returns a visitor specified result.
   */
  R visitChangeRecord(P p, AddRequest change);



  /**
   * Visits an {@code Delete} change record.
   * @param p
   *          A visitor specified parameter.
   * @param change
   *          The {@code Delete} change record.
   *
   * @return Returns a visitor specified result.
   */
  R visitChangeRecord(P p, DeleteRequest change);



  /**
   * Visits an {@code Modify} change record.
   * @param p
   *          A visitor specified parameter.
   * @param change
   *          The {@code Modify} change record.
   *
   * @return Returns a visitor specified result.
   */
  R visitChangeRecord(P p, ModifyRequest change);



  /**
   * Visits an {@code ModifyDN} change record.
   * @param p
   *          A visitor specified parameter.
   * @param change
   *          The {@code ModifyDN} change record.
   *
   * @return Returns a visitor specified result.
   */
  R visitChangeRecord(P p, ModifyDNRequest change);

}
