/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.craft.facade.listener.restore.strategy

import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.get
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.referencedata.craft.CraftAggregateAvro
import com.bosch.pt.csm.cloud.usermanagement.craft.event.submitCraft
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitSystemUserAndActivate
import com.bosch.pt.iot.smartsite.common.facade.listener.AbstractRestoreIntegrationTestV2
import com.bosch.pt.iot.smartsite.craft.model.Craft
import java.util.Locale.GERMANY
import java.util.Locale.UK
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

open class RestoreCraftStrategyTest : AbstractRestoreIntegrationTestV2() {

  private val craftAggregate by lazy { get<CraftAggregateAvro>("craft")!! }
  private val craft by lazy {
    repositories.craftRepository.findOneWithDetailsByIdentifier(getIdentifier("craft"))!!
  }

  @BeforeEach
  fun init() {
    eventStreamGenerator.submitSystemUserAndActivate()
  }

  @Test
  open fun `validate that craft created event was processed successfully`() {
    eventStreamGenerator.submitCraft()

    assertThat(repositories.craftRepository.findAll()).hasSize(1)
    validateCraftAttributes(craft, craftAggregate)
  }

  @Test
  open fun `validate that duplicated craft event is stored only once`() {
    eventStreamGenerator.submitCraft().repeat(1)

    assertThat(repositories.craftRepository.findAll()).hasSize(1)
    validateCraftAttributes(craft, craftAggregate)
  }

  private fun validateCraftAttributes(craft: Craft, craftAggregate: CraftAggregateAvro) {
    validateAuditableAndVersionedEntityAttributes(craft, craftAggregate)

    assertThat(craft.identifier)
        .isEqualTo(craftAggregate.getAggregateIdentifier().getIdentifier().toUUID())
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
  }
}
