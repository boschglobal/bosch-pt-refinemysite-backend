/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.relation.facade.graphql

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
import com.bosch.pt.csm.cloud.projectmanagement.relation.messages.RelationTypeEnumAvro.PART_OF
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.test.get
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@RmsSpringBootTest
class RequiredTasksRelationGraphQlApiIntegrationTest : AbstractGraphQlApiIntegrationTest() {

  val milestone = "projects[0].milestones[0]"
  val milestoneRequiredTask = "projects[0].milestones[0].requiredTasks[0]"
  val task = "projects[0].tasks[0]"
  val taskMilestone = "projects[0].tasks[0].milestones[0]"

  val query =
      """
      query {
        projects {
          milestones {
            id
            version
            requiredTasks {
              id
              version
            }
          }
          tasks {
            id
            version
            milestones {
              id
              version
            }
          }
        }
      }
      """
          .trimIndent()

  lateinit var milestoneAggregate: MilestoneAggregateAvro

  lateinit var requiredTaskAggregate: TaskAggregateAvro

  lateinit var requiredTaskScheduleAggregate: TaskScheduleAggregateAvro

  @BeforeEach
  fun init() {
    setAuthentication("csm-user")
  }

  @Test
  fun `query milestone`() {
    submitEvents()

    // Execute query and validate payload
    val response = graphQlTester.document(query).execute()

    // Milestone
    response.get("$milestone.id").isEqualTo(milestoneAggregate.aggregateIdentifier.identifier)
    response
        .get("$milestone.version")
        .isEqualTo(milestoneAggregate.aggregateIdentifier.version.toString())
  }

  @Test
  fun `query required task of milestone`() {
    submitEvents()

    // Execute query and validate payload
    val response = graphQlTester.document(query).execute()

    // Required task
    response
        .get("$milestoneRequiredTask.id")
        .isEqualTo(requiredTaskAggregate.aggregateIdentifier.identifier)
    response
        .get("$milestoneRequiredTask.version")
        .isEqualTo(requiredTaskAggregate.aggregateIdentifier.version.toString())
  }

  @Test
  fun `query milestone where tasks a part of`() {
    submitEvents()

    // Execute query and validate payload
    val response = graphQlTester.document(query).execute()

    // Task
    response.get("$task.id").isEqualTo(requiredTaskAggregate.aggregateIdentifier.identifier)
    response
        .get("$task.version")
        .isEqualTo(requiredTaskAggregate.aggregateIdentifier.version.toString())
    response.get("$taskMilestone.id").isEqualTo(milestoneAggregate.aggregateIdentifier.identifier)
    response
        .get("$taskMilestone.version")
        .isEqualTo(milestoneAggregate.aggregateIdentifier.version.toString())
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

    milestoneAggregate =
        eventStreamGenerator
            .submitMilestone {
              it.workarea = getByReference("workArea")
              it.craft = getByReference("projectCraft")
            }
            .get("milestone")!!

    requiredTaskAggregate =
        eventStreamGenerator
            .submitTask("requiredTask") {
              it.assignee = getByReference("participant2")
              it.workarea = getByReference("workArea")
            }
            .get("requiredTask")!!

    requiredTaskScheduleAggregate =
        eventStreamGenerator
            .submitTaskSchedule("requiredTaskSchedule")
            .get("requiredTaskSchedule")!!

    eventStreamGenerator.submitRelation("r1") {
      it.critical = false
      it.source = getByReference("requiredTask")
      it.target = getByReference("milestone")
      it.type = PART_OF
    }
  }
}
