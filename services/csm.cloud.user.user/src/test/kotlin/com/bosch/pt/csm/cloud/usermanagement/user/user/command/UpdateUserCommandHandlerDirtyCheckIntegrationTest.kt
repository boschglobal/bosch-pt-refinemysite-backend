/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.user.user.command

import com.bosch.pt.csm.cloud.common.messages.IsoCountryCodeEnumAvro
import com.bosch.pt.csm.cloud.common.messages.IsoCountryCodeEnumAvro.GB
import com.bosch.pt.csm.cloud.common.model.IsoCountryCodeEnum
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.referencedata.craft.CraftTranslationAvro
import com.bosch.pt.csm.cloud.usermanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.usermanagement.application.config.EnableAllKafkaListeners
import com.bosch.pt.csm.cloud.usermanagement.common.AbstractIntegrationTest
import com.bosch.pt.csm.cloud.usermanagement.common.extensions.toUserId
import com.bosch.pt.csm.cloud.usermanagement.common.messages.GenderEnumAvro
import com.bosch.pt.csm.cloud.usermanagement.craft.craft.CraftId
import com.bosch.pt.csm.cloud.usermanagement.craft.craft.toCraftId
import com.bosch.pt.csm.cloud.usermanagement.craft.event.submitCraft
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserAggregateAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventEnumAvro.UPDATED
import com.bosch.pt.csm.cloud.usermanagement.user.user.GenderEnum
import com.bosch.pt.csm.cloud.usermanagement.user.user.api.PhoneNumberCommandDto
import com.bosch.pt.csm.cloud.usermanagement.user.user.api.UpdateUserCommand
import com.bosch.pt.csm.cloud.usermanagement.user.user.command.handler.UpdateUserCommandHandler
import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.model.PhoneNumber
import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.model.PhoneNumberType
import java.util.Locale
import org.apache.commons.lang3.LocaleUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@EnableAllKafkaListeners
@SmartSiteSpringBootTest
class UpdateUserCommandHandlerDirtyCheckIntegrationTest : AbstractIntegrationTest() {

  @Autowired private lateinit var cut: UpdateUserCommandHandler

  private lateinit var userAggregate: UserAggregateAvro

  private val phoneNumber1 = PhoneNumber(PhoneNumberType.HOME, "+33", "123456")
  private val phoneNumber2 = PhoneNumber(PhoneNumberType.BUSINESS, "+33", "7891011")

  @BeforeEach
  fun init() {
    eventStreamGenerator
        .submitUser(asReference = "user") {
          it.phoneNumbers = emptyList()
          it.country = GB
        }
        .submitCraft(asReference = "craft1") {
          it.defaultName = "Craft1"
          it.translations = listOf(CraftTranslationAvro(Locale.UK.country, "Roofer"))
        }
        .submitCraft(asReference = "craft2") {
          it.defaultName = "Craft2"
          it.translations =
              listOf(
                  CraftTranslationAvro(Locale.GERMANY.country, "Schreiner"),
                  CraftTranslationAvro(Locale.UK.country, "carpenter"))
        }

    userAggregate = eventStreamGenerator.get<UserAggregateAvro>("user")!!

    assertThat(userAggregate.getGender()).isEqualTo(GenderEnumAvro.MALE)
    assertThat(userAggregate.getEmail()).contains("@test.de")
    assertThat(userAggregate.getFirstName()).isNotEqualTo("Martha")
    assertThat(userAggregate.getLastName()).isNotEqualTo("King")
    assertThat(userAggregate.getPhoneNumbers()).isEmpty()
    assertThat(userAggregate.getRegistered()).isTrue
    assertThat(userAggregate.getAdmin()).isFalse
    assertThat(userAggregate.getCrafts()).isEmpty()
    assertThat(userAggregate.getEulaAcceptedDate()).isNotNull
    assertThat(userAggregate.getCountry()).isEqualTo(GB)
    assertThat(userAggregate.getLocale()).isEqualTo(Locale.UK.toString())

    setAuthentication("user")
  }

