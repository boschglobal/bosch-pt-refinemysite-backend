/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.user.user.command.mapper

import com.bosch.pt.csm.cloud.common.command.mapper.AbstractAvroSnapshotMapper
import com.bosch.pt.csm.cloud.common.messages.IsoCountryCodeEnumAvro
import com.bosch.pt.csm.cloud.usermanagement.common.UsermanagementAggregateTypeEnum.USER
import com.bosch.pt.csm.cloud.usermanagement.common.messages.GenderEnumAvro
import com.bosch.pt.csm.cloud.usermanagement.common.messages.PhoneNumberAvro
import com.bosch.pt.csm.cloud.usermanagement.common.messages.PhoneNumberTypeEnumAvro
import com.bosch.pt.csm.cloud.usermanagement.common.utility.toAggregateIdentifierAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserAggregateAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventEnumAvro
import com.bosch.pt.csm.cloud.usermanagement.user.user.command.snapshotstore.UserSnapshot
import java.time.ZoneOffset
import org.apache.avro.specific.SpecificRecordBase

object UserAvroSnapshotMapper : AbstractAvroSnapshotMapper<UserSnapshot>() {

  override fun <E : Enum<*>> toAvroMessageWithNewVersion(
      snapshot: UserSnapshot,
      eventType: E
  ): SpecificRecordBase {
    val phoneNumbers =
        snapshot.phonenumbers.map {
          PhoneNumberAvro.newBuilder()
              .setPhoneNumberType(PhoneNumberTypeEnumAvro.valueOf(it.phoneNumberType.toString()))
              .setCountryCode(it.countryCode)
              .setCallNumber(it.callNumber)
              .build()
        }

    val userAggregateAvro =
        UserAggregateAvro.newBuilder()
            .setAggregateIdentifier(toAggregateIdentifierAvroWithNextVersion(snapshot))
            .setAuditingInformation(toUpdatedAuditingInformationAvro(snapshot))
            .setEmail(snapshot.email)
            .setFirstName(snapshot.firstName)
            .setGender(
                if (snapshot.gender == null) null
                else GenderEnumAvro.valueOf(snapshot.gender.toString()))
            .setLastName(snapshot.lastName)
            .setPhoneNumbers(phoneNumbers)
            .setPosition(snapshot.position)
            .setRegistered(snapshot.registered)
            .setAdmin(snapshot.admin)
            .setUserId(snapshot.externalUserId)
            .setCrafts(snapshot.crafts.map { it.toAggregateIdentifierAvro() })
            .setEulaAcceptedDate(
                if (snapshot.eulaAcceptedDate == null) null
                else
                    snapshot.eulaAcceptedDate!!
                        .atStartOfDay(ZoneOffset.UTC)
                        .toInstant()
                        .toEpochMilli())
            .setLocked(snapshot.locked)
            .setLocale(if (snapshot.locale == null) null else snapshot.locale.toString())
            .setCountry(
                if (snapshot.country == null) null
                else IsoCountryCodeEnumAvro.valueOf(snapshot.country!!.name))

    return UserEventAvro.newBuilder()
        .setName(eventType as UserEventEnumAvro)
        .setAggregateBuilder(userAggregateAvro)
        .build()
  }

  override fun getAggregateType() = USER.name

  override fun getRootContextIdentifier(snapshot: UserSnapshot) = snapshot.identifier.toUuid()
}
