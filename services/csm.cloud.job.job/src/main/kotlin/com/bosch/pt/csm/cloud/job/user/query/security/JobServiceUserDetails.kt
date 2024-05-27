/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.job.user.query.security

import com.bosch.pt.csm.cloud.common.facade.rest.UserLocale
import com.bosch.pt.csm.cloud.job.job.api.UserIdentifier
import java.util.Locale
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

data class JobServiceUserDetails(
    val identifier: UserIdentifier,
    private val userLocale: Locale?,
    private var authorities: Collection<GrantedAuthority> = emptySet()
) : UserDetails, UserLocale {

  override fun getAuthorities(): Collection<GrantedAuthority> {
    return authorities
  }

  override fun getPassword(): String {
    return "secret"
  }

  override fun getUsername(): String {
    return identifier.value
  }

  override fun isAccountNonExpired(): Boolean {
    return true
  }

  override fun isAccountNonLocked(): Boolean {
    return true
  }

  override fun isCredentialsNonExpired(): Boolean {
    return true
  }

  override fun isEnabled(): Boolean {
    return true
  }

  override fun getUserLocale(): Locale? {
    return userLocale
  }
}
