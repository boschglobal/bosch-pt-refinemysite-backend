/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.exporter.boundary

import com.bosch.pt.csm.cloud.common.extensions.toEpochMilli
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro.CSM
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMilestone
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMilestoneList
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitRelation
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskSchedule
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkArea
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkAreaList
import com.bosch.pt.csm.cloud.projectmanagement.relation.messages.RelationTypeEnumAvro
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.project.exporter.api.ProjectExportFormatEnum
import com.bosch.pt.iot.smartsite.project.exporter.api.ProjectExportParameters
import com.bosch.pt.iot.smartsite.project.exporter.boundary.ProjectExportService.Companion.FIELD_ALIAS_CRAFT
import com.bosch.pt.iot.smartsite.project.importer.model.ImportModel
import com.bosch.pt.iot.smartsite.project.importer.submitProjectImportFeatureToggle
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired

@EnableAllKafkaListeners
class ExportRelationIntegrationTest : AbstractExportIntegrationTest() {

  @Autowired private lateinit var projectExportService: ProjectExportService

  private val projectIdentifier by lazy { getIdentifier("p2").asProjectId() }
  private val project by lazy { checkNotNull(repositories.findProject(projectIdentifier)) }

  @BeforeEach
  fun init() {
    val nextMonday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY))

    eventStreamGenerator
        .setupDatasetTestData()
        .submitProjectImportFeatureToggle()
        .submitProject("p2")
        .submitProjectCraftG2("pc1") { it.name = "pc1" }
        .submitParticipantG3("p2Csm1") {
          it.user = getByReference("userCsm1")
          it.role = CSM
        }
        .submitWorkArea(asReference = "w1") { it.name = "w1" }
        .submitWorkAreaList(asReference = "wal") {
          it.workAreas = listOf(getByReference("w1"))
          it.project = getByReference("p2")
        }
        .submitTask(asReference = "t1") {
          it.project = getByReference("p2")
          it.workarea = getByReference("w1")
          it.craft = getByReference("pc1")
          it.name = "t1"
          it.description = "description"
        }
        .submitProjectCraftG2("pc1") { it.name = "pc1" }
        .submitTask(asReference = "t2") {
          it.project = getByReference("p2")
          it.workarea = getByReference("w1")
          it.craft = getByReference("pc1")
          it.name = "t2"
        }
        .submitTaskSchedule("s1") {
          it.task = getByReference("t1")
          it.start = nextMonday.toEpochMilli()
          it.end = nextMonday.plusDays(1).toEpochMilli()
        }
        .submitTaskSchedule("s2") {
          it.task = getByReference("t2")
          it.start = nextMonday.toEpochMilli()
          it.end = nextMonday.plusDays(1).toEpochMilli()
        }
    setAuthentication("userCsm1")

    projectEventStoreUtils.reset()
  }

  @ParameterizedTest
  @ValueSource(strings = ["MS_PROJECT_XML", "PRIMAVERA_P6_XML"])
  fun exportTaskTaskRelation(format: String) {
    eventStreamGenerator.submitRelation("r1") {
      it.type = RelationTypeEnumAvro.FINISH_TO_START
      it.source = getByReference("t1")
      it.target = getByReference("t2")
    }

    val exportedFile =
        projectExportService.export(
            project,
            ProjectExportParameters(
                format = ProjectExportFormatEnum.valueOf(format),
                includeMilestones = true,
                includeComments = false))

    val importModel = readExportedFile(exportedFile, project, FIELD_ALIAS_CRAFT)
    verifyExportTaskTaskRelationSharedValues(importModel)
  }

  @ParameterizedTest
  @ValueSource(strings = ["MS_PROJECT_XML", "PRIMAVERA_P6_XML"])
  fun reExportTaskTaskRelation(format: String) {

    // First export
    exportTaskTaskRelation(format)

    // Don't change anything, re-export and verify IDs
    val exportedFile =
        projectExportService.export(
            project,
            ProjectExportParameters(
                format = ProjectExportFormatEnum.valueOf(format),
                includeMilestones = true,
                includeComments = false))

    val importModel = readExportedFile(exportedFile, project, FIELD_ALIAS_CRAFT)
    verifyExportTaskTaskRelationSharedValues(importModel)
  }

  private fun verifyExportTaskTaskRelationSharedValues(importModel: ImportModel) {
    assertThat(importModel.workAreas).hasSize(1)
    val w1 = importModel.workAreas[0]
    assertThat(w1.name).isEqualTo("w1")
    validateExportedIds(w1, "w1", 1, 1)

    assertThat(importModel.crafts).hasSize(1)
    val craft1 = importModel.crafts.single { it.name == "pc1" }
    assertThat(craft1).isNotNull
    validateExportedIds(craft1, "t1", 2, 2)

    assertThat(importModel.tasks).hasSize(2)
    val t1 = importModel.tasks.single { it.name == "t1" }
    assertThat(t1.workAreaId).isEqualTo(w1.id)
    assertThat(t1.craftId).isEqualTo(craft1.id)
    assertThat(t1.notes).isEqualTo("description")
    validateExportedIds(t1, "t1", 2, 2)

    val t2 = importModel.tasks.single { it.name == "t2" }
    assertThat(t2.workAreaId).isEqualTo(w1.id)
    assertThat(t1.craftId).isEqualTo(craft1.id)
    validateExportedIds(t2, "t2", 3, 3)

    assertThat(importModel.taskSchedules).hasSize(2)
    val s1 = importModel.taskSchedules.single { it.taskId == t1.id }
    assertThat(s1.start).isEqualTo(getEventStartDate("s1"))
    assertThat(s1.end).isEqualTo(getEventEndDate("s1"))
    validateExportedIds(s1, "t1", 2, 2)

    val s2 = importModel.taskSchedules.single { it.taskId == t2.id }
    assertThat(s2.start).isEqualTo(getEventStartDate("s2"))
    assertThat(s2.end).isEqualTo(getEventEndDate("s2"))
    validateExportedIds(s2, "t2", 3, 3)

    assertThat(importModel.relations).hasSize(1)
    val r1 = importModel.relations.first()
    assertThat(r1.sourceId.id).isEqualTo(t1.id.id)
    assertThat(r1.targetId.id).isEqualTo(t2.id.id)
  }

  @ParameterizedTest
  @ValueSource(strings = ["MS_PROJECT_XML", "PRIMAVERA_P6_XML"])
  fun exportTaskMilestoneRelation(format: String) {
    val nextMonday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY))

    eventStreamGenerator
        .submitMilestone("m1") {
          it.name = "m1"
          it.date = nextMonday.plusDays(2).toEpochMilli()
          it.workarea = getByReference("w1")
          it.craft = getByReference("pc1")
        }
        .submitMilestoneList("ml1") {
          it.milestones = listOf(getByReference("m1"))
          it.date = nextMonday.plusDays(2).toEpochMilli()
        }
        .submitRelation("r1") {
          it.type = RelationTypeEnumAvro.FINISH_TO_START
          it.source = getByReference("t1")
          it.target = getByReference("m1")
        }

    val exportedFile =
        projectExportService.export(
            project,
            ProjectExportParameters(
                format = ProjectExportFormatEnum.valueOf(format),
                includeMilestones = true,
                includeComments = false))

    val importModel = readExportedFile(exportedFile, project, FIELD_ALIAS_CRAFT)
    verifyExportTaskMilestoneRelationSharedValues(importModel)
  }

  @ParameterizedTest
  @ValueSource(strings = ["MS_PROJECT_XML", "PRIMAVERA_P6_XML"])
  fun reExportTaskMilestoneRelation(format: String) {

    // First export
    exportTaskMilestoneRelation(format)

    // Don't change anything, re-export and validate IDs
    val exportedFile =
        projectExportService.export(
            project,
            ProjectExportParameters(
                format = ProjectExportFormatEnum.valueOf(format),
                includeMilestones = true,
                includeComments = false))

    val importModel = readExportedFile(exportedFile, project, FIELD_ALIAS_CRAFT)
    verifyExportTaskMilestoneRelationSharedValues(importModel)
  }

  private fun verifyExportTaskMilestoneRelationSharedValues(importModel: ImportModel) {
    val nextMonday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY))

    assertThat(importModel.workAreas).hasSize(1)
    val w1 = importModel.workAreas[0]
    assertThat(w1.name).isEqualTo("w1")
    validateExportedIds(w1, "w1", 1, 1)

    assertThat(importModel.crafts).hasSize(1)
    val craft1 = importModel.crafts.single { it.name == "pc1" }
    assertThat(craft1).isNotNull
    validateExportedIds(craft1, "t1", 2, 2)

    assertThat(importModel.tasks).hasSize(2)
    val t1 = importModel.tasks.single { it.name == "t1" }
    assertThat(t1.workAreaId).isEqualTo(w1.id)
    assertThat(t1.craftId).isEqualTo(craft1.id)
    assertThat(t1.notes).isEqualTo("description")
    validateExportedIds(t1, "t1", 2, 2)

    val t2 = importModel.tasks.single { it.name == "t2" }
    assertThat(t2.workAreaId).isEqualTo(w1.id)
    assertThat(t2.craftId).isEqualTo(craft1.id)
    validateExportedIds(t2, "t2", 3, 3)

    assertThat(importModel.taskSchedules).hasSize(2)
    val s1 = importModel.taskSchedules.single { it.taskId == t1.id }
    assertThat(s1.start).isEqualTo(getEventStartDate("s1"))
    assertThat(s1.end).isEqualTo(getEventEndDate("s1"))
    validateExportedIds(s1, "t1", 2, 2)

    val s2 = importModel.taskSchedules.single { it.taskId == t2.id }
    assertThat(s2.start).isEqualTo(getEventStartDate("s2"))
    assertThat(s2.end).isEqualTo(getEventEndDate("s2"))
    validateExportedIds(s2, "t2", 3, 3)

    assertThat(importModel.milestones).hasSize(1)
    val m1 = importModel.milestones.first()
    assertThat(m1.workAreaId).isEqualTo(w1.id)
    assertThat(m1.name).isEqualTo("m1")
    assertThat(m1.date).isEqualTo(nextMonday.plusDays(2))
    assertThat(m1.header).isFalse
    assertThat(m1.notes).isEmpty()
    assertThat(m1.craftId).isEqualTo(craft1.id)
    validateExportedIds(m1, "m1", 4, 4)

    assertThat(importModel.relations).hasSize(1)
    val r1 = importModel.relations.first()
    assertThat(r1.sourceId.id).isEqualTo(t1.id.id)
    assertThat(r1.targetId.id).isEqualTo(m1.id.id)
  }

  @ParameterizedTest
  @ValueSource(strings = ["MS_PROJECT_XML", "PRIMAVERA_P6_XML"])
  fun exportMilestoneTaskRelation(format: String) {
    val nextMonday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY))

    eventStreamGenerator
        .submitMilestone("m1") {
          it.name = "m1"
          it.date = nextMonday.plusDays(2).toEpochMilli()
          it.workarea = getByReference("w1")
          it.craft = getByReference("pc1")
        }
        .submitMilestoneList("ml1") {
          it.milestones = listOf(getByReference("m1"))
          it.date = nextMonday.plusDays(2).toEpochMilli()
        }
        .submitRelation("r1") {
          it.type = RelationTypeEnumAvro.FINISH_TO_START
          it.source = getByReference("m1")
          it.target = getByReference("t1")
        }

    val exportedFile =
        projectExportService.export(
            project,
            ProjectExportParameters(
                format = ProjectExportFormatEnum.valueOf(format),
                includeMilestones = true,
                includeComments = false))

    val importModel = readExportedFile(exportedFile, project, FIELD_ALIAS_CRAFT)
    verifyExportMilestoneTaskRelation(importModel)
  }

  @ParameterizedTest
  @ValueSource(strings = ["MS_PROJECT_XML", "PRIMAVERA_P6_XML"])
  fun reExportMilestoneTaskRelation(format: String) {

    // First export
    exportMilestoneTaskRelation(format)

    // Don't change anything, re-export and validate IDs
    val exportedFile =
        projectExportService.export(
            project,
            ProjectExportParameters(
                format = ProjectExportFormatEnum.valueOf(format),
                includeMilestones = true,
                includeComments = false))

    val importModel = readExportedFile(exportedFile, project, FIELD_ALIAS_CRAFT)
    verifyExportMilestoneTaskRelation(importModel)
  }

  private fun verifyExportMilestoneTaskRelation(importModel: ImportModel) {
    val nextMonday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY))

    assertThat(importModel.workAreas).hasSize(1)
    val w1 = importModel.workAreas[0]
    assertThat(w1.name).isEqualTo("w1")
    validateExportedIds(w1, "w1", 1, 1)

    assertThat(importModel.crafts).hasSize(1)
    val craft1 = importModel.crafts.single { it.name == "pc1" }
    assertThat(craft1).isNotNull
    validateExportedIds(craft1, "t1", 2, 2)

    assertThat(importModel.tasks).hasSize(2)
    val t1 = importModel.tasks.single { it.name == "t1" }
    assertThat(t1.workAreaId).isEqualTo(w1.id)
    assertThat(t1.craftId).isEqualTo(craft1.id)
    assertThat(t1.notes).isEqualTo("description")
    validateExportedIds(t1, "t1", 2, 2)

    val t2 = importModel.tasks.single { it.name == "t2" }
    assertThat(t2.workAreaId).isEqualTo(w1.id)
    assertThat(t2.craftId).isEqualTo(craft1.id)
    validateExportedIds(t2, "t2", 3, 3)

    assertThat(importModel.taskSchedules).hasSize(2)
    val s1 = importModel.taskSchedules.single { it.taskId == t1.id }
    assertThat(s1.start).isEqualTo(getEventStartDate("s1"))
    assertThat(s1.end).isEqualTo(getEventEndDate("s1"))
    validateExportedIds(s1, "t1", 2, 2)

    val s2 = importModel.taskSchedules.single { it.taskId == t2.id }
    assertThat(s2.start).isEqualTo(getEventStartDate("s2"))
    assertThat(s2.end).isEqualTo(getEventEndDate("s2"))
    validateExportedIds(s2, "t2", 3, 3)

    assertThat(importModel.milestones).hasSize(1)
    val m1 = importModel.milestones.first()
    assertThat(m1.workAreaId).isEqualTo(w1.id)
    assertThat(m1.name).isEqualTo("m1")
    assertThat(m1.date).isEqualTo(nextMonday.plusDays(2))
    assertThat(m1.header).isFalse
    assertThat(m1.notes).isEmpty()
    assertThat(m1.craftId).isEqualTo(craft1.id)
    validateExportedIds(m1, "m1", 4, 4)

    assertThat(importModel.relations).hasSize(1)
    val r1 = importModel.relations.first()
    assertThat(r1.sourceId.id).isEqualTo(m1.id.id)
    assertThat(r1.targetId.id).isEqualTo(t1.id.id)
  }

  @ParameterizedTest
  @ValueSource(strings = ["MS_PROJECT_XML", "PRIMAVERA_P6_XML"])
  fun exportMilestoneMilestoneRelation(format: String) {
    val nextMonday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY))

    eventStreamGenerator
        .submitMilestone("m1") {
          it.name = "m1"
          it.date = nextMonday.plusDays(2).toEpochMilli()
          it.workarea = getByReference("w1")
          it.craft = getByReference("pc1")
        }
        .submitMilestoneList("ml1") {
          it.milestones = listOf(getByReference("m1"))
          it.date = nextMonday.plusDays(2).toEpochMilli()
        }
        .submitMilestone("m2") {
          it.name = "m2"
          it.date = nextMonday.plusDays(4).toEpochMilli()
          it.workarea = getByReference("w1")
          it.craft = getByReference("pc1")
        }
        .submitMilestoneList("ml2") {
          it.milestones = listOf(getByReference("m2"))
          it.date = nextMonday.plusDays(4).toEpochMilli()
        }
        .submitRelation("r1") {
          it.type = RelationTypeEnumAvro.FINISH_TO_START
          it.source = getByReference("m1")
          it.target = getByReference("m2")
        }

    val exportedFile =
        projectExportService.export(
            project,
            ProjectExportParameters(
                format = ProjectExportFormatEnum.valueOf(format),
                includeMilestones = true,
                includeComments = false))

    val importModel = readExportedFile(exportedFile, project, FIELD_ALIAS_CRAFT)
    verifyExportMilestoneMilestoneRelation(importModel)
  }

  @ParameterizedTest
  @ValueSource(strings = ["MS_PROJECT_XML", "PRIMAVERA_P6_XML"])
  fun reExportMilestoneMilestoneRelation(format: String) {

    // First export
    exportMilestoneMilestoneRelation(format)

    // Don't change anything, re-export and validate IDs
    val exportedFile =
        projectExportService.export(
            project,
            ProjectExportParameters(
                format = ProjectExportFormatEnum.valueOf(format),
                includeMilestones = true,
                includeComments = false))

    val importModel = readExportedFile(exportedFile, project, FIELD_ALIAS_CRAFT)
    verifyExportMilestoneMilestoneRelation(importModel)
  }

  private fun verifyExportMilestoneMilestoneRelation(importModel: ImportModel) {
    val nextMonday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY))

    assertThat(importModel.workAreas).hasSize(1)
    val w1 = importModel.workAreas[0]
    assertThat(w1.name).isEqualTo("w1")
    validateExportedIds(w1, "w1", 1, 1)

    assertThat(importModel.crafts).hasSize(1)
    val craft1 = importModel.crafts.single { it.name == "pc1" }
    assertThat(craft1).isNotNull
    validateExportedIds(craft1, "t1", 2, 2)

    assertThat(importModel.tasks).hasSize(2)
    val t1 = importModel.tasks.single { it.name == "t1" }
    assertThat(t1.workAreaId).isEqualTo(w1.id)
    assertThat(t1.craftId).isEqualTo(craft1.id)
    assertThat(t1.notes).isEqualTo("description")
    validateExportedIds(t1, "t1", 2, 2)

    val t2 = importModel.tasks.single { it.name == "t2" }
    assertThat(t2.workAreaId).isEqualTo(w1.id)
    assertThat(t2.craftId).isEqualTo(craft1.id)
    validateExportedIds(t2, "t2", 3, 3)

    assertThat(importModel.taskSchedules).hasSize(2)

    assertThat(importModel.milestones).hasSize(2)
    val m1 = importModel.milestones.single { it.name == "m1" }
    assertThat(m1.workAreaId).isEqualTo(w1.id)
    assertThat(m1.name).isEqualTo("m1")
    assertThat(m1.date).isEqualTo(nextMonday.plusDays(2))
    assertThat(m1.header).isFalse
    assertThat(m1.notes).isEmpty()
    assertThat(m1.craftId).isEqualTo(craft1.id)
    validateExportedIds(m1, "m1", 4, 4)

    val m2 = importModel.milestones.single { it.name == "m2" }
    assertThat(m2.workAreaId).isEqualTo(w1.id)
    assertThat(m2.name).isEqualTo("m2")
    assertThat(m2.date).isEqualTo(nextMonday.plusDays(4))
    assertThat(m2.header).isFalse
    assertThat(m2.notes).isEmpty()
    assertThat(m2.craftId).isEqualTo(craft1.id)
    validateExportedIds(m2, "m2", 5, 5)

    assertThat(importModel.relations).hasSize(1)
    val r1 = importModel.relations.first()
    assertThat(r1.sourceId.id).isEqualTo(m1.id.id)
    assertThat(r1.targetId.id).isEqualTo(m2.id.id)
  }

  /** Ensure that finish-to-start relations are exported and part-of relations are skipped. */
  @ParameterizedTest
  @ValueSource(strings = ["MS_PROJECT_XML", "PRIMAVERA_P6_XML"])
  fun doNotExportPartOfRelations(format: String) {
    val nextMonday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY))

    eventStreamGenerator
        .submitMilestone("m1") {
          it.name = "m1"
          it.date = nextMonday.plusDays(2).toEpochMilli()
          it.workarea = getByReference("w1")
          it.craft = getByReference("pc1")
        }
        .submitMilestoneList("ml1") {
          it.milestones = listOf(getByReference("m1"))
          it.date = nextMonday.plusDays(2).toEpochMilli()
        }
        .submitMilestone("m2") {
          it.name = "m2"
          it.date = nextMonday.plusDays(4).toEpochMilli()
          it.workarea = getByReference("w1")
          it.craft = getByReference("pc1")
        }
        .submitMilestoneList("ml2") {
          it.milestones = listOf(getByReference("m2"))
          it.date = nextMonday.plusDays(4).toEpochMilli()
        }
        .submitRelation("r1") {
          it.type = RelationTypeEnumAvro.FINISH_TO_START
          it.source = getByReference("m1")
          it.target = getByReference("m2")
        }
        .submitRelation("r2") {
          it.type = RelationTypeEnumAvro.PART_OF
          it.source = getByReference("t1")
          it.target = getByReference("m1")
        }

    val exportedFile =
        projectExportService.export(
            project,
            ProjectExportParameters(
                format = ProjectExportFormatEnum.valueOf(format),
                includeMilestones = true,
                includeComments = false))

    val importModel = readExportedFile(exportedFile, project, FIELD_ALIAS_CRAFT)
    verifyExportMilestoneMilestoneRelation(importModel)

    assertThat(
            repositories.relationRepository.findOneByIdentifierAndProjectIdentifier(
                getIdentifier("r2"), projectIdentifier.identifier.asProjectId()))
        .isNotNull
  }
}
