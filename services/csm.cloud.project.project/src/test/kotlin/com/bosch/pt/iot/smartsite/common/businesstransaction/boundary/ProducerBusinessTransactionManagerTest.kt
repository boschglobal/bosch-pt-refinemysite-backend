/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.common.businesstransaction.boundary

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.common.test.randomString
import com.bosch.pt.csm.cloud.common.transaction.messages.BatchOperationFinishedEventAvro
import com.bosch.pt.csm.cloud.common.transaction.messages.BatchOperationStartedEventAvro
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitCompany
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitEmployee
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeRoleEnumAvro.CSM
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitSystemUserAndActivate
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.businesstransaction.BusinessTransactionContextHolder
import com.bosch.pt.iot.smartsite.common.businesstransaction.BusinessTransactionContextHolder.isBusinessTransactionActive
import com.bosch.pt.iot.smartsite.common.businesstransaction.boundary.BusinessTransactionPropagation.REQUIRED
import com.bosch.pt.iot.smartsite.common.businesstransaction.boundary.BusinessTransactionPropagation.REQUIRES_NEW
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractIntegrationTestV2
import com.bosch.pt.iot.smartsite.project.businesstransaction.event.BatchOperationFinishedEvent
import com.bosch.pt.iot.smartsite.project.businesstransaction.event.BatchOperationStartedEvent
import com.bosch.pt.iot.smartsite.project.eventstore.repository.ProjectKafkaEventTestRepository
import com.bosch.pt.iot.smartsite.project.project.ProjectAddressVo
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.project.command.api.CreateProjectCommand
import com.bosch.pt.iot.smartsite.project.project.command.handler.CreateProjectCommandHandler
import com.bosch.pt.iot.smartsite.project.project.shared.model.ProjectCategoryEnum
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.asProjectCraftId
import com.bosch.pt.iot.smartsite.project.task.command.api.CreateTaskCommand
import com.bosch.pt.iot.smartsite.project.task.command.handler.CreateTaskCommandHandler
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum
import java.time.LocalDate
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@EnableAllKafkaListeners
internal class ProducerBusinessTransactionManagerTest : AbstractIntegrationTestV2() {

  @Autowired private lateinit var cut: ProducerBusinessTransactionManager
  @Autowired private lateinit var createTaskCommandHandler: CreateTaskCommandHandler
  @Autowired private lateinit var createProjectCommandHandler: CreateProjectCommandHandler
  @Autowired private lateinit var kafkaEventRepository: ProjectKafkaEventTestRepository

  private val projectIdentifier by lazy { getIdentifier("project").asProjectId() }

  private val startedEvent by lazy { BatchOperationStartedEvent(projectIdentifier) }
  private val finishedEvent by lazy { BatchOperationFinishedEvent(projectIdentifier) }

  @BeforeEach
  fun setup() {
    eventStreamGenerator
        .submitSystemUserAndActivate()
        .submitCompany()
        .submitUser(asReference = "userCsm")
        .submitEmployee { it.roles = listOf(CSM) }
        .submitProject()
        .submitParticipantG3()
        .submitProjectCraftG2()

    setAuthentication(getIdentifier("userCsm"))
    projectEventStoreUtils.reset()
  }

  @Test
  fun `verify events in business transaction are in correct order`() {
    transactionTemplate.execute {
      cut.doInBusinessTransaction(startedEvent, finishedEvent) { createTask() }
    }

    projectEventStoreUtils.verifyContainsInSequence(
        listOf(
            BatchOperationStartedEventAvro::class.java,
            TaskEventAvro::class.java,
            BatchOperationFinishedEventAvro::class.java))
  }

  @Test
  fun `verify events with outer and inner business transaction are in correct order`() {
    transactionTemplate.execute {
      cut.doInBusinessTransaction(startedEvent, finishedEvent) {
        createTask("outer1")
        cut.doInBusinessTransaction(startedEvent, finishedEvent) { createTask("inner") }
        createTask("outer2")
      }
    }

    projectEventStoreUtils.verifyContainsInSequence(
        listOf(
            BatchOperationStartedEventAvro::class.java,
            TaskEventAvro::class.java,
            TaskEventAvro::class.java,
            TaskEventAvro::class.java,
            BatchOperationFinishedEventAvro::class.java))

    projectEventStoreUtils.verifyContainsAndGet(TaskEventAvro::class.java, CREATED, 3, false).also {
      assertThat(it[0].aggregate.name).isEqualTo("outer1")
      assertThat(it[1].aggregate.name).isEqualTo("inner")
      assertThat(it[2].aggregate.name).isEqualTo("outer2")
    }
  }

