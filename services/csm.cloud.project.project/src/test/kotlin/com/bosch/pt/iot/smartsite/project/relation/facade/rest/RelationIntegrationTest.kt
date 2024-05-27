/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.relation.facade.rest

import com.bosch.pt.csm.cloud.common.command.exceptions.EntityOutdatedException
import com.bosch.pt.csm.cloud.common.exceptions.AggregateNotFoundException
import com.bosch.pt.csm.cloud.common.extensions.toEpochMilli
import com.bosch.pt.csm.cloud.common.facade.rest.etag.ETag
import com.bosch.pt.csm.cloud.common.facade.rest.resource.request.CreateBatchRequestResource
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.common.test.randomString
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMilestone
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskSchedule
import com.bosch.pt.csm.cloud.projectmanagement.relation.messages.RelationEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.relation.messages.RelationEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.projectmanagement.relation.messages.RelationEventEnumAvro.DELETED
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.exceptions.DuplicateEntityException
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractDeleteIntegrationTest
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.etag.from
import com.bosch.pt.iot.smartsite.common.i18n.Key.COMMON_VALIDATION_ERROR_ENTITY_DUPLICATED
import com.bosch.pt.iot.smartsite.common.i18n.Key.RELATION_VALIDATION_ERROR_NOT_FOUND
import com.bosch.pt.iot.smartsite.project.milestone.domain.asMilestoneId
import com.bosch.pt.iot.smartsite.project.milestone.facade.rest.MilestoneController
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.Milestone
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.relation.boundary.dto.RelationDto.RelationElementDto
import com.bosch.pt.iot.smartsite.project.relation.facade.rest.resource.request.CreateRelationResource
import com.bosch.pt.iot.smartsite.project.relation.model.RelationElementTypeEnum.MILESTONE
import com.bosch.pt.iot.smartsite.project.relation.model.RelationElementTypeEnum.TASK
import com.bosch.pt.iot.smartsite.project.relation.model.RelationTypeEnum.FINISH_TO_START
import com.bosch.pt.iot.smartsite.project.relation.model.RelationTypeEnum.PART_OF
import com.bosch.pt.iot.smartsite.project.task.domain.asTaskId
import com.bosch.pt.iot.smartsite.project.task.shared.model.Task
import com.bosch.pt.iot.smartsite.util.withMessageKey
import java.time.LocalDate
import java.util.UUID.randomUUID
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@EnableAllKafkaListeners
class RelationIntegrationTest : AbstractDeleteIntegrationTest() {

  @Autowired private lateinit var cut: RelationController

  @Autowired private lateinit var milestoneController: MilestoneController

  private val userCsm by lazy { repositories.findUser(getIdentifier("userCsm1"))!! }
  private val project by lazy { repositories.findProject(getIdentifier("project").asProjectId())!! }
  private val task1 by lazy { repositories.findTaskWithDetails(getIdentifier("task").asTaskId())!! }
  private val task2 by lazy {
    repositories.findTaskWithDetails(getIdentifier("task2").asTaskId())!!
  }
  private val milestone1 by lazy {
    repositories.findMilestone(getIdentifier("milestone").asMilestoneId())!!
  }
  private val milestone2 by lazy {
    repositories.findMilestone(getIdentifier("milestone2").asMilestoneId())!!
  }
  private val relationIdentifier by lazy { getIdentifier("relation") }

  @BeforeEach
  fun init() {
    eventStreamGenerator
        .setupDatasetTestData()
        .submitTask(asReference = "task2")
        .submitMilestone(asReference = "milestone2")
        .submitMilestone(asReference = "milestone2")

    setAuthentication(getIdentifier("userCsm2"))
    projectEventStoreUtils.reset()
  }

  @Test
  fun `verify create batch successfully for finish-to-start relations`() {
    eventStreamGenerator.submitTaskSchedule("t2s") { it.task = getByReference("task2") }

    val createResource =
        CreateBatchRequestResource(
            setOf(
                CreateRelationResource(
                    type = FINISH_TO_START,
                    source = task1.toRelationElementDto(),
                    target = task2.toRelationElementDto()),
                CreateRelationResource(
                    type = FINISH_TO_START,
                    source = task2.toRelationElementDto(),
                    target = milestone2.toRelationElementDto()),
                CreateRelationResource(
                    type = FINISH_TO_START,
                    source = milestone1.toRelationElementDto(),
                    target = milestone2.toRelationElementDto())))

    assertThat(cut.createBatch(project.identifier, createResource).body!!.items)
        .hasSize(createResource.items.size)

    projectEventStoreUtils.verifyContains(RelationEventAvro::class.java, CREATED, 3, false)
  }

