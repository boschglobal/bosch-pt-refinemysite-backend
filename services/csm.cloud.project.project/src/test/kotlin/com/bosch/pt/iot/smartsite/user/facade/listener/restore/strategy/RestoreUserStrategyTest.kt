/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2020
 *
 *  *************************************************************************
 */

package com.bosch.pt.iot.smartsite.user.facade.listener.restore.strategy

import com.bosch.pt.csm.cloud.common.CodeExample
import com.bosch.pt.csm.cloud.common.extensions.toEpochMilli
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.messages.AuditingInformationAvro
import com.bosch.pt.csm.cloud.common.messages.IsoCountryCodeEnumAvro
import com.bosch.pt.csm.cloud.common.model.IsoCountryCodeEnum
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.registerStaticContext
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitCompany
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitEmployee
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeRoleEnumAvro.FM
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.referencedata.craft.CraftAggregateAvro
import com.bosch.pt.csm.cloud.usermanagement.common.messages.GenderEnumAvro
import com.bosch.pt.csm.cloud.usermanagement.common.messages.PhoneNumberAvro
import com.bosch.pt.csm.cloud.usermanagement.common.messages.PhoneNumberTypeEnumAvro
import com.bosch.pt.csm.cloud.usermanagement.craft.event.submitCraft
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitProfilePicture
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitSystemUserAndActivate
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitTestAdminUserAndActivate
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUserTombstones
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserAggregateAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventEnumAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventEnumAvro.REGISTERED
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventEnumAvro.UPDATED
import com.bosch.pt.iot.smartsite.common.facade.listener.AbstractRestoreIntegrationTestV2
import com.bosch.pt.iot.smartsite.user.model.GenderEnum
import com.bosch.pt.iot.smartsite.user.model.PhoneNumber
import com.bosch.pt.iot.smartsite.user.model.PhoneNumberType
import com.bosch.pt.iot.smartsite.user.model.User
import com.bosch.pt.iot.smartsite.util.getIdentifier
import java.time.Instant
import java.time.LocalDate
import java.util.UUID.randomUUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@CodeExample
class RestoreUserStrategyTest : AbstractRestoreIntegrationTestV2() {

  @BeforeEach
  fun setup() {
    eventStreamGenerator
        .registerStaticContext()
        .submitSystemUserAndActivate()
        .submitTestAdminUserAndActivate()
  }

  @Test
  fun `validate user created event`() {
    eventStreamGenerator.submitUserDanielAndActivate()

    repositories.userRepository.findAll().also { assertThat(it).hasSize(3) }

    eventStreamGenerator.get<UserAggregateAvro>("daniel")!!.also { aggregate ->
      repositories.userRepository.findWithDetailsByIdentifier(aggregate.getIdentifier())!!.also {
        assertThat(it.phonenumbers).isEmpty()
        assertThat(it.crafts).isEmpty()
        validateUserAttributes(it, aggregate)
        validateAuditableAndVersionedEntityAttributes(it, aggregate)
      }
    }
  }

  @Test
  fun `validate user created event if the creator doesn't exist anymore`() {
    eventStreamGenerator.submitUserDanielAndActivate {
      it.auditingInformation =
          AuditingInformationAvro.newBuilder()
              .setCreatedBy(getUserIdentifierAvro(randomUUID()))
              .setCreatedDate(Instant.now().toEpochMilli())
              .setLastModifiedBy(getUserIdentifierAvro(randomUUID()))
              .setLastModifiedDate(Instant.now().toEpochMilli())
              .build()
    }

    // Expect that "deleted" creator was recreated
    repositories.userRepository.findAll().also { assertThat(it).hasSize(5) }

    eventStreamGenerator.get<UserAggregateAvro>("daniel")!!.also { aggregate ->
      repositories.userRepository.findWithDetailsByIdentifier(aggregate.getIdentifier())!!.also {
        assertThat(it.phonenumbers).isEmpty()
        assertThat(it.crafts).isEmpty()
        validateUserAttributes(it, aggregate)
        validateAuditableAndVersionedEntityAttributes(it, aggregate)
      }
    }
  }

  @Test
  fun `validate user registered event`() {
    eventStreamGenerator
        .submitUserDanielAndActivate { it.eulaAcceptedDate = null }
        .submitUser("daniel", eventType = REGISTERED) {
          it.eulaAcceptedDate = LocalDate.now().toEpochMilli()
        }

    repositories.userRepository.findAll().also { assertThat(it).hasSize(3) }

    eventStreamGenerator.get<UserAggregateAvro>("daniel")!!.also { aggregate ->
      repositories.userRepository.findWithDetailsByIdentifier(aggregate.getIdentifier())!!.also {
        validateUserAttributes(it, aggregate)
        validateAuditableAndVersionedEntityAttributes(it, aggregate)
      }
    }
  }

