/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

@file:JvmName("CraftEventStreamRandomAggregate")

package com.bosch.pt.csm.cloud.usermanagement.craft.event

import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.messages.AuditingInformationAvro
import com.bosch.pt.csm.cloud.common.test.randomLong
import com.bosch.pt.csm.cloud.common.test.randomString
import com.bosch.pt.csm.cloud.referencedata.craft.common.CraftAggregateTypeEnum
import com.bosch.pt.csm.cloud.referencedata.craft.CraftAggregateAvro
import com.bosch.pt.csm.cloud.referencedata.craft.CraftEventAvro
import com.bosch.pt.csm.cloud.referencedata.craft.CraftEventEnumAvro
import com.bosch.pt.csm.cloud.referencedata.craft.CraftTranslationAvro
import java.util.Locale.GERMANY
import java.util.Locale.UK

fun randomCraft(
    block: ((CraftAggregateAvro) -> Unit)? = null,
    event: CraftEventEnumAvro = CraftEventEnumAvro.CREATED
): CraftEventAvro.Builder {
  val defaultName = randomString()
  val craft =
      CraftAggregateAvro.newBuilder()
          .setAggregateIdentifierBuilder(randomIdentifier(CraftAggregateTypeEnum.CRAFT.value))
          .setAuditingInformation(randomAuditing())
          .setDefaultName(defaultName)
          .setTranslations(
              listOf(
                  CraftTranslationAvro.newBuilder()
                      .setLocale(GERMANY.toString())
                      .setValue(defaultName)
                      .build(),
                  CraftTranslationAvro.newBuilder()
                      .setLocale(UK.toString())
                      .setValue(randomString())
                      .build()))
          .build()
          .also { block?.invoke(it) }

  return CraftEventAvro.newBuilder().setAggregate(craft).setName(event)
}

fun randomAuditing(block: ((AuditingInformationAvro) -> Unit)? = null): AuditingInformationAvro =
    AuditingInformationAvro.newBuilder()
        .setCreatedByBuilder(randomIdentifier())
        .setCreatedDate(randomLong())
        .setLastModifiedByBuilder(randomIdentifier())
        .setLastModifiedDate(randomLong())
        .build()
        .also { block?.invoke(it) }

fun randomIdentifier(type: String = randomString()): AggregateIdentifierAvro.Builder =
    AggregateIdentifierAvro.newBuilder()
        .setIdentifier(randomString())
        .setType(type)
        .setVersion(randomLong())
