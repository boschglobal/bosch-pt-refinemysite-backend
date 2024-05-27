/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.task.facade.rest.resource

import com.bosch.pt.iot.smartsite.project.participant.ParticipantId
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.ProjectCraftId
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.request.SaveTaskResource
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaId

/** Builder for [SaveTaskResource]. */
class SaveTaskResourceBuilder {

  private var projectId: ProjectId? = null
  private var assigneeId: ParticipantId? = null
  private var name: String? = null
  private var description: String? = null
  private var location: String? = null
  private var projectCraftId: ProjectCraftId? = null
  private var workAreaId: WorkAreaId? = null
  private var status: TaskStatusEnum? = null

  /**
   * Set Project ID.
   *
   * @param projectId the project id
   * @return [SaveTaskResourceBuilder]
   */
  fun setProjectId(projectId: ProjectId?): SaveTaskResourceBuilder {
    this.projectId = projectId
    return this
  }

  /**
   * Set Assignee ID.
   *
   * @param assigneeId the assignee id
   * @return [SaveTaskResourceBuilder]
   */
  fun setAssigneeId(assigneeId: ParticipantId?): SaveTaskResourceBuilder {
    this.assigneeId = assigneeId
    return this
  }

  /**
   * Set Name.
   *
   * @param name the name
   * @return [SaveTaskResourceBuilder]
   */
  fun setName(name: String?): SaveTaskResourceBuilder {
    this.name = name
    return this
  }

  /**
   * Set description.
   *
   * @param description the description
   * @return [SaveTaskResourceBuilder]
   */
  fun setDescription(description: String?): SaveTaskResourceBuilder {
    this.description = description
    return this
  }

  /**
   * Set location.
   *
   * @param location the location
   * @return [SaveTaskResourceBuilder]
   */
  fun setLocation(location: String?): SaveTaskResourceBuilder {
    this.location = location
    return this
  }

  /**
   * Set project projectcraft id.
   *
   * @param projectCraftId the project projectcraft id
   * @return [SaveTaskResourceBuilder]
   */
  fun setProjectCraftId(projectCraftId: ProjectCraftId?): SaveTaskResourceBuilder {
    this.projectCraftId = projectCraftId
    return this
  }

  /**
   * Set work area id.
   *
   * @param workAreaId the work area id
   * @return [SaveTaskResourceBuilder]
   */
  fun setWorkAreaId(workAreaId: WorkAreaId?): SaveTaskResourceBuilder {
    this.workAreaId = workAreaId
    return this
  }

  /**
   * Set status.
   *
   * @param status the status
   * @return [SaveTaskResourceBuilder]
   */
  fun setStatus(status: TaskStatusEnum?): SaveTaskResourceBuilder {
    this.status = status
    return this
  }

  /**
   * Method the build the actual instance.
   *
   * @return [SaveTaskResource]
   */
  fun createSaveTaskResource(): SaveTaskResource =
      SaveTaskResource(
          projectId!!,
          projectCraftId!!,
          name!!,
          description,
          location,
          status!!,
          assigneeId,
          workAreaId)
}