  @Test
  fun `verify create batch successfully for part-of relations`() {
    val createResource =
        CreateBatchRequestResource(
            setOf(
                CreateRelationResource(
                    type = PART_OF,
                    source = task1.toRelationElementDto(),
                    target = milestone2.toRelationElementDto()),
                CreateRelationResource(
                    type = PART_OF,
                    source = task2.toRelationElementDto(),
                    target = milestone2.toRelationElementDto())))

    assertThat(cut.createBatch(project.identifier, createResource).body!!.items)
        .hasSize(createResource.items.size)

    projectEventStoreUtils.verifyContains(RelationEventAvro::class.java, CREATED, 2, false)
  }

  @Test
  fun `verify create batch fails if relation task element cannot be found`() {
    val createResource =
        CreateBatchRequestResource(
            setOf(
                CreateRelationResource(
                    type = FINISH_TO_START,
                    source = task1.toRelationElementDto(),
                    target = task2.toRelationElementDto()),
                CreateRelationResource(
                    type = FINISH_TO_START,
                    source = task1.toRelationElementDto(),
                    target = RelationElementDto(randomUUID(), MILESTONE))))

    assertThatExceptionOfType(AggregateNotFoundException::class.java)
        .isThrownBy { cut.createBatch(project.identifier, createResource).body!!.items }
        .withMessageKey(RELATION_VALIDATION_ERROR_NOT_FOUND)

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify create batch fails if relation milestone element cannot be found`() {
    val createResource =
        CreateBatchRequestResource(
            setOf(
                CreateRelationResource(
                    type = FINISH_TO_START,
                    source = RelationElementDto(randomUUID(), TASK),
                    target = task2.toRelationElementDto()),
                CreateRelationResource(
                    type = FINISH_TO_START,
                    source = task1.toRelationElementDto(),
                    target = milestone2.toRelationElementDto())))

    assertThatExceptionOfType(AggregateNotFoundException::class.java)
        .isThrownBy { cut.createBatch(project.identifier, createResource).body!!.items }
        .withMessageKey(RELATION_VALIDATION_ERROR_NOT_FOUND)

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify task cannot be part-of a task`() {
    val createResource =
        CreateRelationResource(
            type = PART_OF,
            source = task1.toRelationElementDto(),
            target = task2.toRelationElementDto())

    assertThatThrownBy { cut.create(project.identifier, createResource) }
        .isInstanceOf(IllegalArgumentException::class.java)

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify milestone cannot be part-of a task`() {
    val createResource =
        CreateRelationResource(
            type = PART_OF,
            source = milestone1.toRelationElementDto(),
            target = task2.toRelationElementDto())

    assertThatThrownBy { cut.create(project.identifier, createResource) }
        .isInstanceOf(IllegalArgumentException::class.java)

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify milestone cannot be part-of a milestone`() {
    val createResource =
        CreateRelationResource(
            type = PART_OF,
            source = milestone1.toRelationElementDto(),
            target = milestone2.toRelationElementDto())

    assertThatThrownBy { cut.create(project.identifier, createResource) }
        .isInstanceOf(IllegalArgumentException::class.java)

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify task cannot be related to itself`() {
    val createResource =
        CreateRelationResource(
            type = PART_OF,
            source = task1.toRelationElementDto(),
            target = task1.toRelationElementDto())

    assertThatThrownBy { cut.create(project.identifier, createResource) }
        .isInstanceOf(IllegalArgumentException::class.java)

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify milestone cannot be related to itself`() {
    val createResource =
        CreateRelationResource(
            type = PART_OF,
            source = milestone1.toRelationElementDto(),
            target = milestone1.toRelationElementDto())

    assertThatThrownBy { cut.create(project.identifier, createResource) }
        .isInstanceOf(IllegalArgumentException::class.java)

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify no duplicate relations can be created`() {
    val createResource =
        CreateRelationResource(
            type = PART_OF,
            source = task1.toRelationElementDto(),
            target = milestone1.toRelationElementDto())

    cut.create(project.identifier, createResource)

    projectEventStoreUtils.verifyContains(RelationEventAvro::class.java, CREATED, 1, true)
    projectEventStoreUtils.reset()

    assertThatThrownBy { cut.create(project.identifier, createResource) }
        .usingRecursiveComparison()
        .withStrictTypeChecking()
        .isEqualTo(DuplicateEntityException(COMMON_VALIDATION_ERROR_ENTITY_DUPLICATED))

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify create finish-to-start relation fails for missing task schedule`() {
    val createResource =
        CreateRelationResource(
            type = FINISH_TO_START,
            source = task2.toRelationElementDto(),
            target = milestone1.toRelationElementDto())

    assertThatThrownBy { cut.create(project.identifier, createResource) }
        .isInstanceOf(IllegalArgumentException::class.java)

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify create finish-to-start relation fails for task schedule without start date`() {
    val task = createTaskWithSchedule(start = null, end = LocalDate.now())
    val createResource =
        CreateRelationResource(
            type = FINISH_TO_START,
            source = task.toRelationElementDto(),
            target = milestone1.toRelationElementDto())

    assertThatThrownBy { cut.create(project.identifier, createResource) }
        .isInstanceOf(IllegalArgumentException::class.java)

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify create finish-to-start relation fails for task schedule without end date`() {
    val task = createTaskWithSchedule(start = LocalDate.now(), end = null)
    val createResource =
        CreateRelationResource(
            type = FINISH_TO_START,
            source = task.toRelationElementDto(),
            target = milestone1.toRelationElementDto())

    assertThatThrownBy { cut.create(project.identifier, createResource) }
        .isInstanceOf(IllegalArgumentException::class.java)

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify create finish-to-start relation fails for task schedule without dates`() {
    val task = createTaskWithSchedule(start = null, end = null)
    val createResource =
        CreateRelationResource(
            type = FINISH_TO_START,
            source = task.toRelationElementDto(),
            target = milestone1.toRelationElementDto())

    assertThatThrownBy { cut.create(project.identifier, createResource) }
        .isInstanceOf(IllegalArgumentException::class.java)

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify delete without ETag succeeds`() {
    cut.delete(project.identifier, relationIdentifier)

    projectEventStoreUtils.verifyContainsAndGet(RelationEventAvro::class.java, DELETED, 1, true)

    assertThatExceptionOfType(AggregateNotFoundException::class.java)
        .isThrownBy { cut.find(project.identifier, relationIdentifier) }
        .withMessageKey(RELATION_VALIDATION_ERROR_NOT_FOUND)
  }

  @Test
  fun `verify delete with wrong ETag fails`() {
    assertThatThrownBy { cut.delete(project.identifier, relationIdentifier, ETag.from(1)) }
        .isInstanceOf(EntityOutdatedException::class.java)

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify relation is deleted when a referenced milestone is deleted`() {
    milestoneController.delete(milestone1.identifier, ETag.from(milestone1.version))

    projectEventStoreUtils.verifyContainsAndGet(RelationEventAvro::class.java, DELETED, 1, false)

    assertThatExceptionOfType(AggregateNotFoundException::class.java)
        .isThrownBy { cut.find(project.identifier, relationIdentifier) }
        .withMessageKey(RELATION_VALIDATION_ERROR_NOT_FOUND)
  }

  @Test
  fun `verify relation is deleted when a referenced task is deleted`() {
    sendDeleteCommand(
        task1.identifier.toUuid(), task1.version, ProjectmanagementAggregateTypeEnum.TASK, userCsm)

    projectEventStoreUtils.verifyContainsAndGet(RelationEventAvro::class.java, DELETED, 1, false)

    assertThatExceptionOfType(AggregateNotFoundException::class.java)
        .isThrownBy { cut.find(project.identifier, relationIdentifier) }
        .withMessageKey(RELATION_VALIDATION_ERROR_NOT_FOUND)
  }

  private fun createTaskWithSchedule(start: LocalDate?, end: LocalDate?): Task {
    val reference = randomString()
    eventStreamGenerator.submitTask(asReference = reference).submitTaskSchedule {
      it.start = start?.toEpochMilli()
      it.end = end?.toEpochMilli()
    }

    return repositories.findTaskWithDetails(getIdentifier(reference).asTaskId())!!
  }

  private fun Task.toRelationElementDto() = RelationElementDto(identifier.toUuid(), TASK)

  private fun Milestone.toRelationElementDto() = RelationElementDto(identifier.toUuid(), MILESTONE)
}