  @Test
  fun `verify Architecture 1 events in business transaction have transaction id`() {
    transactionTemplate.execute {
      cut.doInBusinessTransaction(startedEvent, finishedEvent) { createTask() }
    }

    assertThat(kafkaEventRepository.findAll())
        .hasSize(3)
        .extracting<UUID?> { it.transactionIdentifier }
        .doesNotContainNull()
  }

  // SMAR-18416: Fix business transaction identifier not sent for Arch 2.0 events
  @Test
  fun `verify Architecture 2 events in business transaction have transaction id`() {
    transactionTemplate.execute {
      cut.doInBusinessTransaction(startedEvent, finishedEvent) { createProject() }
    }

    assertThat(kafkaEventRepository.findAll())
        // project event listeners could create additional events
        .hasSizeGreaterThanOrEqualTo(3)
        .extracting<UUID?> { it.transactionIdentifier }
        .doesNotContainNull()
  }

  @Test
  fun `verify two business transactions in different database transactions are in correct order`() {
    transactionTemplate.execute {
      cut.doInBusinessTransaction(startedEvent, finishedEvent) { createTask("first") }
    }
    transactionTemplate.execute {
      cut.doInBusinessTransaction(startedEvent, finishedEvent) { createTask("second") }
    }

    projectEventStoreUtils.verifyContainsInSequence(
        listOf(
            BatchOperationStartedEventAvro::class.java,
            TaskEventAvro::class.java,
            BatchOperationFinishedEventAvro::class.java,
            BatchOperationStartedEventAvro::class.java,
            TaskEventAvro::class.java,
            BatchOperationFinishedEventAvro::class.java))

    projectEventStoreUtils.verifyContainsAndGet(TaskEventAvro::class.java, CREATED, 2, false).also {
      assertThat(it[0].aggregate.name).isEqualTo("first")
      assertThat(it[1].aggregate.name).isEqualTo("second")
    }
  }

  @Test
  fun `verify two business transactions in different database transactions have different transaction ids`() {
    transactionTemplate.execute {
      cut.doInBusinessTransaction(startedEvent, finishedEvent) { createTask() }
    }
    transactionTemplate.execute {
      cut.doInBusinessTransaction(startedEvent, finishedEvent) { createTask() }
    }

    val events = kafkaEventRepository.findAll().sortedBy { it.id }
    val eventsOfFirst = events.take(3)
    val eventsOfSecond = events.takeLast(3)

    val transactionIdOfFirst = eventsOfFirst.map { it.transactionIdentifier }.distinct()
    val transactionIdOfSecond = eventsOfSecond.map { it.transactionIdentifier }.distinct()

    assertThat(events).hasSize(6)
    assertThat(transactionIdOfFirst).hasSize(1).isNotNull
    assertThat(transactionIdOfSecond).hasSize(1).isNotNull
    assertThat(transactionIdOfFirst.single()).isNotEqualTo(transactionIdOfSecond.single())
  }

  @Test
  fun `verify two business transactions in same database transaction are in correct order`() {
    transactionTemplate.execute {
      cut.doInBusinessTransaction(startedEvent, finishedEvent) { createTask("first") }
      cut.doInBusinessTransaction(startedEvent, finishedEvent) { createTask("second") }
    }

    projectEventStoreUtils.verifyContainsInSequence(
        listOf(
            BatchOperationStartedEventAvro::class.java,
            TaskEventAvro::class.java,
            BatchOperationFinishedEventAvro::class.java,
            BatchOperationStartedEventAvro::class.java,
            TaskEventAvro::class.java,
            BatchOperationFinishedEventAvro::class.java))

    projectEventStoreUtils.verifyContainsAndGet(TaskEventAvro::class.java, CREATED, 2, false).also {
      assertThat(it[0].aggregate.name).isEqualTo("first")
      assertThat(it[1].aggregate.name).isEqualTo("second")
    }
  }

  @Test
  fun `verify two business transactions in same database transaction have different transaction ids`() {
    transactionTemplate.execute {
      cut.doInBusinessTransaction(startedEvent, finishedEvent) { createTask() }
      cut.doInBusinessTransaction(startedEvent, finishedEvent) { createTask() }
    }

    val events = kafkaEventRepository.findAll().sortedBy { it.id }
    val eventsOfFirst = events.take(3)
    val eventsOfSecond = events.takeLast(3)

    val transactionIdOfFirst = eventsOfFirst.map { it.transactionIdentifier }.distinct()
    val transactionIdOfSecond = eventsOfSecond.map { it.transactionIdentifier }.distinct()

    assertThat(events).hasSize(6)
    assertThat(transactionIdOfFirst).hasSize(1).isNotNull
    assertThat(transactionIdOfSecond).hasSize(1).isNotNull
    assertThat(transactionIdOfFirst.single()).isNotEqualTo(transactionIdOfSecond.single())
  }

