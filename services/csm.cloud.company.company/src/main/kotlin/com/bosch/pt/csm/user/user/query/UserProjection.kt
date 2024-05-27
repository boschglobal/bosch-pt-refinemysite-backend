/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.user.user.query

import com.bosch.pt.csm.cloud.common.ExcludeFromCodeCoverageGenerated
import com.bosch.pt.csm.cloud.common.Referable
import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.command.snapshotstore.AuditingUser
import com.bosch.pt.csm.cloud.common.facade.rest.UserLocale
import com.bosch.pt.csm.cloud.common.model.IsoCountryCodeEnum
import com.bosch.pt.csm.common.model.AbstractPersistableEntity
import com.bosch.pt.csm.user.user.constants.RoleConstants.ADMIN
import com.bosch.pt.csm.user.user.constants.RoleConstants.USER
import jakarta.persistence.AttributeOverride
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EnumType.STRING
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.Temporal
import jakarta.persistence.TemporalType.TIMESTAMP
import java.util.Date
import java.util.Locale
import java.util.UUID
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.AuthorityUtils.createAuthorityList
import org.springframework.security.core.userdetails.UserDetails

@Entity
@Table(
    name = "PROJECTION_USER",
    indexes =
        [
            Index(name = "UK_Email", columnList = "email", unique = true),
            Index(
                name = "UK_CiamUserIdentifier", columnList = "ciamUserIdentifier", unique = true)])
class UserProjection(
    id: UserId,

    // created by
    @Embedded
    @AttributeOverride(name = "identifier", column = Column(name = "createdBy", nullable = false))
    var createdBy: UserId,

    // created date
    @Temporal(TIMESTAMP) var createdDate: Date,

    // last modified by
    @Embedded
    @AttributeOverride(
        name = "identifier", column = Column(name = "lastModifiedBy", nullable = false))
    var lastModifiedBy: UserId,

    // last modified date
    @Temporal(TIMESTAMP) var lastModifiedDate: Date,

    /** Version for optimistic locking. */
    @Column(nullable = false) var version: Long,

    // CIAM Id
    @Column(nullable = false) var ciamUserIdentifier: String,

    // First name
    @Column(nullable = false) var firstName: String,

    // Last name
    @Column(nullable = false) var lastName: String,

    // Email
    @Column(nullable = false) var email: String,

    // Admin
    @Column(nullable = false) var admin: Boolean = false,

    // Locked
    @Column(nullable = false) var locked: Boolean = false,

    // Locale
    var locale: Locale? = null,

    // Country
    @Enumerated(STRING)
    @Column(columnDefinition = "varchar(255)")
    var country: IsoCountryCodeEnum? = null
) : AbstractPersistableEntity<UserId>(id), UserDetails, UserLocale, AuditingUser, Referable {

  override fun getAuthorities(): Collection<GrantedAuthority> =
      if (admin) USER_AND_ADMIN_AUTHORITIES else USER_AUTHORITY

  override fun getPassword(): String = "secret"

  override fun getUsername(): String = this.email

  @ExcludeFromCodeCoverageGenerated override fun isAccountNonExpired(): Boolean = true

  @ExcludeFromCodeCoverageGenerated override fun isAccountNonLocked(): Boolean = !locked

  @ExcludeFromCodeCoverageGenerated override fun isCredentialsNonExpired(): Boolean = true

  @ExcludeFromCodeCoverageGenerated override fun isEnabled(): Boolean = true

  override fun getDisplayName(): String = "$firstName $lastName"

  override fun getIdentifierUuid(): UUID = id.toUuid()

  override fun getAuditUserId(): UserId = id

  override fun getAuditUserVersion() = version

  override fun getUserLocale() = locale

  companion object {
    private val USER_AND_ADMIN_AUTHORITIES = createAuthorityList(USER.roleName(), ADMIN.roleName())
    private val USER_AUTHORITY = createAuthorityList(USER.roleName())
  }

  override fun getId(): UserId = id
}
