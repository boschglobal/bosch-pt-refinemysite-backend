/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.exporter.model.tree

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMilestone
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitRelation
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkArea
import com.bosch.pt.csm.cloud.projectmanagement.relation.messages.RelationTypeEnumAvro.FINISH_TO_START
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.project.daycard.facade.rest.submitInitProjectData
import com.bosch.pt.iot.smartsite.project.exporter.api.MilestoneExportSchedulingType.MANUALLY_SCHEDULED
import com.bosch.pt.iot.smartsite.project.exporter.api.TaskExportSchedulingType.AUTO_SCHEDULED
import com.bosch.pt.iot.smartsite.project.external.model.ExternalIdType
import com.bosch.pt.iot.smartsite.project.external.model.ObjectType.MILESTONE
import com.bosch.pt.iot.smartsite.project.external.model.ObjectType.TASK
import com.bosch.pt.iot.smartsite.project.external.model.ObjectType.WORKAREA
import com.bosch.pt.iot.smartsite.project.importer.boundary.AbstractImportIntegrationTest
import com.bosch.pt.iot.smartsite.project.milestone.domain.asMilestoneId
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.Milestone
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.project.shared.model.Project
import com.bosch.pt.iot.smartsite.project.task.domain.asTaskId
import com.bosch.pt.iot.smartsite.project.task.shared.model.Task
import com.bosch.pt.iot.smartsite.project.workarea.domain.asWorkAreaId
import com.bosch.pt.iot.smartsite.project.workarea.shared.model.WorkArea
import net.sf.mpxj.ProjectFile
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@EnableAllKafkaListeners
class TreeIntegrationTest : AbstractImportIntegrationTest() {

  private lateinit var project: Project
  private lateinit var projectFile: ProjectFile
  private lateinit var tree: Tree

  @BeforeEach
  fun init() {
    eventStreamGenerator.submitInitProjectData()
    project = repositories.findProject(getIdentifier("project").asProjectId())!!

    projectFile = ProjectFile()
    tree =
        Tree(
            projectFile = projectFile,
            project = project,
            existingExternalIds = emptyList(),
            idType = ExternalIdType.MS_PROJECT,
            allowWorkOnNonWorkingDays = false,
            requestedTaskSchedulingType = AUTO_SCHEDULED,
            requestedMilestoneSchedulingType = MANUALLY_SCHEDULED,
        )
  }

