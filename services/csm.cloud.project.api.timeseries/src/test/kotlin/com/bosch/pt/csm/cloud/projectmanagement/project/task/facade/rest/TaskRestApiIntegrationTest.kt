/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.task.facade.rest

import com.bosch.pt.csm.cloud.common.extensions.toEpochMilli
import com.bosch.pt.csm.cloud.common.extensions.toLocalDateTimeByMillis
import com.bosch.pt.csm.cloud.projectmanagement.application.RmsSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractRestApiIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.task.domain.asTaskId
import com.bosch.pt.csm.cloud.projectmanagement.project.task.extension.asStatus
import com.bosch.pt.csm.cloud.projectmanagement.project.task.facade.rest.resource.response.TaskListResource
import com.bosch.pt.csm.cloud.projectmanagement.task.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.test.eventTimestamp
import java.time.LocalDateTime
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@RmsSpringBootTest
class TaskRestApiIntegrationTest : AbstractRestApiIntegrationTest() {

  lateinit var aggregateV0: TaskAggregateAvro

  lateinit var aggregateV1: TaskAggregateAvro

  @BeforeEach
  fun init() {
    setAuthentication("csm-user")
  }

  @Test
  fun `query task with all parameters set`() {
    submitEvents(true)

    // Execute query
    val taskList = query(false)

    // Validate payload
    assertThat(taskList.tasks).hasSize(2)

    val taskV0 = taskList.tasks[0]
    assertThat(taskV0.id).isEqualTo(aggregateV0.getIdentifier().asTaskId())
    assertThat(taskV0.version).isEqualTo(aggregateV0.aggregateIdentifier.version)
    assertThat(taskV0.name).isEqualTo(aggregateV0.name)
    assertThat(taskV0.description).isEqualTo(aggregateV0.description)
    assertThat(taskV0.location).isEqualTo(aggregateV0.location)
    assertThat(taskV0.status).isEqualTo(aggregateV0.status.asStatus().key)
    assertThat(taskV0.editDate).isEqualTo(aggregateV0.editDate.toLocalDateTimeByMillis())
    assertThat(taskV0.eventTimestamp).isEqualTo(aggregateV0.eventTimestamp())
    assertThat(taskV0.deleted).isFalse()

    val taskV1 = taskList.tasks[1]
    assertThat(taskV1.id).isEqualTo(aggregateV1.getIdentifier().asTaskId())
    assertThat(taskV1.version).isEqualTo(aggregateV1.aggregateIdentifier.version)
    assertThat(taskV1.name).isEqualTo(aggregateV1.name)
    assertThat(taskV1.description).isEqualTo(aggregateV1.description)
    assertThat(taskV1.location).isEqualTo(aggregateV1.location)
    assertThat(taskV1.status).isEqualTo(aggregateV1.status.asStatus().key)
    assertThat(taskV1.editDate).isEqualTo(aggregateV1.editDate.toLocalDateTimeByMillis())
    assertThat(taskV1.eventTimestamp).isEqualTo(aggregateV1.eventTimestamp())
    assertThat(taskV1.deleted).isFalse()
  }

  @Test
  fun `query task with all parameters latest only`() {
    submitEvents(true)

    // Execute query
    val taskList = query(true)

    // Validate payload
    assertThat(taskList.tasks).hasSize(1)
    val taskV1 = taskList.tasks.first()

    assertThat(taskV1.id).isEqualTo(aggregateV1.getIdentifier().asTaskId())
    assertThat(taskV1.version).isEqualTo(aggregateV1.aggregateIdentifier.version)
    assertThat(taskV1.name).isEqualTo(aggregateV1.name)
    assertThat(taskV1.description).isEqualTo(aggregateV1.description)
    assertThat(taskV1.location).isEqualTo(aggregateV1.location)
    assertThat(taskV1.status).isEqualTo(aggregateV1.status.asStatus().key)
    assertThat(taskV1.editDate).isEqualTo(aggregateV1.editDate.toLocalDateTimeByMillis())
    assertThat(taskV1.eventTimestamp).isEqualTo(aggregateV1.eventTimestamp())
    assertThat(taskV1.deleted).isFalse()
  }

  @Test
  fun `query task without optional parameters`() {
    submitEvents(false)

    // Execute query
    val taskList = query(false)

    // Validate payload
    assertThat(taskList.tasks).hasSize(2)

    val taskV0 = taskList.tasks[0]
    assertThat(taskV0.id).isEqualTo(aggregateV0.getIdentifier().asTaskId())
    assertThat(taskV0.version).isEqualTo(aggregateV0.aggregateIdentifier.version)
    assertThat(taskV0.name).isEqualTo(aggregateV0.name)
    assertThat(taskV0.status).isEqualTo(aggregateV0.status.asStatus().key)
    assertThat(taskV0.eventTimestamp).isEqualTo(aggregateV0.eventTimestamp())
    assertThat(taskV0.deleted).isFalse()

    assertThat(taskV0.description).isNull()
    assertThat(taskV0.location).isNull()
    assertThat(taskV0.editDate).isNull()

    val taskV1 = taskList.tasks[1]
    assertThat(taskV1.id).isEqualTo(aggregateV1.getIdentifier().asTaskId())
    assertThat(taskV1.version).isEqualTo(aggregateV1.aggregateIdentifier.version)
    assertThat(taskV1.name).isEqualTo(aggregateV1.name)
    assertThat(taskV1.status).isEqualTo(aggregateV1.status.asStatus().key)
    assertThat(taskV1.eventTimestamp).isEqualTo(aggregateV1.eventTimestamp())
    assertThat(taskV1.deleted).isFalse()

    assertThat(taskV1.description).isNull()
    assertThat(taskV1.location).isNull()
    assertThat(taskV1.editDate).isNull()
  }

