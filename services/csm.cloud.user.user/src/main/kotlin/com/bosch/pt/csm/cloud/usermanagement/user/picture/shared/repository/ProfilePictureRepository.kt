/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.user.picture.shared.repository

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.usermanagement.user.picture.ProfilePictureId
import com.bosch.pt.csm.cloud.usermanagement.user.picture.shared.model.ProfilePicture
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository

interface ProfilePictureRepository : JpaRepository<ProfilePicture, Long> {

  @EntityGraph(attributePaths = ["user"])
  fun findOneWithDetailsByUserIdentifier(userIdentifier: UserId): ProfilePicture?

  fun findOneByUserIdentifier(userIdentifier: UserId): ProfilePicture?

  fun findOneByIdentifier(identifier: ProfilePictureId): ProfilePicture?

  @EntityGraph(attributePaths = ["user"])
  fun findOneWithDetailsByIdentifier(identifier: ProfilePictureId): ProfilePicture?

  @EntityGraph(attributePaths = ["user"])
  fun findAllWithDetailsByUserIdentifierIn(identifiers: Set<UserId>): List<ProfilePicture>

  fun deleteByIdentifier(identifier: ProfilePictureId)

  fun deleteByUserIdentifier(userIdentifier: UserId)
}
