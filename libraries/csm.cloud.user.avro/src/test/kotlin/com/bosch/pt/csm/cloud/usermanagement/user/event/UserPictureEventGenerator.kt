/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.user.event

import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.model.AggregateIdentifier
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator.Companion.DEFAULT_USER
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator.Companion.newAggregateIdentifier
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator.Companion.newAuditingInformation
import com.bosch.pt.csm.cloud.common.test.randomLong
import com.bosch.pt.csm.cloud.usermanagement.common.UsermanagementAggregateTypeEnum
import com.bosch.pt.csm.cloud.usermanagement.common.UsermanagementAggregateTypeEnum.USER
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserPictureAggregateAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserPictureEventAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserPictureEventEnumAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserPictureEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.usermanagement.user.messages.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.usermanagement.user.messages.getUserIdentifier
import java.time.Instant
import java.util.UUID

@JvmOverloads
fun EventStreamGenerator.submitProfilePicture(
    asReference: String = "profilePicture",
    rootContextIdentifier: UUID =
        getContext().lastIdentifierPerType[USER.value]!!.getIdentifier().toUUID(),
    auditUserReference: String = DEFAULT_USER,
    eventType: UserPictureEventEnumAvro = CREATED,
    time: Instant = getContext().timeLineGenerator.next(),
    aggregateModifications: ((UserPictureAggregateAvro.Builder) -> Unit)? = null
): EventStreamGenerator {
  val existingPicture = get<UserPictureAggregateAvro?>(asReference)

  val defaultAggregateModifications: ((UserPictureAggregateAvro.Builder) -> Unit) = {
    setAuditingInformation(it.auditingInformationBuilder, eventType.name, auditUserReference, time)
    it.aggregateIdentifierBuilder.increase(eventType.name)
  }

  val referenceModifications: ((UserPictureAggregateAvro.Builder) -> Unit) = {
    it.user = it.user ?: getContext().lastIdentifierPerType[USER.value]
  }

  val pictureEvent =
      existingPicture.buildEventAvro(
          eventType, defaultAggregateModifications, aggregateModifications, referenceModifications)

  val messageKey =
      AggregateEventMessageKey(
          pictureEvent.getAggregate().buildAggregateIdentifier(), rootContextIdentifier)

  val sentEvent =
      send("user", asReference, messageKey, pictureEvent, time.toEpochMilli())
          as UserPictureEventAvro
  getContext().events[asReference] = sentEvent.getAggregate()

  return this
}

@JvmOverloads
fun EventStreamGenerator.submitProfilePictureTombstones(
    reference: String = "profilePicture",
    messageKey: EventMessageKey? = null
): EventStreamGenerator {
  val picture = get<UserPictureAggregateAvro>(reference)!!

  if (messageKey == null) {
    val maxVersion = picture.getAggregateIdentifier().getVersion()
    val rootContextIdentifier = picture.getUserIdentifier()
    val pictureIdentifier = picture.getAggregateIdentifier()
    for (version in 0..maxVersion) {
      val key =
          AggregateEventMessageKey(
              AggregateIdentifier(
                  pictureIdentifier.getType(), pictureIdentifier.getIdentifier().toUUID(), version),
              rootContextIdentifier,
          )
      sendTombstoneMessage("user", reference, key)
    }
  } else {
    sendTombstoneMessage("user", reference, messageKey)
  }
  return this
}

private fun UserPictureAggregateAvro?.buildEventAvro(
    event: UserPictureEventEnumAvro,
    vararg blocks: ((UserPictureAggregateAvro.Builder) -> Unit)?
) =
    (this?.let { UserPictureEventAvro.newBuilder().setName(event).setAggregate(this) }
            ?: newProfilePicture(event))
        .apply { for (block in blocks) block?.invoke(aggregateBuilder) }
        .build()

private fun newProfilePicture(
    event: UserPictureEventEnumAvro = CREATED
): UserPictureEventAvro.Builder {
  val picture =
      UserPictureAggregateAvro.newBuilder()
          .setAggregateIdentifierBuilder(
              newAggregateIdentifier(UsermanagementAggregateTypeEnum.USERPICTURE.value))
          .setAuditingInformationBuilder(newAuditingInformation())
          .setFileSize(randomLong())
          .setFullAvailable(true)
          .setSmallAvailable(true)
          .setHeight(randomLong())
          .setWidth(randomLong())

  return UserPictureEventAvro.newBuilder().setAggregateBuilder(picture).setName(event)
}
