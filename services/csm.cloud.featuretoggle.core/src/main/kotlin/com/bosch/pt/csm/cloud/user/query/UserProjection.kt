/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.user.query

import com.bosch.pt.csm.cloud.common.ExcludeFromCodeCoverage
import com.bosch.pt.csm.cloud.common.Referable
import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.command.snapshotstore.AuditingUser
import com.bosch.pt.csm.cloud.common.facade.rest.UserLocale
import com.bosch.pt.csm.cloud.common.model.AbstractPersistableEntity
import com.bosch.pt.csm.cloud.user.constants.RoleConstants.ADMIN
import com.bosch.pt.csm.cloud.user.constants.RoleConstants.USER
import java.util.Locale
import java.util.UUID
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.AuthorityUtils.createAuthorityList
import org.springframework.security.core.userdetails.UserDetails

@Entity
@Table(
    name = "PROJECTION_USER",
    indexes =
        [Index(name = "UK_CiamUserIdentifier", columnList = "ciamUserIdentifier", unique = true)])
class UserProjection(
    id: UserId,

    /** Version for optimistic locking. */
    @Column(nullable = false) var version: Long,

    // CIAM Id
    @Column(nullable = false) var ciamUserIdentifier: String,

    // Admin
    @Column(nullable = false) var admin: Boolean = false,

    // Locked
    @Column(nullable = false) var locked: Boolean = false,

    // Locale
    var locale: Locale? = null
) : AbstractPersistableEntity<UserId>(id), UserDetails, UserLocale, AuditingUser, Referable {

  override fun getId(): UserId? = this.id
  override fun getAuthorities(): Collection<GrantedAuthority> =
      if (admin) USER_AND_ADMIN_AUTHORITIES else USER_AUTHORITY

  override fun getPassword(): String = "secret"

  override fun getUsername(): String = this.id.toString()

  @ExcludeFromCodeCoverage override fun isAccountNonExpired(): Boolean = true

  @ExcludeFromCodeCoverage override fun isAccountNonLocked(): Boolean = !locked

  @ExcludeFromCodeCoverage override fun isCredentialsNonExpired(): Boolean = true

  @ExcludeFromCodeCoverage override fun isEnabled(): Boolean = true

  override fun getDisplayName(): String = username

  override fun getIdentifierUuid(): UUID = id.toUuid()

  override fun getAuditUserId(): UserId = id

  override fun getAuditUserVersion() = version

  override fun getUserLocale() = locale

  companion object {
    private val USER_AND_ADMIN_AUTHORITIES = createAuthorityList(USER.roleName(), ADMIN.roleName())
    private val USER_AUTHORITY = createAuthorityList(USER.roleName())
  }
}
