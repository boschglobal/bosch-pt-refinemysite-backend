/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.user.facade.listener.restore.strategy

import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.common.streamable.restoredb.DetachedEntityUpdateCallback
import com.bosch.pt.csm.cloud.usermanagement.common.UsermanagementAggregateTypeEnum.USERPICTURE
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserPictureAggregateAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserPictureEventAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserPictureEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserPictureEventEnumAvro.UPDATED
import com.bosch.pt.iot.smartsite.common.facade.listener.restore.strategy.AbstractRestoreStrategy
import com.bosch.pt.iot.smartsite.common.util.returnUnit
import com.bosch.pt.iot.smartsite.user.model.ProfilePicture
import com.bosch.pt.iot.smartsite.user.model.User
import com.bosch.pt.iot.smartsite.user.repository.ProfilePictureRepository
import com.bosch.pt.iot.smartsite.user.repository.UserRepository
import jakarta.persistence.EntityManager
import java.util.UUID
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Profile("restore-db", "test")
@Component
open class RestoreProfilePictureStrategy(
    private val profilePictureRepository: ProfilePictureRepository,
    userRepository: UserRepository,
    entityManager: EntityManager
) :
    AbstractRestoreStrategy(entityManager, userRepository, profilePictureRepository),
    UserContextRestoreDbStrategy {

  override fun canHandle(
      record: ConsumerRecord<AggregateEventMessageKey, SpecificRecordBase?>
  ): Boolean =
      USERPICTURE.value == record.key().aggregateIdentifier.type &&
          record.value() is UserPictureEventAvro?

  override fun doHandle(record: ConsumerRecord<AggregateEventMessageKey, SpecificRecordBase?>) {
    val event = record.value() as UserPictureEventAvro?

    if (event == null) {
      deleteProfilePicture(record.key().aggregateIdentifier.identifier)
    } else if (event.getName() == CREATED || event.getName() == UPDATED) {
      val aggregate = event.getAggregate()
      val profilePicture = findProfilePicture(aggregate.getAggregateIdentifier())

      if (profilePicture == null) {
        createProfilePicture(aggregate)
      } else {
        updateProfilePicture(profilePicture, aggregate)
      }
    } else {
      handleInvalidEventType(event.getName().name)
    }
  }

  private fun createProfilePicture(aggregate: UserPictureAggregateAvro) {
    val user = findUser(aggregate.getUser())
    val profilePicture = ProfilePicture(user, aggregate.width, aggregate.height, aggregate.fileSize)

    setProfilePictureAttributes(profilePicture, aggregate)
    setAuditAttributes(profilePicture, aggregate.auditingInformation)
    entityManager.persist(profilePicture)
  }

  private fun updateProfilePicture(
      profilePicture: ProfilePicture,
      aggregate: UserPictureAggregateAvro
  ) =
      update(
          profilePicture,
          object : DetachedEntityUpdateCallback<ProfilePicture> {
            override fun update(entity: ProfilePicture) {
              setProfilePictureAttributes(entity, aggregate)
              setAuditAttributes(entity, aggregate.auditingInformation)
            }
          })

  private fun deleteProfilePicture(identifier: UUID) =
      delete(profilePictureRepository.findOneWithDetailsByIdentifier(identifier))

  private fun setProfilePictureAttributes(
      profilePicture: ProfilePicture,
      aggregate: UserPictureAggregateAvro
  ) =
      profilePicture
          .apply {
            fullAvailable = aggregate.getFullAvailable()
            smallAvailable = aggregate.getSmallAvailable()
            identifier = aggregate.getAggregateIdentifier().getIdentifier().toUUID()
            version = aggregate.getAggregateIdentifier().getVersion()
          }
          .returnUnit()

  private fun findProfilePicture(
      aggregateIdentifierAvro: AggregateIdentifierAvro
  ): ProfilePicture? =
      profilePictureRepository.findOneWithDetailsByIdentifier(
          aggregateIdentifierAvro.getIdentifier().toUUID())

  private fun findUser(aggregateIdentifierAvro: AggregateIdentifierAvro): User? =
      userRepository.findOneByIdentifier(aggregateIdentifierAvro.getIdentifier().toUUID())
}