  @Test
  fun `is not dirty`() {
    repositories.userRepository.findOneByIdentifier(userAggregate.toUserId())!!.apply {
      assertThat(this.version).isEqualTo(0)
    }

    cut.handle(userAggregate.toUpdateUserCommand())

    repositories.userRepository.findOneByIdentifier(userAggregate.toUserId())!!.apply {
      assertThat(this.version).isEqualTo(0)
    }

    userEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `is dirty if gender changes`() {
    cut.handle(userAggregate.toUpdateUserCommand().copy(gender = GenderEnum.FEMALE))
    userEventStoreUtils.verifyContainsAndGet(UserEventAvro::class.java, UPDATED, true).let {
      assertThat(it.getAggregate().getGender()).isEqualTo(GenderEnumAvro.FEMALE)
    }
  }

  @Test
  fun `is dirty if firstName changes`() {
    cut.handle(userAggregate.toUpdateUserCommand().copy(firstName = "Jane"))
    userEventStoreUtils.verifyContainsAndGet(UserEventAvro::class.java, UPDATED, true).let {
      assertThat(it.getAggregate().getFirstName()).isEqualTo("Jane")
    }
  }

  @Test
  fun `is dirty if lastName changes`() {
    cut.handle(userAggregate.toUpdateUserCommand().copy(lastName = "King"))
    userEventStoreUtils.verifyContainsAndGet(UserEventAvro::class.java, UPDATED, true).let {
      assertThat(it.getAggregate().getLastName()).isEqualTo("King")
    }
  }

  @Test
  fun `is dirty if position changes`() {
    cut.handle(userAggregate.toUpdateUserCommand().copy(position = "CEO"))
    userEventStoreUtils.verifyContainsAndGet(UserEventAvro::class.java, UPDATED, true).let {
      assertThat(it.getAggregate().getPosition()).isEqualTo("CEO")
    }
  }

  @Test
  fun `is dirty if crafts changes`() {
    cut.handle(
        userAggregate
            .toUpdateUserCommand()
            .copy(
                crafts =
                    listOf(CraftId(getIdentifier("craft1")), CraftId(getIdentifier("craft2")))))
    userEventStoreUtils.verifyContainsAndGet(UserEventAvro::class.java, UPDATED, true).let {
      assertThat(it.getAggregate().getCrafts()).hasSize(2)
    }
  }

  @Test
  fun `is dirty if phone number changes`() {
    cut.handle(
        userAggregate
            .toUpdateUserCommand()
            .copy(
                phoneNumbers =
                    setOf(phoneNumber1, phoneNumber2)
                        .map {
                          PhoneNumberCommandDto(
                              countryCode = it.countryCode,
                              callNumber = it.callNumber,
                              phoneNumberType = it.phoneNumberType,
                          )
                        }
                        .toSet()))
    userEventStoreUtils.verifyContainsAndGet(UserEventAvro::class.java, UPDATED, true).let {
      assertThat(it.getAggregate().getPhoneNumbers()).hasSize(2)
    }
  }

  @Test
  fun `is dirty if locale changes`() {
    cut.handle(userAggregate.toUpdateUserCommand().copy(locale = Locale.GERMANY))
    userEventStoreUtils.verifyContainsAndGet(UserEventAvro::class.java, UPDATED, true).let {
      assertThat(it.getAggregate().getLocale()).isEqualTo(Locale.GERMANY.toString())
    }
  }

  @Test
  fun `is dirty if country changes`() {
    cut.handle(userAggregate.toUpdateUserCommand().copy(country = IsoCountryCodeEnum.DE))
    userEventStoreUtils.verifyContainsAndGet(UserEventAvro::class.java, UPDATED, true).let {
      assertThat(it.getAggregate().getCountry()).isEqualTo(IsoCountryCodeEnumAvro.DE)
    }
  }

  private fun UserAggregateAvro.toUpdateUserCommand() =
      UpdateUserCommand(
          identifier = toUserId(),
          version = 0,
          gender = GenderEnum.valueOf(getGender().name),
          firstName = getFirstName(),
          lastName = getLastName(),
          position = getPosition(),
          crafts = getCrafts().map { craft -> craft.toCraftId() }.toList(),
          phoneNumbers = emptySet(),
          locale = LocaleUtils.toLocale(getLocale()),
          country = IsoCountryCodeEnum.fromCountryCode(getCountry().name)!!)
}
