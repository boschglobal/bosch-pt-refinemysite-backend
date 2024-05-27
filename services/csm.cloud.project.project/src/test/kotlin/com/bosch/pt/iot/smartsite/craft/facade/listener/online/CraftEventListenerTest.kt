/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.craft.facade.listener.online

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.get
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.referencedata.craft.CraftAggregateAvro
import com.bosch.pt.csm.cloud.usermanagement.craft.event.submitCraft
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitSystemUserAndActivate
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractIntegrationTestV2
import com.bosch.pt.iot.smartsite.craft.model.Craft
import java.util.Locale.GERMANY
import java.util.Locale.UK
import java.util.UUID.randomUUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@EnableAllKafkaListeners
open class CraftEventListenerTest : AbstractIntegrationTestV2() {

  private val systemUserIdentifier by lazy { getIdentifier("system") }

  private val craftAggregate by lazy { get<CraftAggregateAvro>("craft")!! }
  private val craft by lazy {
    repositories.craftRepository.findOneWithDetailsByIdentifier(getIdentifier("craft"))!!
  }

  @BeforeEach
  fun setup() {
    eventStreamGenerator.submitSystemUserAndActivate()

    useOnlineListener()
  }

  @Test
  fun `validate that craft created event was processed successfully`() {
    eventStreamGenerator.submitCraft()

    validateCraftAttributes(craft, craftAggregate)
  }

  @Test
  fun `validate that craft created event was processed successfully if creator doesn't exist anymore`() {
    eventStreamGenerator.submitCraft {
      it.auditingInformationBuilder.createdBy = getUserIdentifierAvro(randomUUID())
      it.auditingInformationBuilder.lastModifiedBy = getUserIdentifierAvro(randomUUID())
    }

    validateCraftAttributes(craft, craftAggregate)
  }

  @Test
  fun `validate that duplicated craft event is stored only once`() {
    eventStreamGenerator.submitCraft().repeat(1)

    val crafts = repositories.craftRepository.findAll()

    assertThat(crafts).hasSize(1)
    assertThat(crafts.first().version)
        .isEqualTo(craftAggregate.getAggregateIdentifier().getVersion())
  }

  private fun validateCraftAttributes(craft: Craft, craftAggregate: CraftAggregateAvro) {
    assertThat(craft.version).isEqualTo(craftAggregate.getAggregateIdentifier().getVersion())

    val germanTranslation =
        craft.translations.firstOrNull { translation -> translation.locale == GERMANY.toString() }!!
    val englishTranslation =
        craft.translations.firstOrNull { translation -> translation.locale == UK.toString() }!!

    assertThat(craft.defaultName).isEqualTo(craftAggregate.getDefaultName())
    assertThat(germanTranslation.value)
        .isEqualTo(
            craftAggregate
                .getTranslations()
                .first { translation -> translation.getLocale() == GERMANY.toString() }
                .getValue())
    assertThat(englishTranslation.value)
        .isEqualTo(
            craftAggregate
                .getTranslations()
                .first { translation -> translation.getLocale() == UK.toString() }
                .getValue())
    assertThat(craft.createdBy.get().identifier.toString())
        .isEqualTo(systemUserIdentifier.toString())
  }
}
