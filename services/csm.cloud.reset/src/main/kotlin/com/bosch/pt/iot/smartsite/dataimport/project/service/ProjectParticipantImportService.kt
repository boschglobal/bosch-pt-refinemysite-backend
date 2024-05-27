/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.project.service

import com.bosch.pt.iot.smartsite.dataimport.common.service.ImportService
import com.bosch.pt.iot.smartsite.dataimport.project.api.resource.request.AssignProjectParticipantResource
import com.bosch.pt.iot.smartsite.dataimport.project.model.ProjectParticipant
import com.bosch.pt.iot.smartsite.dataimport.project.rest.ProjectParticipantRestClient
import com.bosch.pt.iot.smartsite.dataimport.security.service.AuthenticationService
import com.bosch.pt.iot.smartsite.dataimport.util.IdRepository
import com.bosch.pt.iot.smartsite.dataimport.util.TypedId
import com.bosch.pt.iot.smartsite.importer.boundary.resource.ResourceTypeEnum
import org.springframework.stereotype.Service

@Service
class ProjectParticipantImportService(
    private val projectParticipantRestClient: ProjectParticipantRestClient,
    private val authenticationService: AuthenticationService,
    private val idRepository: IdRepository
) : ImportService<ProjectParticipant> {

  override fun importData(data: ProjectParticipant) {
    authenticationService.selectUser(data.createWithUserId!!)
    val projectId = idRepository[TypedId.typedId(ResourceTypeEnum.project, data.projectId)]!!

    idRepository.store(
        TypedId.typedId(ResourceTypeEnum.projectparticipant, data.id),
        call { projectParticipantRestClient.assign(projectId, map(data)) }!!.id)
  }

  private fun map(projectParticipant: ProjectParticipant): AssignProjectParticipantResource =
      AssignProjectParticipantResource(projectParticipant.email, projectParticipant.role)
}
