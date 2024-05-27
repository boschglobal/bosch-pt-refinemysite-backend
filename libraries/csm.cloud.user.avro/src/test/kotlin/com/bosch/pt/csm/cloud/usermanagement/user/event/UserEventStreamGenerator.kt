/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.user.event

import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.messages.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.common.test.TimeLineGenerator
import com.bosch.pt.csm.cloud.usermanagement.user.event.listener.UserEventListener
import com.bosch.pt.csm.cloud.usermanagement.user.event.listener.submitProfilePicture
import com.bosch.pt.csm.cloud.usermanagement.user.event.listener.submitProfilePictureTombstoneMessage
import com.bosch.pt.csm.cloud.usermanagement.user.event.listener.submitUser
import com.bosch.pt.csm.cloud.usermanagement.user.event.listener.submitUserTombstoneMessage
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserAggregateAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventEnumAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserPictureAggregateAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserPictureEventEnumAvro
import java.time.Instant
import org.apache.avro.specific.SpecificRecordBase

@Deprecated("to be removed")
class UserEventStreamGenerator(
    private val timeLineGenerator: TimeLineGenerator,
    private val eventListener: UserEventListener,
    private val context: MutableMap<String, SpecificRecordBase>
) {

  fun submitUser(
      name: String = "user",
      eventName: UserEventEnumAvro = UserEventEnumAvro.CREATED,
      time: Instant = timeLineGenerator.next(),
      aggregateModifications: ((UserAggregateAvro) -> Unit)? = null
  ): UserEventStreamGenerator {
    val user = get<UserAggregateAvro?>(name)

    val defaultAggregateModifications: ((UserAggregateAvro) -> Unit) = {
      it.apply {
        val auditingInformation = it.getAuditingInformation()
        when (eventName) {
          UserEventEnumAvro.CREATED -> {
            auditingInformation.setCreatedBy(it.getAggregateIdentifier())
            auditingInformation.setCreatedDate(time.toEpochMilli())
          }
          else -> getAggregateIdentifier().increase()
        }
        auditingInformation.setLastModifiedBy(it.getAggregateIdentifier())
        auditingInformation.setLastModifiedDate(time.toEpochMilli())
      }
    }

    context[name] =
        eventListener.submitUser(
            existingUser = user,
            eventName = eventName,
            userAggregateOperations =
                arrayOf(defaultAggregateModifications, aggregateModifications))
    return this
  }

  fun submitUserTombstone(
      name: String = "user",
      messageKey: AggregateEventMessageKey? = null
  ): UserEventStreamGenerator {
    val user = get<UserAggregateAvro>(name)
    val key =
        messageKey
            ?: AggregateEventMessageKey(
                user.getAggregateIdentifier().buildAggregateIdentifier(),
                user.getAggregateIdentifier().getIdentifier().toUUID())
    eventListener.submitUserTombstoneMessage(key)
    return this
  }

  fun submitProfilePicture(
      name: String = "profilePicture",
      userName: String = "user",
      eventName: UserPictureEventEnumAvro = UserPictureEventEnumAvro.CREATED,
      time: Instant = timeLineGenerator.next(),
      aggregateModifications: ((UserPictureAggregateAvro) -> Unit)? = null
  ): UserEventStreamGenerator {
    val user = get<UserAggregateAvro?>(userName)
    val picture = get<UserPictureAggregateAvro?>(name)

    val defaultAggregateModifications: ((UserPictureAggregateAvro) -> Unit) = {
      it.apply {
        val auditingInformation = it.getAuditingInformation()
        when (eventName) {
          UserPictureEventEnumAvro.CREATED -> {
            user?.apply { auditingInformation.setCreatedBy(getAggregateIdentifier()) }
            auditingInformation.setCreatedDate(time.toEpochMilli())
          }
          else -> getAggregateIdentifier().increase()
        }
        user?.apply {
          auditingInformation.setLastModifiedBy(getAggregateIdentifier())
          setUser(getAggregateIdentifier())
        }
        auditingInformation.setLastModifiedDate(time.toEpochMilli())
      }
    }

    context[name] =
        eventListener.submitProfilePicture(
            existingPicture = picture,
            eventName = eventName,
            userPictureAggregateOperations =
                arrayOf(defaultAggregateModifications, aggregateModifications))
    return this
  }

  fun submitProfilePictureTombstone(
      name: String = "profilePicture",
      messageKey: AggregateEventMessageKey? = null
  ): UserEventStreamGenerator {
    val picture = get<UserPictureAggregateAvro>(name)
    val key =
        messageKey
            ?: AggregateEventMessageKey(
                picture.getAggregateIdentifier().buildAggregateIdentifier(),
                picture.getUserIdentifier().toUUID())

    eventListener.submitProfilePictureTombstoneMessage(key)
    return this
  }

  @Suppress("UNCHECKED_CAST") fun <T> get(name: String): T = context[name] as T

  private fun UserPictureAggregateAvro.getUserIdentifier() = getUser().getIdentifier()

  private fun AggregateIdentifierAvro.increase() = setVersion(getVersion() + 1)
}
