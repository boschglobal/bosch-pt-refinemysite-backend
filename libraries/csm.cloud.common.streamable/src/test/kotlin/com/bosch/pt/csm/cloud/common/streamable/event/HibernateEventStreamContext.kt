/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.streamable.event

import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.test.KafkaListenerFunction
import com.bosch.pt.csm.cloud.common.test.TimeLineGenerator
import com.bosch.pt.csm.cloud.common.test.event.EventStreamContext
import org.apache.avro.specific.SpecificRecordBase

@Deprecated("not used in Architecture 2.0 anymore")
abstract class HibernateEventStreamContext(
    events: MutableMap<String, SpecificRecordBase>,
    lastIdentifierPerType: MutableMap<String, AggregateIdentifierAvro>,
    timeLineGenerator: TimeLineGenerator,
    listeners: MutableMap<String, List<KafkaListenerFunction>>,
    val hibernateListenerAspect: HibernateListenerAspect
) : EventStreamContext(events, lastIdentifierPerType, timeLineGenerator, listeners)
