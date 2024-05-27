/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.user.repository

import java.util.UUID
import org.springframework.cache.annotation.Cacheable

interface UserRepositoryExtension {

  @Cacheable(cacheNames = ["user-display-name"])
  fun findDisplayNameCached(identifier: UUID): String?

  fun deleteUser(identifier: UUID)

  fun savePicture(identifier: UUID, pictureIdentifier: UUID)

  fun deletePicture(identifier: UUID)
}
