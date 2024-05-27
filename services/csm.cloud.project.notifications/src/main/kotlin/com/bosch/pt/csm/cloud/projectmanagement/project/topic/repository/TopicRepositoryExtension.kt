/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.topic.repository

import com.bosch.pt.csm.cloud.projectmanagement.project.topic.model.Topic
import java.util.UUID

interface TopicRepositoryExtension {

    fun findLatest(identifier: UUID, projectIdentifier: UUID): Topic

    fun deleteTopicAndAllRelatedDocuments(identifier: UUID, projectIdentifier: UUID)
}
