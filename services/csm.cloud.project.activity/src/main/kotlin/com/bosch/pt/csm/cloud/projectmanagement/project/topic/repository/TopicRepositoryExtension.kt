/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.topic.repository

import java.util.UUID

interface TopicRepositoryExtension {

  fun deleteTopicAndAllRelatedDocuments(identifier: UUID, projectIdentifier: UUID)
}
