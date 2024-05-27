/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.user.user.shared.model

import com.bosch.pt.csm.cloud.common.ExcludeFromCodeCoverage
import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.command.snapshotstore.AbstractSnapshotEntity
import com.bosch.pt.csm.cloud.common.command.snapshotstore.AuditingUser
import com.bosch.pt.csm.cloud.common.facade.rest.UserLocale
import com.bosch.pt.csm.cloud.common.model.IsoCountryCodeEnum
import com.bosch.pt.csm.cloud.usermanagement.craft.craft.shared.model.Craft
import com.bosch.pt.csm.cloud.usermanagement.user.picture.shared.model.ProfilePicture
import com.bosch.pt.csm.cloud.usermanagement.user.user.GenderEnum
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.EnumType.STRING
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType.EAGER
import jakarta.persistence.FetchType.LAZY
import jakarta.persistence.ForeignKey
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.persistence.Transient
import jakarta.persistence.UniqueConstraint
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDate
import java.util.Locale
import java.util.UUID
import org.apache.commons.lang3.builder.HashCodeBuilder
import org.apache.commons.lang3.builder.ToStringBuilder
import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode.JOIN
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

@Entity
@Table(
    name = "USER_ENTITY",
    uniqueConstraints = [UniqueConstraint(name = "UK_UserId", columnNames = ["user_id"])],
    indexes =
        [
            Index(name = "UK_User_Identifier", columnList = "identifier", unique = true),
            Index(name = "UK_User_Email", columnList = "email", unique = true)])
class User() : AbstractSnapshotEntity<Long, UserId>(), UserDetails, UserLocale, AuditingUser {

  @Fetch(JOIN) // Workaround because bidirectional @OneToOne relations cannot be fetched lazily
  @OneToOne(mappedBy = "user", fetch = LAZY)
  var profilePicture: ProfilePicture? = null

  @Column(name = "user_id", nullable = false, length = MAX_USER_ID_LENGTH)
  var externalUserId: @NotNull @Size(min = 1, max = MAX_USER_ID_LENGTH) String? = null

  @Enumerated(STRING)
  @Column(length = MAX_GENDER_LENGTH, columnDefinition = "varchar(20)")
  var gender: GenderEnum? = null

  @Column(length = MAX_FIRST_NAME_LENGTH)
  var firstName: @Size(max = MAX_FIRST_NAME_LENGTH) String? = null

  @Column(length = MAX_LAST_NAME_LENGTH)
  var lastName: @Size(max = MAX_LAST_NAME_LENGTH) String? = null

  @Column(nullable = false, length = MAX_EMAIL_LENGTH)
  var email: @NotNull @Size(min = 1, max = MAX_EMAIL_LENGTH) String? = null
    set(value) {
      field = value?.lowercase()
    }

  @Column(length = MAX_POSITION_LENGTH)
  var position: @Size(max = MAX_POSITION_LENGTH) String? = null

  @Column(nullable = false) var registered = false

  @Column(nullable = false) var admin = false

  @Column(nullable = false) var locked = false

  var eulaAcceptedDate: LocalDate? = null

  @ManyToMany
  @JoinTable(
      name = "user_craft",
      joinColumns =
          [JoinColumn(name = "user_id", foreignKey = ForeignKey(name = "FK_User_Craft_UserId"))],
      inverseJoinColumns =
          [JoinColumn(name = "craft_id", foreignKey = ForeignKey(name = "FK_User_Craft_CraftId"))])
  var crafts: MutableSet<Craft> = HashSet()

  @ElementCollection(fetch = EAGER)
  @CollectionTable(
      name = "USER_PHONENUMBER",
      foreignKey = ForeignKey(name = "FK_User_PhoneNumber_UserId"),
      joinColumns = [JoinColumn(name = "USER_ID")])
  var phonenumbers: @Size(max = MAX_PHONE_NUMBERS) MutableSet<PhoneNumber> = HashSet()

  var locale: Locale? = null

