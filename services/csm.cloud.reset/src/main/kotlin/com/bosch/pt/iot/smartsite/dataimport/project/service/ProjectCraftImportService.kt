/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.project.service

import com.bosch.pt.iot.smartsite.dataimport.common.service.ImportService
import com.bosch.pt.iot.smartsite.dataimport.project.api.resource.request.CreateProjectCraftResource
import com.bosch.pt.iot.smartsite.dataimport.project.model.ProjectCraft
import com.bosch.pt.iot.smartsite.dataimport.project.rest.ProjectCraftRestClient
import com.bosch.pt.iot.smartsite.dataimport.security.service.AuthenticationService
import com.bosch.pt.iot.smartsite.dataimport.util.IdRepository
import com.bosch.pt.iot.smartsite.dataimport.util.TypedId
import com.bosch.pt.iot.smartsite.importer.boundary.resource.ResourceTypeEnum
import org.springframework.stereotype.Service

@Service
class ProjectCraftImportService(
    private val projectCraftRestClient: ProjectCraftRestClient,
    private val authenticationService: AuthenticationService,
    private val idRepository: IdRepository
) : ImportService<ProjectCraft> {

  override fun importData(data: ProjectCraft) {
    authenticationService.selectUser(data.createWithUserId!!)

    val etag = data.etag
    val projectId = idRepository[TypedId.typedId(ResourceTypeEnum.project, data.projectId)]!!
    val resource = map(data)

    val response = call { projectCraftRestClient.create(etag, projectId, resource) }!!

    idRepository.store(
        TypedId.typedId(ResourceTypeEnum.projectcraft, data.id), response.projectCrafts.last().id)
  }

  private fun map(projectCraft: ProjectCraft): CreateProjectCraftResource =
      CreateProjectCraftResource(projectCraft.name, projectCraft.color)
}
