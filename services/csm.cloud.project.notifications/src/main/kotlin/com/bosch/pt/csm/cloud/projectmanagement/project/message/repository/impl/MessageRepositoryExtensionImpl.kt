/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.message.repository.impl

import com.bosch.pt.csm.cloud.projectmanagement.common.repository.Collections
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.impl.CommonAttributeNames.ID_IDENTIFIER
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.impl.CommonAttributeNames.ID_TYPE
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.impl.CriteriaOperator
import com.bosch.pt.csm.cloud.projectmanagement.project.common.repository.impl.ProjectAttributeNames.ID_TYPE_VALUE_MESSAGE
import com.bosch.pt.csm.cloud.projectmanagement.project.common.repository.impl.ProjectAttributeNames.PROJECT_IDENTIFIER
import com.bosch.pt.csm.cloud.projectmanagement.project.message.repository.MessageRepositoryExtension
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query.query
import java.util.UUID

open class MessageRepositoryExtensionImpl(
    private val mongoOperations: MongoOperations
) : MessageRepositoryExtension {

    override fun deleteMessage(identifier: UUID, projectIdentifier: UUID) {
        val criteria = CriteriaOperator.and(
            belongsToProject(projectIdentifier), isAnyVersionOfMessage(identifier)
        )
        mongoOperations.remove(query(criteria), Collections.PROJECT_STATE)
    }

    private fun belongsToProject(identifier: UUID): Criteria =
        Criteria.where(PROJECT_IDENTIFIER).`is`(identifier)

    private fun isAnyVersionOfMessage(identifier: UUID): Criteria =
        Criteria.where(ID_TYPE).`is`(ID_TYPE_VALUE_MESSAGE).and(ID_IDENTIFIER).`is`(identifier)
}
