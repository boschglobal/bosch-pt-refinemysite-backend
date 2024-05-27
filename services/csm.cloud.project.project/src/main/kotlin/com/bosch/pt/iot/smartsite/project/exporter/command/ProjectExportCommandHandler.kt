/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.exporter.command

import com.bosch.pt.iot.smartsite.project.exporter.api.ProjectExportCommand
import com.bosch.pt.iot.smartsite.project.exporter.boundary.ProjectExportService
import com.bosch.pt.iot.smartsite.project.exporter.command.dto.ProjectExport
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.project.query.ProjectQueryService
import datadog.trace.api.Trace
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import org.slf4j.Logger
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component

@Component
open class ProjectExportCommandHandler(
    private val logger: Logger,
    private val projectExportService: ProjectExportService,
    private val projectQueryService: ProjectQueryService
) {

  @Trace
  @PreAuthorize(
      "@projectAuthorizationComponent.hasReadPermissionOnProject(#command.projectIdentifier)")
  open fun handle(command: ProjectExportCommand): ProjectExport {
    logger.debug("Exporting project as ${command.exportParameters.format} ...")

    // Load project
    val project = projectQueryService.findOneByIdentifier(command.projectIdentifier.asProjectId())!!

    // Export project to xml
    val xml = projectExportService.export(project, command.exportParameters)

    // Return xml to upload it to the download blob storage
    return ProjectExport(xml, getFileName(checkNotNull(project.title)))
  }

  private fun getFileName(title: String) =
      "${DateTimeFormatter.ISO_DATE.format(LocalDate.now())} " +
          "${title.replace(ILLEGAL_CHARACTERS_REGEX.toRegex(), "")}.xml"

  companion object {

    private val ILLEGAL_CHARACTERS_REGEX =
        listOf(
                "/",
                "\n",
                "\r",
                "\t",
                "\u0000",
                "\u000C",
                "`",
                "?",
                "*",
                "\\\\",
                "<",
                ">",
                "|",
                "\"",
                ":")
            .joinToString(separator = "", prefix = "[", postfix = "]")
  }
}