  /**
   * Tree
   * - root
   *     - w1a
   *         - w2a
   *             - w3a
   *                 - t3
   *             - w3b
   *                 - t4
   *                 - t5
   *         - w2b
   *             - t2
   *     - w1b
   *         - t1
   */
  @Nested
  inner class TestWorkArea {

    @Test
    fun `hierarchy inserted in correct order`() {
      // Generate test data via event stream
      importDataForWorkAreaTest()

      // Get data from db
      val t1 = task("t1")
      val t2 = task("t2")
      val t3 = task("t3")
      val t4 = task("t4")
      val t5 = task("t5")
      val w3b = workArea("w3b")
      val w3a = workArea("w3a")
      val w1b = workArea("w1b")
      val w1a = workArea("w1a")
      val w2b = workArea("w2b")
      val w2a = workArea("w2a")

      // Register elements in external id list
      registerAsExternalId(w1a, w2a, w3a, t3, w3b, t4, t5, w2b, t2, w1b, t1)

      // Add data to tree
      tree.addWorkArea(w1a)
      tree.addWorkArea(w1b)
      tree.addWorkArea(w2a)
      tree.addWorkArea(w2b)
      tree.addWorkArea(w3a)
      tree.addWorkArea(w3b)
      tree.addTask(t1)
      tree.addTask(t2)
      tree.addTask(t3)
      tree.addTask(t4)
      tree.addTask(t5)

      // Check exported data
      verifyExportedDataOfWorkAreaTest()
    }

    @Test
    fun `hierarchy inserted in incorrect order`() {
      // Generate test data via event stream
      importDataForWorkAreaTest()

      // Get data from db
      val t1 = task("t1")
      val t2 = task("t2")
      val t3 = task("t3")
      val t4 = task("t4")
      val t5 = task("t5")
      val w3b = workArea("w3b")
      val w3a = workArea("w3a")
      val w1b = workArea("w1b")
      val w1a = workArea("w1a")
      val w2b = workArea("w2b")
      val w2a = workArea("w2a")

      // Register elements in external id list
      registerAsExternalId(w1a, w2a, w3a, t3, w3b, t4, t5, w2b, t2, w1b, t1)

      // Add data to tree
      tree.addTask(t1)
      tree.addTask(t2)
      tree.addTask(t3)
      tree.addTask(t4)
      tree.addTask(t5)
      tree.addWorkArea(w3b)
      tree.addWorkArea(w3a)
      tree.addWorkArea(w1b)
      tree.addWorkArea(w1a)
      tree.addWorkArea(w2b)
      tree.addWorkArea(w2a)

      // Check exported data
      verifyExportedDataOfWorkAreaTest()
    }

    private fun verifyExportedDataOfWorkAreaTest() {
      // Export data to mpxj project file and parse output
      tree.write(messageSource)
      val model = readFile(projectFile, project)

      // Check work areas
      val workAreas = model.workAreas
      assertThat(workAreas).hasSize(6)
      val w1a = workAreas.single { it.guid == getIdentifier("w1a") }
      val w1b = workAreas.single { it.guid == getIdentifier("w1b") }
      val w2a = workAreas.single { it.guid == getIdentifier("w2a") }
      val w2b = workAreas.single { it.guid == getIdentifier("w2b") }
      val w3a = workAreas.single { it.guid == getIdentifier("w3a") }
      val w3b = workAreas.single { it.guid == getIdentifier("w3b") }
      assertThat(w1a.parent).isNull()
      assertThat(w1b.parent).isNull()
      assertThat(w2a.parent).isEqualTo(w1a.identifier)
      assertThat(w2b.parent).isEqualTo(w1a.identifier)
      assertThat(w3a.parent).isEqualTo(w2a.identifier)
      assertThat(w3b.parent).isEqualTo(w2a.identifier)

      // Check tasks
      assertThat(model.tasks).hasSize(5)
      assertThat(task("t1").workArea?.identifier?.identifier).isEqualTo(w1b.guid)
      assertThat(task("t2").workArea?.identifier?.identifier).isEqualTo(w2b.guid)
      assertThat(task("t3").workArea?.identifier?.identifier).isEqualTo(w3a.guid)
      assertThat(task("t4").workArea?.identifier?.identifier).isEqualTo(w3b.guid)
      assertThat(task("t5").workArea?.identifier?.identifier).isEqualTo(w3b.guid)

      // Check external ids
      val externalIds = tree.externalIds.values
      val eW1a = externalIds.single { it.objectIdentifier == workArea("w1a").identifier.identifier }
      val eW1b = externalIds.single { it.objectIdentifier == workArea("w1b").identifier.identifier }
      val eW2a = externalIds.single { it.objectIdentifier == workArea("w2a").identifier.identifier }
      val eW2b = externalIds.single { it.objectIdentifier == workArea("w2b").identifier.identifier }
      val eW3a = externalIds.single { it.objectIdentifier == workArea("w3a").identifier.identifier }
      val eW3b = externalIds.single { it.objectIdentifier == workArea("w3b").identifier.identifier }
      val eT1 = externalIds.single { it.objectIdentifier == task("t1").identifier.identifier }
      val eT2 = externalIds.single { it.objectIdentifier == task("t2").identifier.identifier }
      val eT3 = externalIds.single { it.objectIdentifier == task("t3").identifier.identifier }
      val eT4 = externalIds.single { it.objectIdentifier == task("t4").identifier.identifier }
      val eT5 = externalIds.single { it.objectIdentifier == task("t5").identifier.identifier }
      assertThat(eW1a.fileId).isEqualTo(1)
      assertThat(eW2a.fileId).isEqualTo(2)
      assertThat(eW3a.fileId).isEqualTo(3)
      assertThat(eT3.fileId).isEqualTo(4)
      assertThat(eW3b.fileId).isEqualTo(5)
      assertThat(eT4.fileId).isEqualTo(6)
      assertThat(eT5.fileId).isEqualTo(7)
      assertThat(eW2b.fileId).isEqualTo(8)
      assertThat(eT2.fileId).isEqualTo(9)
      assertThat(eW1b.fileId).isEqualTo(10)
      assertThat(eT1.fileId).isEqualTo(11)
    }

    private fun registerAsExternalId(
        w1a: WorkArea,
        w2a: WorkArea,
        w3a: WorkArea,
        t3: Task,
        w3b: WorkArea,
        t4: Task,
        t5: Task,
        w2b: WorkArea,
        t2: Task,
        w1b: WorkArea,
        t1: Task
    ) {
      assertThat(tree.getExternalId(WORKAREA, w1a.identifier.identifier).fileId).isEqualTo(2)
      assertThat(tree.getExternalId(WORKAREA, w2a.identifier.identifier).fileId).isEqualTo(3)
      assertThat(tree.getExternalId(WORKAREA, w3a.identifier.identifier).fileId).isEqualTo(4)
      assertThat(tree.getExternalId(TASK, t3.identifier.identifier).fileId).isEqualTo(5)
      assertThat(tree.getExternalId(WORKAREA, w3b.identifier.identifier).fileId).isEqualTo(6)
      assertThat(tree.getExternalId(TASK, t4.identifier.identifier).fileId).isEqualTo(7)
      assertThat(tree.getExternalId(TASK, t5.identifier.identifier).fileId).isEqualTo(8)
      assertThat(tree.getExternalId(WORKAREA, w2b.identifier.identifier).fileId).isEqualTo(9)
      assertThat(tree.getExternalId(TASK, t2.identifier.identifier).fileId).isEqualTo(10)
      assertThat(tree.getExternalId(WORKAREA, w1b.identifier.identifier).fileId).isEqualTo(11)
      assertThat(tree.getExternalId(TASK, t1.identifier.identifier).fileId).isEqualTo(12)
    }

    private fun importDataForWorkAreaTest() {
      eventStreamGenerator
          .submitWorkArea("w1a") { it.name = "w1a" }
          .submitWorkArea("w1b") { it.name = "w1b" }
          .submitWorkArea("w2a") {
            it.name = "w2a"
            it.parent = getIdentifier("w1a").toString()
          }
          .submitWorkArea("w2b") {
            it.name = "w2b"
            it.parent = getIdentifier("w1a").toString()
          }
          .submitWorkArea("w3a") {
            it.name = "w3a"
            it.parent = getIdentifier("w2a").toString()
          }
          .submitWorkArea("w3b") {
            it.name = "w3b"
            it.parent = getIdentifier("w2a").toString()
          }
          .submitTask("t1") {
            it.name = "t1"
            it.workarea = getByReference("w1b")
          }
          .submitTask("t2") {
            it.name = "t2"
            it.workarea = getByReference("w2b")
          }
          .submitTask("t3") {
            it.name = "t3"
            it.workarea = getByReference("w3a")
          }
          .submitTask("t4") {
            it.name = "t4"
            it.workarea = getByReference("w3b")
          }
          .submitTask("t5") {
            it.name = "t5"
            it.workarea = getByReference("w3b")
          }
    }
  }

