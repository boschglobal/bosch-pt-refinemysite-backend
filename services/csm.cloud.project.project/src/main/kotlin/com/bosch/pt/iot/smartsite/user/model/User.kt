/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.user.model

import com.bosch.pt.csm.cloud.common.ExcludeFromCodeCoverageGenerated
import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.command.snapshotstore.AuditingUser
import com.bosch.pt.csm.cloud.common.extensions.toLocalDateTimeByMillis
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.facade.rest.UserLocale
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReference
import com.bosch.pt.csm.cloud.common.model.IsoCountryCodeEnum
import com.bosch.pt.csm.cloud.usermanagement.common.UsermanagementAggregateTypeEnum
import com.bosch.pt.csm.cloud.usermanagement.common.messages.PhoneNumberAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserAggregateAvro
import com.bosch.pt.iot.smartsite.common.model.AbstractReplicatedEntity
import com.bosch.pt.iot.smartsite.craft.model.Craft
import com.bosch.pt.iot.smartsite.user.constants.RoleConstants
import jakarta.persistence.AssociationOverride
import jakarta.persistence.AssociationOverrides
import jakarta.persistence.AttributeOverride
import jakarta.persistence.AttributeOverrides
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.ForeignKey
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import jakarta.validation.constraints.Size
import java.util.Locale
import java.util.Optional
import java.util.UUID
import java.util.function.Supplier
import org.apache.commons.lang3.LocaleUtils
import org.apache.commons.lang3.builder.ToStringBuilder
import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.userdetails.UserDetails

@Entity
@AttributeOverrides(
    AttributeOverride(name = "createdDate", column = Column(nullable = true)),
    AttributeOverride(name = "lastModifiedDate", column = Column(nullable = true)))
@AssociationOverrides(
    AssociationOverride(
        name = "createdBy",
        joinColumns = [JoinColumn(nullable = true)],
        foreignKey = ForeignKey(name = "FK_User_CreatedBy")),
    AssociationOverride(
        name = "lastModifiedBy",
        joinColumns = [JoinColumn(nullable = true)],
        foreignKey = ForeignKey(name = "FK_User_LastModifiedBy")))
@Table(
    name = "USER_ENTITY",
    uniqueConstraints = [UniqueConstraint(name = "UK_UserId", columnNames = ["user_id"])],
    indexes =
        [
            Index(name = "UK_User_Identifier", columnList = "identifier", unique = true),
            Index(name = "UK_User_Email", columnList = "email", unique = true)])
class User : AbstractReplicatedEntity<Long>, UserDetails, UserLocale, AuditingUser {

  @Fetch(FetchMode.JOIN)
  @OneToOne(mappedBy = "user", fetch = FetchType.LAZY)
  var profilePicture: ProfilePicture? = null

  @field:Size(max = MAX_CIAM_USER_ID_LENGTH)
  @Column(length = MAX_CIAM_USER_ID_LENGTH, name = "user_id")
  var ciamUserIdentifier: String? = null

  @Enumerated(EnumType.STRING)
  @Column(length = MAX_GENDER_LENGTH, columnDefinition = "varchar(20)")
  var gender: GenderEnum? = null

  @field:Size(max = MAX_FIRST_NAME_LENGTH)
  @Column(length = MAX_FIRST_NAME_LENGTH)
  var firstName: String? = null

  @field:Size(max = MAX_LAST_NAME_LENGTH)
  @Column(length = MAX_LAST_NAME_LENGTH)
  var lastName: String? = null

  @field:Size(max = MAX_EMAIL_LENGTH)
  @Column(length = MAX_EMAIL_LENGTH)
  var email: String? = null
    set(value) {
      field = value?.lowercase(Locale.getDefault())
    }

  @field:Size(max = MAX_POSITION_LENGTH)
  @Column(length = MAX_POSITION_LENGTH)
  var position: String? = null

  @Column(nullable = false) var registered = false

  @Column(nullable = false) var admin = false

  @Column(nullable = false) var deleted = false

  @Column(nullable = false) var locked = false

