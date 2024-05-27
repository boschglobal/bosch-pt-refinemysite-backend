/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.util

import com.bosch.pt.csm.cloud.projectmanagement.user.model.User
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.context.SecurityContextHolder

fun authorizeWithUser(user: User, isAdmin: Boolean = false) {
  if (isAdmin) {
    SecurityContextHolder.getContext().authentication =
        UsernamePasswordAuthenticationToken(
            user, "n/a", AuthorityUtils.createAuthorityList("ROLE_USER", "ROLE_ADMIN"))
  } else {
    SecurityContextHolder.getContext().authentication =
        UsernamePasswordAuthenticationToken(
            user, "n/a", AuthorityUtils.createAuthorityList("ROLE_USER"))
  }
}

fun authorizeWithAnonymousUser() {
  SecurityContextHolder.getContext().authentication =
      AnonymousAuthenticationToken("n/a", "n/a", AuthorityUtils.createAuthorityList("USER"))
}

fun <R> doWithAuthorization(user: User?, isAdmin: Boolean = false, procedure: () -> R): R {
  SecurityContextHolder.clearContext()
  if (user != null) {
    authorizeWithUser(user, isAdmin)
  } else {
    authorizeWithAnonymousUser()
  }
  return procedure.invoke().also { SecurityContextHolder.clearContext() }
}
