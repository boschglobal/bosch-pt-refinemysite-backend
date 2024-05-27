/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.projectmanagement.application.security

import com.bosch.pt.csm.cloud.common.command.AsyncRequestScopeAttributes.Companion.executeWithAsyncRequestScope
import com.bosch.pt.csm.cloud.projectmanagement.user.user.query.model.UserProjection
import java.util.concurrent.atomic.AtomicReference
import org.apache.commons.lang3.tuple.Pair
import org.assertj.core.api.Assertions.fail
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.AuthorityUtils.createAuthorityList
import org.springframework.security.test.context.TestSecurityContextHolder
import org.springframework.security.test.context.TestSecurityContextHolder.setAuthentication
import org.springframework.test.context.web.ServletTestExecutionListener.CREATED_BY_THE_TESTCONTEXT_FRAMEWORK
import org.springframework.web.context.request.RequestAttributes
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.WebRequest.SCOPE_REQUEST

object AuthorizationTestUtils {

  /**
   * Keep this number strictly larger than any other timeout that may happen during test execution.
   * Otherwise, the timeout-exception of the other timeout will be lost. This is because
   * [Thread.join] will return before the timeout exception actually happened.
   */
  private const val THREAD_WAIT_TIMEOUT_MS = 30000

  @JvmOverloads
  fun authorizeWithUser(user: UserProjection?, isAdmin: Boolean = false) {
    if (isAdmin) {
      setAuthentication(
          UsernamePasswordAuthenticationToken(
              user, "n/a", createAuthorityList("ROLE_USER", "ROLE_ADMIN")))
    } else {
      setAuthentication(
          UsernamePasswordAuthenticationToken(user, "n/a", createAuthorityList("ROLE_USER")))
    }
  }

  fun doWithAuthorization(user: UserProjection?, procedure: Runnable) =
      doWithAuthorization(user, false, procedure)

  fun doWithAuthorization(userAdminPair: Pair<UserProjection?, Boolean>, procedure: Runnable) =
      doWithAuthorization(userAdminPair.left, userAdminPair.right, procedure)

  fun doWithAuthorization(user: UserProjection?, isAdmin: Boolean, procedure: Runnable) {
    val requestAttributes = RequestContextHolder.getRequestAttributes()

    // Clean up from the request attributes everything that is in the scope of the Web Request
    // This is used to "reset" the cache so the information from the main thread don't pass to the
    // worker thread
    cleanupRequestAttributes(requireNotNull(requestAttributes))

    // Execute procedure in a new Thread because otherwise the procedure will reuse ThreadLocals
    // belonging to a different user. This applies to the TestSecurityContextHolder as well as the
    // RequestContextHolder, which are both backed by a ThreadLocal variables.
    runAsThreadAndWaitForCompletion {
      RequestContextHolder.setRequestAttributes(requestAttributes)
      if (user != null) {
        authorizeWithUser(user, isAdmin)
      } else {
        authorizeWithAnonymousUser()
      }
      procedure.run()
      TestSecurityContextHolder.clearContext()
      RequestContextHolder.resetRequestAttributes()
    }
  }

  fun simulateScheduledJob(procedure: Runnable) = runAsThreadAndWaitForCompletion {
    RequestContextHolder.resetRequestAttributes()
    TestSecurityContextHolder.clearContext()
    executeWithAsyncRequestScope { procedure.run() }
  }

  fun simulateKafkaListener(procedure: Runnable) = runAsThreadAndWaitForCompletion {
    RequestContextHolder.resetRequestAttributes()
    TestSecurityContextHolder.clearContext()
    executeWithAsyncRequestScope { procedure.run() }
  }

  fun authorizeWithAdministrativeUser(user: UserProjection?) = authorizeWithUser(user, true)

  fun authorizeWithStandardUser(user: UserProjection?) = authorizeWithUser(user, false)

  fun authorizeWithAnonymousUser() =
      setAuthentication(AnonymousAuthenticationToken("n/a", "n/a", createAuthorityList("USER")))

  private fun cleanupRequestAttributes(requestAttributes: RequestAttributes) =
      requestAttributes
          .getAttributeNames(SCOPE_REQUEST)
          .filter { attribute -> attribute != CREATED_BY_THE_TESTCONTEXT_FRAMEWORK }
          .forEach { attribute: String ->
            requestAttributes.removeAttribute(attribute, SCOPE_REQUEST)
          }

  @Suppress("ThrowsCount")
  private fun runAsThreadAndWaitForCompletion(procedure: Runnable) {
    val exception = AtomicReference<Throwable>()
    val thread =
        Thread(procedure).apply {
          uncaughtExceptionHandler =
              Thread.UncaughtExceptionHandler { _: Thread?, ex: Throwable -> exception.set(ex) }
        }

    thread.start()

    try {
      thread.join(THREAD_WAIT_TIMEOUT_MS.toLong())
    } catch (e: InterruptedException) {
      throw IllegalStateException(e)
    }

    // note: see documentation of THREAD_WAIT_TIMEOUT_MS to avoid loosing exceptions here
    val ex = exception.get()
    if (ex != null) {
      when (ex) {
        is RuntimeException -> throw ex
        is AssertionError -> throw ex
        else -> fail("Test fail for the following error: ", ex)
      }
    }
  }
}