  private var locale: Locale? = null

  @Column(columnDefinition = "varchar(255)")
  @Enumerated(EnumType.STRING)
  var country: IsoCountryCodeEnum? = null

  @ManyToMany
  @JoinTable(
      name = "user_craft",
      joinColumns =
          [JoinColumn(name = "user_id", foreignKey = ForeignKey(name = "FK_User_Craft_UserId"))],
      inverseJoinColumns =
          [JoinColumn(name = "craft_id", foreignKey = ForeignKey(name = "FK_User_Craft_CraftId"))])
  var crafts: MutableSet<Craft> = HashSet()

  @field:Size(max = MAX_PHONE_NUMBERS)
  @ElementCollection
  @CollectionTable(
      name = "USER_PHONENUMBER",
      foreignKey = ForeignKey(name = "FK_User_PhoneNumber_UserId"),
      joinColumns = [JoinColumn(name = "USER_ID")])
  var phonenumbers: MutableSet<PhoneNumber> = HashSet()

  /** Constructor. */
  constructor() {
    // Just for JPA
  }

  constructor(
      identifier: UUID?,
      version: Long?,
      ciamUserIdentifier: String?,
      gender: GenderEnum?,
      firstName: String?,
      lastName: String?,
      email: String?,
      position: String?,
      registered: Boolean,
      admin: Boolean,
      locked: Boolean,
      locale: Locale?,
      country: IsoCountryCodeEnum?,
      crafts: Set<Craft>?,
      phoneNumbers: Set<PhoneNumber>?
  ) {
    this.identifier = identifier
    this.version = version
    this.ciamUserIdentifier = ciamUserIdentifier
    this.gender = gender
    this.firstName = firstName
    this.lastName = lastName
    this.email = email?.lowercase(Locale.getDefault())
    this.position = position
    this.registered = registered
    this.admin = admin
    this.locked = locked
    this.locale = locale
    this.country = country
    crafts?.let { this.crafts.addAll(it) }
    phoneNumbers?.let { this.phonenumbers.addAll(it) }
  }

  override fun getAuthorities(): Collection<GrantedAuthority> =
      if (admin) USER_AND_ADMIN_AUTHORITIES else USER_AUTHORITY

  public override fun setId(id: Long?) {
    super.setId(id)
  }

  override fun getPassword(): String = "secret"

  override fun getUsername(): String = email!!

  @ExcludeFromCodeCoverageGenerated override fun isAccountNonExpired(): Boolean = true

  @ExcludeFromCodeCoverageGenerated override fun isAccountNonLocked(): Boolean = !locked

  @ExcludeFromCodeCoverageGenerated override fun isCredentialsNonExpired(): Boolean = true

  @ExcludeFromCodeCoverageGenerated override fun isEnabled(): Boolean = true

  override fun getUserLocale(): Locale? = locale

  override fun getAuditUserId() = UserId(identifier!!)

  override fun getAuditUserVersion() = version!!

  @ExcludeFromCodeCoverageGenerated
  override fun toString(): String =
      ToStringBuilder(this)
          .appendSuper(super.toString())
          .append("ciamUserIdentifier", ciamUserIdentifier)
          .append("gender", gender)
          .append(FIRST_NAME, firstName)
          .append(LAST_NAME, lastName)
          .append("email", email)
          .append("position", position)
          .append("registered", registered)
          .append("admin", admin)
          .append("locked", locked)
          .append("locale", locale)
          .append("authorities", authorities)
          .toString()

  fun setUserLocale(locale: Locale?) {
    this.locale = locale
  }

  fun anonymize() {
    admin = false
    crafts.clear()
    deleted = true
    email = null
    firstName = null
    gender = null
    lastName = null
    locked = false
    setUserLocale(null)
    country = null
    phonenumbers.clear()
    position = null
    registered = false
    ciamUserIdentifier = null
    setCreatedBy(null)
    setLastModifiedBy(null)
    setCreatedDate(null)
    setLastModifiedDate(null)
  }

