/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.user.user.shared.model

import com.bosch.pt.csm.cloud.common.model.IsoCountryCodeEnum
import com.bosch.pt.csm.cloud.usermanagement.user.user.GenderEnum.MALE
import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.usermanagement.user.user.UserRoleEnum.USER
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Locale
import java.util.UUID.randomUUID
import org.springframework.security.core.authority.SimpleGrantedAuthority

object UserBuilder {
  fun defaultUser(): User =
      User(
              UserId(),
              "UID${randomUUID()}",
              MALE,
              "Hans",
              "Mustermann",
              "${randomUUID()}@example.com",
              Locale.UK,
              IsoCountryCodeEnum.GB,
              LocalDate.now())
          .apply {
            this.authorities = listOf(SimpleGrantedAuthority(USER.roleName()))
            setCreatedDate(LocalDateTime.now())
            setLastModifiedDate(LocalDateTime.now())
          }
}
