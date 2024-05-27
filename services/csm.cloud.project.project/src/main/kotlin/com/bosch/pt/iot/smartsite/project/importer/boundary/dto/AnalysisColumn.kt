/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.importer.boundary.dto

import com.bosch.pt.iot.smartsite.project.importer.control.dto.ImportColumnType

class AnalysisColumn(val name: String?, val columnType: ImportColumnType, val errorMessageKey: String)
