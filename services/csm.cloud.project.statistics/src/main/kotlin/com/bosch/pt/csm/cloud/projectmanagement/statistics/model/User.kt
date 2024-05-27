/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.projectmanagement.statistics.model

import com.bosch.pt.csm.cloud.common.api.AbstractPersistable
import com.bosch.pt.csm.cloud.common.facade.rest.UserLocale
import com.bosch.pt.csm.cloud.projectmanagement.application.security.RoleConstants.ADMIN
import com.bosch.pt.csm.cloud.projectmanagement.application.security.RoleConstants.USER
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.validation.constraints.Size
import java.util.Locale
import java.util.UUID
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.AuthorityUtils.createAuthorityList
import org.springframework.security.core.userdetails.UserDetails

@Entity
@Table(
    name = "USER_ENTITY",
    indexes =
        [
            Index(name = "UK_User_UserId", columnList = "userId", unique = true),
            Index(name = "UK_User_Identifier", columnList = "identifier", unique = true)])
class User(
    // external identifier
    @Size(min = 1, max = MAX_USER_ID_LENGTH)
    @Column(nullable = false, length = MAX_USER_ID_LENGTH)
    var userId: String,

    // application identifier
    var identifier: UUID,

    // user is admin
    var admin: Boolean = false,

    // user is locked
    var locked: Boolean = false,

    // the locale of the user
    var locale: Locale? = null
) : AbstractPersistable<Long>(), UserDetails, UserLocale {

  override fun getAuthorities(): Collection<GrantedAuthority> =
      if (admin) USER_AND_ADMIN_AUTHORITIES else USER_AUTHORITY

  override fun getPassword() = "secret"

  override fun getUsername() = userId

  override fun isAccountNonExpired() = true

  override fun isAccountNonLocked() = !locked

  override fun isCredentialsNonExpired() = true

  override fun isEnabled() = true

  override fun getUserLocale(): Locale? = locale

  companion object {
    private const val MAX_USER_ID_LENGTH = 100

    private val USER_AND_ADMIN_AUTHORITIES = createAuthorityList(USER.roleName(), ADMIN.roleName())

    private val USER_AUTHORITY = createAuthorityList(USER.roleName())
  }
}