  /**
   * Tree
   * - root
   *     - w1
   *         - t1
   *         - t2
   *         - t3
   */
  @Nested
  inner class TestTask {

    @Test
    fun `inserted in correct order`() {
      importDataForTaskTest()

      val t1 = task("t1")
      val t2 = task("t2")
      val t3 = task("t3")
      val w1 = workArea("w1")

      // Register elements in external id list
      registerAsExternalId(w1, t1, t2, t3)

      tree.addWorkArea(workArea("w1"))
      tree.addTask(t1)
      tree.addTask(t2)
      tree.addTask(t3)

      verifyExportedDataOfTaskTest(w1, t1, t2, t3)
    }

    @Test
    fun `inserted in incorrect order`() {
      importDataForTaskTest()

      val t1 = task("t1")
      val t2 = task("t2")
      val t3 = task("t3")
      val w1 = workArea("w1")

      // Register elements in external id list
      registerAsExternalId(w1, t1, t2, t3)

      tree.addTask(t2)
      tree.addTask(t1)
      tree.addTask(t3)
      tree.addWorkArea(w1)

      verifyExportedDataOfTaskTest(w1, t1, t2, t3)
    }

    private fun verifyExportedDataOfTaskTest(w1: WorkArea, t1: Task, t2: Task, t3: Task) {
      tree.write(messageSource)
      val model = readFile(projectFile, project)

      assertThat(model.workAreas).hasSize(1)
      assertThat(model.tasks).hasSize(3)

      val externalIds = tree.externalIds.values
      val eW1 = externalIds.single { it.objectIdentifier == w1.identifier.identifier }
      val eT1 = externalIds.single { it.objectIdentifier == t1.identifier.identifier }
      val eT2 = externalIds.single { it.objectIdentifier == t2.identifier.identifier }
      val eT3 = externalIds.single { it.objectIdentifier == t3.identifier.identifier }
      assertThat(eW1.fileId).isEqualTo(1)
      assertThat(eT1.fileId).isEqualTo(2)
      assertThat(eT2.fileId).isEqualTo(3)
      assertThat(eT3.fileId).isEqualTo(4)
    }

    private fun registerAsExternalId(w1: WorkArea, t1: Task, t2: Task, t3: Task) {
      assertThat(tree.getExternalId(WORKAREA, w1.identifier.identifier).fileId).isEqualTo(2)
      assertThat(tree.getExternalId(TASK, t1.identifier.identifier).fileId).isEqualTo(3)
      assertThat(tree.getExternalId(TASK, t2.identifier.identifier).fileId).isEqualTo(4)
      assertThat(tree.getExternalId(TASK, t3.identifier.identifier).fileId).isEqualTo(5)
    }

    private fun importDataForTaskTest() {
      eventStreamGenerator
          .submitWorkArea("w1") { it.name = "w1" }
          .submitTask("t1") {
            it.name = "t1"
            it.workarea = getByReference("w1")
          }
          .submitTask("t2") {
            it.name = "t2"
            it.workarea = getByReference("w1")
          }
          .submitTask("t3") {
            it.name = "t3"
            it.workarea = getByReference("w1")
          }
    }
  }

