/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.craft.craft.command.mapper

import com.bosch.pt.csm.cloud.common.command.mapper.AbstractAvroSnapshotMapper
import com.bosch.pt.csm.cloud.referencedata.craft.CraftAggregateAvro
import com.bosch.pt.csm.cloud.referencedata.craft.CraftEventAvro
import com.bosch.pt.csm.cloud.referencedata.craft.CraftEventEnumAvro
import com.bosch.pt.csm.cloud.referencedata.craft.CraftTranslationAvro
import com.bosch.pt.csm.cloud.usermanagement.craft.craft.command.snapshotstore.CraftSnapshot

object CraftAvroSnapshotMapper : AbstractAvroSnapshotMapper<CraftSnapshot>() {

  override fun <E : Enum<*>> toAvroMessageWithNewVersion(snapshot: CraftSnapshot, eventType: E) =
      CraftEventAvro.newBuilder()
          .setName(eventType as CraftEventEnumAvro)
          .setAggregateBuilder(
              CraftAggregateAvro.newBuilder()
                  .setAggregateIdentifier(toAggregateIdentifierAvroWithNextVersion(snapshot))
                  .setAuditingInformation(toUpdatedAuditingInformationAvro(snapshot))
                  .setDefaultName(snapshot.defaultName)
                  .setTranslations(
                      snapshot.translations.map {
                        CraftTranslationAvro.newBuilder()
                            .setLocale(it.locale)
                            .setValue(it.value)
                            .build()
                      }))
          .build()

  override fun getRootContextIdentifier(snapshot: CraftSnapshot) = snapshot.identifier.toUuid()

  override fun getAggregateType() = "CRAFT"
}
