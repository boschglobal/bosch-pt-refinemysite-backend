/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.user.user.command.snapshotstore

import com.bosch.pt.csm.cloud.common.CodeExample
import com.bosch.pt.csm.cloud.common.messages.IsoCountryCodeEnumAvro.GB
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.referencedata.craft.CraftAggregateAvro
import com.bosch.pt.csm.cloud.usermanagement.common.AbstractRestoreIntegrationTest
import com.bosch.pt.csm.cloud.usermanagement.common.extensions.toUserId
import com.bosch.pt.csm.cloud.usermanagement.common.messages.GenderEnumAvro.MALE
import com.bosch.pt.csm.cloud.usermanagement.common.messages.PhoneNumberAvro
import com.bosch.pt.csm.cloud.usermanagement.common.messages.PhoneNumberTypeEnumAvro.MOBILE
import com.bosch.pt.csm.cloud.usermanagement.craft.event.submitCraft
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitProfilePicture
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUserTombstones
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserAggregateAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventEnumAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventEnumAvro.REGISTERED
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventEnumAvro.UPDATED
import com.bosch.pt.csm.cloud.usermanagement.user.messages.getIdentifier
import com.bosch.pt.csm.cloud.usermanagement.user.user.GenderEnum
import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.model.PhoneNumber
import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.model.PhoneNumberType
import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.model.User
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test

@CodeExample
class RestoreUserSnapshotTest : AbstractRestoreIntegrationTest() {

  @Test
  fun `validate user created event`() {
    eventStreamGenerator.submitUserDanielAndActivate()

    assertThat(repositories.userRepository.findAll()).hasSize(3)

    val userAggregate = eventStreamGenerator.get<UserAggregateAvro>("daniel")!!
    val user = repositories.userRepository.findWithDetailsByIdentifier(userAggregate.toUserId())!!

    assertThat(user.phonenumbers).hasSize(0)
    assertThat(user.crafts).hasSize(0)

    validateUserAttributes(user, userAggregate)
    validateAuditingInformation(user, userAggregate)
  }

  @Test
  fun `validate user registered event`() {
    eventStreamGenerator
        .submitUserDanielAndActivate {
          it.registered = false
          it.firstName = null
          it.lastName = null
          it.admin = false
          it.gender = null
          it.phoneNumbers = emptyList()
          it.position = null
          it.phoneNumbers = emptyList()
          it.crafts = emptyList()
        }
        .submitUserDanielAndActivate(eventType = REGISTERED) {
          it.email = "daniel@smartsite.com"
          it.registered = true
          it.firstName = "Daniel"
          it.lastName = "Düsentrieb"
          it.admin = false
          it.gender = MALE
          it.phoneNumbers =
              listOf(
                  PhoneNumberAvro.newBuilder()
                      .setPhoneNumberType(MOBILE)
                      .setCountryCode("+49")
                      .setCallNumber("123-4567890")
                      .build())
          it.position = "Super Hero"
          it.crafts = emptyList()
          it.locale = "en_GB"
          it.country = GB
        }

    val userAggregate = eventStreamGenerator.get<UserAggregateAvro>("daniel")!!
    repositories.userRepository.findWithDetailsByIdentifier(userAggregate.toUserId())!!.also {
      validateUserAttributes(it, userAggregate)
      validateAuditingInformation(it, userAggregate)
      validatePhoneNumbers(it, userAggregate)
    }
  }

  @Test
  fun `validate user updated event`() {
    eventStreamGenerator.submitCraft().submitUserDanielAndActivate().submitUserDanielAndActivate(
        eventType = UPDATED) {
      it.phoneNumbers =
          listOf(
              PhoneNumberAvro.newBuilder()
                  .setPhoneNumberType(MOBILE)
                  .setCountryCode("+49")
                  .setCallNumber("123-4567890")
                  .build())
      it.crafts = listOf(getByReference("craft"))
    }

    assertThat(repositories.userRepository.findAll()).hasSize(3)

    val craftAggregate = eventStreamGenerator.get<CraftAggregateAvro>("craft")!!
    val userAggregate = eventStreamGenerator.get<UserAggregateAvro>("daniel")!!

    transactionTemplate.executeWithoutResult {
      repositories.userRepository.findWithDetailsByIdentifier(userAggregate.toUserId())!!.also {
        validateUserAttributes(it, userAggregate)
        validatePhoneNumbers(it, userAggregate)
        validateCrafts(it, craftAggregate)
        validateAuditingInformation(it, userAggregate)
      }
    }
  }

