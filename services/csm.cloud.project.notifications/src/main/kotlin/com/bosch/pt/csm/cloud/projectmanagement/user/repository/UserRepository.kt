/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.user.repository

import com.bosch.pt.csm.cloud.projectmanagement.user.model.User
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.mongodb.repository.MongoRepository
import java.util.UUID

interface UserRepository : MongoRepository<User, UUID>, UserRepositoryExtension {

    @Cacheable(cacheNames = ["user"])
    fun findOneCachedByExternalIdentifier(externalIdentifier: String): User?

    @Cacheable(cacheNames = ["user"])
    fun findOneCachedByIdentifier(identifier: UUID): User?
}
