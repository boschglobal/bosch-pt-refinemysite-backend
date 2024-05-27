/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.craft.repository

import com.bosch.pt.csm.cloud.projectmanagement.project.craft.model.ProjectCraft
import java.util.UUID

interface ProjectCraftRepositoryExtension {

    fun findLatest(identifier: UUID, projectIdentifier: UUID): ProjectCraft

    fun deleteProjectCraft(identifier: UUID, projectIdentifier: UUID)
}
