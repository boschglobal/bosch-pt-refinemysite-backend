/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.user.repository.impl

import com.bosch.pt.csm.cloud.projectmanagement.common.repository.AttributeNames.Common.ID
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.AttributeNames.User.DISPLAY_NAME
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.AttributeNames.User.USER_PICTURE_IDENTIFIER
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.Collections.USER_STATE
import com.bosch.pt.csm.cloud.projectmanagement.user.model.User
import com.bosch.pt.csm.cloud.projectmanagement.user.repository.UserRepositoryExtension
import java.util.UUID
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.core.query.Criteria.where
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update

open class UserRepositoryExtensionImpl(private val mongoOperations: MongoOperations) :
    UserRepositoryExtension {

  override fun findDisplayNameCached(identifier: UUID): String? {
    val query = findUserQuery(identifier).apply { fields().include(DISPLAY_NAME).exclude(ID) }

    return mongoOperations.findOne(query, FindDisplayNameProjection::class.java, USER_STATE)
        ?.displayName
  }

  override fun deleteUser(identifier: UUID) {
    mongoOperations.remove(findUserQuery(identifier), USER_STATE)
  }

  override fun savePicture(identifier: UUID, pictureIdentifier: UUID) {
    mongoOperations.updateFirst(
        findUserQuery(identifier),
        Update().set(USER_PICTURE_IDENTIFIER, pictureIdentifier),
        User::class.java)
  }

  override fun deletePicture(identifier: UUID) {
    mongoOperations.updateFirst(
        findUserQuery(identifier), Update().unset(USER_PICTURE_IDENTIFIER), User::class.java)
  }

  private fun findUserQuery(identifier: UUID) = Query().addCriteria(where(ID).`is`(identifier))

  data class FindDisplayNameProjection(val displayName: String? = null)
}
