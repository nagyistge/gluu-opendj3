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

package org.opends.sdk.ldap;



import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.opends.sdk.Connection;
import org.opends.sdk.ErrorResultException;
import org.opends.sdk.ResultCode;
import org.opends.sdk.requests.Requests;
import org.opends.sdk.responses.Result;
import org.opends.sdk.responses.ResultFuture;
import org.opends.sdk.responses.ResultHandler;



/**
 * Abstract result future implementation.
 */
abstract class AbstractResultFutureImpl<R extends Result> implements
    ResultFuture, Runnable
{
  private final Connection connection;
  private final ResultHandler<R> handler;
  private final ExecutorService handlerExecutor;
  private final int messageID;
  private final Semaphore invokerLock = new Semaphore(1);
  private final CountDownLatch latch = new CountDownLatch(1);

  private volatile boolean isCancelled = false;
  private volatile R result = null;



  AbstractResultFutureImpl(int messageID, ResultHandler<R> handler,
      Connection connection, ExecutorService handlerExecutor)
  {
    this.messageID = messageID;
    this.handler = handler;
    this.connection = connection;
    this.handlerExecutor = handlerExecutor;
  }



  public synchronized boolean cancel(boolean b)
  {
    if (!isDone())
    {
      isCancelled = true;
      connection.abandon(Requests.newAbandonRequest(messageID));
      latch.countDown();
      return true;
    }
    else
    {
      return false;
    }
  }



  public R get() throws InterruptedException, ErrorResultException
  {
    latch.await();
    return get0();
  }



  public R get(long timeout, TimeUnit unit)
      throws InterruptedException, TimeoutException,
      ErrorResultException
  {
    if (!latch.await(timeout, unit))
    {
      throw new TimeoutException();
    }
    return get0();
  }



  public int getMessageID()
  {
    return messageID;
  }



  public boolean isCancelled()
  {
    return isCancelled;
  }



  public boolean isDone()
  {
    return latch.getCount() == 0;
  }



  public void run()
  {
    if (result.getResultCode().isExceptional())
    {
      ErrorResultException e = ErrorResultException.wrap(result);
      handler.handleError(e);
    }
    else
    {
      handler.handleResult(result);
    }
  }



  synchronized void handleErrorResult(Result result)
  {
    R errorResult =
        newErrorResult(result.getResultCode(), result
            .getDiagnosticMessage(), result.getCause());
    handleResult(errorResult);
  }



  abstract R newErrorResult(ResultCode resultCode,
      String diagnosticMessage, Throwable cause);



  void handleResult(R result)
  {
    if (!isDone())
    {
      this.result = result;
      latch.countDown();
      invokeHandler(this);
    }
  }



  protected void invokeHandler(final Runnable runnable)
  {
    if (handler == null)
    {
      return;
    }

    try
    {
      invokerLock.acquire();

      handlerExecutor.submit(new Runnable()
      {
        public void run()
        {
          runnable.run();
          invokerLock.release();
        }
      });
    }
    catch (InterruptedException e)
    {
      // TODO: what should we do now?
    }
  }



  private R get0() throws CancellationException, ErrorResultException
  {
    if (isCancelled())
    {
      throw new CancellationException();
    }
    else if (result.getResultCode().isExceptional())
    {
      throw ErrorResultException.wrap(result);
    }
    else
    {
      return result;
    }
  }
}
