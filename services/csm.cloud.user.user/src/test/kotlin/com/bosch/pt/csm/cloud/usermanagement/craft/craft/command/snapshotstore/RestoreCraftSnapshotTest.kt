/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.craft.craft.command.snapshotstore

import com.bosch.pt.csm.cloud.common.CodeExample
import com.bosch.pt.csm.cloud.common.extensions.toLocalDateTimeByMillis
import com.bosch.pt.csm.cloud.common.messages.AuditingInformationAvro
import com.bosch.pt.csm.cloud.referencedata.craft.CraftAggregateAvro
import com.bosch.pt.csm.cloud.usermanagement.common.AbstractRestoreIntegrationTest
import com.bosch.pt.csm.cloud.usermanagement.craft.craft.toCraftId
import com.bosch.pt.csm.cloud.usermanagement.craft.craft.shared.model.Craft
import com.bosch.pt.csm.cloud.usermanagement.craft.event.submitCraft
import com.bosch.pt.csm.cloud.usermanagement.common.util.randomUUID
import com.bosch.pt.csm.cloud.usermanagement.common.util.toUUID
import java.time.Instant.now
import java.util.Locale
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@CodeExample
class RestoreCraftSnapshotTest : AbstractRestoreIntegrationTest() {

  @Test
  fun `validate that craft created event was processed successfully`() {
    eventStreamGenerator.submitCraft()

    repositories.craftRepository.findAll().also { assertThat(it).hasSize(1) }

    val craftAggregate = eventStreamGenerator.get<CraftAggregateAvro>("craft")!!

    transactionTemplate.executeWithoutResult {
      repositories.craftRepository.findOneByIdentifier(craftAggregate.toCraftId())!!.also {
        validateCraftAttributes(it, craftAggregate)
        validateAuditingInformation(it, craftAggregate)
      }
    }
  }

  @Test
  fun `validate that craft created event was processed successfully if creator doesn't exist anymore`() {

    val randomUuid = randomUUID()

    eventStreamGenerator.submitCraft {
      it.auditingInformation =
          AuditingInformationAvro.newBuilder()
              .setCreatedBy(getUserIdentifierAvro(randomUuid))
              .setCreatedDate(now().toEpochMilli())
              .setLastModifiedBy(getUserIdentifierAvro(randomUuid))
              .setLastModifiedDate(now().toEpochMilli())
              .build()
    }

    repositories.craftRepository.findAll().also { assertThat(it).hasSize(1) }

    val craftAggregate = eventStreamGenerator.get<CraftAggregateAvro>("craft")!!

    transactionTemplate.executeWithoutResult {
      repositories.craftRepository.findOneByIdentifier(craftAggregate.toCraftId())!!.also {
        validateCraftAttributes(it, craftAggregate)

        assertThat(it.createdBy.get().identifier).isEqualTo(randomUuid)
        assertThat(it.lastModifiedBy.get().identifier).isEqualTo(randomUuid)
        assertThat(it.createdDate.get())
            .isEqualTo(
                craftAggregate.getAuditingInformation().getCreatedDate().toLocalDateTimeByMillis())
        assertThat(it.lastModifiedDate.get())
            .isEqualTo(
                craftAggregate
                    .getAuditingInformation()
                    .getLastModifiedDate()
                    .toLocalDateTimeByMillis())
      }
    }
  }

  @Test
  fun `validate that duplicated craft event is stored only once`() {
    eventStreamGenerator.submitCraft().repeat(1)

    repositories.craftRepository.findAll().also { assertThat(it).hasSize(1) }

    val craftAggregate = eventStreamGenerator.get<CraftAggregateAvro>("craft")!!

    transactionTemplate.executeWithoutResult {
      repositories.craftRepository.findOneByIdentifier(craftAggregate.toCraftId())!!.also {
        validateCraftAttributes(it, craftAggregate)
        validateAuditingInformation(it, craftAggregate)
      }
    }
  }

  private fun validateCraftAttributes(craft: Craft, craftAggregate: CraftAggregateAvro) {
    assertThat(craft.getIdentifierUuid())
        .isEqualTo(craftAggregate.getAggregateIdentifier().getIdentifier().toUUID())
    assertThat(craft.version).isEqualTo(craftAggregate.getAggregateIdentifier().getVersion())

    val germanTranslation =
        craft.translations.firstOrNull { translation ->
          translation.locale == Locale.GERMANY.toString()
        }!!
    val englishTranslation =
        craft.translations.firstOrNull { translation ->
          translation.locale == Locale.UK.toString()
        }!!

    assertThat(craft.defaultName).isEqualTo(craftAggregate.getDefaultName())
    assertThat(germanTranslation.value)
        .isEqualTo(
            craftAggregate
                .getTranslations()
                .first { translation -> translation.getLocale() == Locale.GERMANY.toString() }
                .getValue())
    assertThat(englishTranslation.value)
        .isEqualTo(
            craftAggregate
                .getTranslations()
                .first { translation -> translation.getLocale() == Locale.UK.toString() }
                .getValue())
  }
}
