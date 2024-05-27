/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.importer.facade.rest.resource.request

class ProjectImportAnalyzeResource(

    // Read working areas hierarchically or read them from a column.
    val readWorkAreasHierarchically: Boolean? = null,

    // Optional column to read project crafts from
    val craftColumn: ProjectImportAnalyzeCraftColumnResource? = null,

    // Optional column to read working areas from
    val workAreaColumn: ProjectImportAnalyzeWorkAreaColumnResource? = null
)
