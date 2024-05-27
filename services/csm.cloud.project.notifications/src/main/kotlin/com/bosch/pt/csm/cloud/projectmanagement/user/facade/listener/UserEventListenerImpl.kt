/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.user.facade.listener

import com.bosch.pt.csm.cloud.common.businesstransaction.boundary.EventRecord
import com.bosch.pt.csm.cloud.common.extensions.toLocalDateTimeByMillis
import com.bosch.pt.csm.cloud.common.kafka.logConsumption
import com.bosch.pt.csm.cloud.common.messages.IsoCountryCodeEnumAvro
import com.bosch.pt.csm.cloud.common.messages.IsoCountryCodeEnumAvro.AR
import com.bosch.pt.csm.cloud.common.messages.IsoCountryCodeEnumAvro.BR
import com.bosch.pt.csm.cloud.common.messages.IsoCountryCodeEnumAvro.CA
import com.bosch.pt.csm.cloud.common.messages.IsoCountryCodeEnumAvro.CL
import com.bosch.pt.csm.cloud.common.messages.IsoCountryCodeEnumAvro.CO
import com.bosch.pt.csm.cloud.common.messages.IsoCountryCodeEnumAvro.CR
import com.bosch.pt.csm.cloud.common.messages.IsoCountryCodeEnumAvro.CU
import com.bosch.pt.csm.cloud.common.messages.IsoCountryCodeEnumAvro.DO
import com.bosch.pt.csm.cloud.common.messages.IsoCountryCodeEnumAvro.EC
import com.bosch.pt.csm.cloud.common.messages.IsoCountryCodeEnumAvro.GT
import com.bosch.pt.csm.cloud.common.messages.IsoCountryCodeEnumAvro.HN
import com.bosch.pt.csm.cloud.common.messages.IsoCountryCodeEnumAvro.HT
import com.bosch.pt.csm.cloud.common.messages.IsoCountryCodeEnumAvro.JM
import com.bosch.pt.csm.cloud.common.messages.IsoCountryCodeEnumAvro.MX
import com.bosch.pt.csm.cloud.common.messages.IsoCountryCodeEnumAvro.NI
import com.bosch.pt.csm.cloud.common.messages.IsoCountryCodeEnumAvro.PA
import com.bosch.pt.csm.cloud.common.messages.IsoCountryCodeEnumAvro.PE
import com.bosch.pt.csm.cloud.common.messages.IsoCountryCodeEnumAvro.PY
import com.bosch.pt.csm.cloud.common.messages.IsoCountryCodeEnumAvro.SV
import com.bosch.pt.csm.cloud.common.messages.IsoCountryCodeEnumAvro.US
import com.bosch.pt.csm.cloud.common.messages.IsoCountryCodeEnumAvro.UY
import com.bosch.pt.csm.cloud.common.messages.IsoCountryCodeEnumAvro.VE
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.application.config.ProcessStateOnlyProperties
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.listener.BaseEventProcessor
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.listener.strategies.state.CleanUpStateStrategy
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.listener.strategies.state.UpdateStateStrategy
import com.bosch.pt.csm.cloud.usermanagement.user.event.listener.UserEventListener
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventEnumAvro
import datadog.trace.api.Trace
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Profile("!kafka-user-listener-disabled")
@Component
class UserEventListenerImpl(
    updateStateStrategies: Set<UpdateStateStrategy>,
    cleanUpStateStrategies: Set<CleanUpStateStrategy>,
    processStateOnlyProperties: ProcessStateOnlyProperties,
) :
    BaseEventProcessor(updateStateStrategies, cleanUpStateStrategies, processStateOnlyProperties),
    UserEventListener {

  @Trace
  @KafkaListener(topics = ["\${custom.kafka.bindings.user.kafkaTopic}"])
  override fun listenToUserEvents(
      record: ConsumerRecord<EventMessageKey, SpecificRecordBase?>,
      ack: Acknowledgment
  ) {
    LOGGER.logConsumption(record)

    val eventRecord = record.toEventRecord()
    updateState(eventRecord)

    if (!shouldProcessStateOnly(eventRecord)) {
      notifyAdmin(record)
    }

    cleanUpState(eventRecord)
    ack.acknowledge()
  }

  /**
   * In order to assign users (that completed the sing-up) to companies, a notification is sent via
   * TODO to a private channel to notify the administration team to do so
   */
  private fun notifyAdmin(record: ConsumerRecord<*, SpecificRecordBase?>) {
    if (record.value() != null &&
        record.value() is UserEventAvro &&
        (record.value() as UserEventAvro).name in
            arrayOf(UserEventEnumAvro.CREATED, UserEventEnumAvro.REGISTERED)) {
      val userAggregate = (record.value() as UserEventAvro).aggregate
      if (userAggregate.registered) {
        val message = "A new user completed the sign-up: ${userAggregate.email}"
        if (userAggregate.country.isForUsAdmin()) {
            // TODO: Send message here
        } else {
            //TODO: Send message here
        }
      }
    }
  }

  override fun toString() =
      // workaround to avoid illegal reflection access warning
      // by spring proxies (due to java 11)
      "${javaClass.name}@${Integer.toHexString(hashCode())}"

  // List of countries that us admins are responsible for
  private fun IsoCountryCodeEnumAvro.isForUsAdmin() =
      this in
          setOf(
              AR,
              BR,
              CA,
              CL,
              CO,
              CR,
              CU,
              DO,
              EC,
              GT,
              HN,
              HT,
              JM,
              MX,
              NI,
              PA,
              PE,
              PY,
              SV,
              US,
              UY,
              VE)

  companion object {
    private val LOGGER = LoggerFactory.getLogger(UserEventListener::class.java)
  }

  private fun ConsumerRecord<EventMessageKey, SpecificRecordBase?>.toEventRecord() =
      EventRecord(this.key(), this.value(), this.timestamp().toLocalDateTimeByMillis())
}
