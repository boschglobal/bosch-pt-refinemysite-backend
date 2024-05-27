/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.config

import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.test.KafkaListenerFunction
import com.bosch.pt.csm.cloud.common.test.TimeLineGenerator
import com.bosch.pt.csm.cloud.common.test.event.EventStreamContext
import com.bosch.pt.csm.cloud.common.util.ThreadUtil.runAsThreadAndWaitForCompletion
import org.apache.avro.specific.SpecificRecordBase
import org.springframework.security.test.context.TestSecurityContextHolder
import org.springframework.web.context.request.RequestContextHolder

class NotificationServiceEventStreamContext(
    events: MutableMap<String, SpecificRecordBase>,
    lastIdentifierPerType: MutableMap<String, AggregateIdentifierAvro>,
    timeLineGenerator: TimeLineGenerator,
    listeners: MutableMap<String, List<KafkaListenerFunction>>
) : EventStreamContext(events, lastIdentifierPerType, timeLineGenerator, listeners) {

  override fun send(runnable: Runnable) {
    simulateKafkaListener { runnable.run() }
  }

  private fun simulateKafkaListener(procedure: Runnable) {
    runAsThreadAndWaitForCompletion(10_000) {
      RequestContextHolder.resetRequestAttributes()
      TestSecurityContextHolder.clearContext()
      procedure.run()
    }
  }
}
