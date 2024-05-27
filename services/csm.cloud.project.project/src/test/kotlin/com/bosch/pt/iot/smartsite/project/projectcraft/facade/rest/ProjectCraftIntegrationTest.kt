/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.projectcraft.facade.rest

import com.bosch.pt.csm.cloud.common.command.exceptions.EntityOutdatedException
import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.common.facade.rest.etag.ETag
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftEventG2Avro
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftListEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftListEventEnumAvro.ITEMADDED
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftListEventEnumAvro.REORDERED
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMilestone
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftList
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractIntegrationTestV2
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.etag.from
import com.bosch.pt.iot.smartsite.common.i18n.Key.COMMON_VALIDATION_ERROR_ENTITY_OUTDATED
import com.bosch.pt.iot.smartsite.common.i18n.Key.PROJECT_CRAFT_LIST_VALIDATION_ERROR_INVALID_POSITION
import com.bosch.pt.iot.smartsite.common.i18n.Key.PROJECT_CRAFT_VALIDATION_ERROR_CRAFT_IN_USE
import com.bosch.pt.iot.smartsite.common.i18n.Key.PROJECT_CRAFT_VALIDATION_ERROR_USED_NAME
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.ProjectCraftId
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.asProjectCraftId
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.asProjectCraftListId
import com.bosch.pt.iot.smartsite.project.projectcraft.facade.rest.resource.request.ReorderProjectCraftResource
import com.bosch.pt.iot.smartsite.project.projectcraft.facade.rest.resource.request.SaveProjectCraftResource
import com.bosch.pt.iot.smartsite.project.projectcraft.util.ProjectCraftTestUtil
import com.bosch.pt.iot.smartsite.util.withMessageKey
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@EnableAllKafkaListeners
class ProjectCraftIntegrationTest : AbstractIntegrationTestV2() {

  @Autowired private lateinit var cut: ProjectCraftController

  private val testUser by lazy { repositories.findUser(getIdentifier("userCsm2"))!! }
  private val projectIdentifier by lazy { getIdentifier("project").asProjectId() }
  private val projectCraftIdentifier by lazy { getIdentifier("projectCraft").asProjectCraftId() }
  private val projectCraftListIdentifier by lazy {
    getIdentifier("projectCraftList").asProjectCraftListId()
  }

  @BeforeEach
  fun init() {
    eventStreamGenerator.setupDatasetTestData()

    setAuthentication(getIdentifier("userCsm2"))
    projectEventStoreUtils.reset()
  }

