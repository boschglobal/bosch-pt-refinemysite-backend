/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2024
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.user.query

import com.bosch.pt.csm.cloud.common.api.UserId
import java.util.Locale
import java.util.UUID
import java.util.UUID.randomUUID

class UserProjectionBuilder private constructor() {

  private var identifier: UUID? = null
  private var version: Long? = null
  private var ciamUserIdentifier: String? = null
  private var admin = false
  private var locked = false
  private var locale: Locale? = null

  fun withCiamUserId(ciamUserIdentifier: String): UserProjectionBuilder {
    this.ciamUserIdentifier = ciamUserIdentifier
    return this
  }

  fun withIdentifier(identifier: UUID): UserProjectionBuilder {
    this.identifier = identifier
    return this
  }

  fun withVersion(version: Long): UserProjectionBuilder {
    this.version = version
    return this
  }

  fun asAdmin(): UserProjectionBuilder {
    admin = true
    return this
  }

  fun asLocked(): UserProjectionBuilder {
    locked = true
    return this
  }

  fun withLocale(locale: Locale): UserProjectionBuilder {
    this.locale = locale
    return this
  }

  fun build(): UserProjection =
      UserProjection(
          UserId(requireNotNull(identifier)),
          requireNotNull(version),
          requireNotNull(ciamUserIdentifier),
          admin,
          locked,
          locale)

  companion object {

    fun user(): UserProjectionBuilder =
        randomUUID().let {
          UserProjectionBuilder().withCiamUserId("UID$it").withIdentifier(it).withVersion(0L)
        }
  }
}
