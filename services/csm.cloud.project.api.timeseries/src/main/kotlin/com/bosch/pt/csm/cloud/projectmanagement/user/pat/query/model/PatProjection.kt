/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.user.pat.query.model

import com.bosch.pt.csm.cloud.common.facade.rest.UserLocale
import com.bosch.pt.csm.cloud.projectmanagement.application.security.IdentifiableUserDetails
import com.bosch.pt.csm.cloud.projectmanagement.application.security.RoleConstants.USER
import com.bosch.pt.csm.cloud.projectmanagement.user.pat.domain.PatId
import com.bosch.pt.csm.cloud.projectmanagement.user.user.domain.UserId
import java.time.LocalDateTime
import java.util.Locale
import java.util.UUID
import org.apache.commons.lang3.LocaleUtils
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.TypeAlias
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.AuthorityUtils.createAuthorityList

const val PAT_PROJECTION = "PatProjection"

@Document(PAT_PROJECTION)
@TypeAlias(PAT_PROJECTION)
data class PatProjection(
    @Id val identifier: PatId,
    val version: Long,
    val description: String,
    val impersonatedUserIdentifier: UserId,
    val hash: String,
    val type: PatTypeEnum,
    val scopes: List<PatScopeEnum>,
    val issuedAt: LocalDateTime,
    val expiresAt: LocalDateTime,
    val locked: Boolean = false,
    val eventDate: LocalDateTime,
    val locale: Locale? = null
) : IdentifiableUserDetails, UserLocale {

  companion object {
    val USER_AUTHORITY = createAuthorityList(USER.roleName())
    private val defaultLocale: Locale = LocaleUtils.toLocale("en_GB")
  }

  override fun getAuthorities(): Collection<GrantedAuthority> = USER_AUTHORITY

  override fun getPassword(): String = "secret"

  override fun getUsername(): String = identifier.toString()

  override fun isAccountNonExpired(): Boolean = true

  override fun isAccountNonLocked(): Boolean = !locked

  override fun isCredentialsNonExpired(): Boolean = !expiresAt.isBefore(LocalDateTime.now())

  override fun isEnabled(): Boolean = scopes.isNotEmpty()

  override fun userIdentifier(): UUID = impersonatedUserIdentifier.value

  override fun getUserLocale(): Locale = locale ?: defaultLocale
}

enum class PatScopeEnum {
  TIMELINE_API_READ,
  GRAPHQL_API_READ
}

enum class PatTypeEnum {
  RMSPAT1
}
