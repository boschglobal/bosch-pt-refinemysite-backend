/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.importer.boundary

import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.common.facade.rest.etag.ETag
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.featuretogglemanagement.common.SubjectTypeEnum.COMPANY
import com.bosch.pt.csm.cloud.featuretogglemanagement.feature.event.addSubjectToWhitelistOfFeature
import com.bosch.pt.csm.cloud.featuretogglemanagement.feature.event.createFeature
import com.bosch.pt.iot.smartsite.common.authorization.AbstractAuthorizationIntegrationTestV2
import com.bosch.pt.iot.smartsite.common.facade.rest.UserTypeAccess
import com.bosch.pt.iot.smartsite.common.i18n.Key.IMPORT_IMPOSSIBLE_UNSUPPORTED_FILE_TYPE
import com.bosch.pt.iot.smartsite.featuretoggle.query.FeatureEnum.PROJECT_IMPORT
import com.bosch.pt.iot.smartsite.project.importer.model.ProjectImport
import com.bosch.pt.iot.smartsite.project.importer.model.ProjectImportStatus.PLANNING
import com.bosch.pt.iot.smartsite.project.importer.repository.ImportBlobStorageRepository
import com.bosch.pt.iot.smartsite.project.importer.repository.MalwareScanResult.SAFE
import com.bosch.pt.iot.smartsite.project.importer.repository.ProjectImportRepository
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.project.shared.repository.ProjectRepository
import io.mockk.every
import java.io.ByteArrayInputStream
import java.time.LocalDateTime
import net.sf.mpxj.ProjectFile
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.AccessDeniedException

class ProjectImportServiceAuthorizationIntegrationTest : AbstractAuthorizationIntegrationTestV2() {

  @Autowired private lateinit var cut: ProjectImportService

  @Autowired private lateinit var projectRepository: ProjectRepository

  @Autowired private lateinit var projectImportRepository: ProjectImportRepository

  @Autowired private lateinit var blobStorageRepository: ImportBlobStorageRepository

  private val content = "fakeMppFile".toByteArray()

  private val fileName = "test.mpp"

  @BeforeEach
  fun init() {
    eventStreamGenerator
        .createFeature(asReference = "projectImport") { it.featureName = PROJECT_IMPORT.name }
        .addSubjectToWhitelistOfFeature(asReference = "projectImport") {
          it.type = COMPANY.name
          it.subjectRef = getIdentifier("company").toString()
          it.featureName = PROJECT_IMPORT.name
        }
        .addSubjectToWhitelistOfFeature(asReference = "projectImport") {
          it.type = COMPANY.name
          it.subjectRef = getIdentifier("otherCompany").toString()
          it.featureName = PROJECT_IMPORT.name
        }
  }

  @ParameterizedTest
  @MethodSource("csmWithAccess")
  fun `verify upload authorized`(userType: UserTypeAccess) {
    every { blobStorageRepository.getMalwareScanResultBlocking(any()) } returns SAFE
    every { blobStorageRepository.read(any()) } returns ByteArrayInputStream(content)

    // Try to import to project with existing data (which is cheaper than creating a new project).
    checkAccessWith(userType) {
      // Try to import to project with existing data which fails, but it is cheaper than creating a
      // new valid project. Ignores functional errors. Authorization errors are handled by the
      // checkAccessWith method.
      try {
        cut.upload(
            getIdentifier("project").asProjectId(), content, fileName, "application/msproject")
      } catch (e: PreconditionViolationException) {
        assertThat(e.messageKey).isEqualTo(IMPORT_IMPOSSIBLE_UNSUPPORTED_FILE_TYPE)
      }
    }
  }

  @ParameterizedTest
  @MethodSource("csmWithAccess")
  fun `verify analyze authorized`(userType: UserTypeAccess) {
    createProjectImport()

    checkAccessWith(userType) {
      // Try to import to project with existing data which fails, but it is cheaper than creating a
      // new valid project. Ignores functional errors. Authorization errors are handled correctly by
      // the checkAccessWith method.
      try {
        cut.analyze(getIdentifier("project").asProjectId(), false, null, null, ETag.from("0"))
      } catch (e: PreconditionViolationException) {
        assertThat(e.messageKey).isEqualTo(IMPORT_IMPOSSIBLE_UNSUPPORTED_FILE_TYPE)
      }
    }
  }

  @ParameterizedTest
  @MethodSource("csmWithAccess")
  fun `verify import authorized`(userType: UserTypeAccess) {
    createProjectImport()
    val project = projectRepository.findOneByIdentifier(getIdentifier("project").asProjectId())!!

    checkAccessWith(userType) {
      try {
        // Try to import to project with existing data which fails, but it is cheaper than creating
        // a new valid project. Ignores functional errors. Authorization errors are handled
        // correctly by the checkAccessWith method.
        cut.import(project, ProjectFile(), false, null, null, null, null)
      } catch (e: NullPointerException) {
        assertThat(e.message).isEqualTo("projectFile must not be null")
      }
    }
  }

  @ParameterizedTest
  @MethodSource("csmWithAccess")
  fun `verify is import possible only for CSMs`(userType: UserTypeAccess) {
    val project = projectRepository.findOneByIdentifier(getIdentifier("project").asProjectId())!!

    checkAccessWith(userType) {
      // Check expects that an exception is thrown if access is denied
      if (!cut.isImportPossible(project)) throw AccessDeniedException("Access not possible")
    }
  }

  private fun createProjectImport() {
    projectImportRepository.save(
        ProjectImport(getIdentifier("project").asProjectId(), "abc", PLANNING, LocalDateTime.now()))
  }
}
