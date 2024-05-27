/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.craft.boundary

import com.bosch.pt.iot.smartsite.application.SmartSiteMockKTest
import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.iot.smartsite.common.i18n.Key.CRAFT_VALIDATION_ERROR_NO_DEFAULT_NAME
import com.bosch.pt.iot.smartsite.craft.model.CraftBuilder.Companion.craft
import com.bosch.pt.iot.smartsite.craft.repository.CraftRepository
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import java.util.Locale.ENGLISH
import java.util.UUID.randomUUID
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationEventPublisher

@SmartSiteMockKTest
class CraftServiceTest {

  @Suppress("Unused", "UnusedPrivateMember")
  @MockK
  private lateinit var publisher: ApplicationEventPublisher

  @MockK private lateinit var craftRepository: CraftRepository

  @InjectMockKs private lateinit var cut: CraftService

  @Test
  fun verifySaveCraftWithBlankDefaultName() {
    val throwable = catchThrowable { cut.create(craft().withDefaultName(null).build()) }

    assertThat(throwable)
        .isInstanceOf(PreconditionViolationException::class.java)
        .extracting("messageKey")
        .asString()
        .contains(CRAFT_VALIDATION_ERROR_NO_DEFAULT_NAME)
  }

  @Test
  fun verifySaveCraftWithDefaultName() {
    val identifier = randomUUID()
    val craft = craft().withIdentifier(identifier).withDefaultName("default name").build()

    every { craftRepository.save(craft) } returns craft

    // call the actual function and assert
    val savedCraftIdentifier = cut.create(craft)
    assertThat(savedCraftIdentifier).isNotNull.isEqualTo(identifier)
  }

  @Test
  fun verifyFindOneByIdentifier() {
    val identifier = randomUUID()
    val craft = craft().withIdentifier(identifier).build()

    every { craftRepository.findOneByIdentifier(identifier) } returns craft

    val foundCraft = cut.findOneByIdentifier(identifier)
    assertThat(foundCraft).usingRecursiveComparison().isEqualTo(craft)
  }

  @Test
  fun verifyFindByIdentifierInThrowsExceptionIfNotAllAreFound() {
    val identifier1 = randomUUID()
    val identifier2 = randomUUID()
    val craft = craft().withIdentifier(identifier1).build()
    val identifiers = setOf(identifier1, identifier2)
    val crafts = setOf(craft)

    every { craftRepository.findByIdentifierIn(identifiers) } returns crafts

    assertThatExceptionOfType(PreconditionViolationException::class.java).isThrownBy {
      cut.findByIdentifierIn(identifiers)
    }
  }

  @Test
  fun verifyFindByIdentifiersAndTranslationsLocaleEmptyInput() {
    val projections = cut.findByIdentifiersAndTranslationsLocale(emptyList(), ENGLISH.language)

    assertThat(projections).isEmpty()
  }
}
