/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.task.repository

import java.util.UUID

interface TaskRepositoryExtension {

  fun deleteTaskAndAllRelatedDocuments(identifier: UUID, projectIdentifier: UUID)
}
