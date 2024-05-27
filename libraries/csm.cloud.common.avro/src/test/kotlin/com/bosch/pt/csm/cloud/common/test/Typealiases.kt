/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.test

import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import kotlin.reflect.KFunction2
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.support.Acknowledgment

typealias EventConsumerRecord = ConsumerRecord<EventMessageKey, SpecificRecordBase?>

typealias KafkaListenerFunction = KFunction2<EventConsumerRecord, Acknowledgment, Unit>
