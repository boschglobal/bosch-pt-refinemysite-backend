/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2024
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.user.boundary

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.api.asUserId
import com.bosch.pt.csm.cloud.common.command.snapshotstore.AbstractSnapshotEntity
import com.bosch.pt.csm.cloud.common.exceptions.AggregateNotFoundException
import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.common.i18n.Key.USER_VALIDATION_ERROR_NOT_FOUND
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.dto.DayCardDto
import com.bosch.pt.iot.smartsite.project.taskschedule.shared.model.dto.TaskScheduleSlotWithDayCardDto
import com.bosch.pt.iot.smartsite.user.model.User
import com.bosch.pt.iot.smartsite.user.repository.UserRepository
import datadog.trace.api.Trace
import java.util.UUID
import org.slf4j.Logger
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation.MANDATORY
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(
    private val userRepository: UserRepository,
    private val profilePictureService: ProfilePictureService,
    private val logger: Logger
) {

  @Trace
  @Transactional(readOnly = true)
  @NoPreAuthorize(usedByController = true)
  fun findAllByIdentifiers(identifiers: Set<UUID>): List<User> =
      userRepository.findAllByIdentifierIn(identifiers)

  @Trace
  @Transactional(readOnly = true)
  @NoPreAuthorize
  fun findOneByIdentifier(identifier: UUID): User {
    val user = userRepository.findOneByIdentifier(identifier)
    if (user == null || user.deleted) {
      throw AggregateNotFoundException(USER_VALIDATION_ERROR_NOT_FOUND, identifier.toString())
    }
    return user
  }

  @Trace
  @Transactional(readOnly = true)
  @NoPreAuthorize
  fun findOne(identifier: UUID): User? = userRepository.findOneByIdentifier(identifier)

  @Trace
  @NoPreAuthorize
  @Transactional(readOnly = true)
  fun findAll(identifiers: Set<UUID>) = userRepository.findAllByIdentifierIn(identifiers)

  @Trace
  @NoPreAuthorize
  @Transactional(readOnly = true)
  fun findAuditUsers(snapshotEntity: Collection<AbstractSnapshotEntity<*, *>>): Map<UserId, User> =
      snapshotEntity
          .map { setOf(it.createdBy.get().identifier, it.lastModifiedBy.get().identifier) }
          .flatten()
          .toSet()
          .let { findAll(it) }
          .associateBy { it.identifier!!.asUserId() }

  @Trace
  @NoPreAuthorize
  @Transactional(readOnly = true)
  fun findAuditUsersFromDayCardDtos(dayCardDtos: Collection<DayCardDto>): Map<UserId, User> =
      dayCardDtos
          .map { setOf(it.createdByIdentifier.identifier, it.lastModifiedByIdentifier.identifier) }
          .flatten()
          .toSet()
          .let { findAll(it) }
          .associateBy { it.identifier!!.asUserId() }

  @Trace
  @NoPreAuthorize
  @Transactional(readOnly = true)
  fun findAuditUsersForMessageDtos(
      messageDtos:
          Collection<com.bosch.pt.iot.smartsite.project.message.shared.model.dto.MessageDto>
  ): Map<UserId, User> =
      messageDtos
          .map { setOf(it.createdBy.identifier, it.lastModifiedBy.identifier) }
          .flatten()
          .toSet()
          .let { findAll(it) }
          .associateBy { it.identifier!!.asUserId() }

  @Trace
  @NoPreAuthorize
  @Transactional(readOnly = true)
  fun findAuditUsersFromTaskScheduleSlotWithDayCardDto(
      scheduleSlotWithDayCardDtos: Collection<TaskScheduleSlotWithDayCardDto>
  ): Map<UserId, User> =
      scheduleSlotWithDayCardDtos
          .map {
            setOf(it.slotsDayCardCreatedBy?.identifier, it.slotsDayCardLastModifiedBy?.identifier)
          }
          .flatten()
          .toSet()
          .let { findAll(it as Set<UUID>) }
          .associateBy { it.identifier!!.asUserId() }

  @Trace
  @Transactional(readOnly = true)
  @NoPreAuthorize
  fun findOneWithPictureByUserId(userId: String): User? =
      userRepository.findOneWithPictureByCiamUserIdentifier(userId)

  @Trace
  @Transactional(propagation = MANDATORY)
  @NoPreAuthorize
  fun update(user: User): UUID {
    requireNotNull(user.ciamUserIdentifier) { "CIAM identifier must not be null" }
    val existingUser = userRepository.findOneByCiamUserIdentifier(user.ciamUserIdentifier!!)
    check(existingUser != null) {
      "Cannot update user ${user.identifier} because no such user was found."
    }

    // instruct Hibernate to update existingUser with values from passed user
    user.id = existingUser.id

    return userRepository.save(user).identifier!!
  }

  @Trace
  @Transactional(propagation = MANDATORY)
  @NoPreAuthorize
  fun create(user: User): UUID {
    requireNotNull(user.ciamUserIdentifier) { "CIAM identifier must not be null" }
    val existingUser = userRepository.findOneByCiamUserIdentifier(user.ciamUserIdentifier!!)

    // idempotency: return if this user exists already
    if (existingUser != null && existingUser.isSameUser(user)) {
      logger.warn(
          "Trying or create user ${user.identifier} that exists already. Because both users share the " +
              "same identifier and CIAM identifier, we assume this is due to a duplicated event. " +
              "For idempotency, this event will be ignored.")
      return user.identifier!!
    }

    check(existingUser == null) {
      "Cannot create user ${user.identifier} because there is already a user with the same CIAM identifier. " +
          "Probably the user was deleted and then recreated but the deleted event has not been processed yet."
    }
    return userRepository.save(user).identifier!!
  }

  @Trace
  @Transactional(propagation = MANDATORY)
  @NoPreAuthorize
  fun anonymizeUser(userUuid: UUID, version: Long) {
    val userToDelete = userRepository.findWithDetailsByIdentifier(userUuid)
    if (userToDelete != null) {
      profilePictureService.deleteProfilePictureByUser(userUuid)
      userToDelete.anonymize()
      userToDelete.version = version
      userRepository.save(userToDelete)
    }
  }

  private fun User.isSameUser(other: User) =
      identifier == other.identifier &&
          ciamUserIdentifier == other.ciamUserIdentifier &&
          version == other.version
}
