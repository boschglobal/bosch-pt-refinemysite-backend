/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.project.service

import com.bosch.pt.iot.smartsite.dataimport.common.service.DateBasedImportService
import com.bosch.pt.iot.smartsite.dataimport.project.api.resource.request.CreateProjectResource
import com.bosch.pt.iot.smartsite.dataimport.project.api.resource.request.SearchParticipantResource
import com.bosch.pt.iot.smartsite.dataimport.project.model.ParticipantRoleEnum
import com.bosch.pt.iot.smartsite.dataimport.project.model.Project
import com.bosch.pt.iot.smartsite.dataimport.project.rest.ProjectParticipantRestClient
import com.bosch.pt.iot.smartsite.dataimport.project.rest.ProjectRestClient
import com.bosch.pt.iot.smartsite.dataimport.security.service.AuthenticationService
import com.bosch.pt.iot.smartsite.dataimport.util.IdRepository
import com.bosch.pt.iot.smartsite.dataimport.util.TypedId
import com.bosch.pt.iot.smartsite.importer.boundary.resource.ResourceTypeEnum
import java.time.LocalDate
import org.springframework.stereotype.Service

@Service
class ProjectImportService(
    private val projectRestClient: ProjectRestClient,
    private val participantRestClient: ProjectParticipantRestClient,
    private val authenticationService: AuthenticationService,
    private val idRepository: IdRepository
) : DateBasedImportService<Project> {

  override fun importData(data: Project) = importData(data, LocalDate.now())

  override fun importData(data: Project, rootDate: LocalDate) {
    authenticationService.selectUser(data.createWithUserId!!)
    val createdProject = call { projectRestClient.create(map(data, rootDate)) }!!
    idRepository.store(TypedId.typedId(ResourceTypeEnum.project, data.id), createdProject.id)

    // Add the CSM's participant to the idRepository to be able to use it later as an assignee
    // This is required when importing the "bosch" sample data set
    val csms =
        call {
          participantRestClient.findParticipants(
              createdProject.id, SearchParticipantResource(roles = setOf(ParticipantRoleEnum.CSM)))
        }!!
    val participantId = "${data.id}-${data.createWithUserId}"
    idRepository.store(
        TypedId.typedId(ResourceTypeEnum.projectparticipant, participantId), csms.items.first().id)
  }

  private fun map(project: Project, rootDate: LocalDate): CreateProjectResource =
      CreateProjectResource(
          project.client,
          project.description,
          rootDate.plusDays(project.end.toLong()),
          rootDate.plusDays(project.start.toLong()),
          project.projectNumber,
          project.title,
          project.category,
          project.address)
}
