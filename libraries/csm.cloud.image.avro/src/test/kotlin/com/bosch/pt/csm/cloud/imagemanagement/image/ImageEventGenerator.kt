/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.imagemanagement.image

import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.model.AggregateIdentifier
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator
import com.bosch.pt.csm.cloud.image.messages.ImageDeletedEventAvro
import com.bosch.pt.csm.cloud.image.messages.ImageScaledEventAvro
import java.time.Instant
import java.util.UUID
import org.apache.avro.specific.SpecificRecordBase

@JvmOverloads
fun EventStreamGenerator.profilePictureImageScaled(
    asReference: String,
    userReference: String,
    time: Instant = getContext().timeLineGenerator.next(),
    aggregateModifications: ((ImageScaledEventAvro.Builder) -> Unit)? = null
): EventStreamGenerator =
    imageScaled(
        asReference, "USERPICTURE", getIdentifier(userReference), time, aggregateModifications)

@JvmOverloads
fun EventStreamGenerator.profilePictureImageDeleted(
    asReference: String,
    userReference: String,
    time: Instant = getContext().timeLineGenerator.next(),
    aggregateModifications: ((ImageDeletedEventAvro.Builder) -> Unit)? = null
): EventStreamGenerator =
    imageDeleted(
        asReference, "USERPICTURE", getIdentifier(userReference), time, aggregateModifications)

@JvmOverloads
fun EventStreamGenerator.projectPictureImageScaled(
    asReference: String,
    projectReference: String,
    time: Instant = getContext().timeLineGenerator.next(),
    aggregateModifications: ((ImageScaledEventAvro.Builder) -> Unit)? = null
): EventStreamGenerator =
    imageScaled(
        asReference,
        "PROJECTPICTURE",
        getIdentifier(projectReference),
        time,
        aggregateModifications)

@JvmOverloads
fun EventStreamGenerator.projectPictureImageDeleted(
    asReference: String,
    projectReference: String,
    time: Instant = getContext().timeLineGenerator.next(),
    aggregateModifications: ((ImageDeletedEventAvro.Builder) -> Unit)? = null
): EventStreamGenerator =
    imageDeleted(
        asReference,
        "PROJECTPICTURE",
        getIdentifier(projectReference),
        time,
        aggregateModifications)

@JvmOverloads
fun EventStreamGenerator.taskAttachmentImageScaled(
    asReference: String,
    projectReference: String,
    time: Instant = getContext().timeLineGenerator.next(),
    aggregateModifications: ((ImageScaledEventAvro.Builder) -> Unit)? = null
): EventStreamGenerator =
    imageScaled(
        asReference,
        "TASKATTACHMENT",
        getIdentifier(projectReference),
        time,
        aggregateModifications)

@JvmOverloads
fun EventStreamGenerator.taskAttachmentImageDeleted(
    asReference: String,
    projectReference: String,
    time: Instant = getContext().timeLineGenerator.next(),
    aggregateModifications: ((ImageDeletedEventAvro.Builder) -> Unit)? = null
): EventStreamGenerator =
    imageDeleted(
        asReference,
        "TASKATTACHMENT",
        getIdentifier(projectReference),
        time,
        aggregateModifications)

@JvmOverloads
fun EventStreamGenerator.topicAttachmentImageScaled(
    asReference: String,
    projectReference: String,
    time: Instant = getContext().timeLineGenerator.next(),
    aggregateModifications: ((ImageScaledEventAvro.Builder) -> Unit)? = null
): EventStreamGenerator =
    imageScaled(
        asReference,
        "TOPICATTACHMENT",
        getIdentifier(projectReference),
        time,
        aggregateModifications)

@JvmOverloads
fun EventStreamGenerator.topicAttachmentImageDeleted(
    asReference: String,
    projectReference: String,
    time: Instant = getContext().timeLineGenerator.next(),
    aggregateModifications: ((ImageDeletedEventAvro.Builder) -> Unit)? = null
): EventStreamGenerator =
    imageDeleted(
        asReference,
        "TOPICATTACHMENT",
        getIdentifier(projectReference),
        time,
        aggregateModifications)

@JvmOverloads
fun EventStreamGenerator.messageAttachmentImageScaled(
    asReference: String,
    projectReference: String,
    time: Instant = getContext().timeLineGenerator.next(),
    aggregateModifications: ((ImageScaledEventAvro.Builder) -> Unit)? = null
): EventStreamGenerator =
    imageScaled(
        asReference,
        "MESSAGEATTACHMENT",
        getIdentifier(projectReference),
        time,
        aggregateModifications)

@JvmOverloads
fun EventStreamGenerator.messageAttachmentImageDeleted(
    asReference: String,
    projectReference: String,
    time: Instant = getContext().timeLineGenerator.next(),
    aggregateModifications: ((ImageDeletedEventAvro.Builder) -> Unit)? = null
): EventStreamGenerator =
    imageDeleted(
        asReference,
        "MESSAGEATTACHMENT",
        getIdentifier(projectReference),
        time,
        aggregateModifications)

@JvmOverloads
fun EventStreamGenerator.imageScaled(
    asReference: String,
    aggregateType: String,
    rootContextIdentifier: UUID,
    time: Instant = getContext().timeLineGenerator.next(),
    aggregateModifications: ((ImageScaledEventAvro.Builder) -> Unit)? = null
): EventStreamGenerator {

  val defaultAggregateModifications: ((ImageScaledEventAvro.Builder) -> Unit) = {}

  val event =
      ImageScaledEventAvro.newBuilder()
          .apply { defaultAggregateModifications.invoke(this) }
          .apply { aggregateModifications?.invoke(this) }
          .build()

  val key =
      AggregateEventMessageKey(
          AggregateIdentifier(aggregateType, event.identifier.toUUID(), 0L), rootContextIdentifier)

  sendEvent(asReference, key, event, time)
  return this
}

@JvmOverloads
fun EventStreamGenerator.imageDeleted(
    asReference: String,
    aggregateType: String,
    rootContextIdentifier: UUID,
    time: Instant = getContext().timeLineGenerator.next(),
    aggregateModifications: ((ImageDeletedEventAvro.Builder) -> Unit)? = null
): EventStreamGenerator {

  val defaultAggregateModifications: ((ImageDeletedEventAvro.Builder) -> Unit) = {}

  val event =
      ImageDeletedEventAvro.newBuilder()
          .apply { defaultAggregateModifications.invoke(this) }
          .apply { aggregateModifications?.invoke(this) }
          .build()

  val key =
      AggregateEventMessageKey(
          AggregateIdentifier(aggregateType, event.identifier.toUUID(), 0L), rootContextIdentifier)

  sendEvent(asReference, key, event, time)
  return this
}

private fun EventStreamGenerator.sendEvent(
    asReference: String,
    key: EventMessageKey,
    event: SpecificRecordBase,
    time: Instant
) {
  val sentEvent = send("image", asReference, key, event, time.toEpochMilli()) as SpecificRecordBase
  getContext().events[asReference] = sentEvent
  getContext().lastRootContextIdentifier =
      (key as AggregateEventMessageKey).aggregateIdentifier.toAvro()
}
