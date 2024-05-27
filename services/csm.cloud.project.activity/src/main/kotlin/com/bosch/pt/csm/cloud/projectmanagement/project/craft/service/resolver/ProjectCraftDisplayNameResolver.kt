/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.craft.service.resolver

import com.bosch.pt.csm.cloud.projectmanagement.activity.model.UnresolvedObjectReference
import com.bosch.pt.csm.cloud.projectmanagement.common.service.resolver.DisplayNameResolver
import com.bosch.pt.csm.cloud.projectmanagement.project.craft.service.ProjectCraftService
import org.springframework.stereotype.Component

@Component
class ProjectCraftDisplayNameResolver(private val projectCraftService: ProjectCraftService) :
    DisplayNameResolver {

  override val type = "PROJECTCRAFT"

  override fun getDisplayName(objectReference: UnresolvedObjectReference): String =
      projectCraftService.findLatest(
              objectReference.identifier, objectReference.contextRootIdentifier)
          .name
}
