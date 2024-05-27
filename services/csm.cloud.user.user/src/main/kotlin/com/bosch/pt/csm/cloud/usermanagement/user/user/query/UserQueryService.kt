/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.user.user.query

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.exceptions.AggregateNotFoundException
import com.bosch.pt.csm.cloud.usermanagement.application.security.AdminAuthorization
import com.bosch.pt.csm.cloud.usermanagement.application.security.NoPreAuthorize
import com.bosch.pt.csm.cloud.usermanagement.application.security.SecurityContextHelper.getCurrentUser
import com.bosch.pt.csm.cloud.usermanagement.common.translation.Key.USER_VALIDATION_ERROR_NOT_FOUND
import com.bosch.pt.csm.cloud.usermanagement.user.authorization.boundary.AdminUserAuthorizationService
import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.model.User
import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.repository.UserRepository
import java.util.stream.Collectors
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserQueryService(
    private val userRepository: UserRepository,
    private val adminUserAuthorizationService: AdminUserAuthorizationService
) {

  @AdminAuthorization
  @Transactional(readOnly = true)
  fun findAllUsers(pageable: Pageable): Page<User> =
      adminUserAuthorizationService.getRestrictedCountries().let { restrictedCountries ->
        userRepository.findAllIdentifiers(restrictedCountries, pageable).let {
          PageImpl(
              userRepository.findAllWithDetailsByIdentifierIn(
                  it.get().collect(Collectors.toSet()), pageable.getSortOr(Sort.unsorted())),
              pageable,
              it.totalElements)
        }
      }

  @Transactional(readOnly = true)
  @NoPreAuthorize
  fun findOneByIdentifier(identifier: UserId): User =
      userRepository.findOneByIdentifier(identifier)
          ?: throw AggregateNotFoundException(
              USER_VALIDATION_ERROR_NOT_FOUND, identifier.toString())

  @Transactional(readOnly = true)
  @AdminAuthorization
  fun findOneWithDetails(identifier: UserId): User =
      userRepository.findWithDetailsByIdentifier(identifier)?.also {
        assertAuthorizedToReadUser(it)
      } ?: throw AggregateNotFoundException(USER_VALIDATION_ERROR_NOT_FOUND, identifier.toString())

  @Transactional(readOnly = true)
  @NoPreAuthorize
  fun findCurrentUserWithDetails(): User =
      userRepository.findWithDetailsByIdentifier(getCurrentUser().identifier)
          ?: throw AggregateNotFoundException(
              USER_VALIDATION_ERROR_NOT_FOUND, getCurrentUser().identifier.toString())

  @Transactional(readOnly = true)
  @NoPreAuthorize
  fun findOneWithPictureByUserId(externalUserId: String): User? =
      userRepository.findOneWithPictureByExternalUserId(externalUserId)

  @Transactional(readOnly = true)
  @NoPreAuthorize
  fun findOneByUserId(userId: UserId): User? = userRepository.findOneByIdentifier(userId)

  @AdminAuthorization
  @Transactional(readOnly = true)
  fun suggestUsersByTerm(term: String?, pageable: Pageable): Page<User> =
      adminUserAuthorizationService.getRestrictedCountries().let {
        userRepository.suggestUsersByTerm(term, it, pageable)
      }

  private fun assertAuthorizedToReadUser(user: User) {
    if (!authorizedForCountryOfUser(user)) {
      throw AccessDeniedException("Unauthorized to access company of that country")
    }
  }

  private fun authorizedForCountryOfUser(user: User): Boolean =
      adminUserAuthorizationService.getRestrictedCountries().let {
        it.isEmpty() || it.contains(user.country)
      }
}
