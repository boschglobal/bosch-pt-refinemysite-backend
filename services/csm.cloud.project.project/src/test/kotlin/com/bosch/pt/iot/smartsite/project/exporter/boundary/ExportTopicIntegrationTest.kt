/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.exporter.boundary

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTopicG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkArea
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkAreaList
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.project.exporter.api.ProjectExportFormatEnum
import com.bosch.pt.iot.smartsite.project.exporter.api.ProjectExportParameters
import com.bosch.pt.iot.smartsite.project.exporter.boundary.ProjectExportService.Companion.FIELD_ALIAS_CRAFT
import com.bosch.pt.iot.smartsite.project.importer.model.ImportModel
import com.bosch.pt.iot.smartsite.project.importer.submitProjectImportFeatureToggle
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired

@EnableAllKafkaListeners
class ExportTopicIntegrationTest : AbstractExportIntegrationTest() {

  @Autowired private lateinit var projectExportService: ProjectExportService

  private val projectIdentifier by lazy { getIdentifier("p2").asProjectId() }
  private val project by lazy { checkNotNull(repositories.findProject(projectIdentifier)) }

  @BeforeEach
  fun init() {
    eventStreamGenerator
        .setupDatasetTestData()
        .submitProjectImportFeatureToggle()
        .submitProject("p2")
        .submitProjectCraftG2("pc1") { it.name = "pc1" }
        .submitParticipantG3("p2Csm1") {
          it.user = getByReference("userCsm1")
          it.role = ParticipantRoleEnumAvro.CSM
        }
        .submitWorkArea(asReference = "w1") { it.name = "w1" }
        .submitWorkAreaList(asReference = "wal") {
          it.workAreas = listOf(getByReference("w1"))
          it.project = getByReference("p2")
        }

    setAuthentication("userCsm1")

    projectEventStoreUtils.reset()
  }

  @ParameterizedTest
  @ValueSource(strings = ["MS_PROJECT_XML", "PRIMAVERA_P6_XML"])
  fun exportTaskDescriptionWithTopics(format: String) {
    eventStreamGenerator
        .submitTask(asReference = "t1") {
          it.project = getByReference("p2")
          it.workarea = getByReference("w1")
          it.craft = getByReference("pc1")
          it.name = "t1"
          it.description = "description"
        }
        .submitTopicG2("to1") {
          it.task = getByReference("t1")
          it.description = "t1"
        }
        .submitTopicG2("to2") {
          it.task = getByReference("t1")
          it.description = "t2"
        }

    val exportedFile =
        projectExportService.export(
            project,
            ProjectExportParameters(
                format = ProjectExportFormatEnum.valueOf(format),
                includeMilestones = true,
                includeComments = true))

    val importModel = readExportedFile(exportedFile, project, FIELD_ALIAS_CRAFT)
    verifyExportTaskDescriptionWithTopicsSharedValues(importModel)
  }

  @ParameterizedTest
  @ValueSource(strings = ["MS_PROJECT_XML", "PRIMAVERA_P6_XML"])
  fun reExportTaskDescriptionWithTopics(format: String) {

    // First export
    exportTaskDescriptionWithTopics(format)

    // Don't change anything, just re-export and verify IDs
    val exportedFile =
        projectExportService.export(
            project,
            ProjectExportParameters(
                format = ProjectExportFormatEnum.valueOf(format),
                includeMilestones = true,
                includeComments = true))

    val importModel = readExportedFile(exportedFile, project, FIELD_ALIAS_CRAFT)
    verifyExportTaskDescriptionWithTopicsSharedValues(importModel)
  }

  private fun verifyExportTaskDescriptionWithTopicsSharedValues(importModel: ImportModel) {
    assertThat(importModel.workAreas).hasSize(1)
    val w1 = importModel.workAreas[0]
    assertThat(w1.name).isEqualTo("w1")
    validateExportedIds(w1, "w1", 1, 1)

    assertThat(importModel.crafts).hasSize(1)
    val craft1 = importModel.crafts.single { it.name == "pc1" }
    assertThat(craft1).isNotNull
    validateExportedIds(craft1, "t1", 2, 2)

    assertThat(importModel.tasks).hasSize(1)
    val t1 = importModel.tasks.single { it.name == "t1" }
    assertThat(t1.workAreaId).isEqualTo(w1.id)
    assertThat(t1.craftId).isEqualTo(craft1.id)
    assertThat(t1.notes).isEqualTo("description\n\nt1\n\nt2")
    validateExportedIds(t1, "t1", 2, 2)
  }

