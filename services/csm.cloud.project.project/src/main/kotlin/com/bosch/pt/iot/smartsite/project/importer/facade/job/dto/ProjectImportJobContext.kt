/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.importer.facade.job.dto

import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReference

data class ProjectImportJobContext(val project: ResourceReference, val fileName: String)
