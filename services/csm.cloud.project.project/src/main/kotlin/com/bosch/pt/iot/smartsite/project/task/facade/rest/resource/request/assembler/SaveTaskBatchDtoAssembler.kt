/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.request.assembler

import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.task.domain.asTaskId
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.request.CreateTaskBatchResource
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.request.SaveTaskResourceWithIdentifierAndVersion
import com.bosch.pt.iot.smartsite.project.task.shared.dto.SaveTaskBatchDto

object SaveTaskBatchDtoAssembler {
  fun assembleOfCreate(
      createTaskBatchResources: Collection<CreateTaskBatchResource>
  ): Collection<SaveTaskBatchDto> = createTaskBatchResources.map { assemble(it) }

  fun assembleOfUpdate(
      saveTaskResourceWithIdentifierAndVersions:
          Collection<SaveTaskResourceWithIdentifierAndVersion>
  ): Collection<SaveTaskBatchDto> = saveTaskResourceWithIdentifierAndVersions.map { assemble(it) }

  private fun assemble(createTaskBatchResource: CreateTaskBatchResource): SaveTaskBatchDto =
      SaveTaskBatchDto(
          createTaskBatchResource.id?.asTaskId() ?: TaskId(),
          null,
          createTaskBatchResource.name,
          createTaskBatchResource.description,
          createTaskBatchResource.location,
          createTaskBatchResource.status,
          createTaskBatchResource.projectCraftId,
          createTaskBatchResource.assigneeId,
          createTaskBatchResource.workAreaId)

  private fun assemble(
      saveTaskResourceWithIdentifierAndVersion: SaveTaskResourceWithIdentifierAndVersion
  ): SaveTaskBatchDto =
      SaveTaskBatchDto(
          saveTaskResourceWithIdentifierAndVersion.id.asTaskId(),
          saveTaskResourceWithIdentifierAndVersion.version,
          saveTaskResourceWithIdentifierAndVersion.name,
          saveTaskResourceWithIdentifierAndVersion.description,
          saveTaskResourceWithIdentifierAndVersion.location,
          saveTaskResourceWithIdentifierAndVersion.status,
          saveTaskResourceWithIdentifierAndVersion.projectCraftId,
          saveTaskResourceWithIdentifierAndVersion.assigneeId,
          saveTaskResourceWithIdentifierAndVersion.workAreaId)
}
