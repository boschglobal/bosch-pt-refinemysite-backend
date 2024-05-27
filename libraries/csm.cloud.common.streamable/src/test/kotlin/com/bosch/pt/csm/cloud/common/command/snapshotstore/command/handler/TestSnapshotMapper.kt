/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.command.snapshotstore.command.handler

import com.bosch.pt.csm.cloud.common.command.mapper.AbstractAvroSnapshotMapper
import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro

object TestSnapshotMapper : AbstractAvroSnapshotMapper<TestSnapshot>() {

  override fun <E : Enum<*>> toAvroMessageWithNewVersion(
      snapshot: TestSnapshot,
      eventType: E
  ): AggregateIdentifierAvro = fakeMessage(snapshot)

  private fun fakeMessage(snapshot: TestSnapshot) =
      AggregateIdentifierAvro.newBuilder()
          .setType("BLAH")
          .setIdentifier(snapshot.identifier.toString())
          .setVersion(snapshot.version)
          .build()

  override fun getAggregateType() = "USER"

  override fun getRootContextIdentifier(snapshot: TestSnapshot) = snapshot.rootContextIdentifier
}