  @ParameterizedTest
  @ValueSource(strings = ["MS_PROJECT_XML", "PRIMAVERA_P6_XML"])
  fun exportTopics(format: String) {
    eventStreamGenerator
        .submitTask(asReference = "t1") {
          it.project = getByReference("p2")
          it.workarea = getByReference("w1")
          it.craft = getByReference("pc1")
          it.name = "t1"
          it.description = null
        }
        .submitTopicG2("to1") {
          it.task = getByReference("t1")
          it.description = "t1"
        }
        .submitTopicG2("to2") {
          it.task = getByReference("t1")
          it.description = "t2"
        }

    val exportedFile =
        projectExportService.export(
            project,
            ProjectExportParameters(
                format = ProjectExportFormatEnum.valueOf(format),
                includeMilestones = true,
                includeComments = true))

    val importModel = readExportedFile(exportedFile, project, FIELD_ALIAS_CRAFT)
    verifyExportTopicsSharedValues(importModel)
  }

  @ParameterizedTest
  @ValueSource(strings = ["MS_PROJECT_XML", "PRIMAVERA_P6_XML"])
  fun reExportTopics(format: String) {

    // First export
    exportTopics(format)

    // Don't change anything, just re-export and verify IDs
    val exportedFile =
        projectExportService.export(
            project,
            ProjectExportParameters(
                format = ProjectExportFormatEnum.valueOf(format),
                includeMilestones = true,
                includeComments = true))

    val importModel = readExportedFile(exportedFile, project, FIELD_ALIAS_CRAFT)
    verifyExportTopicsSharedValues(importModel)
  }

  private fun verifyExportTopicsSharedValues(importModel: ImportModel) {
    assertThat(importModel.workAreas).hasSize(1)
    val w1 = importModel.workAreas[0]
    assertThat(w1.name).isEqualTo("w1")
    validateExportedIds(w1, "w1", 1, 1)

    assertThat(importModel.crafts).hasSize(1)
    val craft1 = importModel.crafts.single { it.name == "pc1" }
    assertThat(craft1).isNotNull
    validateExportedIds(craft1, "t1", 2, 2)

    assertThat(importModel.tasks).hasSize(1)
    val t1 = importModel.tasks.single { it.name == "t1" }
    assertThat(t1.workAreaId).isEqualTo(w1.id)
    assertThat(t1.craftId).isEqualTo(craft1.id)
    assertThat(t1.notes).isEqualTo("t1\n\nt2")
    validateExportedIds(t1, "t1", 2, 2)
  }

  @ParameterizedTest
  @ValueSource(strings = ["MS_PROJECT_XML", "PRIMAVERA_P6_XML"])
  fun exportNoTopics(format: String) {
    eventStreamGenerator.submitTask(asReference = "t1") {
      it.project = getByReference("p2")
      it.workarea = getByReference("w1")
      it.craft = getByReference("pc1")
      it.name = "t1"
      it.description = null
    }

    val exportedFile =
        projectExportService.export(
            project,
            ProjectExportParameters(
                format = ProjectExportFormatEnum.valueOf(format),
                includeMilestones = true,
                includeComments = true))

    val importModel = readExportedFile(exportedFile, project, FIELD_ALIAS_CRAFT)
    verifyExportNoTopicsSharedValues(importModel)
  }

  @ParameterizedTest
  @ValueSource(strings = ["MS_PROJECT_XML", "PRIMAVERA_P6_XML"])
  fun reExportNoTopics(format: String) {

    // First export
    exportNoTopics(format)

    // Don't change anything, just re-export and verify IDs
    val exportedFile =
        projectExportService.export(
            project,
            ProjectExportParameters(
                format = ProjectExportFormatEnum.valueOf(format),
                includeMilestones = true,
                includeComments = true))

    val importModel = readExportedFile(exportedFile, project, FIELD_ALIAS_CRAFT)
    verifyExportNoTopicsSharedValues(importModel)
  }

  private fun verifyExportNoTopicsSharedValues(importModel: ImportModel) {
    assertThat(importModel.workAreas).hasSize(1)
    val w1 = importModel.workAreas[0]
    assertThat(w1.name).isEqualTo("w1")
    validateExportedIds(w1, "w1", 1, 1)

    assertThat(importModel.crafts).hasSize(1)
    val craft1 = importModel.crafts.single { it.name == "pc1" }
    assertThat(craft1).isNotNull
    validateExportedIds(craft1, "t1", 2, 2)

    assertThat(importModel.tasks).hasSize(1)
    val t1 = importModel.tasks.single { it.name == "t1" }
    assertThat(t1.workAreaId).isEqualTo(w1.id)
    assertThat(t1.craftId).isEqualTo(craft1.id)
    assertThat(t1.notes).isNull()
    validateExportedIds(t1, "t1", 2, 2)
  }
}
