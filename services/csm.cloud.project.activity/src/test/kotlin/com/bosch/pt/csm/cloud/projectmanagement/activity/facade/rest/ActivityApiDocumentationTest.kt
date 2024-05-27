/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.activity.facade.rest

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.projectmanagement.activity.facade.rest.resources.response.ActivityListResource.Companion.LINK_PREVIOUS
import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractApiDocumentationTest
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitDayCardG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTopicG2
import com.bosch.pt.csm.cloud.projectmanagement.task.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskStatusEnumAvro.OPEN
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicCriticalityEnumAvro.CRITICAL
import java.util.Date
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.restdocs.RestDocumentationExtension
import org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel
import org.springframework.restdocs.hypermedia.HypermediaDocumentation.links
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.payload.JsonFieldType.ARRAY
import org.springframework.restdocs.payload.JsonFieldType.OBJECT
import org.springframework.restdocs.payload.JsonFieldType.STRING
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.restdocs.request.RequestDocumentation.queryParameters

@ExtendWith(RestDocumentationExtension::class)
@SmartSiteSpringBootTest
@DisplayName("Document activity api")
class ActivityApiDocumentationTest : AbstractApiDocumentationTest() {

  private val task by lazy { context["task"] as TaskAggregateAvro }

  @BeforeEach
  fun init() {

    eventStreamGenerator
        .setUserContext("fm-user")
        .submitTask {
          it.assignee = getByReference("fm-participant")
          it.name = "task"
          it.status = OPEN
          // setting all non-mandatory fields to null
          it.location = null
          it.description = null
        }
        .submitDayCardG2 {
          it.title = "Daycard Title"
          it.manpower = 1F.toBigDecimal()
          it.notes = "Daycard Notes"
          it.reason = null
        }
        .submitTopicG2 {
          it.description = "Topic Description"
          it.criticality = CRITICAL
        }
  }

  @Test
  fun `for getting a list of activities`() {

    val actives =
        repositories.activityRepository.findAllByContextTask(
            task.getIdentifier(), PageRequest.of(0, 3, Sort.by(Sort.Order.desc("event.date"))))

    val firstActivityIdentifier = actives.first().identifier

    requestActivitiesWithBefore(task, firstActivityIdentifier)
        .andDo(
            document(
                "get-task-activities",
                ACTIVITY_PATH_PARAMETERS_DESCRIPTORS,
                ACTIVITY_REQUEST_PARAMETERS_DESCRIPTORS,
                ACTIVITY_LIST_RESPONSE_FIELD,
                ACTIVITY_LINK_DESCRIPTORS))
  }

  companion object {

    private val ACTIVITY_PATH_PARAMETERS_DESCRIPTORS =
        pathParameters(parameterWithName("taskId").description("ID of the task"))

    private val ACTIVITY_REQUEST_PARAMETERS_DESCRIPTORS =
        queryParameters(
            parameterWithName("before")
                .description(
                    "Optional identifier of activity to find activities older than this one")
                .optional(),
            parameterWithName("limit")
                .description("Optional limit how many changes to load at once. Maximum is 50.")
                .optional(),
        )

    private val ACTIVITY_RESPONSE_FIELD_DESCRIPTORS =
        listOf(
            fieldWithPath("id").description("Identifier of the activity").type(STRING),
            fieldWithPath("date")
                .description("Date when the functional change was saved")
                .type(Date::class),
            fieldWithPath("user.displayName")
                .description("Name of the user that created the activity")
                .type(STRING),
            fieldWithPath("user.id")
                .description("ID of the user that created the activity")
                .type(STRING),
            fieldWithPath("user.picture")
                .description("URL of the user profile picture that created the activity")
                .type(STRING),
            fieldWithPath("description")
                .description(
                    "Activity description with message template and map placeholder values")
                .type(OBJECT),
            fieldWithPath("description.template")
                .description("The description template message string with placeholders keys")
                .type(STRING),
            fieldWithPath("description.values")
                .description("The description map placeholder values for the template")
                .type(OBJECT),
            fieldWithPath("description.values.originator")
                .description("Values for \${originator} placeholder")
                .type(OBJECT),
            fieldWithPath("description.values.originator.type")
                .description("Type of \${originator} placeholder value object")
                .type(STRING),
            fieldWithPath("description.values.originator.id")
                .description("Id of \${originator} placeholder value object")
                .type(STRING),
            fieldWithPath("description.values.originator.text")
                .description("Value to display for \${originator} placeholder in template string")
                .type(STRING),
            fieldWithPath("description.values.daycard")
                .description("Values for \${daycard} placeholder")
                .type(OBJECT),
            fieldWithPath("description.values.daycard.type")
                .description("Type of \${daycard} placeholder value object")
                .type(STRING),
            fieldWithPath("description.values.daycard.id")
                .description("Id of \${daycard} placeholder value object")
                .type(STRING),
            fieldWithPath("description.values.daycard.text")
                .description("Value to display for \${daycard} placeholder in template string")
                .type(STRING),
            fieldWithPath("changes").description("A list of changes for the activity").type(ARRAY),
            fieldWithPath("_embedded")
                .description("Embedded object(s) of the activity")
                .type(ARRAY)
                .optional(),
            subsectionWithPath("_embedded.attachments").ignored(),
            subsectionWithPath("_links").optional().ignored())

    private val ACTIVITY_LIST_RESPONSE_FIELD =
        responseFields(
                fieldWithPath("activities[]").description("A list of activities").type(ARRAY),
                subsectionWithPath("_links").ignored())
            .andWithPrefix("activities[].", ACTIVITY_RESPONSE_FIELD_DESCRIPTORS)

    private val ACTIVITY_LINK_DESCRIPTORS =
        links(
            linkWithRel(LINK_PREVIOUS)
                .description("Optional link to older activities if more data is available"),
        )
  }
}