  /**
   * Tree
   * - root
   *     - w1
   *         - t1
   *         - t2
   *         - m1
   */
  @Nested
  inner class TestMilestone {

    @Test
    fun `inserted in correct order`() {
      importDataForMilestoneTest()

      val t1 = task("t1")
      val t2 = task("t2")
      val m1 = milestone("m1")
      val w1 = workArea("w1")

      // Register elements in external id list
      registerAsExternalId(w1, t1, t2, m1)

      tree.addWorkArea(workArea("w1"))
      tree.addTask(t1)
      tree.addTask(t2)
      tree.addMilestone(m1)

      verifyExportedDataOfMilestoneTest(w1, t1, t2, m1)
    }

    @Test
    fun `inserted in incorrect order`() {
      importDataForMilestoneTest()

      val t1 = task("t1")
      val t2 = task("t2")
      val m1 = milestone("m1")
      val w1 = workArea("w1")

      // Register elements in external id list
      registerAsExternalId(w1, t1, t2, m1)

      tree.addMilestone(m1)
      tree.addTask(t2)
      tree.addTask(t1)
      tree.addWorkArea(workArea("w1"))

      verifyExportedDataOfMilestoneTest(w1, t1, t2, m1)
    }

    private fun verifyExportedDataOfMilestoneTest(w1: WorkArea, t1: Task, t2: Task, m1: Milestone) {
      tree.write(messageSource)
      val model = readFile(projectFile, project)

      assertThat(model.workAreas).hasSize(1)
      assertThat(model.tasks).hasSize(2)
      assertThat(model.milestones).hasSize(1)

      val externalIds = tree.externalIds.values
      val eW1 = externalIds.single { it.objectIdentifier == w1.identifier.identifier }
      val eT1 = externalIds.single { it.objectIdentifier == t1.identifier.identifier }
      val eT2 = externalIds.single { it.objectIdentifier == t2.identifier.identifier }
      val eM1 = externalIds.single { it.objectIdentifier == m1.identifier.identifier }
      assertThat(eW1.fileId).isEqualTo(1)
      assertThat(eT1.fileId).isEqualTo(2)
      assertThat(eT2.fileId).isEqualTo(3)
      assertThat(eM1.fileId).isEqualTo(4)
    }

    private fun registerAsExternalId(w1: WorkArea, t1: Task, t2: Task, m1: Milestone) {
      assertThat(tree.getExternalId(WORKAREA, w1.identifier.identifier).fileId).isEqualTo(2)
      assertThat(tree.getExternalId(TASK, t1.identifier.identifier).fileId).isEqualTo(3)
      assertThat(tree.getExternalId(TASK, t2.identifier.identifier).fileId).isEqualTo(4)
      assertThat(tree.getExternalId(MILESTONE, m1.identifier.identifier).fileId).isEqualTo(5)
    }

    private fun importDataForMilestoneTest() {
      eventStreamGenerator
          .submitWorkArea("w1") { it.name = "w1" }
          .submitTask("t1") {
            it.name = "t1"
            it.workarea = getByReference("w1")
          }
          .submitTask("t2") {
            it.name = "t2"
            it.workarea = getByReference("w1")
          }
          .submitMilestone("m1") {
            it.name = "m1"
            it.workarea = getByReference("w1")
          }
    }
  }

