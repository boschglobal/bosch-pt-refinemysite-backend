/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.importer.facade.rest.resource.response

import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.AbstractResource
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.fasterxml.jackson.annotation.JsonProperty

class ProjectImportUploadResource(
    @JsonProperty("id") val identifier: ProjectId,
    val version: Long,
    val columns: List<ProjectImportColumnResource>
) : AbstractResource()