  @Test
  fun `verify business transaction is no longer active after exception`() {
    assertThrows(IllegalStateException::class.java) {
      transactionTemplate.execute {
        cut.doInBusinessTransaction(startedEvent, finishedEvent) { throw IllegalStateException() }
      }
    }
    assertThat(isBusinessTransactionActive()).isFalse
  }

  @Test
  fun `verify starting business transaction when another one is still active fails for propagation REQUIRES_NEW`() {
    assertThrows(IllegalStateException::class.java) {
      transactionTemplate.execute {
        cut.startTransaction(startedEvent)
        cut.startTransaction(startedEvent, propagation = REQUIRES_NEW)
      }
    }
    // clean up to avoid starting the next test with an active business transaction
    BusinessTransactionContextHolder.closeContext()
  }

  @Test
  fun `verify starting new business transaction succeeds for propagation REQUIRES_NEW`() {
    transactionTemplate.execute { cut.startTransaction(startedEvent, propagation = REQUIRES_NEW) }

    assertThat(isBusinessTransactionActive()).isTrue

    // clean up to avoid starting the next test with an active business transaction
    BusinessTransactionContextHolder.closeContext()
  }

  @Test
  fun `verify starting business transaction when another one is still active succeeds for propagation REQUIRED`() {
    transactionTemplate.execute {
      cut.startTransaction(startedEvent)
      cut.startTransaction(startedEvent, propagation = REQUIRED)
    }
    assertThat(isBusinessTransactionActive()).isTrue

    // clean up to avoid starting the next test with an active business transaction
    BusinessTransactionContextHolder.closeContext()
    BusinessTransactionContextHolder.closeContext()
  }

  @Test
  fun `verify business transaction is still active after finishing inner business transaction`() {
    transactionTemplate.execute {
      cut.startTransaction(startedEvent)
      cut.startTransaction(startedEvent)
      cut.finishTransaction(finishedEvent)
    }
    assertThat(isBusinessTransactionActive()).isTrue

    // clean up to avoid starting the next test with an active business transaction
    BusinessTransactionContextHolder.closeContext()
  }

  @Test
  fun `verify business transacrtion is no longer active after finishing outermost business transaction`() {
    transactionTemplate.execute {
      cut.startTransaction(startedEvent)
      cut.startTransaction(startedEvent)
      cut.finishTransaction(finishedEvent)
      cut.finishTransaction(finishedEvent)
    }
    assertThat(isBusinessTransactionActive()).isFalse
  }

  @Test
  fun `verify finishing business transaction fails when none exists yet`() {
    assertThrows(IllegalStateException::class.java) {
      transactionTemplate.execute { cut.finishTransaction(startedEvent) }
    }
  }

  @Test
  fun `verify starting business transaction fails if event is not a started event`() {
    assertThrows(IllegalArgumentException::class.java) {
      transactionTemplate.execute { cut.startTransaction(finishedEvent) }
    }

    // clean up to avoid starting the next test with an active business transaction
    BusinessTransactionContextHolder.closeContext()
  }

  @Test
  fun `verify finishing business transaction fails if event is not a finished event`() {
    assertThrows(IllegalArgumentException::class.java) {
      transactionTemplate.execute {
        cut.startTransaction(startedEvent)
        cut.finishTransaction(startedEvent)
      }
    }

    // clean up to avoid starting the next test with an active business transaction
    BusinessTransactionContextHolder.closeContext()
  }

  private fun createTask(name: String = randomString()) =
      createTaskCommandHandler.handle(
          CreateTaskCommand(
              identifier = TaskId(),
              projectIdentifier = projectIdentifier,
              name = name,
              description = randomString(),
              location = randomString(),
              projectCraftIdentifier = getIdentifier("projectCraft").asProjectCraftId(),
              assigneeIdentifier = null,
              workAreaIdentifier = null,
              status = TaskStatusEnum.DRAFT))

  private fun createProject(name: String = randomString()) =
      createProjectCommandHandler.handle(
          CreateProjectCommand(
              ProjectId(),
              null,
              null,
              LocalDate.now(),
              LocalDate.now(),
              randomString(),
              name,
              ProjectCategoryEnum.NB,
              ProjectAddressVo(
                  randomString(),
                  randomString().substring((0..9)),
                  randomString(),
                  randomString().substring((0..9)))))
}
