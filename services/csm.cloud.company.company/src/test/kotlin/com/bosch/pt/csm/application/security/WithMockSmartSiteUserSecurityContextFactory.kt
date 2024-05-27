/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.application.security

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.api.asUserId
import com.bosch.pt.csm.user.user.model.UserBuilder
import java.util.UUID
import java.util.UUID.randomUUID
import org.apache.commons.lang3.LocaleUtils.toLocale
import org.apache.commons.lang3.StringUtils.EMPTY
import org.apache.commons.lang3.StringUtils.isNotBlank
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.test.context.support.WithSecurityContextFactory
import org.springframework.test.util.ReflectionTestUtils

/** A [WithMockSmartSiteUserSecurityContextFactory] that works with [ ]. */
class WithMockSmartSiteUserSecurityContextFactory :
    WithSecurityContextFactory<WithMockSmartSiteUser> {

  override fun createSecurityContext(withUser: WithMockSmartSiteUser): SecurityContext {
    val username = if (isNotBlank(withUser.username)) withUser.username else withUser.value
    val firstName = if (isNotBlank(withUser.firstName)) withUser.firstName else EMPTY
    val lastName = if (isNotBlank(withUser.firstName)) withUser.lastName else EMPTY
    val email = if (isNotBlank(withUser.email)) withUser.email else EMPTY
    val locale = if (isNotBlank(withUser.userLocale)) toLocale(withUser.userLocale) else null
    val identifier =
        if (isNotBlank(withUser.identifier)) UUID.fromString(withUser.identifier).asUserId()
        else randomUUID().asUserId()

    val creator = UserBuilder.user().withIdentifier(UserId()).build()

    var principal =
        UserBuilder.user()
            .withFirstName(firstName)
            .withLastName(lastName)
            .withEmail(email)
            .withIdentifier(identifier)
            .withCreatedBy(creator.id)
            .withLastModifiedBy(creator.id)
            .withVersion(withUser.version)
            .withLocale(locale)
            .withAdmin(withUser.admin)
            .build()

    ReflectionTestUtils.setField(principal, "id", withUser.id)
    ReflectionTestUtils.setField(principal, "username", username)

    val authentication: Authentication =
        UsernamePasswordAuthenticationToken(principal, principal.password, principal.authorities)

    val context = SecurityContextHolder.createEmptyContext()
    context.authentication = authentication

    return context
  }
}
