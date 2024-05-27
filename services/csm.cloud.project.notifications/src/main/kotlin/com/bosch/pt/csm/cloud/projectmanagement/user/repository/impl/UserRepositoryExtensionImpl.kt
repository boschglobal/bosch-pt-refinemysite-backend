/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.user.repository.impl

import com.bosch.pt.csm.cloud.projectmanagement.common.repository.Collections
import com.bosch.pt.csm.cloud.projectmanagement.user.model.User
import com.bosch.pt.csm.cloud.projectmanagement.user.repository.UserRepositoryExtension
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import java.time.Instant
import java.util.Date
import java.util.UUID

open class UserRepositoryExtensionImpl(
    private val mongoOperations: MongoOperations
) : UserRepositoryExtension {

    override fun saveLastSeen(userIdentifier: UUID, lastSeen: Date) {
        mongoOperations.updateFirst(
            findUserQuery(userIdentifier), Update().set("lastSeen", lastSeen), User::class.java
        )
    }

    override fun saveUserPictureIdentifier(userIdentifier: UUID, userPictureIdentifier: UUID) {
        mongoOperations.updateFirst(
            findUserQuery(userIdentifier),
            Update().set("userPictureIdentifier", userPictureIdentifier),
            User::class.java
        )
    }

    override fun deleteUserPictureIdentifier(userIdentifier: UUID) {
        mongoOperations.updateFirst(
            findUserQuery(userIdentifier), Update().unset("userPictureIdentifier"), User::class.java
        )
    }

    override fun deleteUser(identifier: UUID) {
        mongoOperations.remove(findUserQuery(identifier), Collections.USER_STATE)
    }

    override fun findDisplayName(identifier: UUID): String? {
        val query = findUserQuery(identifier)
        query.fields().include("displayName").exclude("_id")
        return mongoOperations.findOne(
            query, FindDisplayNameProjection::class.java,
            Collections.USER_STATE
        )?.displayName
    }

    private fun findUserQuery(userIdentifier: UUID): Query =
        Query().addCriteria(Criteria.where("_id").`is`(userIdentifier))

    override fun findLastSeen(userIdentifier: UUID): Instant? {
        val query = findUserQuery(userIdentifier)
        query.fields().include("lastSeen").exclude("_id")
        return mongoOperations.findOne(
            query, FindLastSeenProjection::class.java,
            Collections.USER_STATE
        )?.lastSeen
    }

    data class FindDisplayNameProjection(val displayName: String? = null)

    data class FindLastSeenProjection(val lastSeen: Instant? = null)
}
