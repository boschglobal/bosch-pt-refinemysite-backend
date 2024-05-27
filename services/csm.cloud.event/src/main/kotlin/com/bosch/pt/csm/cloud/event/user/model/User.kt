/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2018
 *
 * *************************************************************************
 */
package com.bosch.pt.csm.cloud.event.user.model

import java.util.UUID
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

class User(
    val identifier: UUID,
    private var authorities: Collection<GrantedAuthority> = HashSet()
) : UserDetails {

  override fun getAuthorities(): Collection<GrantedAuthority> = authorities

  override fun getPassword(): String = "secret"

  override fun getUsername(): String = identifier.toString()

  override fun isAccountNonExpired(): Boolean = true

  override fun isAccountNonLocked(): Boolean = true

  override fun isCredentialsNonExpired(): Boolean = true

  override fun isEnabled(): Boolean = true
}
