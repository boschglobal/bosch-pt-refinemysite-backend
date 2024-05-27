/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.facade.graphql

import com.bosch.pt.csm.cloud.common.extensions.toEpochMilli
import com.bosch.pt.csm.cloud.common.extensions.toLocalDateByMillis
import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.projectmanagement.application.RmsSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractGraphQlApiIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskSchedule
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleSlotAvro
import com.bosch.pt.csm.cloud.projectmanagement.taskschedule.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.taskschedule.message.getVersion
import com.bosch.pt.csm.cloud.projectmanagement.test.eventDate
import com.bosch.pt.csm.cloud.projectmanagement.test.get
import com.bosch.pt.csm.cloud.projectmanagement.test.isNull
import java.time.LocalDate
import java.util.UUID.randomUUID
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@RmsSpringBootTest
class TaskScheduleGraphQlApiIntegrationTest : AbstractGraphQlApiIntegrationTest() {

  val schedule = "projects[0].tasks[0].schedule"

  val query =
      """
      query {
        projects {
          tasks {
            schedule {
              id
              version
              start
              end
              eventDate
            }
          }
        }
      }
      """
          .trimIndent()

  lateinit var aggregateV0: TaskScheduleAggregateAvro

  lateinit var aggregateV1: TaskScheduleAggregateAvro

  @BeforeEach
  fun init() {
    setAuthentication("csm-user")
  }

  @Test
  fun `query schedule with all parameters set`() {
    submitEvents(true)

    // Execute query and validate payload
    val response = graphQlTester.document(query).execute()
    response.get("$schedule.id").isEqualTo(aggregateV1.getIdentifier().toString())
    response.get("$schedule.version").isEqualTo(aggregateV1.getVersion().toString())
    response
        .get("$schedule.start")
        .isEqualTo(this.aggregateV1.start.toLocalDateByMillis().toString())
    response.get("$schedule.end").isEqualTo(this.aggregateV1.end.toLocalDateByMillis().toString())
    response.get("$schedule.eventDate").isEqualTo(aggregateV1.eventDate())
  }

  @Test
  fun `query schedule without optional parameters`() {
    submitEvents(false)

    // Execute query and validate mandatory fields
    val response = graphQlTester.document(query).execute()
    response.get("$schedule.id").isEqualTo(aggregateV1.getIdentifier().toString())
    response.get("$schedule.version").isEqualTo(aggregateV1.getVersion().toString())
    response.get("$schedule.eventDate").isEqualTo(aggregateV1.eventDate())

    // Check optional attributes
    response.isNull("$schedule.end")
    response.isNull("$schedule.start")
  }

  @Test
  fun `query deleted schedule`() {
    submitEvents(true)
    eventStreamGenerator.submitTaskSchedule(
        "taskSchedule", eventType = TaskScheduleEventEnumAvro.DELETED)

    // Execute query and validate payload
    val response = graphQlTester.document(query).execute()
    response.isNull(schedule)
  }

  private fun submitEvents(includeOptionals: Boolean) {
    aggregateV0 =
        eventStreamGenerator
            .submitProject()
            .submitCsmParticipant()
            .submitProjectCraftG2()
            .submitTask()
            .submitTaskSchedule {
              if (includeOptionals) {
                it.start = LocalDate.now().toEpochMilli()
                it.end = LocalDate.now().toEpochMilli()
              } else {
                it.start = null
                it.end = null
              }
            }
            .get("taskSchedule")!!

    aggregateV1 =
        eventStreamGenerator
            .submitTaskSchedule(eventType = TaskScheduleEventEnumAvro.UPDATED) {
              it.slots.add(
                  TaskScheduleSlotAvro(
                      LocalDate.now().toEpochMilli(),
                      AggregateIdentifierAvro(
                          randomUUID().toString(),
                          0L,
                          ProjectmanagementAggregateTypeEnum.DAYCARD.name)))
            }
            .get("taskSchedule")!!
  }
}