  @Column(columnDefinition = "varchar(255)")
  @Enumerated(STRING)
  var country: IsoCountryCodeEnum? = null

  @Transient private var authorities: Collection<GrantedAuthority> = HashSet()

  /**
   * This constructor contains all mandatory attributes of a user. The attribute definitions of the
   * class are sometimes optional, although some attributes are actually mandatory. This made the
   * code to restore the snapshots from the kafka events easier and in addition, we have historical
   * events in the stream where attributes likes firstname or lastername are null. This was the case
   * when users where technically created before the actual sign up. For new users, this is no
   * longer happening, since these fields are now mandatory in the registration command.
   *
   * Please use this constructor to create a user with all mandatory fields set.
   */
  constructor(
      identifier: UserId,
      externalUserId: String,
      gender: GenderEnum,
      firstName: String,
      lastName: String,
      email: String,
      locale: Locale,
      country: IsoCountryCodeEnum,
      eulaAcceptedDate: LocalDate
  ) : this() {
    this.identifier = identifier
    this.externalUserId = externalUserId
    this.email = email
    this.gender = gender
    this.firstName = firstName
    this.lastName = lastName
    this.locale = locale
    this.country = country
    this.eulaAcceptedDate = eulaAcceptedDate
  }

  override fun getAuditUserId() = identifier

  override fun getAuditUserVersion() = version

  @ExcludeFromCodeCoverage
  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other !is User) {
      return false
    }
    return externalUserId == other.externalUserId
  }

  @ExcludeFromCodeCoverage
  override fun hashCode(): Int = HashCodeBuilder(17, 31).append(externalUserId).toHashCode()

  override fun getAuthorities(): Collection<GrantedAuthority> = authorities

  fun setAuthorities(authorities: Collection<GrantedAuthority>) {
    this.authorities = authorities
  }

  @ExcludeFromCodeCoverage override fun getPassword() = "secret"

  @ExcludeFromCodeCoverage override fun getUsername(): String? = email

  @ExcludeFromCodeCoverage override fun isAccountNonExpired() = true

  @ExcludeFromCodeCoverage override fun isAccountNonLocked() = !locked

  @ExcludeFromCodeCoverage override fun isCredentialsNonExpired() = true

  @ExcludeFromCodeCoverage override fun isEnabled() = true

  override fun getDisplayName(): String? = getFullName()

  override fun getIdentifierUuid(): UUID = this.identifier.identifier

  @ExcludeFromCodeCoverage
  override fun toString(): String =
      ToStringBuilder(this)
          .apply {
            appendSuper(super.toString())
            append("userId", externalUserId)
            append("gender", gender)
            append(FIRST_NAME, firstName)
            append(LAST_NAME, lastName)
            append("email", email)
            append("position", position)
            append("registered", registered)
            append("admin", admin)
            append("authorities", getAuthorities())
            append("eulaAcceptedDate", eulaAcceptedDate)
            append("locked", locked)
            append("locale", locale)
            append("country", country)
            // user maybe not loaded with details. skip potential lazy exception calls
            // append("craftIds", getCrafts());
            // append("phonenumbers", getPhonenumbers());
          }
          .toString()

  /**
   * Returns the full name of the user.
   *
   * @return the full name
   */
  fun getFullName(): String? =
      if (firstName != null) {
        if (lastName != null) {
          "$firstName $lastName"
        } else firstName
      } else lastName

  override fun getUserLocale() = locale

  companion object {
    const val FIRST_NAME = "firstName"
    const val LAST_NAME = "lastName"
    const val MAX_EMAIL_LENGTH = 255
    const val MAX_FIRST_NAME_LENGTH = 50
    const val MAX_LAST_NAME_LENGTH = 50
    const val MAX_POSITION_LENGTH = 100
    const val MAX_USER_ID_LENGTH = 100
    const val MAX_PHONE_NUMBERS = 5
    private const val MAX_GENDER_LENGTH = 20
  }
}