  @Test
  fun `validate user updated event`() {
    eventStreamGenerator
        .submitCraft()
        .submitUserDanielAndActivate { it.crafts = listOf(getByReference("craft")) }
        .submitUser("daniel", eventType = UPDATED) {
          it.phoneNumbers =
              listOf(
                  PhoneNumberAvro.newBuilder()
                      .setPhoneNumberType(PhoneNumberTypeEnumAvro.MOBILE)
                      .setCountryCode("+49")
                      .setCallNumber("123-4567890")
                      .build())
        }
        .submitUser("daniel", eventType = UPDATED) {
          it.phoneNumbers =
              listOf(
                  PhoneNumberAvro.newBuilder()
                      .setPhoneNumberType(PhoneNumberTypeEnumAvro.MOBILE)
                      .setCountryCode("+49")
                      .setCallNumber("123-4567890123")
                      .build())
        }

    repositories.userRepository.findAll().also { assertThat(it).hasSize(3) }

    val craftAggregate = eventStreamGenerator.get<CraftAggregateAvro>("craft")!!
    val userAggregate = eventStreamGenerator.get<UserAggregateAvro>("daniel")!!

    transactionTemplate.executeWithoutResult {
      repositories.userRepository.findOneByIdentifier(userAggregate.getIdentifier())!!.also {
        validateUserAttributes(it, userAggregate)
        validatePhoneNumbers(it, userAggregate)
        validateCrafts(it, craftAggregate)
        validateAuditableAndVersionedEntityAttributes(it, userAggregate)
      }
    }
  }

  @Test
  fun `validate user tombstone event anonymizes a user`() {
    eventStreamGenerator
        .submitUserDanielAndActivate()
        .submitProfilePicture()
        .setUserContext("admin")
        .submitCompany()
        .submitEmployee { it.roles = listOf(FM) }
        .setUserContext("daniel")
        .submitProject()
        .submitParticipantG3()
        .submitUserTombstones("daniel")

    val userAggregate = eventStreamGenerator.get<UserAggregateAvro>("daniel")!!

    repositories.userRepository.findWithDetailsByIdentifier(userAggregate.getIdentifier())!!.also {
      assertThat(it).isNotNull
      assertThat(it.admin).isFalse
      assertThat(it.crafts).isEmpty()
      assertThat(it.deleted).isTrue
      assertThat(it.email).isNull()
      assertThat(it.firstName).isNull()
      assertThat(it.gender).isNull()
      assertThat(it.lastName).isNull()
      assertThat(it.phonenumbers).isEmpty()
      assertThat(it.position).isNull()
      assertThat(it.registered).isFalse
      assertThat(it.getUserLocale()).isNull()
      assertThat(it.country).isNull()
      assertThat(it.ciamUserIdentifier).isNull()
      assertThat(it.createdBy).isNotPresent
      assertThat(it.lastModifiedBy).isNotPresent
      assertThat(it.createdDate).isNotPresent
      assertThat(it.lastModifiedDate).isNotPresent
    }

    repositories.profilePictureRepository
        .findOneByUserIdentifier(userAggregate.getIdentifier())
        .also { assertThat(it).isNull() }

    // Send event again to test idempotency
    eventStreamGenerator.repeat(1)
  }

  private fun validateUserAttributes(user: User, userAggregate: UserAggregateAvro) {
    assertThat(user.identifier)
        .isEqualTo(userAggregate.getAggregateIdentifier().getIdentifier().toUUID())
    assertThat(user.version).isEqualTo(userAggregate.getAggregateIdentifier().getVersion())

    assertThat(user.email).isEqualTo(userAggregate.getEmail())
    assertThat(user.gender).isEqualTo(GenderEnum.valueOf(userAggregate.getGender().name))
    assertThat(user.firstName).isEqualTo(userAggregate.getFirstName())
    assertThat(user.lastName).isEqualTo(userAggregate.getLastName())
    assertThat(user.position).isEqualTo(userAggregate.getPosition())
    assertThat(user.getUserLocale()?.toString()).isEqualTo(userAggregate.getLocale())
    assertThat(user.country).isEqualTo(IsoCountryCodeEnum.valueOf(userAggregate.getCountry().name))

    if (userAggregate.getAdmin()) {
      assertThat(user.admin).isTrue
    } else {
      assertThat(user.admin).isFalse
    }
    if (userAggregate.getRegistered()) {
      assertThat(user.registered).isTrue
    } else {
      assertThat(user.registered).isFalse
    }
  }

  private fun validatePhoneNumbers(user: User, userAggregate: UserAggregateAvro) {
    assertThat(user.phonenumbers).hasSize(1)

    val phoneNumberAggregate: PhoneNumberAvro =
        userAggregate.getPhoneNumbers().first {
          it.getPhoneNumberType() == PhoneNumberTypeEnumAvro.MOBILE
        }
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
      eventType: UserEventEnumAvro = UserEventEnumAvro.CREATED,
      aggregateModifications: ((UserAggregateAvro.Builder) -> Unit)? = null
  ): EventStreamGenerator {
    submitUser("daniel", eventType = eventType) {
      it.firstName = "Daniel"
      it.lastName = "Düsentrieb"
      it.gender = GenderEnumAvro.MALE
      it.position = "Super Hero"
      it.email = "daniel@smartsite.com"
      it.crafts = emptyList()
      it.phoneNumbers = emptyList()
      it.locale = "en_GB"
      it.country = IsoCountryCodeEnumAvro.GB
      if (aggregateModifications != null) {
        it.also(aggregateModifications)
      }
    }
    setUserContext("daniel")
    return this
  }
}