  /**
   * Tree
   * - root
   *     - w1
   *         - t1
   *         - m1
   */
  @Nested
  inner class TestRelation {

    @Test
    fun `relation inserted`() {
      importDataForRelationTest()

      val t1 = task("t1")
      val r1 = relation("r1")
      val m1 = milestone("m1")
      val w1 = workArea("w1")

      // Register elements in external id list
      registerAsExternalId(w1, t1, m1)

      tree.addWorkArea(w1)
      tree.addTask(t1)
      tree.addMilestone(m1)
      tree.addRelation(r1)

      verifyExportedDataOfRelationTest()
    }

    private fun verifyExportedDataOfRelationTest() {
      tree.write(messageSource)
      val model = readFile(projectFile, project)

      assertThat(model.workAreas).hasSize(1)
      assertThat(model.tasks).hasSize(1)
      assertThat(model.milestones).hasSize(1)
      assertThat(model.relations).hasSize(1)

      val relation = model.relations[0]
      assertThat(model.tasks.single { it.id.id == relation.sourceId.id }).isNotNull
      assertThat(model.milestones.single { it.id.id == relation.targetId.id }).isNotNull
    }

    private fun registerAsExternalId(w1: WorkArea, t1: Task, m1: Milestone) {
      assertThat(tree.getExternalId(WORKAREA, w1.identifier.identifier).fileId).isEqualTo(2)
      assertThat(tree.getExternalId(TASK, t1.identifier.identifier).fileId).isEqualTo(3)
      assertThat(tree.getExternalId(MILESTONE, m1.identifier.identifier).fileId).isEqualTo(4)
    }

    private fun importDataForRelationTest() {
      eventStreamGenerator
          .submitWorkArea("w1") { it.name = "w1" }
          .submitTask("t1") {
            it.name = "t1"
            it.workarea = getByReference("w1")
          }
          .submitMilestone("m1") {
            it.name = "m1"
            it.workarea = getByReference("w1")
          }
          .submitRelation("r1") {
            it.source = getByReference("t1")
            it.target = getByReference("m1")
            it.type = FINISH_TO_START
          }
    }
  }

  private fun relation(ref: String) =
      repositories.findRelation(getIdentifier(ref), project.identifier)!!

  private fun milestone(ref: String) =
      repositories.findMilestoneWithDetails(getIdentifier(ref).asMilestoneId())!!

  private fun task(ref: String) = repositories.findTaskWithDetails(getIdentifier(ref).asTaskId())!!

  private fun workArea(ref: String) = repositories.findWorkArea(getIdentifier(ref).asWorkAreaId())!!
}
