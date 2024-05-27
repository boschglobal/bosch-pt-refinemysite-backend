/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.user.model

import com.bosch.pt.csm.cloud.common.facade.rest.UserLocale
import com.bosch.pt.csm.cloud.projectmanagement.application.security.RoleConstants
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.Collections.USER_STATE
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
    val gender: GenderEnum? = null,
    val userPictureIdentifier: UUID? = null,
    val admin: Boolean? = false,
    val locked: Boolean = false,
    private val locale: Locale? = null
) : UserDetails, UserLocale {

  override fun getAuthorities(): List<GrantedAuthority> =
      when {
        admin != null && admin -> USER_AND_ADMIN_AUTHORITIES
        else -> USER_AUTHORITY
      }

  override fun getPassword() = "secret"

  override fun getUsername() = this.externalIdentifier

  override fun isAccountNonExpired() = true

  override fun isAccountNonLocked() = !locked

  override fun isCredentialsNonExpired() = true

  override fun isEnabled() = true

  override fun getUserLocale(): Locale? = locale

  companion object {
    private val USER_AND_ADMIN_AUTHORITIES =
        AuthorityUtils.createAuthorityList(
            RoleConstants.USER.roleName(), RoleConstants.ADMIN.roleName())

    private val USER_AUTHORITY = AuthorityUtils.createAuthorityList(RoleConstants.USER.roleName())
  }
}

enum class GenderEnum {
  MALE,
  FEMALE
}
