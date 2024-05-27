/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.user.user.shared.repository

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.model.IsoCountryCodeEnum
import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.model.User
import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.repository.dto.UserNameDto
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.EntityGraph.EntityGraphType.LOAD
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface UserRepository : JpaRepository<User, Long> {

  /**
   * Find all user identifiers.
   *
   * @param restrictedCountries when provided, only users of those countries are found
   * @param pageable the pageable to load paged data
   * @return page of user identifiers to return
   */
  @Query(
      "select user.identifier from User user " +
          "where :#{#restrictedCountries.size()} = 0 or user.country in :restrictedCountries")
  fun findAllIdentifiers(
      @Param("restrictedCountries") restrictedCountries: Set<IsoCountryCodeEnum>,
      pageable: Pageable
  ): Page<UserId>

  fun findAllByExternalUserIdIn(externalUserIds: List<String>): List<User>

  /**
   * Finds sorted users with eager loading some relations for a list of user identifiers.
   *
   * @param identifiers identifiers for users
   * @param sort defines the sorting
   * @return sorted list of [User]
   */
  @EntityGraph(attributePaths = ["crafts", "phonenumbers"])
  fun findAllWithDetailsByIdentifierIn(identifiers: Set<UserId>, sort: Sort): List<User>

  /**
   * Finds unique user by its identifier.
   *
   * @param identifier identifier for user to find
   * @return the [User] or null if none found
   */
  fun findOneByIdentifier(identifier: UserId): User?

  @EntityGraph(attributePaths = ["profilePicture", "crafts", "phonenumbers"], type = LOAD)
  fun findWithDetailsByIdentifier(identifier: UserId): User?

  /**
   * Finds unique user by its exernal user id.
   *
   * @param externalUserId user id for user to find
   * @return the [User] or null if none found
   */
  @EntityGraph(attributePaths = ["profilePicture"])
  fun findOneWithPictureByExternalUserId(externalUserId: String): User?

  fun existsByIdentifier(identifier: UserId): Boolean

  /**
   * Find users by a given term containing parts of the first and/or the last name of a users.
   *
   * @param term the term to find users for
   * @param restrictedCountries when provided, only users of those countries are found
   * @param pageable the pagable to load paged data
   * @return page of users to return
   */
  @Query(
      "select u from User u where u.registered = true and u.locked = false " +
          "and (upper(concat(u.firstName, ' ', u.lastName)) like upper(concat('%', :term, '%')) " +
          "or upper(u.email) like upper(concat('%', :term, '%'))) " +
          "and (:#{#restrictedCountries.size()} = 0 or u.country in :restrictedCountries)")
  fun suggestUsersByTerm(
      @Param("term") term: String?,
      @Param("restrictedCountries") restrictedCountries: Set<IsoCountryCodeEnum>,
      pageable: Pageable
  ): Page<User>

  fun deleteByIdentifier(identifier: UserId)

  fun findUserNamesByIdentifierIn(identifier: Set<UserId>): List<UserNameDto>
}
