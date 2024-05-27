/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.projectcraft.command.snapshotstore

import com.bosch.pt.csm.cloud.common.command.snapshotstore.AbstractSnapshotEntityCache
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.ProjectCraftListId
import com.bosch.pt.iot.smartsite.project.projectcraft.shared.model.ProjectCraftList
import com.bosch.pt.iot.smartsite.project.projectcraft.shared.repository.ProjectCraftListRepository
import org.springframework.stereotype.Component
import org.springframework.web.context.annotation.RequestScope

@Component
@RequestScope
open class ProjectCraftListSnapshotEntityCache(private val repository: ProjectCraftListRepository) :
    AbstractSnapshotEntityCache<ProjectCraftListId, ProjectCraftList>() {

  override fun loadOneFromDatabase(identifier: ProjectCraftListId) =
      repository.findOneByIdentifier(identifier)
}