  @Nested
  inner class `Create project craft` {

    @Test
    fun `verify create project craft with a given identifier and null position`() {
      val projectCraftIdentifier = ProjectCraftId()
      val resource = SaveProjectCraftResource("New Craft", "#FFFFF")

      val projectCraftListResource =
          cut.create(projectIdentifier, projectCraftIdentifier, resource, ETag.from("0")).body!!

      assertThat(projectCraftListResource.projectCrafts.size).isEqualTo(2)
      assertThat(projectCraftListResource.projectCrafts.last().id)
          .isEqualTo(projectCraftIdentifier.toUuid())
      assertThat(projectCraftListResource.projectCrafts.last().name).isEqualTo(resource.name)
      assertThat(projectCraftListResource.projectCrafts.last().color).isEqualTo(resource.color)

      projectEventStoreUtils
          .verifyContainsAndGet(
              ProjectCraftEventG2Avro::class.java, ProjectCraftEventEnumAvro.CREATED, 1, false)
          .also {
            ProjectCraftTestUtil.verifyCreatedAggregate(
                it[0].aggregate, resource, projectIdentifier, testUser)
          }

      val updateProjectCraftList = repositories.findProjectCraftList(projectCraftListIdentifier)!!
      projectEventStoreUtils
          .verifyContainsAndGet(ProjectCraftListEventAvro::class.java, ITEMADDED, 1, false)
          .also {
            ProjectCraftTestUtil.verifyUpdatedAggregate(
                it[0].aggregate, projectIdentifier, updateProjectCraftList)
          }
    }

    @Test
    fun `verify create project crafts in the given position`() {
      // Add one craft in the last position ( non-null position value )
      val resourceLast = SaveProjectCraftResource("Position last with non-null", "#FFFFF", 2)
      cut.create(projectIdentifier, ProjectCraftId(), resourceLast, ETag.from("0")).body!!

      // Add one craft in the first position
      val resourceFirst = SaveProjectCraftResource("Position 1", "#FFFFF", 1)
      cut.create(projectIdentifier, ProjectCraftId(), resourceFirst, ETag.from("1")).body!!

      // Add one craft in the second position
      val resourceSecond = SaveProjectCraftResource("Position 2", "#FFFFF", 2)
      cut.create(projectIdentifier, ProjectCraftId(), resourceSecond, ETag.from("2")).body!!

      val projectCraftListResource = cut.findAll(projectIdentifier).body!!

      assertThat(projectCraftListResource.projectCrafts.size).isEqualTo(4)
      assertThat(projectCraftListResource.projectCrafts.elementAt(0).name)
          .isEqualTo(resourceFirst.name)
      assertThat(projectCraftListResource.projectCrafts.elementAt(1).name)
          .isEqualTo(resourceSecond.name)
      assertThat(projectCraftListResource.projectCrafts.last().name).isEqualTo(resourceLast.name)

      projectEventStoreUtils
          .verifyContainsAndGet(
              ProjectCraftEventG2Avro::class.java, ProjectCraftEventEnumAvro.CREATED, 3, false)
          .also {
            ProjectCraftTestUtil.verifyCreatedAggregate(
                it[0].aggregate, resourceLast, projectIdentifier, testUser)
            ProjectCraftTestUtil.verifyCreatedAggregate(
                it[1].aggregate, resourceFirst, projectIdentifier, testUser)
            ProjectCraftTestUtil.verifyCreatedAggregate(
                it[2].aggregate, resourceSecond, projectIdentifier, testUser)
          }

      val updateProjectCraftList = repositories.findProjectCraftList(projectCraftListIdentifier)!!
      projectEventStoreUtils
          .verifyContainsAndGet(ProjectCraftListEventAvro::class.java, ITEMADDED, 3, false)
          .also {
            ProjectCraftTestUtil.verifyUpdatedAggregate(
                it[2].aggregate, projectIdentifier, updateProjectCraftList)
          }
    }

    @Test
    fun `verify create project craft with wrong etag fails`() {
      val resource = SaveProjectCraftResource("New Craft", "#FFFFF")

      assertThatExceptionOfType(EntityOutdatedException::class.java)
          .isThrownBy { cut.create(projectIdentifier, ProjectCraftId(), resource, ETag.from("10")) }
          .withMessageKey(COMMON_VALIDATION_ERROR_ENTITY_OUTDATED)

      projectEventStoreUtils.verifyEmpty()
    }

    @Test
    fun `verify create project craft with an used name fails`() {
      val projectCraft = repositories.findProjectCraft(projectCraftIdentifier)!!
      val resource = SaveProjectCraftResource(projectCraft.name.uppercase(), "#FFFFF")

      assertThatExceptionOfType(PreconditionViolationException::class.java)
          .isThrownBy { cut.create(projectIdentifier, ProjectCraftId(), resource, ETag.from("0")) }
          .withMessageKey(PROJECT_CRAFT_VALIDATION_ERROR_USED_NAME)

      projectEventStoreUtils.verifyEmpty()
    }

    @Test
    fun `verify create project craft with incorrect position fails`() {
      val resource = SaveProjectCraftResource("New Craft", "#FFFFF", 10)

      assertThatExceptionOfType(PreconditionViolationException::class.java)
          .isThrownBy { cut.create(projectIdentifier, ProjectCraftId(), resource, ETag.from("0")) }
          .withMessageKey(PROJECT_CRAFT_LIST_VALIDATION_ERROR_INVALID_POSITION)

      projectEventStoreUtils.verifyEmpty()
    }

    @Test
    fun `verify create project craft with the craft list full fails`() {
      // Generate the 1000 crafts ( the first one comes from the init() )
      // This method is simpler than using the event generator
      for (index in 1 until 1000) {
        val resource = SaveProjectCraftResource("Project Craft number $index", "#FFFFF")
        cut.create(projectIdentifier, ProjectCraftId(), resource, ETag.from(index - 1L))
      }
      projectEventStoreUtils.reset()

      val finalResource = SaveProjectCraftResource("Project Craft number 1001", "#FFFFF")
      assertThatExceptionOfType(PreconditionViolationException::class.java)
          .isThrownBy {
            cut.create(projectIdentifier, ProjectCraftId(), finalResource, ETag.from("999"))
          }
          .withMessageKey(PROJECT_CRAFT_LIST_VALIDATION_ERROR_INVALID_POSITION)

      projectEventStoreUtils.verifyEmpty()
    }
  }

