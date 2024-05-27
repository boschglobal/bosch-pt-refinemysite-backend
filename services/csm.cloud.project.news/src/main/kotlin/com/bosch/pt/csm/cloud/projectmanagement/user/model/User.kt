/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.projectmanagement.user.model

import com.bosch.pt.csm.cloud.projectmanagement.application.security.RoleConstants.ADMIN
import com.bosch.pt.csm.cloud.projectmanagement.application.security.RoleConstants.USER
import java.util.UUID
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.validation.constraints.Size
import com.bosch.pt.csm.cloud.common.api.AbstractPersistable
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.userdetails.UserDetails

@Entity
@Table(
    name = "USER_ENTITY",
    indexes =
        [
            Index(name = "UK_User_UserId", columnList = "userId", unique = true),
            Index(name = "UK_User_Identifier", columnList = "identifier", unique = true)])
class User(
    // userId
    @field:Size(min = 1, max = MAX_USER_ID_LENGTH)
    @Column(nullable = false, length = MAX_USER_ID_LENGTH)
    var userId: String,

    // identifier
    var identifier: UUID,

    // admin
    @Column(nullable = false) var admin: Boolean = false,

    // locked
    @Column(nullable = false) var locked: Boolean = false
) : AbstractPersistable<Long>(), UserDetails {

  override fun getAuthorities(): Collection<GrantedAuthority> =
      if (admin) USER_AND_ADMIN_AUTHORITIES else USER_AUTHORITY

  override fun getPassword(): String = "secret"

  override fun getUsername(): String = userId

  override fun isAccountNonExpired(): Boolean = true

  override fun isAccountNonLocked(): Boolean = !locked

  override fun isCredentialsNonExpired(): Boolean = true

  override fun isEnabled(): Boolean = true

  companion object {
    private const val MAX_USER_ID_LENGTH = 100

    private val USER_AND_ADMIN_AUTHORITIES =
        AuthorityUtils.createAuthorityList(USER.roleName(), ADMIN.roleName())

    private val USER_AUTHORITY = AuthorityUtils.createAuthorityList(USER.roleName())
  }
}
