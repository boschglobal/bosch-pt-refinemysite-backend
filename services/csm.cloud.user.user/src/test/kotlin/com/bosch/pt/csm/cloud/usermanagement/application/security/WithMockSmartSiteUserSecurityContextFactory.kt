/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.application.security

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.model.IsoCountryCodeEnum.GB
import com.bosch.pt.csm.cloud.usermanagement.user.user.GenderEnum.MALE
import com.bosch.pt.csm.cloud.usermanagement.user.user.UserRoleEnum.ADMIN
import com.bosch.pt.csm.cloud.usermanagement.user.user.UserRoleEnum.USER
import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.model.User
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Locale
import java.util.Locale.UK
import org.apache.commons.lang3.LocaleUtils.toLocale
import org.apache.commons.lang3.StringUtils.EMPTY
import org.apache.commons.lang3.StringUtils.isBlank
import org.apache.commons.lang3.StringUtils.isNotBlank
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.AuthorityUtils.createAuthorityList
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.test.context.support.WithSecurityContextFactory
import org.springframework.test.util.ReflectionTestUtils

/** A [WithMockSmartSiteUserSecurityContextFactory] that works with [ ]. */
internal class WithMockSmartSiteUserSecurityContextFactory :
    WithSecurityContextFactory<WithMockSmartSiteUser> {

  override fun createSecurityContext(withUser: WithMockSmartSiteUser): SecurityContext {
    val username = if (isNotBlank(withUser.username)) withUser.username else withUser.value
    val firstName = if (isNotBlank(withUser.firstName)) withUser.firstName else EMPTY
    val lastName = if (isNotBlank(withUser.firstName)) withUser.lastName else EMPTY
    val email = if (isNotBlank(withUser.email)) withUser.email else EMPTY
    val gender = withUser.gender
    val locale: Locale = if (isNotBlank(withUser.userLocale)) toLocale(withUser.userLocale) else UK
    val grantedAuthorities = evaluateGrantedAuthorities(withUser)

    val creator =
        User(
            UserId(),
            "creator",
            MALE,
            "Crea",
            "Tor",
            "crea.tor@example.com",
            UK,
            GB,
            LocalDate.now())

    val principal =
        User(
                getIdentifier(withUser),
                username,
                gender,
                firstName,
                lastName,
                email,
                locale,
                GB,
                LocalDate.now())
            .apply {
              this.authorities = grantedAuthorities
              setCreatedBy(creator.identifier)
              setCreatedDate(LocalDateTime.now())
              setLastModifiedBy(creator.identifier)
              setLastModifiedDate(LocalDateTime.now())
              admin = withUser.admin
              ReflectionTestUtils.setField(this, "id", withUser.id)
            }

    val authentication: Authentication =
        UsernamePasswordAuthenticationToken(principal, principal.password, principal.authorities)

    return SecurityContextHolder.createEmptyContext().apply { this.authentication = authentication }
  }

  private fun getIdentifier(mockUser: WithMockSmartSiteUser) =
      when (isBlank(mockUser.identifier)) {
        true -> UserId()
        else -> UserId(mockUser.identifier)
      }

  private fun evaluateGrantedAuthorities(withUser: WithMockSmartSiteUser): List<GrantedAuthority> =
      if (withUser.admin) createAuthorityList(USER.roleName(), ADMIN.roleName())
      else createAuthorityList(USER.roleName())
}
