/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.user.repository

import org.springframework.cache.annotation.Cacheable
import java.time.Instant
import java.util.Date
import java.util.UUID

interface UserRepositoryExtension {

    fun saveLastSeen(userIdentifier: UUID, lastSeen: Date)

    fun saveUserPictureIdentifier(userIdentifier: UUID, userPictureIdentifier: UUID)

    fun deleteUserPictureIdentifier(userIdentifier: UUID)

    fun deleteUser(identifier: UUID)

    @Cacheable(cacheNames = ["user-display-name"])
    fun findDisplayName(identifier: UUID): String?

    fun findLastSeen(userIdentifier: UUID): Instant?
}
