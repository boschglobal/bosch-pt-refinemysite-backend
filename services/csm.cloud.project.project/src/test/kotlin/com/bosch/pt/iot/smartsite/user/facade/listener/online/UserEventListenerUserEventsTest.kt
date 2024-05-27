/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.user.facade.listener.online

import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.get
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitCompany
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitEmployee
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventG3Avro
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectAggregateAvro
import com.bosch.pt.csm.cloud.usermanagement.common.messages.GenderEnumAvro
import com.bosch.pt.csm.cloud.usermanagement.common.messages.PhoneNumberAvro
import com.bosch.pt.csm.cloud.usermanagement.common.messages.PhoneNumberTypeEnumAvro
import com.bosch.pt.csm.cloud.usermanagement.craft.event.submitCraft
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitSystemUserAndActivate
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUserTombstones
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserAggregateAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventEnumAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.getIdentifier
import com.bosch.pt.csm.cloud.usermanagement.user.messages.getVersion
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractIntegrationTestV2
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantStatusEnum
import com.bosch.pt.iot.smartsite.user.model.GenderEnum.MALE
import com.bosch.pt.iot.smartsite.user.model.PhoneNumberType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@EnableAllKafkaListeners
open class UserEventListenerUserEventsTest : AbstractIntegrationTestV2() {

  @BeforeEach
  fun setup() {
    eventStreamGenerator.submitSystemUserAndActivate().submitCraft()
  }

  @Test
  fun `validate user created event`() {
    eventStreamGenerator
        .submitUser(asReference = "daniel") {
          it.firstName = "Daniel"
          it.lastName = "Düsentrieb"
          it.crafts = emptyList()
          it.email = "daniel@smartsite.com"
          it.registered = false
          it.phoneNumbers = emptyList()
        }
        // test idempotency
        .repeat(1)

    val userAggregate: UserAggregateAvro = get("daniel")!!

    repositories.userRepository
        .findWithDetailsByIdentifier(
            userAggregate.getAggregateIdentifier().getIdentifier().toUUID())!!
        .apply {
          assertThat(email).isEqualTo(userAggregate.getEmail())
          assertThat(firstName).isEqualTo(userAggregate.getFirstName())
          assertThat(lastName).isEqualTo(userAggregate.getLastName())

          assertThat(admin).isFalse
          assertThat(deleted).isFalse
          assertThat(registered).isFalse

          assertThat(phonenumbers).isEmpty()
          assertThat(crafts).isEmpty()
          assertThat(version).isEqualTo(userAggregate.getVersion())
        }
  }

