/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.project.facade.rest.resource.request

import com.bosch.pt.csm.cloud.common.facade.rest.etag.ETag
import com.bosch.pt.csm.cloud.common.facade.rest.resource.validation.StringEnumeration
import com.bosch.pt.iot.smartsite.project.project.command.api.CreateProjectCommand
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.command.api.UpdateProjectCommand
import com.bosch.pt.iot.smartsite.project.project.facade.rest.resource.response.ProjectAddressDto
import com.bosch.pt.iot.smartsite.project.project.shared.model.Project.Companion.MAX_PROJECT_CLIENT_LENGTH
import com.bosch.pt.iot.smartsite.project.project.shared.model.Project.Companion.MAX_PROJECT_DESCRIPTION_LENGTH
import com.bosch.pt.iot.smartsite.project.project.shared.model.Project.Companion.MAX_PROJECT_NUMBER_LENGTH
import com.bosch.pt.iot.smartsite.project.project.shared.model.Project.Companion.MAX_PROJECT_TITLE_LENGTH
import com.bosch.pt.iot.smartsite.project.project.shared.model.ProjectCategoryEnum
import com.bosch.pt.iot.smartsite.project.project.shared.model.ProjectCategoryEnum.Companion.ENUM_VALUES
import java.time.LocalDate
import jakarta.validation.Valid
import jakarta.validation.constraints.Size

class SaveProjectResource(

    /** The client of the project. */
    @field:Size(max = MAX_PROJECT_CLIENT_LENGTH) var client: String? = null,

    /** The description of the project. */
    @field:Size(max = MAX_PROJECT_DESCRIPTION_LENGTH) var description: String? = null,

    /** Start date of the project. */
    var start: LocalDate,

    /** End date of the project. */
    var end: LocalDate,

    /** The project number. */
    @field:Size(min = 1, max = MAX_PROJECT_NUMBER_LENGTH) var projectNumber: String,

    /** The title of the project. */
    @field:Size(min = 1, max = MAX_PROJECT_TITLE_LENGTH) var title: String,

    /** Project category. */
    @field:StringEnumeration(enumClass = ProjectCategoryEnum::class, enumValues = ENUM_VALUES)
    var category: ProjectCategoryEnum? = null,

    /** The [ProjectAddressDto]s of this [SaveProjectResource]. */
    @field:Valid var address: ProjectAddressDto
) {
  fun toCommand(identifier: ProjectId?) =
      CreateProjectCommand(
          identifier = identifier ?: ProjectId(),
          client = client,
          description = description,
          start = start,
          end = end,
          projectNumber = projectNumber,
          title = title,
          category = category,
          address = address.toValueObject())

  fun toCommand(identifier: ProjectId, eTag: ETag) =
      UpdateProjectCommand(
          identifier = identifier,
          version = eTag.toVersion(),
          client = client,
          description = description,
          start = start,
          end = end,
          projectNumber = projectNumber,
          title = title,
          category = category,
          address = address.toValueObject())
}
