/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.user.model

import com.bosch.pt.csm.cloud.common.model.IsoCountryCodeEnum
import com.bosch.pt.iot.smartsite.craft.model.Craft
import com.bosch.pt.iot.smartsite.user.model.GenderEnum.MALE
import java.time.LocalDateTime
import java.time.LocalDateTime.now
import java.util.Locale
import java.util.UUID
import java.util.UUID.randomUUID
import org.apache.commons.lang3.StringUtils.EMPTY
import org.springframework.test.util.ReflectionTestUtils

class UserBuilder private constructor() {

  private var createdBy: User? = null
  private var createdDate = now()
  private var lastModifiedBy: User? = null
  private var lastModifiedDate = now()
  private var identifier: UUID? = null
  private var version: Long? = null
  private var userId: String? = null
  private var gender: GenderEnum? = null
  private var firstName: String? = null
  private var lastName: String? = null
  private var email: String? = null
  private var position: String? = null
  private var registered = false
  private var admin = false
  private var crafts: Set<Craft> = HashSet()
  private var phonenumbers: Set<PhoneNumber> = HashSet()
  private var internalVersion: Long? = null
  private var technicalId: Long? = null
  private var deleted = false
  private var locked = false
  private var locale: Locale? = null
  private var country: IsoCountryCodeEnum? = null
  private var createsHimself = false

  fun withUserId(userId: String?): UserBuilder = apply { this.userId = userId }

  fun withGender(gender: GenderEnum?): UserBuilder = apply { this.gender = gender }

  fun withFirstName(firstName: String?): UserBuilder = apply { this.firstName = firstName }

  fun withLastName(lastName: String?): UserBuilder = apply { this.lastName = lastName }

  fun withEmail(email: String?): UserBuilder = apply { this.email = email }

  fun withCreatedDate(createdDate: LocalDateTime?): UserBuilder = apply {
    this.createdDate = createdDate
  }

  fun withLastModifiedDate(lastModifiedDate: LocalDateTime?): UserBuilder = apply {
    this.lastModifiedDate = lastModifiedDate
  }

  fun withCreatedBy(createdBy: User?): UserBuilder = apply { this.createdBy = createdBy }

  fun withCreatedByIsTheUserItself(): UserBuilder = apply { createsHimself = true }

  fun withLastModifiedBy(lastModifiedBy: User?): UserBuilder = apply {
    this.lastModifiedBy = lastModifiedBy
  }

  fun withIdentifier(identifier: UUID?): UserBuilder = apply { this.identifier = identifier }

  fun withVersion(version: Long?): UserBuilder = apply { this.version = version }

  fun asDeleted(deleted: Boolean): UserBuilder = apply { this.deleted = deleted }

  fun asRegistered(): UserBuilder = apply { registered = true }

  fun asUnregistered(): UserBuilder = apply { registered = false }

  fun withPosition(position: String?): UserBuilder = apply { this.position = position }

  fun asAdmin(): UserBuilder = apply { admin = true }

  fun asLocked(): UserBuilder = apply { locked = true }

  fun withLocale(locale: Locale?): UserBuilder = apply { this.locale = locale }

  fun withCountry(country: IsoCountryCodeEnum?): UserBuilder = apply { this.country = country }

  fun withCrafts(crafts: Set<Craft>): UserBuilder = apply { this.crafts = crafts }

  fun withPhoneNumbers(phoneNumbers: Set<PhoneNumber>): UserBuilder = apply {
    this.phonenumbers = phoneNumbers
  }

  fun withInternalVersion(internalVersion: Long?): UserBuilder = apply {
    this.internalVersion = internalVersion
  }

  fun withTechnicalId(technicalId: Long?): UserBuilder = apply { this.technicalId = technicalId }

  fun build(): User {
    val user =
        User(
            identifier,
            version,
            userId,
            gender,
            firstName,
            lastName,
            email,
            position,
            registered,
            admin,
            locked,
            locale,
            country,
            crafts,
            phonenumbers)
    if (createdDate != null) {
      user.setCreatedDate(createdDate)
    }
    if (lastModifiedDate != null) {
      user.setLastModifiedDate(lastModifiedDate)
    }
    if (internalVersion != null) {
      ReflectionTestUtils.setField(user, "version", internalVersion)
    }
    if (technicalId != null) {
      ReflectionTestUtils.setField(user, "id", technicalId)
    }
    if (createsHimself) {
      user.setCreatedBy(user)
      user.setLastModifiedBy(user)
    } else {
      user.setCreatedBy(createdBy)
      user.setLastModifiedBy(lastModifiedBy)
    }
    user.deleted = deleted
    return user
  }

  companion object {

    @JvmStatic
    fun user(): UserBuilder =
        with(randomUUID()) {
          UserBuilder()
              .withUserId("UID$this")
              .withFirstName("Hans")
              .withLastName("Mustermann")
              .withEmail(this.toString() + "hans.mustermann@example.com")
              .withPosition(EMPTY)
              .withCrafts(HashSet())
              .withPhoneNumbers(HashSet())
              .withGender(MALE)
              .withIdentifier(this)
              .withInternalVersion(0L)
        }
  }
}
