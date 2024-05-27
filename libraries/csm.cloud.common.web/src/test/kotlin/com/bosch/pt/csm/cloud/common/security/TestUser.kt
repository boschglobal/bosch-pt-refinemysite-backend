/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.security

import com.bosch.pt.csm.cloud.common.facade.rest.UserLocale
import java.util.Locale
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.userdetails.UserDetails

class TestUser(
    private var userId: String,
    var locked: Boolean = false,
    var locale: Locale? = null
) : UserDetails, UserLocale {

  override fun getAuthorities(): Collection<GrantedAuthority> =
      AuthorityUtils.createAuthorityList("testUserAuthority")

  override fun getPassword() = "secret"

  override fun getUsername() = userId

  override fun isAccountNonExpired() = true

  override fun isAccountNonLocked() = !locked

  override fun isCredentialsNonExpired() = true

  override fun isEnabled() = true

  override fun getUserLocale(): Locale? = locale
}
