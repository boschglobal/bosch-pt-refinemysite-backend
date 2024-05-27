/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.importer.command

import com.bosch.pt.iot.smartsite.project.importer.api.ProjectImportCommand
import com.bosch.pt.iot.smartsite.project.importer.boundary.ProjectImportService
import com.bosch.pt.iot.smartsite.project.importer.model.ProjectImport
import com.bosch.pt.iot.smartsite.project.importer.model.ProjectImportStatus.DONE
import com.bosch.pt.iot.smartsite.project.importer.model.ProjectImportStatus.FAILED
import com.bosch.pt.iot.smartsite.project.importer.repository.ImportBlobStorageRepository
import com.bosch.pt.iot.smartsite.project.project.query.ProjectQueryService
import datadog.trace.api.Trace
import java.io.ByteArrayInputStream
import org.slf4j.Logger
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component

@Component
open class ProjectImportCommandHandler(
    private val logger: Logger,
    private val blobStorageRepository: ImportBlobStorageRepository,
    private val projectQueryService: ProjectQueryService,
    private val projectImportService: ProjectImportService,
) {

  @Trace
  @PreAuthorize(
      "@projectAuthorizationComponent.hasReadPermissionOnProject(#command.projectIdentifier)")
  open fun handle(command: ProjectImportCommand) {
    logger.debug("Import project ...")

    var projectImport: ProjectImport? = null
    try {
      LocaleContextHolder.setLocale(command.locale)

      // Load project, import object and blob
      val project = projectQueryService.findOneByIdentifier(command.projectIdentifier)!!
      projectImport = requireNotNull(projectImportService.findImportObject(project.identifier))
      val blob = requireNotNull(blobStorageRepository.find(projectImport.blobName))

      ProjectImportParameterLogger.log(
          blob.blobName,
          projectImport.readWorkingAreasHierarchically,
          projectImport.craftColumn,
          projectImport.workAreaColumn)

      // Read / parse the file
      val projectFile = projectImportService.readProjectFile(ByteArrayInputStream(blob.data))

      // Import
      projectImportService.import(
          project,
          projectFile,
          projectImport.readWorkingAreasHierarchically ?: false,
          projectImport.craftColumn,
          projectImport.craftColumnFieldType,
          projectImport.workAreaColumn,
          projectImport.workAreaColumnFieldType)

      // Update the import status
      projectImport.status = DONE
      projectImportService.save(projectImport)
    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
      // Update the import object if the import failed
      projectImport?.let {
        it.status = FAILED
        projectImportService.save(it)
      }
      throw e
    }
  }
}
