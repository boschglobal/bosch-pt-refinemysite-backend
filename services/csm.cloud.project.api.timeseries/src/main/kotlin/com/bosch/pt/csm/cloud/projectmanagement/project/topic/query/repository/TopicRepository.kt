/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.topic.query.repository

import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.ProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.task.domain.TaskId
import com.bosch.pt.csm.cloud.projectmanagement.project.topic.domain.TopicId
import com.bosch.pt.csm.cloud.projectmanagement.project.topic.query.model.Topic
import org.springframework.data.mongodb.repository.MongoRepository

interface TopicRepository : MongoRepository<Topic, TopicId> {

  fun deleteAllByProject(projectId: ProjectId)

  fun findOneByIdentifier(identifier: TopicId): Topic?

  fun findAllByTaskInAndDeletedFalse(taskIds: List<TaskId>): List<Topic>

  fun findAllByProjectIn(projectIds: List<ProjectId>): List<Topic>

  fun findAllByProjectInAndDeletedFalse(projectIds: List<ProjectId>): List<Topic>
}
