/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.user.user.command.snapshotstore

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.command.handler.CommandHandler
import com.bosch.pt.csm.cloud.common.command.snapshotstore.AuditableSnapshot
import com.bosch.pt.csm.cloud.common.command.snapshotstore.VersionedSnapshot
import com.bosch.pt.csm.cloud.common.model.AggregateIdentifier
import com.bosch.pt.csm.cloud.common.model.IsoCountryCodeEnum
import com.bosch.pt.csm.cloud.usermanagement.user.user.GenderEnum
import com.bosch.pt.csm.cloud.usermanagement.user.user.api.PhoneNumberCommandDto
import com.bosch.pt.csm.cloud.usermanagement.user.user.command.mapper.UserAvroSnapshotMapper
import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.model.PhoneNumber
import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.model.PhoneNumberType
import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.model.User
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Locale

data class UserSnapshot(
    override var identifier: UserId,
    override var version: Long,
    override var createdDate: LocalDateTime?,
    override var createdBy: UserId?,
    override var lastModifiedDate: LocalDateTime?,
    override var lastModifiedBy: UserId?,
    val externalUserId: String?,
    var gender: GenderEnum?,
    var firstName: String?,
    var lastName: String?,
    var email: String?,
    var position: String?,
    var registered: Boolean,
    var admin: Boolean,
    var locked: Boolean,
    var eulaAcceptedDate: LocalDate?,
    var locale: Locale?,
    var country: IsoCountryCodeEnum?,
    var crafts: Set<AggregateIdentifier>,
    var phonenumbers: Set<PhoneNumberValueObject>
) : VersionedSnapshot, AuditableSnapshot {

  constructor(
      user: User
  ) : this(
      user.identifier,
      user.version,
      user.createdDate.orElse(null),
      user.createdBy.orElse(null),
      user.lastModifiedDate.orElse(null),
      user.lastModifiedBy.orElse(null),
      user.externalUserId,
      user.gender,
      user.firstName,
      user.lastName,
      user.email,
      user.position,
      user.registered,
      user.admin,
      user.locked,
      user.eulaAcceptedDate,
      user.locale,
      user.country,
      user.crafts.map { it.asAggregateIdentifier() }.toSet(),
      user.phonenumbers.map { it.asValueObject() }.toSet())

  fun toCommandHandler() = CommandHandler.of(this, UserAvroSnapshotMapper)
}

fun User.asValueObject() = UserSnapshot(this)

data class PhoneNumberValueObject(
    var countryCode: String?,
    var phoneNumberType: PhoneNumberType?,
    var callNumber: String?
) {
  constructor(
      phoneNumber: PhoneNumber
  ) : this(phoneNumber.countryCode, phoneNumber.phoneNumberType, phoneNumber.callNumber)

  constructor(
      phoneNumber: PhoneNumberCommandDto
  ) : this(phoneNumber.countryCode, phoneNumber.phoneNumberType, phoneNumber.callNumber)
}

fun PhoneNumber.asValueObject() = PhoneNumberValueObject(this)
