/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.util

import java.util.concurrent.atomic.AtomicReference

object ThreadUtil {

  /**
   * Runs the procedure as a new thread and blocks until the procedure is complete or a timeout
   * happens. If procedure throws an exception, this exception will be rethrown to the caller
   * thread.
   *
   * @param procedure the procedure to be executed by the thread
   * @param timeout the maximum time to wait for the procedure to complete. Keep this number
   * strictly larger than any other timeout that may happen during test execution. Otherwise, the
   * timeout exception of the other timeout will be lost. This is because [Thread.join] will return
   * before the timeout exception actually happened.
   */
  @Suppress("ThrowsCount")
  fun runAsThreadAndWaitForCompletion(timeout: Long, procedure: Runnable) {
    val thread = Thread(procedure)
    val exception = AtomicReference<Throwable>()
    thread.uncaughtExceptionHandler = Thread.UncaughtExceptionHandler { _, ex -> exception.set(ex) }
    thread.start()

    try {
      thread.join(timeout)
    } catch (e: InterruptedException) {
      throw IllegalStateException(e)
    }

    if (thread.isAlive) {
      throw IllegalStateException(
          "The thread is still alive after waiting $timeout ms. Either it did not end execution in time or it " +
              "is blocked. Consider increasing the wait timeout, or check why the thread is still running/blocked.")
    }

    val ex = exception.get()
    if (ex != null) {
      throw ex
    }
  }
}
