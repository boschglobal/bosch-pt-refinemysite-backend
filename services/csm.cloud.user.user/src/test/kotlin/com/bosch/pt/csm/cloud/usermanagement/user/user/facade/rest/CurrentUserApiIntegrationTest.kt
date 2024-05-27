/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.user.user.facade.rest

import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.common.model.IsoCountryCodeEnum
import com.bosch.pt.csm.cloud.common.model.IsoCountryCodeEnum.US
import com.bosch.pt.csm.cloud.usermanagement.common.AbstractApiIntegrationTest
import com.bosch.pt.csm.cloud.usermanagement.craft.craft.CraftId
import com.bosch.pt.csm.cloud.usermanagement.craft.event.submitCraft
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.csm.cloud.usermanagement.user.user.GenderEnum.MALE
import com.bosch.pt.csm.cloud.usermanagement.user.user.facade.rest.resource.datastructure.PhoneNumberDto
import com.bosch.pt.csm.cloud.usermanagement.user.user.facade.rest.resource.request.CreateCurrentUserResource
import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.model.PhoneNumberType
import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.model.UnregisteredUser
import java.util.Locale
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.OK
import org.springframework.security.access.AccessDeniedException

class CurrentUserApiIntegrationTest : AbstractApiIntegrationTest() {

  private lateinit var phoneNumber: PhoneNumberDto
  private lateinit var createCurrentUserResource: CreateCurrentUserResource

  @Autowired private lateinit var cut: CurrentUserController

  @Value("\${locale.supported}") private lateinit var supportedLocales: Set<String>

  @BeforeEach
  fun init() {
    eventStreamGenerator.submitCraft().submitUser("user")

    phoneNumber = PhoneNumberDto("+49", PhoneNumberType.MOBILE, "151 123456789")

    createCurrentUserResource =
        CreateCurrentUserResource(
            gender = MALE,
            firstName = "Max",
            lastName = "Mustermann",
            position = "Foreman",
            phoneNumbers = mutableSetOf(phoneNumber),
            craftIds = listOf(CraftId(eventStreamGenerator.getIdentifier("craft"))),
            eulaAccepted = true,
            locale = Locale.ENGLISH,
            country = IsoCountryCodeEnum.DE,
        )
  }

  // test for all supported locales because user crafts will be translated depending on the locale
  @TestFactory
  fun getCurrentUserSuccessfulForLocale() =
      supportedLocales
          .map { Locale.forLanguageTag(it.replace("_", "-")) }
          .map { locale ->
            dynamicTest(locale.toLanguageTag()) {
              doWithLocale(locale) {
                val user = setAuthentication("user")
                val response = cut.getCurrentUser(user)
                assertThat(response.statusCode).isEqualTo(OK)

                val currentUserResource = response.body
                assertThat(currentUserResource).isNotNull
                assertThat(currentUserResource!!.id).isNotNull.isEqualTo(user.getIdentifierUuid())
                assertThat(currentUserResource.email).isEqualTo(user.email)
              }
            }
          }

  @Test
  fun getCurrentUserFailsForUnregisteredUser() {
    val unregisteredUser = setSecurityContextAsUnregisteredUser()
    val response = cut.getCurrentUser(unregisteredUser)
    assertThat(response.statusCode).isEqualTo(NOT_FOUND)
  }

  @Test
  fun createCurrentUserFailsForAlreadyRegisteredUser() {
    val user = setAuthentication("user")
    val unregisteredUser = UnregisteredUser(user.externalUserId!!, user.email!!)

    assertThatExceptionOfType(AccessDeniedException::class.java).isThrownBy {
      cut.registerCurrentUser(createCurrentUserResource, unregisteredUser)
    }
  }

  @Test
  fun createCurrentUserFailsForEulaNotAcceptedAndCountryUS() {
    val unregisteredUser = setSecurityContextAsUnregisteredUser()
    val createCurrentUserResourceNotAccepted =
        createCurrentUserResource.copy(eulaAccepted = false, country = US)

    assertThatExceptionOfType(PreconditionViolationException::class.java).isThrownBy {
      cut.registerCurrentUser(createCurrentUserResourceNotAccepted, unregisteredUser)
    }
  }

  @Test
  fun createCurrentUserFailsForUnknownCraftId() {
    val unregisteredUser = setSecurityContextAsUnregisteredUser()
    val createCurrentUserResourceUnknownCrafts =
        createCurrentUserResource.copy(craftIds = listOf(CraftId.random()))

    assertThatExceptionOfType(PreconditionViolationException::class.java).isThrownBy {
      cut.registerCurrentUser(createCurrentUserResourceUnknownCrafts, unregisteredUser)
    }
  }

  private fun doWithLocale(locale: Locale, procedure: () -> Unit) {
    val oldLocale = LocaleContextHolder.getLocale()
    LocaleContextHolder.setLocale(locale)
    try {
      procedure()
    } finally {
      // make sure to restore the locale even if the test fails to not affect following tests
      LocaleContextHolder.setLocale(oldLocale)
    }
  }
}
