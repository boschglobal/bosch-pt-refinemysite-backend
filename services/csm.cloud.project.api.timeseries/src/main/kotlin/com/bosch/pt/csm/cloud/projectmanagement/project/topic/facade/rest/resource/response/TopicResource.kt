/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.topic.facade.rest.resource.response

import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.ProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.task.domain.TaskId
import com.bosch.pt.csm.cloud.projectmanagement.project.topic.domain.TopicId

data class TopicResource(
    val id: TopicId,
    val version: Long,
    val project: ProjectId,
    val task: TaskId,
    val criticality: String,
    val description: String? = null,
    val deleted: Boolean,
    val eventTimestamp: Long
)
