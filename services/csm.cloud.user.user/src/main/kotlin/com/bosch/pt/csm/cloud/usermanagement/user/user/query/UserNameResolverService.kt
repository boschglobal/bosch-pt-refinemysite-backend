/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.user.user.query

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.api.UuidIdentifiable
import com.bosch.pt.csm.cloud.usermanagement.application.security.NoPreAuthorize
import com.bosch.pt.csm.cloud.usermanagement.user.user.query.dto.ResolvedUserNamesDto
import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.repository.UserRepository
import org.springframework.data.domain.Auditable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserNameResolverService(val userRepository: UserRepository) {

  @Transactional(readOnly = true)
  @NoPreAuthorize
  fun <T : UuidIdentifiable> findUserDisplayNames(auditable: Auditable<T, *, *>) =
      findUserDisplayNames(setOf(auditable))

  @Transactional(readOnly = true)
  @NoPreAuthorize
  fun <T : UuidIdentifiable> findUserDisplayNames(
      auditables: Set<Auditable<T, *, *>>
  ): ResolvedUserNamesDto =
      extractUserIds(auditables)
          .let {
            userRepository.findUserNamesByIdentifierIn(it).associate { username ->
              username.identifier to "${username.firstName} ${username.firstName}"
            }
          }
          .let { ResolvedUserNamesDto(it) }

  private fun <T : UuidIdentifiable> extractUserIds(
      auditables: Set<Auditable<T, *, *>>
  ): Set<UserId> =
      auditables
          .map { setOf(it.createdBy.get().toUuid(), it.lastModifiedBy.get().toUuid()) }
          .flatten()
          .map(::UserId)
          .toSet()
}
