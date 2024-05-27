/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

@file:Suppress("MatchingDeclarationName")

package com.bosch.pt.iot.smartsite.project.project.command.api

import com.bosch.pt.iot.smartsite.project.project.ProjectAddressVo
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.shared.model.ProjectCategoryEnum
import java.time.LocalDate

@Suppress("UnnecessaryAbstractClass") abstract class ProjectCommand(open val identifier: ProjectId)

data class CreateProjectCommand(
    override val identifier: ProjectId,
    val client: String? = null,
    val description: String? = null,
    val start: LocalDate,
    val end: LocalDate,
    val projectNumber: String,
    val title: String,
    val category: ProjectCategoryEnum? = null,
    val address: ProjectAddressVo? = null
) : ProjectCommand(identifier)

data class UpdateProjectCommand(
    override val identifier: ProjectId,
    val version: Long,
    val client: String? = null,
    val description: String? = null,
    val start: LocalDate,
    val end: LocalDate,
    val projectNumber: String,
    val title: String,
    val category: ProjectCategoryEnum? = null,
    val address: ProjectAddressVo? = null
) : ProjectCommand(identifier)

data class DeleteProjectCommand(override val identifier: ProjectId) : ProjectCommand(identifier)
