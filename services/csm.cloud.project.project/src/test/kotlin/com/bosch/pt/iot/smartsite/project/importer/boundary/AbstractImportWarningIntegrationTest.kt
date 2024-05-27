/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.importer.boundary

import com.bosch.pt.csm.cloud.common.blob.model.Blob
import com.bosch.pt.csm.cloud.common.blob.model.BlobMetadata
import com.bosch.pt.iot.smartsite.common.i18n.Key
import com.bosch.pt.iot.smartsite.common.i18n.Key.IMPORT_VALIDATION_CRAFT_COLUMN_NAME_UNKNOWN
import com.bosch.pt.iot.smartsite.project.importer.boundary.dto.AnalysisColumn
import com.bosch.pt.iot.smartsite.project.importer.control.dto.ActivityCodeColumn
import com.bosch.pt.iot.smartsite.project.importer.control.dto.CustomFieldColumn
import com.bosch.pt.iot.smartsite.project.importer.control.dto.ImportColumnType.ACTIVITY_CODE
import com.bosch.pt.iot.smartsite.project.importer.control.dto.ImportColumnType.CUSTOM_FIELD
import com.bosch.pt.iot.smartsite.project.importer.control.dto.ImportColumnType.RESOURCE
import com.bosch.pt.iot.smartsite.project.importer.control.dto.ImportColumnType.TASK_FIELD
import com.bosch.pt.iot.smartsite.project.importer.control.dto.ImportColumnType.USER_DEFINED_FIELD
import com.bosch.pt.iot.smartsite.project.importer.control.dto.ResourcesColumn
import com.bosch.pt.iot.smartsite.project.importer.control.dto.TaskFieldColumn
import com.bosch.pt.iot.smartsite.project.importer.control.dto.UserDefinedFieldColumn
import com.bosch.pt.iot.smartsite.project.importer.model.ProjectImport
import com.bosch.pt.iot.smartsite.project.importer.model.ProjectImportStatus
import com.bosch.pt.iot.smartsite.project.importer.repository.ImportBlobStorageRepository
import com.bosch.pt.iot.smartsite.project.importer.repository.ProjectImportRepository
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.ninjasquad.springmockk.MockkBean
import io.mockk.clearMocks
import io.mockk.every
import java.time.LocalDateTime
import org.junit.jupiter.api.AfterEach
import org.springframework.core.io.Resource

@Suppress("UnnecessaryAbstractClass")
abstract class AbstractImportWarningIntegrationTest : AbstractImportIntegrationTest() {

  @MockkBean private lateinit var blobStorageRepository: ImportBlobStorageRepository

  @MockkBean private lateinit var projectImportRepository: ProjectImportRepository

  @AfterEach
  fun reset() {
    clearMocks(blobStorageRepository, projectImportRepository)
  }

  fun analysisColumn(name: String, file: Resource, discipline: Boolean): AnalysisColumn {
    val projectFile = readProject(file)
    val internalName = name.replace(" ", "_")
    val columnFromFile =
        projectReader.readColumns(projectFile).firstOrNull {
          it.name == name || it.name == internalName
        }

    val type =
        when (columnFromFile) {
          is UserDefinedFieldColumn -> USER_DEFINED_FIELD
          is ResourcesColumn -> RESOURCE
          is TaskFieldColumn -> TASK_FIELD
          is CustomFieldColumn -> CUSTOM_FIELD
          is ActivityCodeColumn -> ACTIVITY_CODE
          else -> error("Unsupported column type detected")
        }

    val actualName =
        when (type) {
          ACTIVITY_CODE,
          RESOURCE -> columnFromFile.name
          else -> columnFromFile.fieldType?.name()
        }

    val errorMessage =
        if (discipline) IMPORT_VALIDATION_CRAFT_COLUMN_NAME_UNKNOWN
        else Key.IMPORT_VALIDATION_WORK_AREA_COLUMN_NAME_UNKNOWN

    return when (projectFile.projectProperties.fileType) {
      SupportedFileTypes.MSPDI.name -> AnalysisColumn(actualName, type, errorMessage)
      SupportedFileTypes.MPP.name -> AnalysisColumn(actualName, type, errorMessage)
      SupportedFileTypes.PP.name,
      SupportedFileTypes.PMXML.name,
      SupportedFileTypes.XER.name -> {
        AnalysisColumn(actualName, USER_DEFINED_FIELD, errorMessage)
      }
      else -> error("Unsupported file type")
    }
  }

  fun mockBlobAndProjectImport(projectIdentifier: ProjectId, file: Resource) {
    val blob =
        Blob(
            "blob1",
            file.inputStream.readAllBytes(),
            BlobMetadata.fromMap(emptyMap()),
            "application/msproject")

    val projectImport =
        ProjectImport(
                projectIdentifier, blob.blobName, ProjectImportStatus.PLANNING, LocalDateTime.now())
            .apply { this.version = 0 }

    every { projectImportRepository.findOneByProjectIdentifier(projectIdentifier) } returns
        projectImport
    every { blobStorageRepository.find(blob.blobName) } returns blob
    every { projectImportRepository.saveAndFlush(projectImport) } returns projectImport
  }
}
