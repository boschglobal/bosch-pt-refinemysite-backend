/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.craft.facade.rest

import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.referencedata.craft.CraftTranslationAvro
import com.bosch.pt.csm.cloud.usermanagement.common.AbstractApiIntegrationTest
import com.bosch.pt.csm.cloud.usermanagement.common.facade.rest.resource.CreateTranslationResource
import com.bosch.pt.csm.cloud.usermanagement.common.translation.Key.CRAFT_VALIDATION_ERROR_NO_DEFAULT_NAME
import com.bosch.pt.csm.cloud.usermanagement.craft.craft.CraftId
import com.bosch.pt.csm.cloud.usermanagement.craft.craft.facade.rest.CraftController
import com.bosch.pt.csm.cloud.usermanagement.craft.craft.facade.rest.resource.CreateCraftResource
import com.bosch.pt.csm.cloud.usermanagement.craft.event.submitCraft
import java.util.Locale
import java.util.Locale.ENGLISH
import java.util.Locale.FRANCE
import java.util.Locale.GERMANY
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus.OK

class CraftApiIntegrationTest : AbstractApiIntegrationTest() {

  @Autowired private lateinit var cut: CraftController

  @Value("\${locale.supported}") private lateinit var supportedLocales: Set<String>

  @Test
  fun `saving a craft with blank default name fails`() {
    val resource: CreateCraftResource =
        CreateCraftResource().apply {
          // a craft's default name is taken from the "en" translation
          translations = setOf(CreateTranslationResource(ENGLISH.language, ""))
        }

    setAuthentication("admin")

    assertThatThrownBy { cut.create(CraftId.random(), resource) }
        .isInstanceOf(PreconditionViolationException::class.java)
        .hasFieldOrPropertyWithValue("messageKey", CRAFT_VALIDATION_ERROR_NO_DEFAULT_NAME)
  }

  @Test
  fun `saving a craft without default language fails`() {
    val resource: CreateCraftResource =
        CreateCraftResource().apply {
          // a craft's default name is taken from the "en" translation
          translations = setOf(CreateTranslationResource(FRANCE.language, "Craft"))
        }

    setAuthentication("admin")

    assertThatThrownBy { cut.create(CraftId.random(), resource) }
        .isInstanceOf(PreconditionViolationException::class.java)
        .hasFieldOrPropertyWithValue("messageKey", CRAFT_VALIDATION_ERROR_NO_DEFAULT_NAME)
  }

  @TestFactory
  fun `finding a craft by identifier for locale`() =
      supportedLocales
          .map { it.toLocale() }
          .map { locale ->
            dynamicTest(locale.toLanguageTag()) {
              val translationMap =
                  mapOf(
                      "de" to "de translation",
                      "en" to "en translation",
                      "es" to "es translation",
                      "fr" to "fr translation",
                      "pt" to "pt translation",
                  )

              eventStreamGenerator.submitCraft("craft1") {
                it.defaultName = translationMap["en"]
                it.translations = translationMap.buildTranslations()
              }

              val craft =
                  repositories.craftRepository.findOneByIdentifier(
                      CraftId(eventStreamGenerator.getIdentifier("craft1")))!!

              doWithLocale(locale) { cut.findOneByIdentifier(craft.identifier) }
                  .also {
                    assertThat(it.statusCode).isEqualTo(OK)
                    assertThat(it.body!!.identifier).isEqualTo(craft.identifier)
                    assertThat(it.body!!.name).isEqualTo(translationMap[locale.language])
                  }
            }
          }

  @Test
  fun `finding a craft by identifier falls back to default name for missing translation`() {
    eventStreamGenerator.submitCraft("craft1") {
      it.defaultName = "en translation 1"
      it.translations = mapOf("en" to "en translation 1").buildTranslations()
    }

    val craft =
        repositories.craftRepository.findOneByIdentifier(
            CraftId(eventStreamGenerator.getIdentifier("craft1")))!!

    doWithLocale(GERMANY) { cut.findOneByIdentifier(craft.identifier) }
        .also {
          assertThat(it.statusCode).isEqualTo(OK)
          assertThat(it.body!!.identifier).isEqualTo(craft.identifier)
          assertThat(it.body!!.name).isEqualTo("en translation 1")
        }
  }

  @Test
  fun `finding all crafts by locale falls back to default name for missing translations`() {
    // create craft translated to both languages

    eventStreamGenerator
        .submitCraft("craft1") {
          it.defaultName = "en translation 1"
          it.translations =
              mapOf("de" to "de translation 1", "en" to "en translation 1").buildTranslations()
        }
        .submitCraft("craft2") {
          it.defaultName = "en translation 2"
          it.translations = mapOf("en" to "en translation 2").buildTranslations()
        }

    doWithLocale(GERMANY) { cut.findAllCraftsByLanguage(GERMANY, PageRequest.of(0, 10)) }
        .also {
          assertThat(it.statusCode).isEqualTo(OK)
          assertThat(it.body!!.crafts)
              .extracting<String> { it!!.name }
              .containsAll(
                  setOf(
                      "de translation 1",
                      "en translation 2", // the expected fallback translation
                  ))
        }
  }

  private fun <T> doWithLocale(locale: Locale, procedure: () -> T): T {
    val oldLocale = LocaleContextHolder.getLocale()
    LocaleContextHolder.setLocale(locale)
    try {
      return procedure()
    } finally {
      // make sure to restore the locale even if the test fails to not affect following tests
      LocaleContextHolder.setLocale(oldLocale)
    }
  }

  private fun Map<String, String>.buildTranslations() =
      map { CraftTranslationAvro.newBuilder().setLocale(it.key).setValue(it.value).build() }
          .toList()

  private fun String.toLocale() = Locale.forLanguageTag(this.replace("_", "-"))
}
