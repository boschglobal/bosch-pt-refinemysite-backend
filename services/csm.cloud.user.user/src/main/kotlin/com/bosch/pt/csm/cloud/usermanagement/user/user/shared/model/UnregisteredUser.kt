/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.user.user.shared.model

import com.bosch.pt.csm.cloud.common.facade.rest.UserLocale
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

class UnregisteredUser(private val username: String, val email: String) : UserDetails, UserLocale {

  override fun getAuthorities(): Collection<GrantedAuthority?> = emptyList()

  override fun getPassword(): String? = null

  override fun getUsername(): String = username

  override fun isAccountNonExpired() = true

  override fun isAccountNonLocked() = true

  override fun isCredentialsNonExpired() = true

  override fun isEnabled() = true

  override fun getUserLocale() = null
}
