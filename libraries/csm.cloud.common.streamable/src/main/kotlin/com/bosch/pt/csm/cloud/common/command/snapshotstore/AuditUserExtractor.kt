/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.common.command.snapshotstore

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.api.UserReference
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.InsufficientAuthenticationException
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder

/**
 * This helper method extracts the [UserId] from the object in the security context. The object that
 * the application puts into the security context has to implements the [AuditingUser] interface to
 * make this work.
 */
object AuditUserExtractor {

  fun getCurrentUserReference(): UserReference =
      authentication?.principal
          ?.apply {
            if (this !is AuditingUser) {
              throw InsufficientAuthenticationException(
                  "Security context does not contain object implementing AuditingUser interface")
            }
          }
          ?.let { (it as AuditingUser).toUserReference() }
          ?: throw InsufficientAuthenticationException("Security context is empty")

  private val authentication: Authentication?
    get() =
        SecurityContextHolder.getContext().authentication.let {
          return when (it != null && it.isAuthenticated && it !is AnonymousAuthenticationToken) {
            true -> it
            else -> null
          }
        }
}
