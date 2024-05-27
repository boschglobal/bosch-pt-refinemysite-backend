/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.job.job.facade.listener

import com.bosch.pt.csm.cloud.common.kafka.logConsumption
import com.bosch.pt.csm.cloud.common.model.key.CommandMessageKey
import com.bosch.pt.csm.cloud.job.job.api.CompleteJobCommand
import com.bosch.pt.csm.cloud.job.job.api.EnqueueJobCommand
import com.bosch.pt.csm.cloud.job.job.api.FailJobCommand
import com.bosch.pt.csm.cloud.job.job.api.JobCommand
import com.bosch.pt.csm.cloud.job.job.api.JobIdentifier
import com.bosch.pt.csm.cloud.job.job.api.StartJobCommand
import com.bosch.pt.csm.cloud.job.job.api.UserIdentifier
import com.bosch.pt.csm.cloud.job.job.command.handler.JobCommandDispatcher
import com.bosch.pt.csm.cloud.job.messages.CompleteJobCommandAvro
import com.bosch.pt.csm.cloud.job.messages.EnqueueJobCommandAvro
import com.bosch.pt.csm.cloud.job.messages.FailJobCommandAvro
import com.bosch.pt.csm.cloud.job.messages.StartJobCommandAvro
import datadog.trace.api.Trace
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.Logger
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class JobCommandListener(
    private val jobCommandDispatcher: JobCommandDispatcher,
    private val logger: Logger
) {

  @Trace
  @KafkaListener(
      topics = ["\${custom.kafka.bindings.job-command.kafkaTopic}"],
      groupId = "\${custom.kafka.listener.job-command.groupId}",
      clientIdPrefix = "\${custom.kafka.listener.job-command.clientIdPrefix}",
      concurrency = "\${custom.kafka.listener.job-command.concurrency}",
      containerFactory = "transactionalKafkaListenerContainerFactory")
  fun listen(record: ConsumerRecord<CommandMessageKey, SpecificRecord>) {
    logger.logConsumption(record)
    jobCommandDispatcher.dispatch(record.value().toCommand())
  }

  private fun SpecificRecord.toCommand(): JobCommand =
      when (this) {
        is EnqueueJobCommandAvro -> this.toCommand()
        is StartJobCommandAvro -> this.toCommand()
        is CompleteJobCommandAvro -> this.toCommand()
        is FailJobCommandAvro -> this.toCommand()
        else -> error("Unknown job command type: ${this.javaClass.simpleName}")
      }

  private fun EnqueueJobCommandAvro.toCommand() =
      EnqueueJobCommand(
          getJobType(),
          JobIdentifier(getAggregateIdentifier().getIdentifier()),
          UserIdentifier(getUserIdentifier()),
          getJsonSerializedContext().toJsonSerializedObject(),
          getJsonSerializedCommand().toJsonSerializedObject())

  private fun StartJobCommandAvro.toCommand() =
      StartJobCommand(JobIdentifier(getAggregateIdentifier().getIdentifier()))

  private fun CompleteJobCommandAvro.toCommand() =
      CompleteJobCommand(
          JobIdentifier(getAggregateIdentifier().getIdentifier()),
          getSerializedResult().toJsonSerializedObject())

  private fun FailJobCommandAvro.toCommand() =
      FailJobCommand(JobIdentifier(getAggregateIdentifier().getIdentifier()))
}
