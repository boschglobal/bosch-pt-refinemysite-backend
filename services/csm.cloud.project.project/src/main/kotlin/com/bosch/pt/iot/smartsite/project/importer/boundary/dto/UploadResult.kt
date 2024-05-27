/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.importer.boundary.dto

import com.bosch.pt.iot.smartsite.project.importer.control.dto.ImportColumn

class UploadResult(val columns: List<ImportColumn>, val version: Long)
