/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.common.streamable.restoredb

import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord

@Deprecated("To be replaces with new architecture")
interface RestoreDbStrategy {

  fun canHandle(record: ConsumerRecord<AggregateEventMessageKey, SpecificRecordBase?>): Boolean

  fun handle(record: ConsumerRecord<AggregateEventMessageKey, SpecificRecordBase?>)
}
