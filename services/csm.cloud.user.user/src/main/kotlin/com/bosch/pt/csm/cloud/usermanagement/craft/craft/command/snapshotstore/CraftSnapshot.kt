/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.craft.craft.command.snapshotstore

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.command.handler.CommandHandler
import com.bosch.pt.csm.cloud.common.command.snapshotstore.AuditableSnapshot
import com.bosch.pt.csm.cloud.common.command.snapshotstore.VersionedSnapshot
import com.bosch.pt.csm.cloud.usermanagement.common.model.Translation
import com.bosch.pt.csm.cloud.usermanagement.craft.craft.CraftId
import com.bosch.pt.csm.cloud.usermanagement.craft.craft.command.mapper.CraftAvroSnapshotMapper
import com.bosch.pt.csm.cloud.usermanagement.craft.craft.shared.model.Craft
import java.time.LocalDateTime

@Suppress("LongParameterList")
class CraftSnapshot(
    override var identifier: CraftId,
    override var version: Long,
    override var createdDate: LocalDateTime?,
    override var createdBy: UserId?,
    override var lastModifiedDate: LocalDateTime?,
    override var lastModifiedBy: UserId?,
    var defaultName: String?,
    var translations: List<CraftTranslationValueObject>
) : VersionedSnapshot, AuditableSnapshot {

  constructor(
      craft: Craft
  ) : this(
      craft.identifier,
      craft.version,
      craft.createdDate.orElse(null),
      craft.createdBy.orElse(null),
      craft.lastModifiedDate.orElse(null),
      craft.lastModifiedBy.orElse(null),
      craft.defaultName,
      craft.translations.map { it.asSnapshot() })

  fun toCommandHandler() = CommandHandler.of(this, CraftAvroSnapshotMapper)
}

fun Craft.asSnapshot() = CraftSnapshot(this)

data class CraftTranslationValueObject(var locale: String?, var value: String?) {
  constructor(translation: Translation) : this(translation.locale, translation.value)
}

fun Translation.asSnapshot() = CraftTranslationValueObject(this)
