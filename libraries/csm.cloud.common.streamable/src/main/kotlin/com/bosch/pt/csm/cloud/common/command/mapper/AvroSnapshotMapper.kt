/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.command.mapper

import com.bosch.pt.csm.cloud.common.command.snapshotstore.VersionedSnapshot
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import org.apache.avro.specific.SpecificRecordBase

/**
 * This interface defines a mapper that transforms an aggregate snapshot into AVRO. It is used when
 * aggregates are designed to send aggregate snapshots with every change. Only a single mapper
 * (implementation of this interface) is required for an aggregate of this kind.
 */
interface AvroSnapshotMapper<T : VersionedSnapshot> {

  fun toMessageKeyWithCurrentVersion(snapshot: T): AggregateEventMessageKey

  fun toMessageKeyWithNewVersion(snapshot: T): AggregateEventMessageKey

  fun <E : Enum<*>> toAvroMessageWithNewVersion(snapshot: T, eventType: E): SpecificRecordBase
}
