/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.user.user.model

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.extensions.toDate
import com.bosch.pt.csm.cloud.common.model.IsoCountryCodeEnum
import com.bosch.pt.csm.user.user.query.UserProjection
import java.time.LocalDateTime
import java.time.LocalDateTime.now
import java.util.Locale
import org.springframework.test.util.ReflectionTestUtils

/** Builder object for [UserProjection]. */
class UserBuilder
/** Private constructor. */
private constructor() {

  private var createdBy: UserId? = null
  private var createdDate = now()
  private var lastModifiedBy: UserId? = null
  private var lastModifiedDate = now()
  private var identifier: UserId? = null
  private var version: Long? = null
  private var userId: String? = null
  private var firstName: String? = null
  private var lastName: String? = null
  private var email: String? = null
  private var admin = false
  private var internalVersion: Long? = null
  private var technicalId: Long? = null
  private var locked = false
  private var locale: Locale? = null
  private var country: IsoCountryCodeEnum? = null

  /**
   * Sets the user-id (from eIDP).
   *
   * @param userId the user-id
   * @return the builder
   */
  fun withUserId(userId: String): UserBuilder = this.apply { this.userId = userId }

  fun withFirstName(firstName: String): UserBuilder = this.apply { this.firstName = firstName }

  fun withLastName(lastName: String): UserBuilder = this.apply { this.lastName = lastName }

  fun withEmail(email: String): UserBuilder = this.apply { this.email = email }

  fun withCreatedDate(createdDate: LocalDateTime): UserBuilder =
      this.apply { this.createdDate = createdDate }

  fun withLastModifiedDate(lastModifiedDate: LocalDateTime): UserBuilder =
      this.apply { this.lastModifiedDate = lastModifiedDate }

  fun withCreatedBy(createdBy: UserId): UserBuilder = this.apply { this.createdBy = createdBy }

  fun withLastModifiedBy(lastModifiedBy: UserId): UserBuilder =
      this.apply { this.lastModifiedBy = lastModifiedBy }

  fun withIdentifier(identifier: UserId): UserBuilder = this.apply { this.identifier = identifier }

  fun withVersion(version: Long): UserBuilder = this.apply { this.version = version }

  fun withAdmin(admin: Boolean): UserBuilder = this.apply { this.admin = admin }

  fun asAdmin(): UserBuilder = this.apply { admin = true }

  fun withLocale(locale: Locale?): UserBuilder = this.apply { this.locale = locale }

  fun withCountry(country: IsoCountryCodeEnum): UserBuilder = this.apply { this.country = country }

  /**
   * Sets the internal version number of the user for optimistic locking and calculating the ETag.
   *
   * @param internalVersion the internal version
   * @return the builder
   */
  fun withInternalVersion(internalVersion: Long): UserBuilder =
      this.apply { this.internalVersion = internalVersion }

  /**
   * Sets the technical id for the user.
   *
   * @param technicalId the technical id
   * @return the builder
   */
  fun withTechnicalId(technicalId: Long): UserBuilder =
      this.apply { this.technicalId = technicalId }

  fun build(): UserProjection {
    val user =
        UserProjection(
            identifier!!,
            createdBy!!,
            createdDate!!.toDate(),
            lastModifiedBy!!,
            lastModifiedDate!!.toDate(),
            version!!,
            userId!!,
            firstName!!,
            lastName!!,
            email!!,
            admin,
            locked,
            locale,
            country)

    if (internalVersion != null) {
      ReflectionTestUtils.setField(user, "version", internalVersion)
    }
    if (technicalId != null) {
      ReflectionTestUtils.setField(user, "id", technicalId)
    }

    return user
  }

  companion object {

    fun user(): UserBuilder {
      val identifier = UserId()
      return UserBuilder()
          .withIdentifier(identifier)
          .withCreatedBy(identifier)
          .withCreatedDate(now())
          .withLastModifiedBy(identifier)
          .withLastModifiedDate(now())
          .withVersion(0L)
          .withUserId("UID$identifier")
          .withFirstName("Hans")
          .withLastName("Mustermann")
          .withEmail(identifier.toString() + "hans.mustermann@example.com")
    }
  }
}
