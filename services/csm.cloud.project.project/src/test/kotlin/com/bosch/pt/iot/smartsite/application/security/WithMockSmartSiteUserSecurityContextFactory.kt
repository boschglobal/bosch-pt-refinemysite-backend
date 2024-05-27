/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.application.security

import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.model.IsoCountryCodeEnum
import com.bosch.pt.iot.smartsite.user.model.User
import com.bosch.pt.iot.smartsite.user.model.UserBuilder.Companion.user
import java.time.LocalDateTime
import java.util.UUID.randomUUID
import org.apache.commons.lang3.LocaleUtils
import org.apache.commons.lang3.StringUtils.EMPTY
import org.apache.commons.lang3.StringUtils.isNotBlank
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.test.context.support.WithSecurityContextFactory
import org.springframework.test.util.ReflectionTestUtils

class WithMockSmartSiteUserSecurityContextFactory :
    WithSecurityContextFactory<WithMockSmartSiteUser> {

  override fun createSecurityContext(withUser: WithMockSmartSiteUser): SecurityContext {
    val username = if (isNotBlank(withUser.username)) withUser.username else withUser.value
    val firstName = if (isNotBlank(withUser.firstName)) withUser.firstName else EMPTY
    val lastName = if (isNotBlank(withUser.firstName)) withUser.lastName else EMPTY
    val email = if (isNotBlank(withUser.email)) withUser.email else EMPTY
    val gender = withUser.gender
    val locale = if (isNotBlank(withUser.locale)) LocaleUtils.toLocale(withUser.locale) else null
    val country =
        if (isNotBlank(withUser.country)) IsoCountryCodeEnum.valueOf(withUser.country) else null

    val identifier =
        if (isNotBlank(withUser.identifier)) withUser.identifier.toUUID() else randomUUID()

    val principal =
        User(
            identifier,
            withUser.version,
            username,
            gender,
            firstName,
            lastName,
            email,
            withUser.position,
            withUser.registered,
            withUser.admin,
            withUser.locked,
            locale,
            country,
            emptySet(),
            emptySet())

    val creator = user().build()
    creator.identifier = randomUUID()

    principal.setCreatedBy(creator)
    principal.setCreatedDate(LocalDateTime.now())
    principal.setLastModifiedBy(creator)
    principal.setLastModifiedDate(LocalDateTime.now())

    ReflectionTestUtils.setField(principal, "id", withUser.id)

    val authentication: Authentication =
        UsernamePasswordAuthenticationToken(principal, principal.password, principal.authorities)

    return SecurityContextHolder.createEmptyContext().apply { this.authentication = authentication }
  }
}
