/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.projectmanagement.user.user.query.model

import com.bosch.pt.csm.cloud.common.ExcludeFromCodeCoverage
import com.bosch.pt.csm.cloud.common.Referable
import com.bosch.pt.csm.cloud.common.facade.rest.UserLocale
import com.bosch.pt.csm.cloud.common.model.IsoCountryCodeEnum
import com.bosch.pt.csm.cloud.projectmanagement.application.security.IdentifiableUserDetails
import com.bosch.pt.csm.cloud.projectmanagement.application.security.RoleConstants.ADMIN
import com.bosch.pt.csm.cloud.projectmanagement.application.security.RoleConstants.USER
import com.bosch.pt.csm.cloud.projectmanagement.craft.craft.domain.CraftId
import com.bosch.pt.csm.cloud.projectmanagement.translation.shared.model.TranslatableEnum
import com.bosch.pt.csm.cloud.projectmanagement.user.user.domain.UserId
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Locale
import java.util.UUID
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.TypeAlias
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.AuthorityUtils.createAuthorityList

const val USER_PROJECTION = "UserProjection"

@Document(USER_PROJECTION)
@TypeAlias(USER_PROJECTION)
data class UserProjection(
    @Id val identifier: UserId,
    val version: Long,
    val idpIdentifier: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val position: String? = null,
    val eulaAcceptedDate: LocalDate? = null,
    val admin: Boolean = false,
    val locked: Boolean = false,
    val locale: Locale? = null,
    val country: IsoCountryCodeEnum? = null,
    val crafts: List<CraftId>,
    val phoneNumbers: List<PhoneNumber>,
    val eventAuthor: UserId,
    val eventDate: LocalDateTime,
    val history: List<UserVersion>
) : IdentifiableUserDetails, UserLocale, Referable {

  override fun getAuthorities(): Collection<GrantedAuthority> =
      if (admin) USER_AND_ADMIN_AUTHORITIES else USER_AUTHORITY

  override fun getPassword(): String = "secret"

  override fun getUsername(): String = this.email

  @ExcludeFromCodeCoverage override fun isAccountNonExpired(): Boolean = true

  @ExcludeFromCodeCoverage override fun isAccountNonLocked(): Boolean = !locked

  @ExcludeFromCodeCoverage override fun isCredentialsNonExpired(): Boolean = true

  @ExcludeFromCodeCoverage override fun isEnabled(): Boolean = true

  override fun getDisplayName(): String = "$firstName $lastName"

  override fun getIdentifierUuid(): UUID = identifier.value

  override fun userIdentifier(): UUID = identifier.value

  override fun getUserLocale() = locale

  companion object {
    private val USER_AND_ADMIN_AUTHORITIES = createAuthorityList(USER.roleName(), ADMIN.roleName())
    private val USER_AUTHORITY = createAuthorityList(USER.roleName())
  }
}

data class PhoneNumber(
    val countryCode: String,
    val phoneNumberType: PhoneNumberTypeEnum,
    val callNumber: String
)

enum class PhoneNumberTypeEnum(private val type: String) : TranslatableEnum {
  ASSISTANT("ASSISTANT"),
  BUSINESS("BUSINESS"),
  FAX("FAX"),
  HOME("HOME"),
  MOBILE("MOBILE"),
  ORGANIZATION("ORGANIZATION"),
  OTHER("OTHER"),
  PAGER("PAGER");

  companion object {
    const val KEY_PREFIX: String = "PHONE_NUMBER_TYPE_"
  }

  val shortKey: String
    get() = this.type

  override val key: String
    get() = "${KEY_PREFIX}${this.type}"

  override val messageKey: String
    get() = "${PhoneNumberTypeEnum::class.simpleName}_$this"
}

data class UserVersion(
    val version: Long,
    val idpIdentifier: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val position: String? = null,
    val eulaAcceptedDate: LocalDate? = null,
    val admin: Boolean = false,
    val locked: Boolean = false,
    val locale: Locale? = null,
    val country: IsoCountryCodeEnum? = null,
    val crafts: List<CraftId>,
    val phoneNumbers: List<PhoneNumber>,
    val eventAuthor: UserId,
    val eventDate: LocalDateTime,
)
