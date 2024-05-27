/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
@file:Suppress("Filename", "MatchingDeclarationName")

package com.bosch.pt.iot.smartsite.project.importer.api

import com.bosch.pt.iot.smartsite.project.project.ProjectId
import java.util.Locale

data class ProjectImportCommand(
    val locale: Locale,
    val projectIdentifier: ProjectId,
)
