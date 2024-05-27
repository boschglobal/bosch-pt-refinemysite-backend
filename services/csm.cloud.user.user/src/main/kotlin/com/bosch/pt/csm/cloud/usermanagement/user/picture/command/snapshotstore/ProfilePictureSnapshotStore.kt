/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.user.picture.command.snapshotstore

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.api.toUserId
import com.bosch.pt.csm.cloud.common.command.snapshotstore.AbstractSnapshotStoreJpa
import com.bosch.pt.csm.cloud.common.exceptions.AggregateNotFoundException
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.usermanagement.common.UsermanagementAggregateTypeEnum.USERPICTURE
import com.bosch.pt.csm.cloud.usermanagement.common.translation.Key.USER_VALIDATION_ERROR_NOT_FOUND
import com.bosch.pt.csm.cloud.usermanagement.common.translation.Key.USER_VALIDATION_ERROR_PROFILE_PICTURE_NOT_FOUND
import com.bosch.pt.csm.cloud.usermanagement.user.eventstore.UserContextSnapshotStore
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserPictureAggregateAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserPictureEventAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserPictureEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserPictureEventEnumAvro.UPDATED
import com.bosch.pt.csm.cloud.usermanagement.user.picture.ProfilePictureId
import com.bosch.pt.csm.cloud.usermanagement.user.picture.shared.model.ProfilePicture
import com.bosch.pt.csm.cloud.usermanagement.user.picture.shared.repository.ProfilePictureRepository
import com.bosch.pt.csm.cloud.usermanagement.user.picture.toProfilePictureId
import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.repository.UserRepository
import java.util.UUID
import org.apache.avro.specific.SpecificRecordBase
import org.springframework.stereotype.Component

@Component
class ProfilePictureSnapshotStore(
    private val repository: ProfilePictureRepository,
    private val userRepository: UserRepository
) :
    AbstractSnapshotStoreJpa<
        UserPictureEventAvro, ProfilePictureSnapshot, ProfilePicture, ProfilePictureId>(),
    UserContextSnapshotStore {

  override fun findOrFail(identifier: ProfilePictureId): ProfilePictureSnapshot =
      repository.findOneByIdentifier(identifier)?.asSnapshot()
          ?: throw AggregateNotFoundException(
              USER_VALIDATION_ERROR_PROFILE_PICTURE_NOT_FOUND, identifier.toString())

  override fun findInternal(identifier: UUID): ProfilePicture? =
      repository.findOneByIdentifier(ProfilePictureId(identifier))

  fun findSnapshotByUserIdentifierOrFail(userIdentifier: UserId): ProfilePictureSnapshot =
      repository.findOneByUserIdentifier(userIdentifier)?.asSnapshot()
          ?: throw AggregateNotFoundException(
              USER_VALIDATION_ERROR_PROFILE_PICTURE_NOT_FOUND, userIdentifier.toString())

  override fun handlesMessage(key: AggregateEventMessageKey, message: SpecificRecordBase): Boolean =
      key.aggregateIdentifier.type == USERPICTURE.name &&
          message is UserPictureEventAvro &&
          message.name.let { it == CREATED || it == UPDATED }

  override fun handlesTombstoneMessage(key: AggregateEventMessageKey): Boolean =
      key.aggregateIdentifier.type == USERPICTURE.name

  override fun updateInternal(event: UserPictureEventAvro, currentSnapshot: ProfilePicture?): Long =
      if (currentSnapshot == null) {
        // workaround for missing tombstone messages
        repository.deleteByUserIdentifier(event.aggregate.user.toUserId())
        repository.flush()

        createProfilePicture(event)
      } else updateProfilePicture(currentSnapshot, event)

  override fun handleTombstoneMessage(key: AggregateEventMessageKey) {
    deleteProfilePicture(ProfilePictureId(key.aggregateIdentifier.identifier))
  }

  override fun isDeletedEvent(message: SpecificRecordBase) = false

  private fun createProfilePicture(event: UserPictureEventAvro) =
      event.getAggregate().let {
        ProfilePicture(
                it.toProfilePictureId(),
                findUserOrFail(it.user.toUserId()),
                it.width,
                it.height,
                it.fileSize)
            .let { picture ->
              setProfilePictureAttributes(picture, it)
              setAuditAttributes(picture, it.auditingInformation)
              repository.saveAndFlush(picture).version
            }
      }

  private fun updateProfilePicture(profilePicture: ProfilePicture, event: UserPictureEventAvro) =
      profilePicture.let {
        setProfilePictureAttributes(it, event.aggregate)
        setAuditAttributes(it, event.aggregate.auditingInformation)
        repository.saveAndFlush(it).version
      }

  private fun deleteProfilePicture(identifier: ProfilePictureId) =
      repository.deleteByIdentifier(identifier)

  private fun setProfilePictureAttributes(
      profilePicture: ProfilePicture,
      aggregate: UserPictureAggregateAvro
  ) =
      profilePicture.apply {
        fileSize = aggregate.fileSize
        height = aggregate.height
        width = aggregate.width
        setFullAvailable(aggregate.fullAvailable)
        setSmallAvailable(aggregate.smallAvailable)
      }

  private fun findUserOrFail(identifier: UserId) =
      userRepository.findOneByIdentifier(identifier)
          ?: throw AggregateNotFoundException(
              USER_VALIDATION_ERROR_NOT_FOUND, identifier.toString())
}
