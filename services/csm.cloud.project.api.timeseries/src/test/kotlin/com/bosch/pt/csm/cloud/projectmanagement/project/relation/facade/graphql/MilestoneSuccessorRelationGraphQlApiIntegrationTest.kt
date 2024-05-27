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
import com.bosch.pt.csm.cloud.projectmanagement.project.milestone.extension.asMilestoneType
import com.bosch.pt.csm.cloud.projectmanagement.relation.messages.RelationTypeEnumAvro.FINISH_TO_START
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.test.eventDate
import com.bosch.pt.csm.cloud.projectmanagement.test.get
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@RmsSpringBootTest
class MilestoneSuccessorRelationGraphQlApiIntegrationTest : AbstractGraphQlApiIntegrationTest() {

  val successorMilestone = "projects[0].milestones[0].successorMilestones[0]"
  val successorTask = "projects[0].milestones[0].successorTasks[0]"
  val milestone = "projects[0].milestones[0]"

  val query =
      """
      query {
        projects {
          milestones {
            id
            version
            name
            type
            date
            global
            description
            eventDate
            craft {
              id
              version
            }
            workArea {
              id
              version
            }
            successorMilestones {
              id
              version
            }
            successorTasks {
              id
              version
            }
          }
        }
      }
      """
          .trimIndent()

  lateinit var milestoneAggregate: MilestoneAggregateAvro

  lateinit var successorTaskAggregate: TaskAggregateAvro

  lateinit var successorTaskScheduleAggregate: TaskScheduleAggregateAvro

  lateinit var successorMilestoneAggregate: MilestoneAggregateAvro

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
    response.get("$milestone.name").isEqualTo(milestoneAggregate.name)
    response.get("$milestone.type").isEqualTo(milestoneAggregate.type.asMilestoneType().shortKey)
    response
        .get("$milestone.date")
        .isEqualTo(milestoneAggregate.date.toLocalDateByMillis().toString())
    response.get("$milestone.global").isEqualTo(milestoneAggregate.header)
    response.get("$milestone.description").isEqualTo(milestoneAggregate.description)
    response.get("$milestone.eventDate").isEqualTo(milestoneAggregate.eventDate())
    response.get("$milestone.craft.id").isEqualTo(milestoneAggregate.craft.identifier)
    response.get("$milestone.craft.version").isEqualTo(milestoneAggregate.craft.version.toString())
    response.get("$milestone.workArea.id").isEqualTo(milestoneAggregate.workarea.identifier)
    response
        .get("$milestone.workArea.version")
        .isEqualTo(milestoneAggregate.workarea.version.toString())
  }

  @Test
  fun `query task successor of milestone`() {
    submitEvents()

    // Execute query and validate payload
    val response = graphQlTester.document(query).execute()

    // Successor
    response
        .get("$successorTask.id")
        .isEqualTo(successorTaskAggregate.aggregateIdentifier.identifier)
    response
        .get("$successorTask.version")
        .isEqualTo(successorTaskAggregate.aggregateIdentifier.version.toString())
  }

  @Test
  fun `query milestone successor of milestone`() {
    submitEvents()

    // Execute query and validate payload
    val response = graphQlTester.document(query).execute()

    // Successor
    response
        .get("$successorMilestone.id")
        .isEqualTo(successorMilestoneAggregate.aggregateIdentifier.identifier)
    response
        .get("$successorMilestone.version")
        .isEqualTo(successorMilestoneAggregate.aggregateIdentifier.version.toString())
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
              it.description = "description"
            }
            .get("milestone")!!

    successorTaskAggregate =
        eventStreamGenerator
            .submitTask("successorTask") {
              it.assignee = getByReference("participant2")
              it.workarea = getByReference("workArea")
            }
            .get("successorTask")!!

    successorTaskScheduleAggregate =
        eventStreamGenerator
            .submitTaskSchedule("successorTaskSchedule")
            .get("successorTaskSchedule")!!

    successorMilestoneAggregate =
        eventStreamGenerator
            .submitMilestone("successorMilestone") {
              it.workarea = getByReference("workArea")
              it.craft = getByReference("projectCraft")
            }
            .get("successorMilestone")!!

    eventStreamGenerator.submitRelation("r1") {
      it.critical = false
      it.source = getByReference("milestone")
      it.target = getByReference("successorTask")
      it.type = FINISH_TO_START
    }

    eventStreamGenerator.submitRelation("r2") {
      it.critical = false
      it.source = getByReference("milestone")
      it.target = getByReference("successorMilestone")
      it.type = FINISH_TO_START
    }
  }
}
