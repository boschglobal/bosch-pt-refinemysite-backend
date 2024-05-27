/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

@file:JvmName("UserEventStreamRandomAggregate")

package com.bosch.pt.csm.cloud.usermanagement.user.event

import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.messages.AuditingInformationAvro
import com.bosch.pt.csm.cloud.common.test.randomLong
import com.bosch.pt.csm.cloud.common.test.randomString
import com.bosch.pt.csm.cloud.usermanagement.common.UsermanagementAggregateTypeEnum
import com.bosch.pt.csm.cloud.usermanagement.common.messages.GenderEnumAvro
import com.bosch.pt.csm.cloud.usermanagement.common.messages.PhoneNumberAvro
import com.bosch.pt.csm.cloud.usermanagement.common.messages.PhoneNumberTypeEnumAvro
import com.bosch.pt.csm.cloud.usermanagement.user.UserReferencedAggregateTypesEnum
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserAggregateAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventEnumAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserPictureAggregateAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserPictureEventAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserPictureEventEnumAvro

@Deprecated("to be removed")
fun randomUser(
    block: ((UserAggregateAvro) -> Unit)? = null,
    event: UserEventEnumAvro = UserEventEnumAvro.CREATED
): UserEventAvro.Builder {
  val user =
      UserAggregateAvro.newBuilder()
          .setAggregateIdentifierBuilder(
              randomIdentifier(UsermanagementAggregateTypeEnum.USER.value))
          .setAuditingInformation(randomAuditing())
          .setUserId(randomString())
          .setCrafts(listOf(randomIdentifier(UserReferencedAggregateTypesEnum.CRAFT.value).build()))
          .setEmail("test@test.de")
          .setFirstName(randomString())
          .setGender(GenderEnumAvro.MALE)
          .setLastName(randomString())
          .setPhoneNumbers(
              listOf(
                  PhoneNumberAvro.newBuilder()
                      .setPhoneNumberType(PhoneNumberTypeEnumAvro.BUSINESS)
                      .setCountryCode("0049")
                      .setCallNumber("123456")
                      .build()))
          .setRegistered(true)
          .setAdmin(false)
          .build()
          .also { block?.invoke(it) }

  return UserEventAvro.newBuilder().setAggregate(user).setName(event)
}

@Deprecated("to be removed")
fun randomProfilePicture(
    block: ((UserPictureAggregateAvro) -> Unit)? = null,
    event: UserPictureEventEnumAvro = UserPictureEventEnumAvro.CREATED
): UserPictureEventAvro.Builder {
  val picture =
      UserPictureAggregateAvro.newBuilder()
          .setAggregateIdentifierBuilder(
              randomIdentifier(UsermanagementAggregateTypeEnum.USERPICTURE.value))
          .setAuditingInformation(randomAuditing())
          .setUserBuilder(randomIdentifier(UsermanagementAggregateTypeEnum.USER.value))
          .setFileSize(randomLong())
          .setFullAvailable(true)
          .setSmallAvailable(true)
          .setHeight(randomLong())
          .setWidth(randomLong())
          .build()
          .also { block?.invoke(it) }

  return UserPictureEventAvro.newBuilder().setAggregate(picture).setName(event)
}

@Deprecated("to be removed")
fun randomAuditing(block: ((AuditingInformationAvro) -> Unit)? = null): AuditingInformationAvro =
    AuditingInformationAvro.newBuilder()
        .setCreatedByBuilder(randomIdentifier())
        .setCreatedDate(randomLong())
        .setLastModifiedByBuilder(randomIdentifier())
        .setLastModifiedDate(randomLong())
        .build()
        .also { block?.invoke(it) }

@Deprecated("to be removed")
fun randomIdentifier(type: String = randomString()) =
    AggregateIdentifierAvro.newBuilder().setIdentifier(randomString()).setType(type).setVersion(0)