  @Test
  fun `validate user updated event with removed phone numbers and eula accepted date set`() {
    eventStreamGenerator
        .submitUserDanielAndActivate {
          it.phoneNumbers =
              listOf(
                  PhoneNumberAvro.newBuilder()
                      .setPhoneNumberType(MOBILE)
                      .setCountryCode("+49")
                      .setCallNumber("123-4567890")
                      .build(),
                  PhoneNumberAvro.newBuilder()
                      .setPhoneNumberType(MOBILE)
                      .setCountryCode("+43")
                      .setCallNumber("123-4567890")
                      .build())
        }
        .submitUserDanielAndActivate(eventType = UPDATED) {
          it.phoneNumbers =
              listOf(
                  PhoneNumberAvro.newBuilder()
                      .setPhoneNumberType(MOBILE)
                      .setCountryCode("+49")
                      .setCallNumber("123-4567890")
                      .build())
          it.eulaAcceptedDate = 1606210479
        }

    assertThat(repositories.userRepository.findAll()).hasSize(3)

    val userAggregate = eventStreamGenerator.get<UserAggregateAvro>("daniel")!!

    transactionTemplate.executeWithoutResult {
      repositories.userRepository.findWithDetailsByIdentifier(userAggregate.toUserId())!!.also {
        validateUserAttributes(it, userAggregate)
        validatePhoneNumbers(it, userAggregate)
        validateAuditingInformation(it, userAggregate)
      }
    }
  }

  @Test
  fun `validate user tombstone event deletes a user`() {
    eventStreamGenerator
        .submitUserDanielAndActivate()
        .submitProfilePicture()
        .submitUserTombstones("daniel")

    val userAggregate = eventStreamGenerator.get<UserAggregateAvro>("daniel")!!
    repositories.userRepository.findOneByIdentifier(userAggregate.toUserId()).also {
      assertThat(it).isNull()
    }
    repositories.profilePictureRepository.findOneByUserIdentifier(userAggregate.toUserId()).also {
      assertThat(it).isNull()
    }

    // Send event again to test idempotency
    eventStreamGenerator.repeat(1)
  }

  @Test
  fun `validate user deleted event is no longer supported`() {
    eventStreamGenerator.submitUserDanielAndActivate().submitProfilePicture()

    assertThatExceptionOfType(IllegalStateException::class.java).isThrownBy {
      eventStreamGenerator.submitUserDanielAndActivate(eventType = DELETED)
    }
  }

  private fun validateUserAttributes(user: User, userAggregate: UserAggregateAvro) {
    user.apply {
      assertThat(getIdentifierUuid()).isEqualTo(userAggregate.getIdentifier())
      assertThat(version).isEqualTo(userAggregate.getAggregateIdentifier().getVersion())

      assertThat(email).isEqualTo(userAggregate.getEmail())
      assertThat(gender).isEqualTo(GenderEnum.valueOf(userAggregate.getGender().name))
      assertThat(firstName).isEqualTo(userAggregate.getFirstName())
      assertThat(lastName).isEqualTo(userAggregate.getLastName())
      assertThat(position).isEqualTo(userAggregate.getPosition())
      if (userAggregate.getAdmin()) {
        assertThat(admin).isTrue
      } else {
        assertThat(admin).isFalse
      }
      if (userAggregate.getRegistered()) {
        assertThat(registered).isTrue
      } else {
        assertThat(registered).isFalse
      }
      assertThat(locale?.toString()).isEqualTo(userAggregate.getLocale())
      assertThat(country?.name).isEqualTo(userAggregate.getCountry()?.name)
    }
  }

  private fun validatePhoneNumbers(user: User, userAggregate: UserAggregateAvro) {
    assertThat(user.phonenumbers).hasSize(1)

    val phoneNumberAggregate: PhoneNumberAvro =
        userAggregate.getPhoneNumbers().first { it.getPhoneNumberType() == MOBILE }
    val phoneNumber: PhoneNumber =
        user.phonenumbers.first { it.phoneNumberType == PhoneNumberType.MOBILE }

    assertThat(phoneNumber.countryCode).isEqualTo(phoneNumberAggregate.getCountryCode())
    assertThat(phoneNumber.callNumber).isEqualTo(phoneNumberAggregate.getCallNumber())
  }

  private fun validateCrafts(user: User, craftAggregate: CraftAggregateAvro) {
    assertThat(user.crafts).hasSize(1)
    val restoredCraft = user.crafts.first()
    assertThat(restoredCraft.defaultName).isEqualTo(craftAggregate.getDefaultName())
    assertThat(restoredCraft.translations).hasSize(craftAggregate.getTranslations().size)
    for (translation in craftAggregate.getTranslations()) {
      val restoredTranslation =
          restoredCraft.translations.first { it.locale == translation.getLocale() }
      assertThat(restoredTranslation).isNotNull
      assertThat(restoredTranslation.value).isEqualTo(translation.getValue())
    }
  }

  private fun EventStreamGenerator.submitUserDanielAndActivate(
      eventType: UserEventEnumAvro = CREATED,
      aggregateModifications: ((UserAggregateAvro.Builder) -> Unit)? = null
  ): EventStreamGenerator {
    submitUser("daniel", eventType = eventType) {
      it.firstName = "Daniel"
      it.lastName = "Düsentrieb"
      it.gender = MALE
      it.position = "Super Hero"
      it.email = "daniel@smartsite.com"
      it.phoneNumbers = emptyList()
      if (aggregateModifications != null) {
        it.also(aggregateModifications)
      }
    }
    setUserContext("daniel")
    return this
  }
}
