/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 *  *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.participant.repository.impl

import com.bosch.pt.csm.cloud.projectmanagement.common.repository.Collections
import com.bosch.pt.csm.cloud.projectmanagement.project.common.repository.impl.ProjectAttributeNames.PROJECT_IDENTIFIER
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.model.Participant
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.repository.ParticipantRepositoryExtension
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.core.query.Criteria.where
import org.springframework.data.mongodb.core.query.Query.query
import java.util.UUID

open class ParticipantRepositoryExtensionImpl(
    private val mongoOperations: MongoOperations
) : ParticipantRepositoryExtension {

    override fun findAllByProjectIdentifier(projectIdentifier: UUID): List<Participant> {
        val query = query(where("_class").`is`("Participant").and(PROJECT_IDENTIFIER).`is`(projectIdentifier))
        return mongoOperations.find(query, Participant::class.java, Collections.PROJECT_STATE)
    }
}
