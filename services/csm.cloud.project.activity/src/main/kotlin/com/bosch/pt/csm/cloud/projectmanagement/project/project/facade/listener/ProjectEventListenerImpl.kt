/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.project.facade.listener

import com.bosch.pt.csm.cloud.common.kafka.logConsumption
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.activity.facade.listener.ActivityCreator
import com.bosch.pt.csm.cloud.projectmanagement.activity.facade.listener.StateManager
import com.bosch.pt.csm.cloud.projectmanagement.project.event.listener.ProjectEventListener
import datadog.trace.api.Trace
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Profile("!kafka-project-listener-disabled")
@Component
class ProjectEventListenerImpl(
    private val activityCreator: ActivityCreator,
    private val stateManager: StateManager
) : ProjectEventListener {

  @Trace
  @KafkaListener(topics = ["\${custom.kafka.bindings.project.kafkaTopic}"])
  override fun listenToProjectEvents(
      record: ConsumerRecord<EventMessageKey, SpecificRecordBase?>,
      ack: Acknowledgment
  ) {
    LOGGER.logConsumption(record)
    val key = record.key()
    val value = record.value()

    stateManager.updateState(key, value)
    activityCreator.createActivities(key, value)
    stateManager.cleanUpState(key, value)
    ack.acknowledge()
  }

  /** Workaround to avoid illegal reflection access warning by spring proxies (due to java 11) */
  override fun toString() = "${javaClass.name}@${Integer.toHexString(hashCode())}"

  companion object {
    private val LOGGER = LoggerFactory.getLogger(ProjectEventListenerImpl::class.java)
  }
}
