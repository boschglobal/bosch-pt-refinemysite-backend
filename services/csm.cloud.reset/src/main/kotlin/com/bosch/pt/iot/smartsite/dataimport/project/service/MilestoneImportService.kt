/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.project.service

import com.bosch.pt.iot.smartsite.dataimport.common.service.DateBasedImportService
import com.bosch.pt.iot.smartsite.dataimport.project.api.resource.request.CreateMilestoneResource
import com.bosch.pt.iot.smartsite.dataimport.project.model.Milestone
import com.bosch.pt.iot.smartsite.dataimport.project.rest.MilestoneRestClient
import com.bosch.pt.iot.smartsite.dataimport.security.service.AuthenticationService
import com.bosch.pt.iot.smartsite.dataimport.util.IdRepository
import com.bosch.pt.iot.smartsite.dataimport.util.TypedId
import com.bosch.pt.iot.smartsite.importer.boundary.resource.ResourceTypeEnum
import java.time.LocalDate
import org.springframework.stereotype.Service

@Service
class MilestoneImportService(
    private val milestoneRestClient: MilestoneRestClient,
    private val authenticationService: AuthenticationService,
    private val idRepository: IdRepository
) : DateBasedImportService<Milestone> {

  override fun importData(data: Milestone) {
    importData(data, LocalDate.now())
  }

  override fun importData(data: Milestone, rootDate: LocalDate) {
    authenticationService.selectUser(data.createWithUserId)
    idRepository.store(
        TypedId.typedId(ResourceTypeEnum.milestone, data.id),
        call { milestoneRestClient.create(map(data, rootDate)) }!!.id)
  }

  private fun map(milestone: Milestone, rootDate: LocalDate): CreateMilestoneResource =
      CreateMilestoneResource(
          milestone.name,
          milestone.type,
          rootDate.plusDays(milestone.date.toLong()),
          milestone.header,
          idRepository[TypedId.typedId(ResourceTypeEnum.project, milestone.projectId)],
          milestone.description,
          milestone.craftId?.let {
            idRepository[TypedId.typedId(ResourceTypeEnum.projectcraft, it)]
          },
          milestone.workAreaId?.let {
            idRepository[TypedId.typedId(ResourceTypeEnum.workarea, it)]
          })
}
