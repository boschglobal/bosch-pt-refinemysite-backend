/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.importer.facade.rest.resource.response

import com.bosch.pt.iot.smartsite.project.importer.control.dto.ImportColumnType

class ProjectImportColumnResource(
    val name: String,
    val columnType: ImportColumnType,
    val fieldType: String?
)
