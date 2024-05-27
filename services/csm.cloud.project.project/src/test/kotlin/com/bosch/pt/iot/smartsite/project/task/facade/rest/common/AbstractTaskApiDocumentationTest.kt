/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.task.facade.rest.common

import com.bosch.pt.csm.cloud.common.facade.rest.resource.request.UpdateBatchRequestResource
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitCompany
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitEmployee
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeRoleEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitSystemUserAndActivate
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractApiDocumentationTestV2
import com.bosch.pt.iot.smartsite.common.facade.rest.ConstrainedFields
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.request.CreateTaskBatchResource
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.request.FilterTaskListResource
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.request.SaveTaskResource
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.request.SaveTaskResourceWithIdentifierAndVersion
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response.TaskResource.Companion.LINK_ACCEPT
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response.TaskResource.Companion.LINK_ASSIGN
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response.TaskResource.Companion.LINK_CLOSE
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response.TaskResource.Companion.LINK_CONSTRAINTS
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response.TaskResource.Companion.LINK_CONSTRAINTS_UPDATE
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response.TaskResource.Companion.LINK_DELETE
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response.TaskResource.Companion.LINK_RESET
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response.TaskResource.Companion.LINK_START
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response.TaskResource.Companion.LINK_TASK_SCHEDULE
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response.TaskResource.Companion.LINK_TASK_UPDATE
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response.TaskResource.Companion.LINK_TOPIC
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response.TaskResource.Companion.LINK_TOPIC_CREATE
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response.TaskResource.Companion.LINK_UNASSIGN
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum
import org.apache.commons.lang3.StringUtils
import org.junit.jupiter.api.BeforeEach
import org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel
import org.springframework.restdocs.hypermedia.HypermediaDocumentation.links
import org.springframework.restdocs.hypermedia.LinksSnippet
import org.springframework.restdocs.payload.FieldDescriptor
import org.springframework.restdocs.payload.JsonFieldType.ARRAY
import org.springframework.restdocs.payload.JsonFieldType.BOOLEAN
import org.springframework.restdocs.payload.JsonFieldType.NUMBER
import org.springframework.restdocs.payload.JsonFieldType.OBJECT
import org.springframework.restdocs.payload.JsonFieldType.STRING
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath
import org.springframework.restdocs.payload.ResponseFieldsSnippet
import org.springframework.restdocs.snippet.Attributes.key

@EnableAllKafkaListeners
open class AbstractTaskApiDocumentationTest : AbstractApiDocumentationTestV2() {

  @BeforeEach
  fun createBaseProject() {
    eventStreamGenerator
        .submitSystemUserAndActivate()
        .submitCompany()
        .submitUser("fm-user")
        .submitEmployee("fm-employee") { it.roles = listOf(EmployeeRoleEnumAvro.FM) }
        .submitUser("csm-user")
        .submitEmployee("csm-employee") { it.roles = listOf(EmployeeRoleEnumAvro.CSM) }
        .setUserContext("csm-user")
        .submitProject()
        .submitParticipantG3("csm-participant") {
          it.user = EventStreamGeneratorStaticExtensions.getByReference("csm-user")
          it.role = ParticipantRoleEnumAvro.CSM
        }
        .submitParticipantG3("fm-participant") {
          it.user = EventStreamGeneratorStaticExtensions.getByReference("fm-user")
          it.role = ParticipantRoleEnumAvro.FM
        }
        .submitProjectCraftG2()
  }

  protected fun <T> List<T>.second(): T = this.get(1)

