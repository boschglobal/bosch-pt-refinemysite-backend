/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.user.model

import com.bosch.pt.csm.cloud.common.facade.rest.UserLocale
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.Collections.USER_STATE
import com.bosch.pt.csm.cloud.projectmanagement.application.security.RoleConstants
import java.time.Instant
import java.util.Locale
import java.util.UUID
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.TypeAlias
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.userdetails.UserDetails

@Document(collection = USER_STATE)
@TypeAlias("User")
class User(
    @Id var identifier: UUID,
    val displayName: String,
    // User ID from Bosch CIAM
    val externalIdentifier: String? = null,
    val lastSeen: Instant? = null,
    val gender: GenderEnum? = null,
    val userPictureIdentifier: UUID? = null,
    val admin: Boolean? = false,
    val locked: Boolean = false,
    private val locale: Locale? = null
) : UserDetails, UserLocale {

  override fun getAuthorities(): Collection<GrantedAuthority>? =
      if (admin != null && admin) USER_AND_ADMIN_AUTHORITIES else USER_AUTHORITY

  override fun getPassword(): String = "secret"

  override fun getUsername(): String? = this.externalIdentifier

  override fun isAccountNonExpired(): Boolean = true

  override fun isAccountNonLocked(): Boolean = !locked

  override fun isCredentialsNonExpired(): Boolean = true

  override fun isEnabled(): Boolean = true

  override fun getUserLocale(): Locale? = locale

  companion object {
    private val USER_AND_ADMIN_AUTHORITIES =
        AuthorityUtils.createAuthorityList(
            RoleConstants.USER.roleName(), RoleConstants.ADMIN.roleName())

    private val USER_AUTHORITY = AuthorityUtils.createAuthorityList(RoleConstants.USER.roleName())
  }
}

enum class GenderEnum {

  /** Male gender. */
  MALE,

  /** Female gender. */
  FEMALE
}
