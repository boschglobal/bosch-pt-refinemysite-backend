/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.user.repository

import com.bosch.pt.iot.smartsite.common.repository.ReplicatedEntityRepository
import com.bosch.pt.iot.smartsite.user.model.ProfilePicture
import java.util.UUID
import org.springframework.data.jpa.repository.EntityGraph

/** Repository for [ProfilePicture]s. */
interface ProfilePictureRepository : ReplicatedEntityRepository<ProfilePicture, Long> {

  @EntityGraph(attributePaths = ["user", "createdBy", "lastModifiedBy"])
  fun findOneWithDetailsByIdentifier(identifier: UUID): ProfilePicture?

  fun findOneByIdentifier(identifier: UUID): ProfilePicture?

  fun findOneByUserIdentifier(userIdentifier: UUID): ProfilePicture?
}
