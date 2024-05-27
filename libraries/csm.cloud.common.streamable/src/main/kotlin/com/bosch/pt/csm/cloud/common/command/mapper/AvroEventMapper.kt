/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.command.mapper

import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import org.apache.avro.specific.SpecificRecordBase

/**
 * This interface defines a mapper that transforms an internal event representation into AVRO. It is
 * used when aggregates are designed to be event-sourced and to send fine-grained events. For every
 * aggregate event one mapper (meaning implementation of this interface) is required.
 */
interface AvroEventMapper {
  fun canMap(event: Any): Boolean
  fun mapToKey(event: Any, version: Long): AggregateEventMessageKey
  fun mapToValue(event: Any, version: Long): SpecificRecordBase
}