  @Test
  fun `validate user registered event`() {
    eventStreamGenerator
        .submitUser(asReference = "daniel") {
          it.email = "daniel@smartsite.com"
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
        .submitUser(asReference = "daniel", eventType = UserEventEnumAvro.REGISTERED) {
          it.email = "daniel@smartsite.com"
          it.registered = true
          it.firstName = "Daniel"
          it.lastName = "Düsentrieb"
          it.admin = false
          it.gender = GenderEnumAvro.MALE
          it.phoneNumbers =
              listOf(
                  PhoneNumberAvro.newBuilder()
                      .setPhoneNumberType(PhoneNumberTypeEnumAvro.MOBILE)
                      .setCountryCode("+49")
                      .setCallNumber("123-4567890")
                      .build())
          it.position = "Super Hero"
          it.crafts = emptyList()
        }
        // test idempotency
        .repeat(2)

    val userAggregate: UserAggregateAvro = get("daniel")!!

    transactionTemplate.executeWithoutResult {
      repositories.userRepository
          .findOneByIdentifier(userAggregate.getAggregateIdentifier().getIdentifier().toUUID())!!
          .apply {
            assertThat(email).isEqualTo(userAggregate.getEmail())
            assertThat(gender).isEqualTo(MALE)
            assertThat(firstName).isEqualTo(userAggregate.getFirstName())
            assertThat(lastName).isEqualTo(userAggregate.getLastName())
            assertThat(position).isEqualTo(userAggregate.getPosition())
            assertThat(admin).isFalse
            assertThat(deleted).isFalse
            assertThat(registered).isTrue

            assertThat(phonenumbers).hasSize(1)
            assertThat(version).isEqualTo(userAggregate.getAggregateIdentifier().getVersion())

            val phoneNumberAggregate: PhoneNumberAvro =
                userAggregate.getPhoneNumbers().first {
                  it.getPhoneNumberType() == PhoneNumberTypeEnumAvro.MOBILE
                }

            phonenumbers
                .first { it.phoneNumberType == PhoneNumberType.MOBILE }
                .apply {
                  assertThat(countryCode).isEqualTo(phoneNumberAggregate.getCountryCode())
                  assertThat(callNumber).isEqualTo(phoneNumberAggregate.getCallNumber())
                }
          }
    }
  }

  @Test
  fun `validate user updated event including update on participants`() {
    eventStreamGenerator
        .submitCompany()
        .submitUser(asReference = "daniel") {
          it.email = "daniel@smartsite.com"
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
        .submitEmployee() { it.user = get<UserAggregateAvro>("daniel")!!.aggregateIdentifier }
        .submitProject()
        .submitParticipantG3() { it.user = get<UserAggregateAvro>("daniel")!!.aggregateIdentifier }
        .submitProject(asReference = "anotherProject")
        .submitParticipantG3(asReference = "anotherParticipant") {
          it.project = get<ProjectAggregateAvro>("anotherProject")!!.aggregateIdentifier
          it.user = get<UserAggregateAvro>("daniel")!!.aggregateIdentifier
        }

    // ensure business logic to be tested is actually triggered in UserEventListenerImpl
    useOnlineListener()
    eventStreamGenerator
        .submitUser(asReference = "daniel", eventType = UserEventEnumAvro.UPDATED) {
          it.email = "daniel.duesentrieb@smartsite.com"
          it.registered = true
          it.firstName = "Daniel"
          it.lastName = "Düsentrieb"
          it.admin = false
          it.gender = GenderEnumAvro.MALE
          it.phoneNumbers =
              listOf(
                  PhoneNumberAvro.newBuilder()
                      .setPhoneNumberType(PhoneNumberTypeEnumAvro.MOBILE)
                      .setCountryCode("+49")
                      .setCallNumber("123-4567890")
                      .build())
          it.position = "Super Hero"
          it.crafts = emptyList()
        }
        // test idempotency
        .repeat(1)

    val userAggregate: UserAggregateAvro = get("daniel")!!

    transactionTemplate.executeWithoutResult {
      repositories.userRepository
          .findOneByIdentifier(userAggregate.getAggregateIdentifier().getIdentifier().toUUID())!!
          .apply {
            assertThat(email).isEqualTo(userAggregate.getEmail())
            assertThat(gender).isEqualTo(MALE)
            assertThat(firstName).isEqualTo(userAggregate.getFirstName())
            assertThat(lastName).isEqualTo(userAggregate.getLastName())
            assertThat(position).isEqualTo(userAggregate.getPosition())

            assertThat(admin).isFalse
            assertThat(deleted).isFalse
            assertThat(registered).isTrue

            assertThat(phonenumbers).hasSize(1)
            assertThat(version).isEqualTo(userAggregate.getAggregateIdentifier().getVersion())

            val phoneNumberAggregate: PhoneNumberAvro =
                userAggregate.getPhoneNumbers().first {
                  it.getPhoneNumberType() == PhoneNumberTypeEnumAvro.MOBILE
                }
            phonenumbers
                .first { it.phoneNumberType == PhoneNumberType.MOBILE }
                .apply {
                  assertThat(countryCode).isEqualTo(phoneNumberAggregate.getCountryCode())
                  assertThat(callNumber).isEqualTo(phoneNumberAggregate.getCallNumber())
                }
          }
    }

    repositories.participantRepository.findAll().apply {
      assertThat(this).hasSize(2)
      assertThat(this[0].email).isEqualTo("daniel.duesentrieb@smartsite.com")
      assertThat(this[1].email).isEqualTo("daniel.duesentrieb@smartsite.com")
    }
  }

  @Test
  fun `validate user tombstone event anonymizes a user and cleans up employees and participants`() {
    useRestoreListener()
    eventStreamGenerator.setupDatasetTestData()
    useOnlineListener()

    // Delete the user
    assertDoesNotThrow {
      eventStreamGenerator
          .submitUserTombstones("userCsm1")
          // test idempotency
          .repeat(1)
    }

    val aggregateUser: UserAggregateAvro = get("userCsm1")!!

    // Assert that the profile picture was deleted
    repositories.profilePictureRepository
        .findOneByUserIdentifier(aggregateUser.getIdentifier())
        .apply { assertThat(this).isNull() }

    // Assert that the user is anonymized
    repositories.userRepository.findWithDetailsByIdentifier(aggregateUser.getIdentifier())!!.apply {
      assertThat(deleted).isTrue
      assertThat(firstName).isNotEqualTo(aggregateUser.getFirstName())
      assertThat(lastName).isNotEqualTo(aggregateUser.getLastName())
      assertThat(email).isNotEqualTo(aggregateUser.getEmail())
      assertThat(phonenumbers).isEmpty()
      assertThat(crafts).isEmpty()
      assertThat(version).isEqualTo(aggregateUser.getVersion())
    }

    // Assert that the employee was deleted
    repositories.findEmployee(getIdentifier("employeeCsm1")).apply { assertThat(this).isNull() }

    // Assert that the participant was deactivated and the email was removed
    repositories.findParticipant(getIdentifier("participantCsm1"))!!.apply {
      assertThat(status).isEqualTo(ParticipantStatusEnum.INACTIVE)
      assertThat(email).isNull()
    }

    projectEventStoreUtils.verifyContainsAndGet(
        ParticipantEventG3Avro::class.java, ParticipantEventEnumAvro.DEACTIVATED)
  }

  /**
   * When a user is deleted and the same real-world user (i.e., same CIAM identifier) registers
   * again in quick succession, a race condition can occur: then, the user created event is
   * processed before the user deleted event.
   *
   * In such a case, the race condition should be detected and an exception should be thrown when
   * processing the created event. This way, the created event can succeed only after the deletion
   * event was successfully processed.
   */
  @Test
  fun `validate out-of-order user tombstone event and user recreation event raises exception`() {
    useRestoreListener()
    eventStreamGenerator.setupDatasetTestData()
    useOnlineListener()

    val deletedUserAggregate = get<UserAggregateAvro>("user")!!

    assertThatExceptionOfType(IllegalStateException::class.java)
        .isThrownBy {
          eventStreamGenerator.submitUser("recreated") { it.userId = deletedUserAggregate.userId }
        }
        .withMessageContaining("there is already a user with the same CIAM identifier")
  }
}
