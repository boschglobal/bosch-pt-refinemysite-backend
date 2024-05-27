/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.relation.facade.rest

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMilestone
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitRelation
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractIntegrationTestV2
import com.bosch.pt.iot.smartsite.project.milestone.domain.asMilestoneId
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.Milestone
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.relation.boundary.dto.RelationDto.RelationElementDto
import com.bosch.pt.iot.smartsite.project.relation.facade.rest.resource.request.FilterRelationResource
import com.bosch.pt.iot.smartsite.project.relation.model.RelationElementTypeEnum.MILESTONE
import com.bosch.pt.iot.smartsite.project.relation.model.RelationElementTypeEnum.TASK
import com.bosch.pt.iot.smartsite.project.relation.model.RelationTypeEnum.FINISH_TO_START
import com.bosch.pt.iot.smartsite.project.task.domain.asTaskId
import com.bosch.pt.iot.smartsite.project.task.shared.model.Task
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable.unpaged

@EnableAllKafkaListeners
class RelationSearchIntegrationTest : AbstractIntegrationTestV2() {

  @Autowired private lateinit var cut: RelationSearchController

  private val project by lazy { repositories.findProject(getIdentifier("project").asProjectId())!! }

  private val task1 by lazy { repositories.findTaskWithDetails(getIdentifier("task").asTaskId())!! }
  private val task3 by lazy {
    repositories.findTaskWithDetails(getIdentifier("task3").asTaskId())!!
  }
  private val task4 by lazy {
    repositories.findTaskWithDetails(getIdentifier("task4").asTaskId())!!
  }
  private val task5 by lazy {
    repositories.findTaskWithDetails(getIdentifier("task5").asTaskId())!!
  }

  private val milestone1 by lazy {
    repositories.findMilestone(getIdentifier("milestone").asMilestoneId())!!
  }
  private val milestone2 by lazy {
    repositories.findMilestone(getIdentifier("milestone2").asMilestoneId())!!
  }

  private val task1FinishToStartMilestone1 by lazy {
    repositories.findRelation(getIdentifier("relation"), project.identifier)!!
  }
  private val task2FinishToStartMilestone1 by lazy {
    repositories.findRelation(getIdentifier("relation2"), project.identifier)!!
  }
  private val task3FinishToStartMilestone1 by lazy {
    repositories.findRelation(getIdentifier("relation3"), project.identifier)!!
  }
  private val task3FinishToStartMilestone2 by lazy {
    repositories.findRelation(getIdentifier("relation4"), project.identifier)!!
  }
  private val task4FinishToStartMilestone2 by lazy {
    repositories.findRelation(getIdentifier("relation5"), project.identifier)!!
  }
  private val milestone2FinishToStartTask5 by lazy {
    repositories.findRelation(getIdentifier("relation6"), project.identifier)!!
  }

  @BeforeEach
  fun init() {
    eventStreamGenerator
        .setupDatasetTestData()
        .submitTask(asReference = "task2")
        .submitTask(asReference = "task3")
        .submitTask(asReference = "task4")
        .submitTask(asReference = "task5")
        .submitMilestone(asReference = "milestone2")
        .submitRelation("relation2") {
          it.source = getByReference("task2")
          it.target = getByReference("milestone")
        }
        .submitRelation("relation3") {
          it.source = getByReference("task3")
          it.target = getByReference("milestone")
        }
        .submitRelation("relation4") {
          it.source = getByReference("task3")
          it.target = getByReference("milestone2")
        }
        .submitRelation("relation5") {
          it.source = getByReference("task4")
          it.target = getByReference("milestone2")
        }
        .submitRelation("relation6") {
          it.source = getByReference("milestone2")
          it.target = getByReference("task5")
        }

    setAuthentication(getIdentifier("userCsm2"))
    projectEventStoreUtils.reset()
  }

  @Test
  fun `verify search all`() {
    val filterResource = FilterRelationResource()

    val resource = cut.search(project.identifier, filterResource, unpaged()).body!!.items

    assertThat(resource)
        .extracting<UUID> { it.identifier }
        .containsExactlyInAnyOrder(
            task1FinishToStartMilestone1.identifier,
            task2FinishToStartMilestone1.identifier,
            task3FinishToStartMilestone1.identifier,
            task3FinishToStartMilestone2.identifier,
            task4FinishToStartMilestone2.identifier,
            milestone2FinishToStartTask5.identifier)
  }

