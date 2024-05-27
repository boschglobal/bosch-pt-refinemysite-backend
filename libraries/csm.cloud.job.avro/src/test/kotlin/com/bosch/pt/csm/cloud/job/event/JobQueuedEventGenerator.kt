/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.job.event

import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.model.AggregateIdentifier
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator.Companion.DEFAULT_USER
import com.bosch.pt.csm.cloud.job.common.JobAggregateTypeEnum.JOB
import com.bosch.pt.csm.cloud.job.messages.JobQueuedEventAvro
import com.bosch.pt.csm.cloud.job.messages.JsonSerializedObjectAvro
import java.time.Instant
import java.util.UUID.randomUUID

fun EventStreamGenerator.submitJobQueuedEvent(
    asReference: String = "job",
    auditUserReference: String = DEFAULT_USER,
    time: Instant = getContext().timeLineGenerator.next(),
    eventModifications: ((JobQueuedEventAvro.Builder) -> Unit)? = null
): EventStreamGenerator {

  val jobIdentifier = randomUUID()

  val key =
      AggregateEventMessageKey(
          AggregateIdentifier(type = JOB.name, identifier = jobIdentifier, version = 0),
          jobIdentifier)

  val event =
      JobQueuedEventAvro.newBuilder()
          .apply {
            this.aggregateIdentifier =
                AggregateIdentifierAvro.newBuilder()
                    .setType(JOB.name)
                    .setIdentifier(jobIdentifier.toString())
                    .setVersion(0)
                    .build()
            jobType = "FANCY_JOB"
            userIdentifier = getIdentifier(getUserName(auditUserReference)).toString()
            timestamp = time.toEpochMilli()
            jsonSerializedContextBuilder =
                JsonSerializedObjectAvro.newBuilder().apply {
                  type = "ClassName"
                  json = "{}"
                }
            jsonSerializedCommandBuilder =
                JsonSerializedObjectAvro.newBuilder().apply {
                  type = "ClassName"
                  json = "{}"
                }
            eventModifications?.invoke(this)
          }
          .build()

  val sentEvent = send("job", asReference, key, event, time.toEpochMilli()) as JobQueuedEventAvro
  getContext().events[asReference] = sentEvent
  return this
}
