/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.security

import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.User
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.AuthorityUtils.createAuthorityList
import org.springframework.security.test.context.TestSecurityContextHolder

fun <R> doWithAuthorization(user: User?, isAdmin: Boolean = false, procedure: () -> R): R {
  TestSecurityContextHolder.clearContext()
  if (user != null) {
    authorizeWithUser(user, isAdmin)
  } else {
    authorizeWithAnonymousUser()
  }
  return procedure.invoke().also { TestSecurityContextHolder.clearContext() }
}

fun authorizeWithUser(user: User, isAdmin: Boolean = false) {
  if (isAdmin) {
    TestSecurityContextHolder.getContext().authentication =
        UsernamePasswordAuthenticationToken(
            user, "n/a", createAuthorityList("ROLE_USER", "ROLE_ADMIN"))
  } else {
    TestSecurityContextHolder.getContext().authentication =
        UsernamePasswordAuthenticationToken(user, "n/a", createAuthorityList("ROLE_USER"))
  }
}

fun authorizeWithAnonymousUser() {
  TestSecurityContextHolder.getContext().authentication =
      AnonymousAuthenticationToken("n/a", "n/a", createAuthorityList("USER"))
}