  @Nested
  inner class `Update project craft` {

    @Test
    fun `verify update project craft with the same name with different case`() {
      val projectCraft = repositories.findProjectCraft(projectCraftIdentifier)!!
      val resource = SaveProjectCraftResource(projectCraft.name.uppercase(), "#FFFFF")

      val projectCraftResource =
          cut.update(projectIdentifier, projectCraft.identifier, resource, ETag.from("0")).body!!

      assertThat(projectCraftResource.name).isEqualTo(projectCraft.name.uppercase())

      val updatedProjectCraft = repositories.findProjectCraft(projectCraft.identifier)!!
      projectEventStoreUtils
          .verifyContainsAndGet(
              ProjectCraftEventG2Avro::class.java, ProjectCraftEventEnumAvro.UPDATED, 1, true)
          .also {
            ProjectCraftTestUtil.verifyUpdatedAggregate(
                it[0].aggregate, updatedProjectCraft, projectIdentifier)
          }
    }

    @Test
    fun `verify update project craft with an used name fails`() {
      // Add another craft to have two with different names
      eventStreamGenerator.submitProjectCraftG2(asReference = "anotherProjectCraft") {
        it.name = "Used name"
        it.color = "#FFFFF"
      }

      val resource = SaveProjectCraftResource("Used name", "#FFFFF")

      assertThatExceptionOfType(PreconditionViolationException::class.java)
          .isThrownBy {
            cut.update(projectIdentifier, projectCraftIdentifier, resource, ETag.from("0"))
          }
          .withMessageKey(PROJECT_CRAFT_VALIDATION_ERROR_USED_NAME)

      projectEventStoreUtils.verifyEmpty()
    }
  }

  @Nested
  inner class `Reorder project craft` {

    @Test
    fun `verify reorder project craft update positions`() {
      // Add multiple project crafts to test properly test the reorder
      eventStreamGenerator
          .submitProjectCraftG2(asReference = "anotherProjectCraft1")
          .submitProjectCraftList(asReference = "projectCraftList", eventType = ITEMADDED) {
            it.projectCrafts =
                listOf(getByReference("projectCraft"), getByReference("anotherProjectCraft1"))
          }
          .submitProjectCraftG2(asReference = "anotherProjectCraft2")
          .submitProjectCraftList(asReference = "projectCraftList", eventType = ITEMADDED) {
            it.projectCrafts =
                listOf(
                    getByReference("projectCraft"),
                    getByReference("anotherProjectCraft1"),
                    getByReference("anotherProjectCraft2"))
          }
          .submitProjectCraftG2(asReference = "anotherProjectCraft3")
          .submitProjectCraftList(asReference = "projectCraftList", eventType = ITEMADDED) {
            it.projectCrafts =
                listOf(
                    getByReference("projectCraft"),
                    getByReference("anotherProjectCraft1"),
                    getByReference("anotherProjectCraft2"),
                    getByReference("anotherProjectCraft3"))
          }
          .submitProjectCraftG2(asReference = "anotherProjectCraft4")
          .submitProjectCraftList(asReference = "projectCraftList", eventType = ITEMADDED) {
            it.projectCrafts =
                listOf(
                    getByReference("projectCraft"),
                    getByReference("anotherProjectCraft1"),
                    getByReference("anotherProjectCraft2"),
                    getByReference("anotherProjectCraft3"),
                    getByReference("anotherProjectCraft4"))
          }

      projectEventStoreUtils.reset()

      val resource = ReorderProjectCraftResource(projectCraftIdentifier, 4)

      val projectCraftListResource = cut.reorder(projectIdentifier, resource, ETag.from("4")).body!!

      assertThat(projectCraftListResource.projectCrafts.size).isEqualTo(5)
      assertThat(projectCraftListResource.projectCrafts.elementAt(0).id)
          .isEqualTo(getIdentifier("anotherProjectCraft1"))
      assertThat(projectCraftListResource.projectCrafts.elementAt(1).id)
          .isEqualTo(getIdentifier("anotherProjectCraft2"))
      assertThat(projectCraftListResource.projectCrafts.elementAt(2).id)
          .isEqualTo(getIdentifier("anotherProjectCraft3"))
      assertThat(projectCraftListResource.projectCrafts.elementAt(3).id)
          .isEqualTo(getIdentifier("projectCraft"))
      assertThat(projectCraftListResource.projectCrafts.elementAt(4).id)
          .isEqualTo(getIdentifier("anotherProjectCraft4"))

      val updateProjectCraftList = repositories.findProjectCraftList(projectCraftListIdentifier)!!
      projectEventStoreUtils
          .verifyContainsAndGet(ProjectCraftListEventAvro::class.java, REORDERED, 1, true)
          .also {
            ProjectCraftTestUtil.verifyUpdatedAggregate(
                it[0].aggregate, projectIdentifier, updateProjectCraftList)
          }
    }

    @Test
    fun `verify reorder project craft for same position`() {
      val resource = ReorderProjectCraftResource(projectCraftIdentifier, 1)

      cut.reorder(projectIdentifier, resource, ETag.from("0"))

      projectEventStoreUtils.verifyEmpty()
    }

    @Test
    fun `verify reorder project craft with wrong etag fails`() {
      val resource = ReorderProjectCraftResource(projectCraftIdentifier, 1)

      assertThatExceptionOfType(EntityOutdatedException::class.java)
          .isThrownBy { cut.reorder(projectIdentifier, resource, ETag.from("10")) }
          .withMessageKey(COMMON_VALIDATION_ERROR_ENTITY_OUTDATED)

      projectEventStoreUtils.verifyEmpty()
    }

    @Test
    fun `verify reorder project craft for incorrect position fails`() {
      val resource = ReorderProjectCraftResource(projectCraftIdentifier, 2)

      assertThatExceptionOfType(PreconditionViolationException::class.java)
          .isThrownBy { cut.reorder(projectIdentifier, resource, ETag.from("0")) }
          .withMessageKey(PROJECT_CRAFT_LIST_VALIDATION_ERROR_INVALID_POSITION)

      projectEventStoreUtils.verifyEmpty()
    }
  }

