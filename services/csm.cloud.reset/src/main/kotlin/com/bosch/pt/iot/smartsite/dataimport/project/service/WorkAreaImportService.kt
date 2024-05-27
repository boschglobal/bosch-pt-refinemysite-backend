/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.project.service

import com.bosch.pt.iot.smartsite.dataimport.common.service.ImportService
import com.bosch.pt.iot.smartsite.dataimport.project.api.resource.request.CreateWorkAreaResource
import com.bosch.pt.iot.smartsite.dataimport.project.model.WorkArea
import com.bosch.pt.iot.smartsite.dataimport.project.rest.WorkAreaRestClient
import com.bosch.pt.iot.smartsite.dataimport.security.service.AuthenticationService
import com.bosch.pt.iot.smartsite.dataimport.util.IdRepository
import com.bosch.pt.iot.smartsite.dataimport.util.TypedId
import com.bosch.pt.iot.smartsite.importer.boundary.resource.ResourceTypeEnum
import org.springframework.stereotype.Service

@Service
class WorkAreaImportService(
    private val workAreaRestClient: WorkAreaRestClient,
    private val authenticationService: AuthenticationService,
    private val idRepository: IdRepository
) : ImportService<WorkArea> {

  override fun importData(data: WorkArea) {
    authenticationService.selectUser(data.createWithUserId)
    val createdWorkArea = call { workAreaRestClient.create(data.etag, map(data)) }!!
    idRepository.store(
        TypedId.typedId(ResourceTypeEnum.workarea, data.id),
        createdWorkArea.workAreas
            .last {
              if (!it.name.equals(data.name)) {
                return@last false
              }
              (it.position == null && data.position == null ||
                  it.position != null && it.position == data.position)
            }
            .id)
  }

  private fun map(workArea: WorkArea): CreateWorkAreaResource =
      CreateWorkAreaResource(
          idRepository[TypedId.typedId(ResourceTypeEnum.project, workArea.projectId)],
          workArea.name,
          workArea.position)
}