  override fun getAggregateType(): String = UsermanagementAggregateTypeEnum.USER.value

  override fun getDisplayName(): String? = Companion.getDisplayName(this.firstName, this.lastName)

  override fun getIdentifierUuid(): UUID = this.identifier!!

  companion object {
    private const val serialVersionUID: Long = -765541368149104242L

    const val FIRST_NAME = "firstName"
    const val LAST_NAME = "lastName"
    const val MAX_EMAIL_LENGTH = 255

    private const val MAX_FIRST_NAME_LENGTH = 50
    private const val MAX_LAST_NAME_LENGTH = 50
    private const val MAX_POSITION_LENGTH = 100
    private const val MAX_CIAM_USER_ID_LENGTH = 100
    private const val MAX_PHONE_NUMBERS = 5
    private const val MAX_GENDER_LENGTH = 20

    private val USER_AND_ADMIN_AUTHORITIES =
        AuthorityUtils.createAuthorityList(
            RoleConstants.USER.roleName(), RoleConstants.ADMIN.roleName())

    private val USER_AUTHORITY = AuthorityUtils.createAuthorityList(RoleConstants.USER.roleName())

    @JvmStatic
    fun referTo(
        user: User?,
        deletedUserReference: Supplier<ResourceReference>
    ): ResourceReference? = referTo(Optional.ofNullable(user), deletedUserReference)

    @JvmStatic
    fun referTo(
        user: Optional<User>,
        deletedUserReference: Supplier<ResourceReference>
    ): ResourceReference? =
        if (user.isPresent)
            if (user.get().deleted) deletedUserReference.get()
            else ResourceReference.from(user.get())
        else null

    @JvmStatic
    fun referTo(
        identifier: UUID?,
        firstName: String?,
        lastName: String?,
        deletedUserReference: Supplier<ResourceReference>,
        isDeleted: Boolean
    ): ResourceReference? {
      if (identifier == null) {
        return null
      }
      return if (isDeleted) deletedUserReference.get()
      else ResourceReference(identifier, getDisplayName(firstName, lastName).orEmpty())
    }

    @JvmStatic
    fun getDisplayName(firstName: String?, lastName: String?): String? =
        if (firstName != null) {
          if (lastName != null) {
            "$firstName $lastName"
          } else firstName
        } else lastName

    fun fromAvroMessage(
        aggregate: UserAggregateAvro,
        crafts: Set<Craft>?,
        createdBy: User?,
        lastModifiedBy: User?
    ): User =
        User(
                aggregate.getAggregateIdentifier().getIdentifier().toUUID(),
                aggregate.getAggregateIdentifier().getVersion(),
                aggregate.getUserId(),
                if (aggregate.getGender() == null) null
                else GenderEnum.valueOf(aggregate.getGender().name),
                aggregate.getFirstName(),
                aggregate.getLastName(),
                aggregate.getEmail(),
                aggregate.getPosition(),
                aggregate.getRegistered(),
                aggregate.getAdmin(),
                aggregate.getLocked(),
                if (aggregate.getLocale() == null) null
                else LocaleUtils.toLocale(aggregate.getLocale()),
                if (aggregate.getCountry() == null) null
                else IsoCountryCodeEnum.valueOf(aggregate.getCountry().name),
                crafts,
                aggregate
                    .getPhoneNumbers()
                    .map { phoneNumber: PhoneNumberAvro ->
                      PhoneNumber(
                          PhoneNumberType.valueOf(phoneNumber.getPhoneNumberType().name),
                          phoneNumber.getCountryCode(),
                          phoneNumber.getCallNumber())
                    }
                    .toSet())
            .apply {
              setCreatedBy(createdBy)
              setLastModifiedBy(lastModifiedBy)
              setCreatedDate(
                  aggregate.getAuditingInformation().getCreatedDate().toLocalDateTimeByMillis())
              setLastModifiedDate(
                  aggregate
                      .getAuditingInformation()
                      .getLastModifiedDate()
                      .toLocalDateTimeByMillis())
            }
  }
}
