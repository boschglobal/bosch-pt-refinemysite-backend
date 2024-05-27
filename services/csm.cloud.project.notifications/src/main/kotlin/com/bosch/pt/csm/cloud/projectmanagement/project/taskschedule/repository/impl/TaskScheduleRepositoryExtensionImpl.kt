/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.repository.impl

import com.bosch.pt.csm.cloud.projectmanagement.common.repository.Collections
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.impl.CommonAttributeNames
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.impl.CommonAttributeNames.ID_IDENTIFIER
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.impl.CommonAttributeNames.ID_TYPE
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.impl.CriteriaOperator
import com.bosch.pt.csm.cloud.projectmanagement.project.common.repository.impl.ProjectAttributeNames.ID_TYPE_VALUE_TASK_SCHEDULE
import com.bosch.pt.csm.cloud.projectmanagement.project.common.repository.impl.ProjectAttributeNames.PROJECT_IDENTIFIER
import com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.model.TaskSchedule
import com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.repository.TaskScheduleRepositoryExtension
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query.query
import java.util.UUID

class TaskScheduleRepositoryExtensionImpl(
    private val mongoOperations: MongoOperations
) : TaskScheduleRepositoryExtension {

    override fun find(identifier: UUID, version: Long, projectIdentifier: UUID): TaskSchedule? = mongoOperations
        .findOne(
            findTaskWithVersion(identifier, version, projectIdentifier),
            TaskSchedule::class.java,
            Collections.PROJECT_STATE
        )

    override fun deleteTaskSchedule(identifier: UUID, projectIdentifier: UUID) {
        val criteria = CriteriaOperator.and(belongsToProject(projectIdentifier), isAnyVersionOfTaskSchedule(identifier))
        mongoOperations.remove(query(criteria), Collections.PROJECT_STATE)
    }

    private fun belongsToProject(identifier: UUID): Criteria = Criteria.where(PROJECT_IDENTIFIER).`is`(identifier)

    private fun isAnyVersionOfTaskSchedule(identifier: UUID): Criteria =
        Criteria.where(ID_TYPE).`is`(ID_TYPE_VALUE_TASK_SCHEDULE).and(ID_IDENTIFIER).`is`(identifier)

    private fun findTaskWithVersion(identifier: UUID, version: Long, projectIdentifier: UUID) = query(
        Criteria.where(ID_TYPE)
            .`is`(ID_TYPE_VALUE_TASK_SCHEDULE)
            .and(ID_IDENTIFIER)
            .`is`(identifier)
            .and(CommonAttributeNames.ID_VERSION)
            .`is`(version)
            // we provide the shard key here to improve performance
            .and(PROJECT_IDENTIFIER)
            .`is`(projectIdentifier)
    )
        .limit(1)
}
