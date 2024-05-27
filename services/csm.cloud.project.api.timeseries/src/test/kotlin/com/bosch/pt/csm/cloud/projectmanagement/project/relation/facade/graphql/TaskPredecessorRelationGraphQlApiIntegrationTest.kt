/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.relation.facade.graphql

import com.bosch.pt.csm.cloud.common.extensions.toLocalDateByMillis
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.projectmanagement.application.RmsSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractGraphQlApiIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMilestone
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitRelation
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskSchedule
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkArea
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkAreaList
import com.bosch.pt.csm.cloud.projectmanagement.relation.messages.RelationTypeEnumAvro.FINISH_TO_START
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.test.get
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@RmsSpringBootTest
class TaskPredecessorRelationGraphQlApiIntegrationTest : AbstractGraphQlApiIntegrationTest() {

  val predecessorMilestone = "projects[0].tasks[0].predecessorMilestones[0]"
  val predecessorTask = "projects[0].tasks[0].predecessorTasks[0]"
  val task = "projects[0].tasks[0]"

  val query =
      """
      query {
        projects {
          tasks {
            id
            version
            craft {
              id
              version
            }
            assignee {
              id
              version
              company {
                id
                version
              }
              user {
                id
                version
              }
            }
            workArea {
              id
              version
            }
            schedule {
              id
              version
              start
              end
            }
            predecessorMilestones {
              id
              version
            }
            predecessorTasks {
              id
              version
            }
          }
        }
      }
      """
          .trimIndent()

  lateinit var taskAggregate: TaskAggregateAvro

  lateinit var taskScheduleAggregate: TaskScheduleAggregateAvro

  lateinit var predecessorTaskAggregate: TaskAggregateAvro

  lateinit var predecessorTaskScheduleAggregate: TaskScheduleAggregateAvro

  lateinit var predecessorMilestoneAggregate: MilestoneAggregateAvro

  @BeforeEach
  fun init() {
    setAuthentication("csm-user")
  }

  @Test
  fun `query task`() {
    submitEvents()

    // Execute query and validate payload
    val response = graphQlTester.document(query).execute()

    // Task
    response.get("$task.id").isEqualTo(taskAggregate.aggregateIdentifier.identifier)
    response.get("$task.version").isEqualTo(taskAggregate.aggregateIdentifier.version.toString())
    response.get("$task.craft.id").isEqualTo(taskAggregate.craft.identifier)
    response.get("$task.craft.version").isEqualTo(taskAggregate.craft.version.toString())
    response.get("$task.assignee.id").isEqualTo(taskAggregate.assignee.identifier)
    response.get("$task.assignee.version").isEqualTo(taskAggregate.assignee.version.toString())
    response.get("$task.workArea.id").isEqualTo(taskAggregate.workarea.identifier)
    response.get("$task.workArea.version").isEqualTo(taskAggregate.workarea.version.toString())
    response
        .get("$task.schedule.id")
        .isEqualTo(taskScheduleAggregate.aggregateIdentifier.identifier)
    response
        .get("$task.schedule.version")
        .isEqualTo(taskScheduleAggregate.aggregateIdentifier.version.toString())
    response
        .get("$task.schedule.start")
        .isEqualTo(taskScheduleAggregate.start.toLocalDateByMillis().toString())
    response
        .get("$task.schedule.end")
        .isEqualTo(taskScheduleAggregate.end.toLocalDateByMillis().toString())
  }

  @Test
  fun `query task predecessors of task`() {
    submitEvents()

    // Execute query and validate payload
    val response = graphQlTester.document(query).execute()

    // Predecessor
    response
        .get("$predecessorTask.id")
        .isEqualTo(predecessorTaskAggregate.aggregateIdentifier.identifier)
    response
        .get("$predecessorTask.version")
        .isEqualTo(predecessorTaskAggregate.aggregateIdentifier.version.toString())
  }

  @Test
  fun `query milestone predecessors of task`() {
    submitEvents()

    // Execute query and validate payload
    val response = graphQlTester.document(query).execute()

    // Predecessor
    response
        .get("$predecessorMilestone.id")
        .isEqualTo(predecessorMilestoneAggregate.aggregateIdentifier.identifier)
    response
        .get("$predecessorMilestone.version")
        .isEqualTo(predecessorMilestoneAggregate.aggregateIdentifier.version.toString())
  }

  private fun submitEvents() {
    eventStreamGenerator
        .submitProject()
        .submitCsmParticipant()
        .submitUser("user2")
        .submitParticipantG3("participant2") { it.user = getByReference("user2") }
        .submitProjectCraftG2()
        .submitWorkArea()
        .submitWorkAreaList { it.workAreas = listOf(getByReference("workArea")) }

    taskAggregate =
        eventStreamGenerator
            .submitTask {
              it.assignee = getByReference("participant2")
              it.workarea = getByReference("workArea")
            }
            .get("task")!!

    taskScheduleAggregate = eventStreamGenerator.submitTaskSchedule().get("taskSchedule")!!

    predecessorTaskAggregate =
        eventStreamGenerator
            .submitTask("predecessorTask") {
              it.assignee = getByReference("participant2")
              it.workarea = getByReference("workArea")
            }
            .get("predecessorTask")!!

    predecessorTaskScheduleAggregate =
        eventStreamGenerator
            .submitTaskSchedule("predecessorTaskSchedule")
            .get("predecessorTaskSchedule")!!

    predecessorMilestoneAggregate =
        eventStreamGenerator
            .submitMilestone {
              it.workarea = getByReference("workArea")
              it.craft = getByReference("projectCraft")
            }
            .get("milestone")!!

    eventStreamGenerator.submitRelation("r1") {
      it.critical = false
      it.source = getByReference("predecessorTask")
      it.target = getByReference("task")
      it.type = FINISH_TO_START
    }

    eventStreamGenerator.submitRelation("r2") {
      it.critical = false
      it.source = getByReference("milestone")
      it.target = getByReference("task")
      it.type = FINISH_TO_START
    }
  }
}
