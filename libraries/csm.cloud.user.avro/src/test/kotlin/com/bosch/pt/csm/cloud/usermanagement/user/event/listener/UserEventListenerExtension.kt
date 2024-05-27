/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.user.event.listener

import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.messages.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.usermanagement.user.event.randomProfilePicture
import com.bosch.pt.csm.cloud.usermanagement.user.event.randomUser
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserAggregateAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventEnumAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserPictureAggregateAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserPictureEventAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserPictureEventEnumAvro
import io.mockk.mockk
import io.mockk.verify
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.support.Acknowledgment

@Deprecated("to be removed")
fun UserEventListener.submitUser(
    existingUser: UserAggregateAvro? = null,
    messageKey: AggregateEventMessageKey? = null,
    eventName: UserEventEnumAvro = UserEventEnumAvro.CREATED,
    vararg userAggregateOperations: ((UserAggregateAvro) -> Unit)?
): UserAggregateAvro {
  val user = existingUser.buildEventAvro(eventName, *userAggregateOperations)

  val aggregateIdentifierAvro = user.getAggregate().getAggregateIdentifier()
  val key =
      messageKey
          ?: AggregateEventMessageKey(
              aggregateIdentifierAvro.buildAggregateIdentifier(),
              aggregateIdentifierAvro.getIdentifier().toUUID())

  return submitEvent(key, user, ::listenToUserEvents).getAggregate()
}

@Deprecated("to be removed")
fun UserEventListener.submitUserTombstoneMessage(messageKey: AggregateEventMessageKey) {
  submitEvent(messageKey, null, ::listenToUserEvents)
}

@Deprecated("to be removed")
fun UserEventListener.submitProfilePicture(
    existingPicture: UserPictureAggregateAvro? = null,
    messageKey: AggregateEventMessageKey? = null,
    eventName: UserPictureEventEnumAvro = UserPictureEventEnumAvro.CREATED,
    vararg userPictureAggregateOperations: ((UserPictureAggregateAvro) -> Unit)?
): UserPictureAggregateAvro {
  val userPicture = existingPicture.buildEventAvro(eventName, *userPictureAggregateOperations)

  val aggregate = userPicture.getAggregate()
  val key =
      messageKey
          ?: AggregateEventMessageKey(
              aggregate.getAggregateIdentifier().buildAggregateIdentifier(),
              aggregate.getUserIdentifier().toUUID())

  return submitEvent(key, userPicture, ::listenToUserEvents).getAggregate()
}

@Deprecated("to be removed")
fun UserEventListener.submitProfilePictureTombstoneMessage(messageKey: AggregateEventMessageKey) {
  submitEvent(messageKey, null, ::listenToUserEvents)
}

@Suppress("unused")
fun <V : SpecificRecordBase?> UserEventListener.submitEvent(
    key: EventMessageKey?,
    value: V,
    listener: (ConsumerRecord<EventMessageKey, SpecificRecordBase?>, Acknowledgment) -> Unit
): V {
  mockk<Acknowledgment>(relaxed = true).apply {
    listener(ConsumerRecord("", 0, 0, key, value), this)
    verify { acknowledge() }
  }
  return value
}

private fun UserAggregateAvro?.buildEventAvro(
    event: UserEventEnumAvro,
    vararg blocks: ((UserAggregateAvro) -> Unit)?
) =
    (this?.let { UserEventAvro(event, this) } ?: randomUser(null, event).build()).apply {
      for (block in blocks) block?.invoke(getAggregate())
    }

private fun UserPictureAggregateAvro?.buildEventAvro(
    event: UserPictureEventEnumAvro,
    vararg blocks: ((UserPictureAggregateAvro) -> Unit)?
) =
    (this?.let { UserPictureEventAvro(event, this) } ?: randomProfilePicture(null, event).build())
        .apply { for (block in blocks) block?.invoke(getAggregate()) }

private fun UserPictureAggregateAvro.getUserIdentifier() = getUser().getIdentifier()
