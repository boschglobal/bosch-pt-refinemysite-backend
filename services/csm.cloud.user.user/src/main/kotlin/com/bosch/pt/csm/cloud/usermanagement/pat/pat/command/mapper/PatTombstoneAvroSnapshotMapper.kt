/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.pat.pat.command.mapper

import com.bosch.pt.csm.cloud.common.command.mapper.AbstractAvroSnapshotMapper
import com.bosch.pt.csm.cloud.common.command.mapper.AvroSnapshotMapper
import com.bosch.pt.csm.cloud.usermanagement.pat.common.PatAggregateTypeEnum.PAT
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.command.snapshotstore.PatSnapshot
import org.springframework.stereotype.Component

/**
 * Needed as [com.bosch.pt.csm.cloud.common.command.handler.CommandHandler] requires an
 * [AvroSnapshotMapper] to handle Tombstone message. Only handles tombstones.
 */
@Component
class PatTombstoneAvroSnapshotMapper : AbstractAvroSnapshotMapper<PatSnapshot>() {

  override fun getAggregateType() = PAT.value

  override fun <E : Enum<*>> toAvroMessageWithNewVersion(snapshot: PatSnapshot, eventType: E) =
      throw NotImplementedError("This mapper is intended for tombstone message handling only!")

  override fun getRootContextIdentifier(snapshot: PatSnapshot) =
      snapshot.impersonatedUser.identifier
}
