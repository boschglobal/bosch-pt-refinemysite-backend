/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.workarea.repository

import com.bosch.pt.csm.cloud.projectmanagement.project.workarea.model.WorkArea
import java.util.UUID

interface WorkAreaRepositoryExtension {

    fun findLatest(identifier: UUID, projectIdentifier: UUID): WorkArea

    fun deleteWorkArea(identifier: UUID, projectIdentifier: UUID)
}