  @Test
  fun `query task without optional parameters latest only`() {
    submitEvents(false)

    // Execute query
    val taskList = query(true)

    // Validate mandatory fields
    assertThat(taskList.tasks).hasSize(1)
    val taskV1 = taskList.tasks.first()

    assertThat(taskV1.id).isEqualTo(aggregateV1.getIdentifier().asTaskId())
    assertThat(taskV1.version).isEqualTo(aggregateV1.aggregateIdentifier.version)
    assertThat(taskV1.name).isEqualTo(aggregateV1.name)
    assertThat(taskV1.status).isEqualTo(aggregateV1.status.asStatus().key)
    assertThat(taskV1.eventTimestamp).isEqualTo(aggregateV1.eventTimestamp())
    assertThat(taskV1.deleted).isFalse()

    // Check optional fields
    assertThat(taskV1.description).isNull()
    assertThat(taskV1.location).isNull()
    assertThat(taskV1.editDate).isNull()
  }

  @Test
  fun `query deleted task`() {
    submitAsDeletedEvents()

    // Execute query
    val taskList = query(false)

    // Validate payload
    assertThat(taskList.tasks).hasSize(2)

    val taskV0 = taskList.tasks[0]
    assertThat(taskV0.id).isEqualTo(aggregateV0.getIdentifier().asTaskId())
    assertThat(taskV0.version).isEqualTo(aggregateV0.aggregateIdentifier.version)
    assertThat(taskV0.name).isEqualTo(aggregateV0.name)
    assertThat(taskV0.description).isEqualTo(aggregateV0.description)
    assertThat(taskV0.location).isEqualTo(aggregateV0.location)
    assertThat(taskV0.status).isEqualTo(aggregateV0.status.asStatus().key)
    assertThat(taskV0.editDate).isEqualTo(aggregateV0.editDate.toLocalDateTimeByMillis())
    assertThat(taskV0.eventTimestamp).isEqualTo(aggregateV0.eventTimestamp())
    assertThat(taskV0.deleted).isFalse()

    val taskV1 = taskList.tasks[1]
    assertThat(taskV1.id).isEqualTo(aggregateV1.getIdentifier().asTaskId())
    assertThat(taskV1.version).isEqualTo(aggregateV1.aggregateIdentifier.version)
    assertThat(taskV1.name).isEqualTo(aggregateV1.name)
    assertThat(taskV1.description).isEqualTo(aggregateV1.description)
    assertThat(taskV1.location).isEqualTo(aggregateV1.location)
    assertThat(taskV1.status).isEqualTo(aggregateV1.status.asStatus().key)
    assertThat(taskV1.editDate).isEqualTo(aggregateV1.editDate.toLocalDateTimeByMillis())
    assertThat(taskV1.eventTimestamp).isEqualTo(aggregateV1.eventTimestamp())
    assertThat(taskV1.deleted).isTrue()
  }

  @Test
  fun `query deleted task latest only`() {
    submitAsDeletedEvents()

    // Execute query
    val taskList = query(true)

    // Validate payload
    assertThat(taskList.tasks).isEmpty()
  }

  private fun submitEvents(includeOptionals: Boolean) {
    eventStreamGenerator.submitProject().submitCsmParticipant().submitProjectCraftG2()

    aggregateV0 =
        eventStreamGenerator
            .submitTask {
              if (includeOptionals) {
                it.description = "Description"
                it.location = "Location"
                it.editDate = LocalDateTime.now().toEpochMilli()
              } else {
                it.description = null
                it.location = null
                it.editDate = null
              }
            }
            .get("task")!!

    aggregateV1 =
        eventStreamGenerator
            .submitTask(eventType = TaskEventEnumAvro.UPDATED) { it.name = "Updated task" }
            .get("task")!!
  }

  private fun submitAsDeletedEvents() {
    eventStreamGenerator.submitProject().submitCsmParticipant().submitProjectCraftG2()

    aggregateV0 =
        eventStreamGenerator
            .submitTask {
              it.description = "Description"
              it.location = "Location"
              it.editDate = LocalDateTime.now().toEpochMilli()
            }
            .get("task")!!

    aggregateV1 =
        eventStreamGenerator.submitTask(eventType = TaskEventEnumAvro.DELETED).get("task")!!
  }

  private fun query(latestOnly: Boolean) =
      super.query(
          latestProjectApi("/projects/tasks"), latestOnly, TaskListResource::class.java)
}