  @Test
  fun `verify search predecessors of milestone`() {
    val filterResource =
        FilterRelationResource(
            types = setOf(FINISH_TO_START), targets = setOf(milestone1.toRelationElementDto()))

    val resource = cut.search(project.identifier, filterResource, unpaged()).body!!.items

    assertThat(resource)
        .extracting<UUID> { it.identifier }
        .containsExactlyInAnyOrder(
            task1FinishToStartMilestone1.identifier,
            task2FinishToStartMilestone1.identifier,
            task3FinishToStartMilestone1.identifier)
  }

  @Test
  fun `verify search successors of task`() {
    val filterResource =
        FilterRelationResource(
            types = setOf(FINISH_TO_START), sources = setOf(task3.toRelationElementDto()))

    val resource = cut.search(project.identifier, filterResource, unpaged()).body!!.items

    assertThat(resource)
        .extracting<UUID> { it.identifier }
        .containsExactlyInAnyOrder(
            task3FinishToStartMilestone1.identifier, task3FinishToStartMilestone2.identifier)
  }

  @Test
  fun `verify search successors of multiple tasks`() {
    val filterResource =
        FilterRelationResource(
            types = setOf(FINISH_TO_START),
            sources = setOf(task1.toRelationElementDto(), task4.toRelationElementDto()))

    val resource = cut.search(project.identifier, filterResource, unpaged()).body!!.items

    assertThat(resource)
        .extracting<UUID> { it.identifier }
        .containsExactlyInAnyOrder(
            task1FinishToStartMilestone1.identifier, task4FinishToStartMilestone2.identifier)
  }

  @Test
  fun `verify search predecessors and successors of milestone`() {
    val filterResource =
        FilterRelationResource(
            types = setOf(FINISH_TO_START),
            sources = setOf(milestone2.toRelationElementDto()),
            targets = setOf(milestone2.toRelationElementDto()))

    val resource = cut.search(project.identifier, filterResource, unpaged()).body!!.items

    assertThat(resource)
        .extracting<UUID> { it.identifier }
        .containsExactlyInAnyOrder(
            task3FinishToStartMilestone2.identifier,
            task4FinishToStartMilestone2.identifier,
            milestone2FinishToStartTask5.identifier)
  }

  @Test
  fun `verify search succeeds for a duplicate source`() {
    val filterResource =
        FilterRelationResource(
            types = setOf(FINISH_TO_START),
            sources = setOf(milestone2.toRelationElementDto(), milestone2.toRelationElementDto()))

    val resource = cut.search(project.identifier, filterResource, unpaged()).body!!.items

    assertThat(resource)
        .extracting<UUID> { it.identifier }
        .containsExactly(milestone2FinishToStartTask5.identifier)
  }

  @Test
  fun `verify search succeeds for a duplicate target`() {
    val filterResource =
        FilterRelationResource(
            types = setOf(FINISH_TO_START),
            targets = listOf(task5.toRelationElementDto(), task5.toRelationElementDto()))

    val resource = cut.search(project.identifier, filterResource, unpaged()).body!!.items

    assertThat(resource)
        .extracting<UUID> { it.identifier }
        .containsExactly(milestone2FinishToStartTask5.identifier)
  }

  @Test
  fun `verify search results order is stable`() {
    val filterResource = FilterRelationResource()

    val resource =
        cut.search(project.identifier, filterResource, PageRequest.of(0, 10)).body!!.items

    val orderedIdentifiers =
        listOf(
                task1FinishToStartMilestone1,
                task2FinishToStartMilestone1,
                task3FinishToStartMilestone1,
                task3FinishToStartMilestone2,
                task4FinishToStartMilestone2,
                milestone2FinishToStartTask5)
            .map { it.identifier }
            .sortedBy { it.toString() }

    assertThat(resource)
        .extracting<UUID> { it.identifier }
        .containsExactly(*orderedIdentifiers.toTypedArray())
  }

  @Test
  fun `verify empty search result`() {
    eventStreamGenerator.submitTask(asReference = "taskWithoutRelation")

    val taskWithoutRelation =
        repositories.findTaskWithDetails(getIdentifier("taskWithoutRelation").asTaskId())!!
    val filterResource =
        FilterRelationResource(sources = listOf(taskWithoutRelation.toRelationElementDto()))

    val resource = cut.search(project.identifier, filterResource, unpaged()).body!!.items

    assertThat(resource).isEmpty()
  }

  private fun Task.toRelationElementDto() = RelationElementDto(identifier.toUuid(), TASK)

  private fun Milestone.toRelationElementDto() = RelationElementDto(identifier.toUuid(), MILESTONE)
}
