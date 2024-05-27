/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.application.security

import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.model.UnregisteredUser
import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.model.User
import java.util.concurrent.Callable
import java.util.concurrent.atomic.AtomicReference
import org.apache.commons.lang3.tuple.Pair
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.test.context.TestSecurityContextHolder
import org.springframework.web.context.request.RequestContextHolder

object AuthorizationTestUtils {

  private const val THREAD_WAIT_TIMEOUT_MS = 10000L

  @JvmOverloads
  fun authorizeWithUser(user: User, isAdmin: Boolean = false) =
      if (isAdmin) {
        TestSecurityContextHolder.setAuthentication(
            UsernamePasswordAuthenticationToken(
                user, "n/a", AuthorityUtils.createAuthorityList("ROLE_USER", "ROLE_ADMIN")))
      } else {
        TestSecurityContextHolder.setAuthentication(
            UsernamePasswordAuthenticationToken(
                user, "n/a", AuthorityUtils.createAuthorityList("ROLE_USER")))
      }

  fun <R> doWithAuthorization(user: User, procedure: Callable<R>) =
      doWithAuthorization(user, false, procedure)

  fun <R> doWithAuthorization(userAdminPair: Pair<User, Boolean>, procedure: Callable<R>) =
      doWithAuthorization(userAdminPair.left, userAdminPair.right, procedure)

  fun <R> doWithAuthorization(user: User?, isAdmin: Boolean, procedure: Callable<R>) {
    val requestAttributes = RequestContextHolder.getRequestAttributes()
    val result = AtomicReference<R>()

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
      try {
        result.set(procedure.call())
      } catch (e: Exception) {
        throw IllegalStateException(e)
      }
      TestSecurityContextHolder.clearContext()
      RequestContextHolder.resetRequestAttributes()
    }
  }

  fun authorizeWithAdministrativeUser(user: User) = authorizeWithUser(user, true)

  fun authorizeWithStandardUser(user: User) = authorizeWithUser(user, false)

  fun authorizeWithAnonymousUser() =
      TestSecurityContextHolder.setAuthentication(
          AnonymousAuthenticationToken("n/a", "n/a", AuthorityUtils.createAuthorityList("USER")))

  fun authorizeWithUnregisteredUser(unregisteredUser: UnregisteredUser) =
      TestSecurityContextHolder.setAuthentication(
          UsernamePasswordAuthenticationToken(
              unregisteredUser, "n/a", AuthorityUtils.createAuthorityList("ROLE_USER")))

  fun simulateKafkaListener(procedure: Runnable) {
    runAsThreadAndWaitForCompletion {
      RequestContextHolder.resetRequestAttributes()
      TestSecurityContextHolder.clearContext()
      procedure.run()
    }
  }

  private fun runAsThreadAndWaitForCompletion(procedure: Runnable) {
    val exception = AtomicReference<Throwable>()
    Thread(procedure).let {
      it.setUncaughtExceptionHandler { _, throwable -> exception.set(throwable) }
      it.start()
      try {
        it.join(THREAD_WAIT_TIMEOUT_MS)
      } catch (e: InterruptedException) {
        throw IllegalStateException(e)
      }
    }
    if (exception.get() != null) {
      throw exception.get()
    }
  }
}