  companion object {
    const val LINK_ASSIGNED_FOREMAN_DESCRIPTION =
        ("Link to the change " +
            "<<api-guide-project-context-tasks.adoc#resources-change-assigned-foreman-of-task-by-id,assignee>>")
    const val LINK_UNASSIGNED_FOREMAN_DESCRIPTION =
        ("Link to the change " +
            "<<api-guide-project-context-tasks.adoc#resources-change-unassigned-foreman-of-task-by-id,unassignee>>")
    const val LINK_SEND_DESCRIPTION =
        "Link to <<api-guide-project-context-tasks.adoc#resources-send-task-list,send>> the task"
    const val LINK_START_DESCRIPTION =
        ("Link to <<api-guide-project-context-tasks.adoc#resources-start-task,change>> " +
            "the task resource to status 'STARTED'")
    const val LINK_CLOSE_DESCRIPTION =
        ("Link to <<api-guide-project-context-tasks.adoc#resources-close-task,change>> " +
            "the task resource to status 'CLOSED'")
    const val LINK_ACCEPT_DESCRIPTION =
        ("Link to <<api-guide-project-context-tasks.adoc#resources-accept-task,change>> " +
            "the task resource to status 'ACCEPTED'")
    const val LINK_RESET_DESCRIPTION =
        ("Link to <<api-guide-project-context-tasks.adoc#resources-reset-task,change>> " +
            "the task resource back to status 'OPEN'")
    const val LINK_CONSTRAINTS_DESCRIPTION =
        "Link to <<api-guide-project-context-tasks.adoc#get-task-constraint-selection-of-task," +
            "get list of constraints>> for the task"
    const val LINK_CONSTRAINTS_UPDATE_DESCRIPTION =
        "Link to <<api-guide-project-context-tasks.adoc#update-task-constraint-selection," +
            "update list of constraints>> for the task"
    const val LINK_TASK_CREATE_DESCRIPTION =
        "Link to <<api-guide-project-context-tasks.adoc#resources-create-task,create the task>>."
    const val LINK_TASK_DELETE_DESCRIPTION =
        "Link to <<api-guide-project-context-tasks.adoc#resources-delete-task,delete a task>>."
    const val LINK_TASK_UPDATE_DESCRIPTION =
        "Link to <<api-guide-project-context-tasks.adoc#resources-update-task,update the task>>."
    const val LINK_TASK_SCHEDULE_DESCRIPTION =
        "Link to <<api-guide-project-context-tasks.adoc#resources-topics-access,get the schedule>> for the task"
    const val LINK_TOPIC_DESCRIPTION =
        "Link to <<api-guide-project-context-tasks.adoc#resources-get-task-schedule,get all topics>> for the task"
    const val LINK_TOPIC_CREATE_DESCRIPTION =
        "Link to <<api-guide-project-context-tasks.adoc#resources-create-topic,create the topic>>."
    const val LINK_NEXT_DESCRIPTION = "Link to the next tasks page"

    @JvmStatic
    protected val SAVE_TASK_REQUEST_FIELDS =
        listOf(
            ConstrainedFields(SaveTaskResource::class.java)
                .withPath("name")
                .description("Name of the task")
                .type(STRING),
            ConstrainedFields(SaveTaskResource::class.java)
                .withPath("description")
                .description("Description of the task")
                .type(STRING),
            ConstrainedFields(SaveTaskResource::class.java)
                .withPath("location")
                .description("Location of the task")
                .type(STRING),
            ConstrainedFields(SaveTaskResource::class.java)
                .withPath("status")
                .description("Status of the task")
                .type(STRING),
            ConstrainedFields(SaveTaskResource::class.java)
                .withPath("projectId")
                .description("ID of associated project")
                .type(STRING),
            ConstrainedFields(SaveTaskResource::class.java)
                .withPath("projectCraftId")
                .description("ID of the assigned project craft for this task")
                .type(STRING),
            ConstrainedFields(SaveTaskResource::class.java)
                .withPath("assigneeId")
                .description("ID of the participant the task should be assigned to (Optional)")
                .optional()
                .type(STRING),
            ConstrainedFields(SaveTaskResource::class.java)
                .withPath("workAreaId")
                .description("ID of the work area the task should be related to (Optional)")
                .optional()
                .type(STRING))

    @JvmStatic
    protected val CREATE_TASK_REQUEST_FIELDS =
        concatRequestFields(
            SAVE_TASK_REQUEST_FIELDS,
            listOf(
                ConstrainedFields(CreateTaskBatchResource::class.java)
                    .withPath("id")
                    .description("ID of the resource to be updated in a batch")
                    .type(STRING)))

    @JvmStatic
    protected val CREATE_BATCH_RESOURCE_REQUEST_FIELDS_SNIPPET =
        requestFields(
                ConstrainedFields(UpdateBatchRequestResource::class.java)
                    .withPath("items[]")
                    .description(
                        "List of versioned references of the resource to be updated in a batch")
                    .type(ARRAY))
            .andWithPrefix("items[].", CREATE_TASK_REQUEST_FIELDS)

    @JvmStatic
    protected val UPDATE_TASK_REQUEST_FIELDS =
        concatRequestFields(
            SAVE_TASK_REQUEST_FIELDS,
            listOf(
                ConstrainedFields(SaveTaskResourceWithIdentifierAndVersion::class.java)
                    .withPath("id")
                    .description("ID of the resource to be updated in a batch")
                    .type(STRING),
                ConstrainedFields(SaveTaskResourceWithIdentifierAndVersion::class.java)
                    .withPath("version")
                    .description("Version of the resource to be updated in a batch")
                    .type(NUMBER)))

    @JvmStatic
    protected val UPDATE_BATCH_RESOURCE_REQUEST_FIELDS_SNIPPET =
        requestFields(
                ConstrainedFields(UpdateBatchRequestResource::class.java)
                    .withPath("items[]")
                    .description(
                        "List of versioned references of the resource to be updated in a batch")
                    .type(ARRAY))
            .andWithPrefix("items[].", UPDATE_TASK_REQUEST_FIELDS)

    @JvmStatic
    protected val SEARCH_TASKS_REQUEST_FIELD_DESCRIPTORS =
        listOf(
            ConstrainedFields(FilterTaskListResource::class.java)
                .withPath("assignees")
                .optional()
                .description(
                    "A JSON object to filter tasks by their assignee (Optional). " +
                        "If participant ids and company ids are both set, those tasks are returned where either " +
                        "the assignee's participant id matches or the assignee's company id, or both.")
                .type(OBJECT),
            ConstrainedFields(FilterTaskListResource::class.java)
                .withPath("assignees.participantIds")
                .optional()
                .description(
                    "Participant Ids used to filter tasks by assigned participant (Optional)")
                .type(ARRAY),
            ConstrainedFields(FilterTaskListResource::class.java)
                .withPath("assignees.companyIds")
                .optional()
                .description("Company Ids used to filter tasks (Optional)")
                .type(ARRAY),
            ConstrainedFields(FilterTaskListResource::class.java)
                .withPath("projectCraftIds")
                .description("Craft Ids used to filter tasks (Optional)")
                .optional()
                .type(ARRAY),
            ConstrainedFields(FilterTaskListResource::class.java)
                .withPath("workAreaIds")
                .optional()
                .description(
                    "Work Area Ids used to filter tasks (Optional). " +
                        "Use `'empty'` to include tasks that do not have a work area")
                .type(ARRAY),
            ConstrainedFields(FilterTaskListResource::class.java)
                .withPath("status")
                .optional()
                .description("Tasks states used to filter tasks (Optional)")
                .attributes(
                    key("constraints")
                        .value("Valid values for the entries are DRAFT,OPEN,STARTED,CLOSED"))
                .type(ARRAY),
            ConstrainedFields(FilterTaskListResource::class.java)
                .withPath("from")
                .optional()
                .description("Date from which filter tasks (Optional)")
                .type("Date"),
            ConstrainedFields(FilterTaskListResource::class.java)
                .withPath("to")
                .optional()
                .description("Date until which filter tasks (Optional)")
                .type("Date"),
            ConstrainedFields(FilterTaskListResource::class.java)
                .withPath("topicCriticality")
                .optional()
                .description(
                    "Topic criticality status used to filter tasks only with topics (Optional)")
                .attributes(
                    key("constraints")
                        .value("Valid values for the entries are CRITICAL, UNCRITICAL"))
                .type(ARRAY),
            ConstrainedFields(FilterTaskListResource::class.java)
                .withPath("hasTopics")
                .optional()
                .description("Boolean used to filter tasks only with topics (Optional)")
                .type(BOOLEAN),
            ConstrainedFields(FilterTaskListResource::class.java)
                .withPath("projectId")
                .optional()
                .description("Project Id to filter tasks (Optional)")
                .type(STRING))

    @JvmStatic
    fun buildSearchTasksRequestFieldDescriptorsV2(pathPrefix: String = "") =
        listOf(
            ConstrainedFields(FilterTaskListResource::class.java)
                .withPath(pathPrefix, "assignees")
                .optional()
                .description(
                    "A JSON object to filter tasks by their assignee (Optional). " +
                        "If participant ids and company ids are both set, those tasks are returned where either " +
                        "the assignee's participant id matches or the assignee's company id, or both.")
                .type(OBJECT),
            ConstrainedFields(FilterTaskListResource::class.java)
                .withPath(pathPrefix, "assignees.participantIds")
                .optional()
                .description(
                    "Participant Ids used to filter tasks by assigned participant (Optional)")
                .type(ARRAY),
            ConstrainedFields(FilterTaskListResource::class.java)
                .withPath(pathPrefix, "assignees.companyIds")
                .optional()
                .description("Company Ids used to filter tasks (Optional)")
                .type(ARRAY),
            ConstrainedFields(FilterTaskListResource::class.java)
                .withPath(pathPrefix, "projectCraftIds")
                .description("Craft Ids used to filter tasks (Optional)")
                .optional()
                .type(ARRAY),
            ConstrainedFields(FilterTaskListResource::class.java)
                .withPath(pathPrefix, "workAreaIds")
                .optional()
                .description(
                    "Work Area Ids used to filter tasks (Optional). " +
                        "Use `'empty'` to include tasks that do not have a work area")
                .type(ARRAY),
            ConstrainedFields(FilterTaskListResource::class.java)
                .withPath(pathPrefix, "status")
                .optional()
                .description("Tasks states used to filter tasks (Optional)")
                .attributes(
                    key("constraints")
                        .value("Valid values for the entries are DRAFT,OPEN,STARTED,CLOSED"))
                .type(ARRAY),
            ConstrainedFields(FilterTaskListResource::class.java)
                .withPath(pathPrefix, "from")
                .optional()
                .description("Date from which filter tasks (Optional)")
                .type("Date"),
            ConstrainedFields(FilterTaskListResource::class.java)
                .withPath(pathPrefix, "to")
                .optional()
                .description("Date until which filter tasks (Optional)")
                .type("Date"),
            ConstrainedFields(FilterTaskListResource::class.java)
                .withPath(pathPrefix, "topicCriticality")
                .optional()
                .description(
                    "Topic criticality status used to filter tasks only with topics (Optional)")
                .attributes(
                    key("constraints")
                        .value("Valid values for the entries are CRITICAL, UNCRITICAL"))
                .type(ARRAY),
            ConstrainedFields(FilterTaskListResource::class.java)
                .withPath(pathPrefix, "hasTopics")
                .optional()
                .description("Boolean used to filter tasks only with topics (Optional)")
                .type(BOOLEAN))

    @JvmStatic
    val TASK_RESPONSE_FIELD_DESCRIPTORS =
        listOf(
            fieldWithPath("id").description("ID of the task"),
            fieldWithPath("version").description("Version of the task"),
            fieldWithPath("project.displayName").description("Name of the task's project"),
            fieldWithPath("project.id").description("ID of the task's project"),
            fieldWithPath("name").description("Name of the task"),
            fieldWithPath("description").description("Description of the task"),
            fieldWithPath("company.displayName")
                .description("Name of the company the task belongs to")
                .type(STRING)
                .optional(),
            fieldWithPath("company.id")
                .description("ID of the company the task belongs to")
                .type(STRING)
                .optional(),
            fieldWithPath("workArea.displayName")
                .description("Name of the work area the task relates to")
                .type(STRING)
                .optional(),
            fieldWithPath("workArea.id")
                .description("ID of the work area the task relates to")
                .type(STRING)
                .optional(),
            fieldWithPath("status")
                .description(
                    "Status of the task (Possible values are: " +
                        StringUtils.join(TaskStatusEnum.values(), ", ") +
                        ")"),
            fieldWithPath("creator.displayName").description("Name of the task creator"),
            fieldWithPath("creator.id").description("ID of the task creator"),
            fieldWithPath("creator.picture")
                .description("URL of the task creator's profile picture"),
            fieldWithPath("assignee.displayName")
                .description("Name of the foreman who should fulfill the task")
                .type(STRING)
                .optional(),
            fieldWithPath("assignee.id")
                .description("ID of the foreman who should fulfill the task")
                .type(STRING)
                .optional(),
            fieldWithPath("assignee.picture")
                .description("URL of the foreman's profile picture")
                .type(STRING)
                .optional(),
            fieldWithPath("location").description("Location for the task"),
            fieldWithPath("assigned").description("Flag indicating if task has been assigned"),
            fieldWithPath("createdBy.displayName").description("Name of the creator of the task"),
            fieldWithPath("createdBy.id").description("ID of the creator of the task"),
            fieldWithPath("createdDate").description("Date of the task creation"),
            fieldWithPath("lastModifiedBy.displayName")
                .description("Name of the user of last modification"),
            fieldWithPath("lastModifiedBy.id").description("ID of the user of last modification"),
            fieldWithPath("lastModifiedDate").description("Date of the last modification"),
            fieldWithPath("projectCraft.id").description("ID of this task's project projectcraft"),
            fieldWithPath("projectCraft.name")
                .description("Name of the task's project projectcraft"),
            fieldWithPath("projectCraft.color")
                .description("Color of the task's project projectcraft"),
            fieldWithPath("projectCraft.createdDate")
                .description("Date of creation of the task's project projectcraft"),
            fieldWithPath("projectCraft.lastModifiedDate")
                .description("Date of last modification of the task's project projectcraft"),
            fieldWithPath("projectCraft.createdBy.displayName")
                .description("Name of the creator of the task's project projectcraft"),
            fieldWithPath("projectCraft.createdBy.id")
                .description("Identifier of the creator of the task's project projectcraft"),
            fieldWithPath("projectCraft.lastModifiedBy.displayName")
                .description("Name of the modifier of the task's project projectcraft"),
            fieldWithPath("projectCraft.lastModifiedBy.id")
                .description("Identifier of the modifier of the task's project projectcraft"),
            fieldWithPath("projectCraft.version")
                .description("Version of the task's project projectcraft"),
            fieldWithPath("projectCraft.color")
                .description("Color of the task's project projectcraft"),
            fieldWithPath("projectCraft.project.displayName")
                .description("Reference name for the projectcraft's project"),
            fieldWithPath("projectCraft.project.id")
                .description("Reference id for the projectcraft's project"),
            fieldWithPath("projectCraft._links")
                .description("Links of the projectcraft")
                .optional(),
            fieldWithPath("editDate")
                .description("Date of last editing (changed status, name or description)")
                .type("Date")
                .optional(),
            subsectionWithPath("projectCraft._links").ignored(),
            subsectionWithPath("_links").ignored(),
            subsectionWithPath("_embedded")
                .optional()
                .description("Embedded resources")
                .type(OBJECT))

    @JvmStatic
    protected val PAGE_TASKS_RESPONSE_FIELDS: ResponseFieldsSnippet =
        responseFields(
                fieldWithPath("tasks[]").description("List of tasks").type(ARRAY),
                fieldWithPath("pageNumber").description("Number of this page"),
                fieldWithPath("pageSize").description("Size of this page"),
                fieldWithPath("totalPages")
                    .description("Total number of available pages for tasks"),
                fieldWithPath("totalElements").description("Total number of tasks available"),
                subsectionWithPath("_links").ignored())
            .andWithPrefix("tasks[].", TASK_RESPONSE_FIELD_DESCRIPTORS)

    @JvmStatic
    protected val TASKS_RESPONSE_FIELDS: ResponseFieldsSnippet =
        responseFields(fieldWithPath("tasks[]").description("List of tasks").type(ARRAY))
            .andWithPrefix("tasks[].", TASK_RESPONSE_FIELD_DESCRIPTORS)
            .and(subsectionWithPath("_links").ignored())

    @JvmStatic
    protected val TASK_LINKS: LinksSnippet =
        links(
            linkWithRel(LINK_ASSIGN).optional().description(LINK_ASSIGNED_FOREMAN_DESCRIPTION),
            linkWithRel(LINK_UNASSIGN).optional().description(LINK_UNASSIGNED_FOREMAN_DESCRIPTION),
            linkWithRel(LINK_START).optional().description(LINK_START_DESCRIPTION),
            linkWithRel(LINK_CLOSE).optional().description(LINK_CLOSE_DESCRIPTION),
            linkWithRel(LINK_ACCEPT).optional().description(LINK_ACCEPT_DESCRIPTION),
            linkWithRel(LINK_RESET).optional().description(LINK_RESET_DESCRIPTION),
            linkWithRel(LINK_TASK_UPDATE).optional().description(LINK_TASK_UPDATE_DESCRIPTION),
            linkWithRel(LINK_DELETE).optional().description(LINK_TASK_DELETE_DESCRIPTION),
            linkWithRel(LINK_TOPIC_CREATE).optional().description(LINK_TOPIC_CREATE_DESCRIPTION),
            linkWithRel(LINK_TOPIC).description(LINK_TOPIC_DESCRIPTION),
            linkWithRel(LINK_TASK_SCHEDULE).optional().description(LINK_TASK_SCHEDULE_DESCRIPTION),
            linkWithRel(LINK_CONSTRAINTS_UPDATE)
                .optional()
                .description(LINK_CONSTRAINTS_UPDATE_DESCRIPTION),
            linkWithRel(LINK_CONSTRAINTS).optional().description(LINK_CONSTRAINTS_DESCRIPTION))

    private fun concatRequestFields(
        fieldDescriptors: List<FieldDescriptor>,
        otherFieldDescriptors: List<FieldDescriptor>
    ): List<FieldDescriptor> = listOf(fieldDescriptors, otherFieldDescriptors).flatten()
  }
}
