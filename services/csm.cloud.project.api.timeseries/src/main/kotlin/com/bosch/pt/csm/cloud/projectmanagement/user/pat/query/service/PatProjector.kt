/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.user.pat.query.service

import com.bosch.pt.csm.cloud.common.extensions.toLocalDateTimeByMillis
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.user.pat.domain.asPatId
import com.bosch.pt.csm.cloud.projectmanagement.user.pat.query.model.PatProjection
import com.bosch.pt.csm.cloud.projectmanagement.user.pat.query.model.PatScopeEnum
import com.bosch.pt.csm.cloud.projectmanagement.user.pat.query.model.PatTypeEnum
import com.bosch.pt.csm.cloud.projectmanagement.user.pat.query.repository.PatProjectionRepository
import com.bosch.pt.csm.cloud.projectmanagement.user.user.domain.UserId
import com.bosch.pt.csm.cloud.projectmanagement.user.user.domain.asUserId
import com.bosch.pt.csm.cloud.projectmanagement.user.user.query.repository.UserProjectionRepository
import com.bosch.pt.csm.cloud.usermanagement.pat.messages.PatCreatedEventAvro
import com.bosch.pt.csm.cloud.usermanagement.pat.messages.PatUpdatedEventAvro
import org.springframework.stereotype.Component

@Component
class PatProjector(
    private val repository: PatProjectionRepository,
    private val userRepository: UserProjectionRepository
) {

  fun onPatCreatedEvent(event: PatCreatedEventAvro) {
    val existingPat =
        repository.findOneByIdentifier(event.aggregateIdentifier.identifier.toUUID().asPatId())
    val user =
        userRepository.findOneByIdentifier(event.impersonatedUser.identifier.toUUID().asUserId())

    if (existingPat == null) {
      repository.save(
          PatProjection(
              event.aggregateIdentifier.identifier.toUUID().asPatId(),
              event.aggregateIdentifier.version,
              event.description,
              event.impersonatedUser.identifier.toUUID().asUserId(),
              event.hash,
              PatTypeEnum.valueOf(event.type.name),
              event.scopes.map { PatScopeEnum.valueOf(it.name) },
              event.issuedAt.toLocalDateTimeByMillis(),
              event.expiresAt.toLocalDateTimeByMillis(),
              user?.locked ?: false,
              event.auditingInformation.date.toLocalDateTimeByMillis(),
              user?.locale))
    }
  }

  fun onPatUpdatedEvent(event: PatUpdatedEventAvro) {
    val existingPat =
        repository.findOneByIdentifier(event.aggregateIdentifier.identifier.toUUID().asPatId())

    if (existingPat != null && event.aggregateIdentifier.version > existingPat.version) {
      val user = userRepository.findOneByIdentifier(existingPat.impersonatedUserIdentifier)
      repository.save(
          existingPat.copy(
              description = event.description,
              eventDate = event.auditingInformation.date.toLocalDateTimeByMillis(),
              expiresAt = event.expiresAt.toLocalDateTimeByMillis(),
              scopes = event.scopes.map { PatScopeEnum.valueOf(it.name) },
              version = event.aggregateIdentifier.version,
              locked = user?.locked ?: existingPat.locked,
              locale = user?.locale))
    }
  }

  fun onPatDeletedEvent(event: AggregateEventMessageKey) {
    if (repository.existsById(event.aggregateIdentifier.identifier.asPatId())) {
      repository.deleteById(event.aggregateIdentifier.identifier.asPatId())
    }
  }

  fun onUserLockedEvent(userId: UserId) {
    repository
        .findAllByImpersonatedUserIdentifier(userId)
        .filter { !it.locked }
        .map { it.copy(locked = true) }
        .forEach { repository.save(it) }
  }

  fun onUserUnlockedEvent(userId: UserId) {
    repository
        .findAllByImpersonatedUserIdentifier(userId)
        .filter { it.locked }
        .map { it.copy(locked = false) }
        .forEach { repository.save(it) }
  }
}
