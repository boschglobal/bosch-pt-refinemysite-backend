/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.common

import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersionProperties
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.PROJECTCRAFT
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftAggregateG2Avro
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantAggregateG3Avro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro.CR
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro.CSM
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro.FM
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMilestone
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkArea
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkAreaList
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.test.Repositories
import com.bosch.pt.csm.cloud.projectmanagement.test.TimeLineGeneratorImpl
import com.bosch.pt.csm.cloud.projectmanagement.util.doWithAuthorization
import com.bosch.pt.csm.cloud.projectmanagement.util.requestBuilder
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaEventEnumAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserAggregateAvro
import com.fasterxml.jackson.databind.ObjectMapper
import java.util.UUID
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.web.context.WebApplicationContext

abstract class AbstractIntegrationTest : AbstractBaseIntegrationTest() {

  @Autowired lateinit var webApplicationContext: WebApplicationContext

  @Autowired lateinit var objectMapper: ObjectMapper

  @Autowired lateinit var apiVersionProperties: ApiVersionProperties

  @Autowired lateinit var repositories: Repositories

  lateinit var mockMvc: MockMvc

  val project by lazy { context["project"] as ProjectAggregateAvro }

  val csmParticipant by lazy { context["csm-participant"] as ParticipantAggregateG3Avro }
  val crParticipant by lazy { context["cr-participant"] as ParticipantAggregateG3Avro }
  val fmParticipant by lazy { context["fm-participant"] as ParticipantAggregateG3Avro }

  val workArea1 by lazy { context["workArea1"] as WorkAreaAggregateAvro }
  val workArea2 by lazy { context["workArea2"] as WorkAreaAggregateAvro }

  val projectCraft1 by lazy { context["projectCraft1"] as ProjectCraftAggregateG2Avro }
  val projectCraft2 by lazy { context["projectCraft2"] as ProjectCraftAggregateG2Avro }

  val milestone1 by lazy { context["milestone1"] as MilestoneAggregateAvro }

  val timeLineGenerator by lazy { eventStreamContext.timeLineGenerator as TimeLineGeneratorImpl }

  @BeforeEach
  fun setup() {
    eventStreamGenerator
        .submitProject()
        .setUserContext("csm-user")
        .submitParticipantG3(asReference = "csm-participant") {
          it.user = getByReference("csm-user")
          it.role = CSM
        }
        .setUserContext("cr-user")
        .submitParticipantG3(asReference = "cr-participant") {
          it.user = getByReference("cr-user")
          it.role = CR
        }
        .setUserContext("fm-user")
        .submitParticipantG3(asReference = "fm-participant") {
          it.user = getByReference("fm-user")
          it.role = FM
        }
        .setUserContext("testadmin")
        .submitWorkArea(asReference = "workArea1") { it.name = "workArea1" }
        .submitWorkArea(asReference = "workArea1", eventType = WorkAreaEventEnumAvro.UPDATED)
        .submitWorkArea(asReference = "workArea2") { it.name = "workArea2" }
        .submitWorkArea(asReference = "workArea2", eventType = WorkAreaEventEnumAvro.UPDATED)
        .submitWorkAreaList {
          it.workAreas = listOf(getByReference("workArea1"), getByReference("workArea2"))
        }
        .submitProjectCraftG2(asReference = "projectCraft1") { it.name = "projectCraft1" }
        .submitProjectCraftG2(
            asReference = "projectCraft1", eventType = ProjectCraftEventEnumAvro.UPDATED)
        .submitProjectCraftG2(asReference = "projectCraft2") { it.name = "projectCraft2" }
        .submitProjectCraftG2(
            asReference = "projectCraft2", eventType = ProjectCraftEventEnumAvro.UPDATED)
        .submitMilestone(asReference = "milestone1") { it.name = "milestone1" }
        .setLastIdentifierForType(PROJECTCRAFT.value, getByReference("projectCraft1"))
  }

  @AfterEach
  fun cleanupBase() {
    SecurityContextHolder.clearContext()
    repositories.truncateDatabase()
  }

  fun requestActivities(
      task: TaskAggregateAvro,
      authorizeAsUser: UserAggregateAvro = fmUser,
      limit: Int = 1
  ): ResultActions =
      doWithAuthorization(repositories.findUser(authorizeAsUser)) {
        mockMvc.perform(
            requestBuilder(
                MockMvcRequestBuilders.get(
                    latestVersionOf("/projects/tasks/{taskId}/activities?limit={limit}"),
                    task.getIdentifier(),
                    limit),
                objectMapper))
      }

  fun requestActivitiesWithBefore(
      task: TaskAggregateAvro,
      before: UUID,
      authorizeAsUser: UserAggregateAvro = fmUser,
      limit: Int = 1
  ): ResultActions =
      doWithAuthorization(repositories.findUser(authorizeAsUser)) {
        mockMvc.perform(
            requestBuilder(
                RestDocumentationRequestBuilders.get(
                    latestVersionOf(
                        "/projects/tasks/{taskId}/activities?before={before}&limit={limit}"),
                    task.getIdentifier(),
                    before,
                    limit),
                objectMapper))
      }

  fun requestActivitiesWithoutLimitAndBefore(
      task: TaskAggregateAvro,
      authorizeAsUser: UserAggregateAvro = fmUser
  ): ResultActions =
      doWithAuthorization(repositories.findUser(authorizeAsUser)) {
        mockMvc.perform(
            requestBuilder(
                MockMvcRequestBuilders.get(
                    latestVersionOf("/projects/tasks/{taskId}/activities"), task.getIdentifier()),
                objectMapper))
      }

  fun latestVersionOf(path: String) = "/v${apiVersionProperties.version.max}$path"

  fun latestVersion(): Int = apiVersionProperties.version.max
}
