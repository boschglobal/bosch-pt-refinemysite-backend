/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.common.security

import com.bosch.pt.csm.cloud.user.query.UserProjection
import org.apache.commons.lang3.tuple.Pair
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.test.context.TestSecurityContextHolder
import org.springframework.web.context.request.RequestContextHolder
import java.util.concurrent.Callable
import java.util.concurrent.atomic.AtomicReference

object AuthorizationTestUtils {

    private const val THREAD_WAIT_TIMEOUT_MS = 10000L

    @JvmOverloads
    fun authorizeWithUser(user: UserProjection, isAdmin: Boolean = false) {
        if (isAdmin) {
            TestSecurityContextHolder.setAuthentication(
                    UsernamePasswordAuthenticationToken(
                            user, "n/a", AuthorityUtils.createAuthorityList("ROLE_USER", "ROLE_ADMIN")))
        } else {
            TestSecurityContextHolder.setAuthentication(
                    UsernamePasswordAuthenticationToken(
                            user, "n/a", AuthorityUtils.createAuthorityList("ROLE_USER")))
        }
    }

    fun <R> doWithAuthorization(user: UserProjection, procedure: Callable<R>): R =
            doWithAuthorization(user, user.admin, procedure)

    fun <R> doWithAuthorization(
            userAdminPair: Pair<UserProjection?, Boolean>,
            procedure: Callable<R>
    ): R = doWithAuthorization(userAdminPair.left, userAdminPair.right, procedure)

    fun <R> doWithAuthorization(user: UserProjection?, isAdmin: Boolean, procedure: Callable<R>): R {
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

        return result.get()
    }

    fun simulateKafkaListener(procedure: Runnable) {
        runAsThreadAndWaitForCompletion {
            RequestContextHolder.resetRequestAttributes()
            TestSecurityContextHolder.clearContext()
            procedure.run()
        }
    }

    fun authorizeWithAdministrativeUser(user: UserProjection) {
        authorizeWithUser(user, true)
    }

    fun authorizeWithStandardUser(user: UserProjection) {
        authorizeWithUser(user, false)
    }

    fun authorizeWithAnonymousUser() {
        TestSecurityContextHolder.setAuthentication(
                AnonymousAuthenticationToken("n/a", "n/a", AuthorityUtils.createAuthorityList("USER")))
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