  @Nested
  inner class `Delete project craft` {

    @Test
    fun `verify delete project craft with wrong etag fails`() {
      // Create an unused project craft to be deleted
      eventStreamGenerator
          .submitProjectCraftG2(asReference = "anotherProjectCraft") {
            it.name = "Another project craft"
            it.color = "#FFFFF"
          }
          .submitProjectCraftList(asReference = "projectCraftList", eventType = ITEMADDED) {
            it.projectCrafts =
                listOf(getByReference("projectCraft"), getByReference("anotherProjectCraft"))
          }

      assertThatExceptionOfType(EntityOutdatedException::class.java)
          .isThrownBy {
            cut.delete(
                projectIdentifier,
                getIdentifier("anotherProjectCraft").asProjectCraftId(),
                ETag.from("1"))
          }
          .withMessageKey(COMMON_VALIDATION_ERROR_ENTITY_OUTDATED)

      projectEventStoreUtils.verifyEmpty()
    }

    @Test
    fun `verify delete project craft used by a task fails`() {
      // Create a new task using a new project craft
      eventStreamGenerator.submitProjectCraftG2(asReference = "anotherProjectCraft").submitTask(
          asReference = "anotherTask") {
            it.name = "Task with project craft"
            it.craft = getByReference("anotherProjectCraft")
          }

      assertThatExceptionOfType(PreconditionViolationException::class.java)
          .isThrownBy {
            cut.delete(
                projectIdentifier,
                getIdentifier("anotherProjectCraft").asProjectCraftId(),
                ETag.from("0"))
          }
          .withMessageKey(PROJECT_CRAFT_VALIDATION_ERROR_CRAFT_IN_USE)

      projectEventStoreUtils.verifyEmpty()
    }

    @Test
    fun `verify delete project craft used by a milestone fails`() {
      // Create a new milestone using a new project craft
      eventStreamGenerator
          .submitProjectCraftG2(asReference = "anotherProjectCraft")
          .submitMilestone(asReference = "anotherMilestone") {
            it.name = "Milestone with project craft"
            it.craft = getByReference("anotherProjectCraft")
          }

      assertThatExceptionOfType(PreconditionViolationException::class.java)
          .isThrownBy {
            cut.delete(
                projectIdentifier,
                getIdentifier("anotherProjectCraft").asProjectCraftId(),
                ETag.from("0"))
          }
          .withMessageKey(PROJECT_CRAFT_VALIDATION_ERROR_CRAFT_IN_USE)

      projectEventStoreUtils.verifyEmpty()
    }
  }
}
