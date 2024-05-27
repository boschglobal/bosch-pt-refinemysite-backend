/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.job.integration

import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.model.key.CommandMessageKey
import com.bosch.pt.csm.cloud.job.common.JobAggregateTypeEnum.JOB
import com.bosch.pt.csm.cloud.job.messages.CompleteJobCommandAvro
import com.bosch.pt.csm.cloud.job.messages.EnqueueJobCommandAvro
import com.bosch.pt.csm.cloud.job.messages.FailJobCommandAvro
import com.bosch.pt.csm.cloud.job.messages.JsonSerializedObjectAvro
import com.bosch.pt.csm.cloud.job.messages.StartJobCommandAvro
import com.bosch.pt.iot.smartsite.application.config.KafkaTopicProperties.Companion.JOB_COMMAND_BINDING
import com.bosch.pt.iot.smartsite.common.kafka.messaging.CommandSendingService
import datadog.trace.api.Trace
import java.util.UUID
import java.util.UUID.randomUUID
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service

@Service
class JobIntegrationService(
    @Lazy private val commandSendingService: CommandSendingService,
    private val jobJsonSerializer: JobJsonSerializer
) {

  @Trace
  fun enqueueJob(
      jobType: String,
      userIdentifier: UUID,
      context: Any,
      command: Any,
      jobIdentifier: UUID = randomUUID()
  ): UUID {
    EnqueueJobCommandAvro.newBuilder()
        .setAggregateIdentifierBuilder(
            AggregateIdentifierAvro.newBuilder().apply {
              type = JOB.name
              identifier = jobIdentifier.toString()
              version = 0
            })
        .setJobType(jobType)
        .setUserIdentifier(userIdentifier.toString())
        .setJsonSerializedContext(jobJsonSerializer.serialize(context).toAvro())
        .setJsonSerializedCommand(jobJsonSerializer.serialize(command).toAvro())
        .build()
        .also {
          commandSendingService.send(generateMessageKey(jobIdentifier), it, JOB_COMMAND_BINDING)
        }
    return jobIdentifier
  }

  @Trace
  fun startJob(identifier: AggregateIdentifierAvro) {
    StartJobCommandAvro.newBuilder().setAggregateIdentifier(identifier).build().also {
      commandSendingService.send(
          generateMessageKey(identifier.getIdentifier().toUUID()), it, JOB_COMMAND_BINDING)
    }
  }

  @Trace
  fun completeJob(identifier: AggregateIdentifierAvro, result: Any) {
    CompleteJobCommandAvro.newBuilder()
        .setAggregateIdentifier(identifier)
        .setSerializedResult(serializeResult(result))
        .build()
        .also {
          commandSendingService.send(
              generateMessageKey(identifier.getIdentifier().toUUID()), it, JOB_COMMAND_BINDING)
        }
  }

  @Trace
  fun failJob(identifier: AggregateIdentifierAvro) {
    FailJobCommandAvro.newBuilder().setAggregateIdentifier(identifier).build().also {
      commandSendingService.send(
          generateMessageKey(identifier.getIdentifier().toUUID()), it, JOB_COMMAND_BINDING)
    }
  }

  private fun generateMessageKey(identifier: UUID) = CommandMessageKey(identifier)

  private fun serializeResult(result: Any) =
      jobJsonSerializer.serialize(result).let {
        JsonSerializedObjectAvro.newBuilder().setType(it.type).setJson(it.json).build()
      }
}

fun JsonSerializedObjectAvro.toJsonSerializedObject() =
    JsonSerializedObject(this.getType(), this.getJson())
