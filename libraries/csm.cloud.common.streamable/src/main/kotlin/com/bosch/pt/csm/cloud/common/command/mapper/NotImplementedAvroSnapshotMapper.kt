/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.command.mapper

import com.bosch.pt.csm.cloud.common.command.snapshotstore.VersionedSnapshot

class NotImplementedAvroSnapshotMapper<T : VersionedSnapshot> : AvroSnapshotMapper<T> {
  override fun toMessageKeyWithCurrentVersion(snapshot: T) = throw NotImplementedError()

  override fun toMessageKeyWithNewVersion(snapshot: T) = throw NotImplementedError()

  override fun <E : Enum<*>> toAvroMessageWithNewVersion(snapshot: T, eventType: E) =
      throw NotImplementedError()
}
