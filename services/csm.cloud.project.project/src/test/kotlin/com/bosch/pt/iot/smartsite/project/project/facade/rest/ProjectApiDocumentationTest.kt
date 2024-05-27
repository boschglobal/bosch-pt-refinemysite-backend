/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.project.facade.rest

import com.bosch.pt.csm.cloud.common.command.messages.DeleteCommandAvro
import com.bosch.pt.csm.cloud.common.extensions.toLocalDateByMillis
import com.bosch.pt.csm.cloud.common.model.key.CommandMessageKey
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitEmployee
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeRoleEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.PARTICIPANT
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.PROJECT
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.WORKAREALIST
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftListEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.message.getCompanyIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.participant.message.getProjectIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.participant.message.getUserIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventG3Avro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantStatusEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.event.listener.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectCategoryEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectEventEnumAvro.UPDATED
import com.bosch.pt.csm.cloud.projectmanagement.workarea.message.getProjectIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaListEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaListEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.workday.messages.WorkdayConfigurationEventAvro
import com.bosch.pt.csm.cloud.usermanagement.common.UsermanagementAggregateTypeEnum.USER
import com.bosch.pt.csm.cloud.usermanagement.common.messages.PhoneNumberAvro
import com.bosch.pt.csm.cloud.usermanagement.common.messages.PhoneNumberTypeEnumAvro
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractApiDocumentationTestV2
import com.bosch.pt.iot.smartsite.common.facade.rest.ApiDocumentationSnippets.ABSTRACT_RESOURCE_FIELD_DESCRIPTORS
import com.bosch.pt.iot.smartsite.common.facade.rest.ConstrainedFields
import com.bosch.pt.iot.smartsite.common.facade.rest.CustomMockMvcResultMatchers.hasIdentifierAndVersion
import com.bosch.pt.iot.smartsite.common.facade.rest.CustomMockMvcResultMatchers.isCreatedBy
import com.bosch.pt.iot.smartsite.common.facade.rest.CustomMockMvcResultMatchers.isLastModifiedBy
import com.bosch.pt.iot.smartsite.common.kafka.AggregateIdentifierUtils.getAggregateIdentifier
import com.bosch.pt.iot.smartsite.common.kafka.messaging.CommandSendingService
import com.bosch.pt.iot.smartsite.project.milestone.facade.rest.MilestoneSearchApiDocumentationTest.Companion.LINK_CREATE_CRAFT_MILESTONE_DESCRIPTION
import com.bosch.pt.iot.smartsite.project.milestone.facade.rest.MilestoneSearchApiDocumentationTest.Companion.LINK_CREATE_INVESTORS_MILESTONE_DESCRIPTION
import com.bosch.pt.iot.smartsite.project.milestone.facade.rest.MilestoneSearchApiDocumentationTest.Companion.LINK_CREATE_PROJECT_MILESTONE_DESCRIPTION
import com.bosch.pt.iot.smartsite.project.milestone.facade.rest.resource.response.MilestoneResource.Companion.LINK_CREATE_CRAFT_MILESTONE
import com.bosch.pt.iot.smartsite.project.milestone.facade.rest.resource.response.MilestoneResource.Companion.LINK_CREATE_INVESTOR_MILESTONE
import com.bosch.pt.iot.smartsite.project.milestone.facade.rest.resource.response.MilestoneResource.Companion.LINK_CREATE_PROJECT_MILESTONE
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.project.facade.rest.ProjectController.Companion.PATH_VARIABLE_PROJECT_ID
import com.bosch.pt.iot.smartsite.project.project.facade.rest.ProjectController.Companion.PROJECTS_ENDPOINT
import com.bosch.pt.iot.smartsite.project.project.facade.rest.ProjectController.Companion.PROJECT_BY_PROJECT_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.project.facade.rest.resource.request.DeleteProjectResource
import com.bosch.pt.iot.smartsite.project.project.facade.rest.resource.request.SaveProjectResource
import com.bosch.pt.iot.smartsite.project.project.facade.rest.resource.response.ProjectAddressDto
import com.bosch.pt.iot.smartsite.project.project.facade.rest.resource.response.ProjectResource.Companion.LINK_COMPANIES
import com.bosch.pt.iot.smartsite.project.project.facade.rest.resource.response.ProjectResource.Companion.LINK_CREATE_PROJECT
import com.bosch.pt.iot.smartsite.project.project.facade.rest.resource.response.ProjectResource.Companion.LINK_DELETE_PROJECT
import com.bosch.pt.iot.smartsite.project.project.facade.rest.resource.response.ProjectResource.Companion.LINK_MILESTONES
import com.bosch.pt.iot.smartsite.project.project.facade.rest.resource.response.ProjectResource.Companion.LINK_PARTICIPANTS
import com.bosch.pt.iot.smartsite.project.project.facade.rest.resource.response.ProjectResource.Companion.LINK_PROJECT_CRAFTS
import com.bosch.pt.iot.smartsite.project.project.facade.rest.resource.response.ProjectResource.Companion.LINK_PROJECT_WORKAREAS
import com.bosch.pt.iot.smartsite.project.project.facade.rest.resource.response.ProjectResource.Companion.LINK_RESCHEDULE
import com.bosch.pt.iot.smartsite.project.project.facade.rest.resource.response.ProjectResource.Companion.LINK_TASKS
import com.bosch.pt.iot.smartsite.project.project.facade.rest.resource.response.ProjectResource.Companion.LINK_UPDATE_PROJECT
import com.bosch.pt.iot.smartsite.project.project.facade.rest.resource.response.ProjectResource.Companion.LINK_WORKDAY_CONFIGURATION
import com.bosch.pt.iot.smartsite.project.project.facade.rest.resource.response.factory.ProjectResourceFactory.Companion.LINK_CALENDAR_CUSTOM_SORT
import com.bosch.pt.iot.smartsite.project.project.facade.rest.resource.response.factory.ProjectResourceFactory.Companion.LINK_COPY
import com.bosch.pt.iot.smartsite.project.project.facade.rest.resource.response.factory.ProjectResourceFactory.Companion.LINK_EXPORT
import com.bosch.pt.iot.smartsite.project.project.facade.rest.resource.response.factory.ProjectResourceFactory.Companion.LINK_IMPORT
import com.bosch.pt.iot.smartsite.project.project.query.ProjectQueryService
import com.bosch.pt.iot.smartsite.project.project.shared.model.Project
import com.bosch.pt.iot.smartsite.project.project.shared.model.ProjectCategoryEnum
import com.bosch.pt.iot.smartsite.project.project.shared.model.ProjectCategoryEnum.OB
import com.bosch.pt.iot.smartsite.project.project.toAggregateReference
import com.bosch.pt.iot.smartsite.project.rfv.facade.rest.RfvApiDocumentationTest.Companion.LINK_UPDATE_RFVS_DESCRIPTION
import com.bosch.pt.iot.smartsite.project.rfv.facade.rest.resource.response.RfvResource.Companion.LINK_UPDATE_RFV
import com.bosch.pt.iot.smartsite.project.taskconstraint.facade.rest.TaskConstraintApiDocumentationTest.Companion.LINK_UPDATE_TASK_CONSTRAINTS_DESCRIPTION
import com.bosch.pt.iot.smartsite.project.taskconstraint.facade.rest.resource.response.TaskConstraintResource.Companion.LINK_UPDATE_CONSTRAINTS
import com.bosch.pt.iot.smartsite.project.workarea.domain.asWorkAreaListId
import com.bosch.pt.iot.smartsite.util.getIdentifier
import com.ninjasquad.springmockk.MockkBean
import io.mockk.verify
import java.time.LocalDate
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE
import java.time.temporal.ChronoUnit.DAYS
import java.util.UUID.randomUUID
import org.apache.commons.lang3.StringUtils
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.Matchers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders.ETAG
import org.springframework.http.HttpHeaders.IF_MATCH
import org.springframework.http.HttpHeaders.LOCATION
import org.springframework.restdocs.headers.HeaderDocumentation.headerWithName
import org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders
import org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders
import org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel
import org.springframework.restdocs.hypermedia.HypermediaDocumentation.links
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse
import org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint
import org.springframework.restdocs.payload.FieldDescriptor
import org.springframework.restdocs.payload.JsonFieldType.ARRAY
import org.springframework.restdocs.payload.JsonFieldType.OBJECT
import org.springframework.restdocs.payload.JsonFieldType.STRING
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.restdocs.snippet.Attributes.key
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@EnableAllKafkaListeners
class ProjectApiDocumentationTest : AbstractApiDocumentationTestV2() {

  @MockkBean(relaxed = true) private lateinit var commandSendingService: CommandSendingService

  private val constrainedField = ConstrainedFields(SaveProjectResource::class.java)

  private val projectRequestFields =
      requestFields(
          constrainedField
              .withPath("constructionSiteManagerId")
              .description("ID of construction site manager employee (Optional for non-admin user)")
              .optional()
              .type(STRING),
          constrainedField
              .withPath("client")
              .description("Name of the project client")
              .optional()
              .type(STRING),
          constrainedField
              .withPath("description")
              .description("Description of the project")
              .optional()
              .type(STRING),
          constrainedField.withPath("start").description("Start date of the project").type("Date"),
          constrainedField.withPath("end").description("End date of the project").type("Date"),
          constrainedField
              .withPath("projectNumber")
              .description("Number of the project")
              .type(STRING),
          constrainedField.withPath("title").description("Title of the project").type(STRING),
          constrainedField
              .withPath("category")
              .description("Category of the project")
              .optional()
              .type(STRING),
          constrainedField.withPath("address").description("Address of the project").type(OBJECT),
          constrainedField
              .withPath("address.city")
              .description("City o the project's address")
              .type(STRING)
              .attributes(key("constraints").value("Size must be between 1 and 100 inclusive")),
          constrainedField
              .withPath("address.houseNumber")
              .description("House number of the project's address")
              .type(STRING)
              .attributes(key("constraints").value("Size must be between 1 and 10 inclusive")),
          constrainedField
              .withPath("address.street")
              .description("Street of the project's address")
              .type(STRING)
              .attributes(key("constraints").value("Size must be between 1 and 100 inclusive")),
          constrainedField
              .withPath("address.zipCode")
              .description("Zip code of the project's address")
              .type(STRING)
              .attributes(key("constraints").value("Size must be between 1 and 10 inclusive")))

  private val projectDeleteRequestFields =
      requestFields(
          constrainedField
              .withPath("title")
              .description("Title of the project to delete")
              .type(STRING))

  private val projectResponseFields = responseFields(PROJECT_RESPONSE_FIELD_DESCRIPTORS)

  private val projectLinks =
      links(
          linkWithRel(LINK_PARTICIPANTS).description(LINK_PARTICIPANTS_DESCRIPTION),
          linkWithRel(LINK_COMPANIES).description(LINK_COMPANIES_DESCRIPTION),
          linkWithRel(LINK_TASKS).description(LINK_TASKS_DESCRIPTION),
          linkWithRel(LINK_PROJECT_WORKAREAS).description(LINK_PROJECT_WORKAREA_DESCRIPTION),
          linkWithRel(LINK_WORKDAY_CONFIGURATION)
              .description(LINK_PROJECT_WORKDAY_CONFIGURATION_DESCRIPTION),
          linkWithRel(LINK_MILESTONES).description(LINK_PROJECT_MILESTONE_DESCRIPTION),
          linkWithRel(LINK_PROJECT_CRAFTS).description(LINK_PROJECT_CRAFT_DESCRIPTION),
          linkWithRel(LINK_UPDATE_PROJECT).description(LINK_UPDATE_PROJECT_DESCRIPTION),
          linkWithRel(LINK_UPDATE_PROJECT).description(LINK_UPDATE_PROJECT_DESCRIPTION).optional(),
          linkWithRel(LINK_DELETE_PROJECT).description(LINK_DELETE_PROJECT_DESCRIPTION).optional(),
          linkWithRel(LINK_RESCHEDULE).description(LINK_RESCHEDULE_PROJECT_DESCRIPTION).optional(),
          linkWithRel(LINK_EXPORT).description(LINK_EXPORT_DESCRIPTION).optional(),
          linkWithRel(LINK_IMPORT).description(LINK_IMPORT_DESCRIPTION).optional(),
          linkWithRel(LINK_COPY).description(LINK_COPY_DESCRIPTION).optional(),
          linkWithRel(LINK_CREATE_CRAFT_MILESTONE)
              .description(LINK_CREATE_CRAFT_MILESTONE_DESCRIPTION),
          linkWithRel(LINK_CREATE_INVESTOR_MILESTONE)
              .description(LINK_CREATE_INVESTORS_MILESTONE_DESCRIPTION)
              .optional(),
          linkWithRel(LINK_CREATE_PROJECT_MILESTONE)
              .description(LINK_CREATE_PROJECT_MILESTONE_DESCRIPTION)
              .optional(),
          linkWithRel(LINK_UPDATE_RFV).description(LINK_UPDATE_RFVS_DESCRIPTION).optional(),
          linkWithRel(LINK_UPDATE_CONSTRAINTS)
              .description(LINK_UPDATE_TASK_CONSTRAINTS_DESCRIPTION)
              .optional(),
          linkWithRel(LINK_CALENDAR_CUSTOM_SORT)
              .description(LINK_CALENDAR_CUSTOM_SORT_DESCRIPTION)
              .optional())

  private val project1 by lazy {
    repositories.findProject(getIdentifier("project").asProjectId())!!
  }
  private val userCreator by lazy { repositories.findUser(getIdentifier("userCsm1"))!! }
  private val userTest by lazy { repositories.findUser(getIdentifier("userCsm2"))!! }

  @BeforeEach
  fun init() {
    eventStreamGenerator
        .setupDatasetTestData()
        .submitUser("userCsm1") {
          it.position = "Project lead 1"
          it.phoneNumbers =
              listOf(
                  PhoneNumberAvro(PhoneNumberTypeEnumAvro.MOBILE, "+49", "12345"),
                  PhoneNumberAvro(PhoneNumberTypeEnumAvro.BUSINESS, "+49", "23333333"),
              )
        }
        .submitUser("userCsm2") { it.position = "Project lead 2" }
        .submitProject(eventType = UPDATED) {
          it.title = EXPECTED_PROJECT1_TITLE
          it.category = ProjectCategoryEnumAvro.OB
          it.description = "Project description"
          it.client = "Project client"
        }
        .submitProject("project2") { it.title = EXPECTED_PROJECT2_TITLE }
        .submitParticipantG3("participantProject2") {
          it.project = getByReference("project2")
          it.user = getByReference("userCsm1")
          it.role = ParticipantRoleEnumAvro.CSM
        }

    setAuthentication(userTest.identifier!!)
    projectEventStoreUtils.reset()
  }

  @Test
  fun verifyAndDocumentCreateProject() {
    val startDate = LocalDate.now()
    val endDate = startDate.plus(2, DAYS)
    val projectAddressDto = ProjectAddressDto("city", "HN", "street", "ZC")
    val saveProjectResource =
        SaveProjectResource(
            client = "client",
            description = "description",
            start = startDate,
            end = endDate,
            projectNumber = "projectNumber",
            title = "title",
            category = OB,
            address = projectAddressDto)

    mockMvc
        .perform(requestBuilder(post(latestVersionOf(PROJECTS_ENDPOINT)), saveProjectResource))
        .andExpectAll(
            status().isCreated,
            header().string(ETAG, "\"0\""),
            header()
                .string(
                    LOCATION,
                    Matchers.matchesRegex(".*${latestVersionOf("/projects/[-a-z0-9]{36}$")}")),
            *hasIdentifierAndVersion(),
            *isCreatedBy(userTest),
            *isLastModifiedBy(userTest),
            jsonPath("$.id").exists(),
            jsonPath("$.client").value("client"),
            jsonPath("$.description").value("description"),
            jsonPath("$.start").value(startDate.format(ISO_LOCAL_DATE)),
            jsonPath("$.end").value(endDate.format(ISO_LOCAL_DATE)),
            jsonPath("$.projectNumber").value("projectNumber"),
            jsonPath("$.category").value(OB.toString()),
            jsonPath("$.address").exists(),
            jsonPath("$.address.city").value("city"),
            jsonPath("$.address.houseNumber").value("HN"),
            jsonPath("$.address.street").value("street"),
            jsonPath("$.address.zipCode").value("ZC"),
            jsonPath("$.constructionSiteManager").exists(),
            jsonPath("$.constructionSiteManager.displayName").value(userTest.getDisplayName()),
            jsonPath("$.constructionSiteManager.position").value(userTest.position),
            jsonPath("$.constructionSiteManager.phoneNumbers").isArray,
            jsonPath("$.constructionSiteManager.phoneNumbers.length()").value(1))
        .andDo(
            document(
                "projects/document-create-project",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                projectRequestFields,
                projectResponseFields,
                responseHeaders(
                    headerWithName(LOCATION).description("Location of created project resource")),
                projectLinks))

    verifyCreatedEventsProjectParticipantAndProjectList()
  }

  @Test
  fun verifyAndDocumentCreateProjectWithIdentifier() {
    val projectIdentifier = randomUUID()
    val startDate = LocalDate.now()
    val endDate = startDate.plus(2, DAYS)
    val projectAddressDto = ProjectAddressDto("city", "HN", "street", "ZC")
    val saveProjectResource =
        SaveProjectResource(
            client = "client",
            description = "description",
            start = startDate,
            end = endDate,
            projectNumber = "projectNumber",
            title = "title",
            category = OB,
            address = projectAddressDto)

    mockMvc
        .perform(
            requestBuilder(
                post(latestVersionOf(PROJECT_BY_PROJECT_ID_ENDPOINT), projectIdentifier),
                saveProjectResource))
        .andExpectAll(
            status().isCreated,
            header().string(ETAG, "\"0\""),
            header()
                .string(
                    LOCATION,
                    Matchers.matchesRegex(".*${latestVersionOf("/projects/$projectIdentifier$")}")),
            *hasIdentifierAndVersion(),
            *isCreatedBy(userTest),
            *isLastModifiedBy(userTest),
            jsonPath("$.id").value(projectIdentifier.toString()),
            jsonPath("$.client").value("client"),
            jsonPath("$.description").value("description"),
            jsonPath("$.start").value(startDate.format(ISO_LOCAL_DATE)),
            jsonPath("$.end").value(endDate.format(ISO_LOCAL_DATE)),
            jsonPath("$.projectNumber").value("projectNumber"),
            jsonPath("$.category").value(OB.toString()),
            jsonPath("$.address").exists(),
            jsonPath("$.address.city").value("city"),
            jsonPath("$.address.houseNumber").value("HN"),
            jsonPath("$.address.street").value("street"),
            jsonPath("$.address.zipCode").value("ZC"),
            jsonPath("$.constructionSiteManager").exists(),
            jsonPath("$.constructionSiteManager.displayName").value(userTest.getDisplayName()),
            jsonPath("$.constructionSiteManager.position").value(userTest.position),
            jsonPath("$.constructionSiteManager.phoneNumbers").isArray,
            jsonPath("$.constructionSiteManager.phoneNumbers.length()").value(1))
        .andDo(
            document(
                "projects/document-create-project-with-identifier",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                projectRequestFields,
                pathParameters(
                    parameterWithName(PATH_VARIABLE_PROJECT_ID).description("ID of the project")),
                projectLinks,
                projectResponseFields,
                responseHeaders(
                    headerWithName(LOCATION).description("Location of created project resource"))))

    verifyCreatedEventsProjectParticipantAndProjectList()
  }

  @Test
  fun verifyAndDocumentUpdateProject() {
    val startDate = LocalDate.now()
    val endDate = startDate.plus(2, DAYS)
    val projectAddressDto = ProjectAddressDto("city", "HN", "street", "ZC")
    val saveProjectResource =
        SaveProjectResource(
            client = "client",
            description = "description",
            start = startDate,
            end = endDate,
            projectNumber = "projectNumber",
            title = "title",
            category = OB,
            address = projectAddressDto)

    val expectedVersion = project1.version + 1

    mockMvc
        .perform(
            requestBuilder(
                put(latestVersionOf(PROJECT_BY_PROJECT_ID_ENDPOINT), project1.identifier),
                saveProjectResource,
                project1.version))
        .andExpectAll(
            status().isOk,
            header().string(ETAG, "\"$expectedVersion\""),
            *hasIdentifierAndVersion(project1.identifier.toUuid(), version = expectedVersion),
            *isCreatedBy(userCreator),
            *isLastModifiedBy(userTest),
            jsonPath("$.id").value(project1.identifier.toString()),
            jsonPath("$.client").value("client"),
            jsonPath("$.description").value("description"),
            jsonPath("$.start").value(startDate.format(ISO_LOCAL_DATE)),
            jsonPath("$.end").value(endDate.format(ISO_LOCAL_DATE)),
            jsonPath("$.projectNumber").value("projectNumber"),
            jsonPath("$.category").value(OB.toString()),
            jsonPath("$.address").exists(),
            jsonPath("$.address.city").value("city"),
            jsonPath("$.address.houseNumber").value("HN"),
            jsonPath("$.address.street").value("street"),
            jsonPath("$.address.zipCode").value("ZC"),
            jsonPath("$.constructionSiteManager").exists(),
            jsonPath("$.constructionSiteManager.displayName").value(userCreator.getDisplayName()),
            jsonPath("$.constructionSiteManager.position").value(userCreator.position),
            jsonPath("$.constructionSiteManager.phoneNumbers").isArray,
            jsonPath("$.constructionSiteManager.phoneNumbers.length()").value(2))
        .andDo(
            document(
                "projects/document-update-project",
                pathParameters(
                    parameterWithName(PATH_VARIABLE_PROJECT_ID).description("ID of the project")),
                requestHeaders(headerWithName(IF_MATCH).description("If-Match header with ETag")),
                projectRequestFields))
        .andDo(
            document(
                "projects/document-update-project",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                projectLinks,
                projectResponseFields))

    projectEventStoreUtils
        .verifyContainsAndGet(ProjectEventAvro::class.java, UPDATED)
        .aggregate
        .also { aggregate ->
          val project = repositories.findProject(aggregate.getIdentifier().asProjectId())!!

          validateUpdatedAggregateAuditInfoAndAggregateIdentifier(aggregate, project, PROJECT)
          verifyProjectAttributes(aggregate, project)
        }
  }

  @Test
  fun verifyCreateProjectWithOnlyMandatoryFields() {
    val projectIdentifier = randomUUID()
    val startDate = LocalDate.now()
    val endDate = startDate.plus(2, DAYS)
    val projectAddressDto = ProjectAddressDto("city", "HN", "street", "ZC")
    val saveProjectResource =
        SaveProjectResource(
            start = startDate,
            end = endDate,
            projectNumber = "projectNumber",
            title = "title",
            address = projectAddressDto)

    mockMvc
        .perform(
            requestBuilder(
                post(latestVersionOf(PROJECT_BY_PROJECT_ID_ENDPOINT), projectIdentifier),
                saveProjectResource))
        .andExpectAll(
            status().isCreated,
            header().string(ETAG, "\"0\""),
            header()
                .string(
                    LOCATION,
                    Matchers.matchesRegex(".*${latestVersionOf("/projects/$projectIdentifier$")}")),
            *hasIdentifierAndVersion(),
            *isCreatedBy(userTest),
            *isLastModifiedBy(userTest),
            jsonPath("$.id").value(projectIdentifier.toString()),
            jsonPath("$.end").exists(),
            jsonPath("$.start").exists(),
            jsonPath("$.projectNumber").value("projectNumber"),
            jsonPath("$.address").exists(),
            jsonPath("$.address.city").value("city"),
            jsonPath("$.address.houseNumber").value("HN"),
            jsonPath("$.address.street").value("street"),
            jsonPath("$.address.zipCode").value("ZC"))

    verifyCreatedEventsProjectParticipantAndProjectList()
  }

  @Test
  fun verifyAndDocumentDeleteProject() {
    val deleteResource = DeleteProjectResource(title = "Project1")

    mockMvc
        .perform(
            requestBuilder(
                delete(latestVersionOf(PROJECT_BY_PROJECT_ID_ENDPOINT), project1.identifier),
                deleteResource))
        .andExpect(status().isNoContent)
        .andDo(
            document(
                "projects/document-delete-project-with-identifier",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                projectDeleteRequestFields,
                pathParameters(
                    parameterWithName(PATH_VARIABLE_PROJECT_ID).description("ID of the project"))))

    // No event is sent since this is an asynch operation that triggers
    // a delete command sent via kafka first
    projectEventStoreUtils.verifyEmpty()

    verify(exactly = 1) {
      commandSendingService.send(
          CommandMessageKey(project1.identifier.toUuid()),
          DeleteCommandAvro(
              getAggregateIdentifier(project1, PROJECT.value),
              getAggregateIdentifier(userTest, USER.value)),
          "project-delete")
    }
  }

  @Test
  fun verifyAndDocumentFindProjectById() {
    mockMvc
        .perform(
            requestBuilder(
                get(latestVersionOf(PROJECT_BY_PROJECT_ID_ENDPOINT), project1.identifier)))
        .andExpect(status().isOk)
        .andExpect(header().string(ETAG, containsString(project1.version.toString())))
        .andExpect(jsonPath("$.title").value(project1.title))
        .andExpect(jsonPath("$.id").value(project1.identifier.toString()))
        .andExpect(jsonPath("$.participants").value(3))
        .andDo(
            document(
                "projects/document-get-project",
                pathParameters(
                    parameterWithName(PATH_VARIABLE_PROJECT_ID).description("ID of the project"))))
        .andDo(
            document(
                "projects/document-get-project",
                preprocessResponse(prettyPrint()),
                projectLinks,
                projectResponseFields))

    projectEventStoreUtils.verifyEmpty()
  }

  /**
   * Verifies finding a project by its id with an CR role provides the correct links.
   *
   * @throws Exception is not expected
   */
  @Test
  fun verifyFindProjectByIdWithEmployeeRoleCr() {
    eventStreamGenerator
        .setLastIdentifierForType(PROJECT.value, project1.identifier.toAggregateReference())
        .submitUser("userCr")
        .submitEmployee("employeeCr") { it.roles = listOf(EmployeeRoleEnumAvro.CR) }
        .submitParticipantG3("participantCr") { it.role = ParticipantRoleEnumAvro.CR }

    setAuthentication("userCr")

    mockMvc
        .perform(
            requestBuilder(
                get(latestVersionOf(PROJECT_BY_PROJECT_ID_ENDPOINT), project1.identifier)))
        .andExpect(status().isOk)
        .andExpect(header().exists(ETAG))
        .andExpect(jsonPath("$.title").value(EXPECTED_PROJECT1_TITLE))
        .andExpect(jsonPath("$.id").value(project1.identifier.toString()))
        .andExpect(jsonPath("$._links.participants").exists())

    projectEventStoreUtils.verifyEmpty()
  }

  /**
   * Verifies finding all projects as construction site manager.
   *
   * @throws Exception is not expected
   */
  @Test
  fun verifyAndDocumentFindAllProjectsAsConstructionSiteManager() {
    mockMvc
        .perform(
            requestBuilder(get(latestVersionOf(PROJECTS_ENDPOINT)))
                .param("size", "20")
                .param("page", "0"))
        .andExpect(status().isOk)
        .andExpectAll(
            jsonPath("$.projects").isArray,
            jsonPath("$.userActivated").value(true),
            jsonPath("$.projects.length()").value(1),
            jsonPath("$.projects[?(@.id == '" + project1.identifier.toString() + "')]").exists())
        .andDo(
            document(
                "projects/document-get-project",
                pathParameters(
                    buildSortingAndPagingParameterDescriptors(
                        ProjectQueryService.PROJECTS_ALLOWED_SORTING_PROPERTIES.keys))))
        .andDo(
            document(
                "projects/document-get-projects",
                preprocessResponse(prettyPrint()),
                links(
                    linkWithRel(LINK_CREATE_PROJECT).description(LINK_CREATE_PROJECT_DESCRIPTION)),
                responseFields(
                    subsectionWithPath("projects").description("Page of projects"),
                    fieldWithPath("userActivated")
                        .description("Flag indicating if a user was assigned to a company yet."),
                    fieldWithPath("pageNumber").description("Number of this page"),
                    fieldWithPath("pageSize").description("Size of this page"),
                    fieldWithPath("totalPages")
                        .description("Total number of available pages for projects"),
                    fieldWithPath("totalElements").description("Total number of project available"),
                    subsectionWithPath("_links").ignored())))

    projectEventStoreUtils.verifyEmpty()
  }

  /**
   * Verifies finding all projects does not return create link for a user that is not a construction
   * site manager.
   *
   * @throws Exception is not expected
   */
  @Test
  fun verifyFindAllProjectsAsNotConstructionSiteManager() {
    setAuthentication("user")

    mockMvc
        .perform(requestBuilder(get(latestVersionOf(PROJECTS_ENDPOINT))))
        .andExpect(status().isOk)
        .andExpect(jsonPath("$._links.create").doesNotExist())

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun verifyFindingNonExistingProjectFails() {
    mockMvc
        .perform(requestBuilder(get(latestVersionOf(PROJECT_BY_PROJECT_ID_ENDPOINT), randomUUID())))
        .andExpect(status().isForbidden)

    projectEventStoreUtils.verifyEmpty()
  }

  private fun verifyCreatedEventsProjectParticipantAndProjectList() {
    projectEventStoreUtils.verifyContainsInSequence(
        ProjectEventAvro::class.java,
        ParticipantEventG3Avro::class.java,
        ProjectCraftListEventAvro::class.java,
        WorkAreaListEventAvro::class.java,
        WorkdayConfigurationEventAvro::class.java)

    projectEventStoreUtils
        .verifyContainsAndGet(ProjectEventAvro::class.java, ProjectEventEnumAvro.CREATED, 1, false)
        .first()
        .aggregate
        .also { aggregate ->
          val project = repositories.findProject(aggregate.getIdentifier().asProjectId())!!
          validateCreatedAggregateAuditInfoAndAggregateIdentifier(aggregate, PROJECT, userTest)
          verifyProjectAttributes(aggregate, project)
        }

    projectEventStoreUtils
        .verifyContainsAndGet(
            ParticipantEventG3Avro::class.java, ParticipantEventEnumAvro.CREATED, 1, false)
        .first()
        .aggregate
        .also { aggregate ->
          val participant = repositories.findParticipant(aggregate.getIdentifier())!!
          validateCreatedAggregateAuditInfoAndAggregateIdentifier(aggregate, PARTICIPANT, userTest)
          assertThat(aggregate.getCompanyIdentifier()).isEqualTo(participant.company!!.identifier)
          assertThat(aggregate.getProjectIdentifier().asProjectId())
              .isEqualTo(participant.project!!.identifier)
          assertThat(aggregate.getUserIdentifier()).isEqualTo(participant.user!!.identifier)
          assertThat(aggregate.role.name).isEqualTo(participant.role!!.name)
          assertThat(aggregate.role).isEqualTo(ParticipantRoleEnumAvro.CSM)
          assertThat(aggregate.status.name).isEqualTo(participant.status!!.name)
          assertThat(aggregate.status).isEqualTo(ParticipantStatusEnumAvro.ACTIVE)
        }

    projectEventStoreUtils
        .verifyContainsAndGet(
            WorkAreaListEventAvro::class.java, WorkAreaListEventEnumAvro.CREATED, 1, false)
        .first()
        .aggregate
        .also { aggregate ->
          val workAreaList =
              repositories.findWorkAreaList(aggregate.getIdentifier().asWorkAreaListId())!!
          validateCreatedAggregateAuditInfoAndAggregateIdentifier(aggregate, WORKAREALIST, userTest)
          assertThat(aggregate.getProjectIdentifier().asProjectId())
              .isEqualTo(workAreaList.project.identifier)
          assertThat(aggregate.workAreas).isEmpty()
        }
  }

  private fun verifyProjectAttributes(aggregate: ProjectAggregateAvro, entity: Project) {
    assertThat(aggregate.client).isEqualTo(entity.client)
    assertThat(aggregate.description).isEqualTo(entity.description)
    assertThat(aggregate.end.toLocalDateByMillis()).isEqualTo(entity.end)
    assertThat(aggregate.start.toLocalDateByMillis()).isEqualTo(entity.start)
    assertThat(aggregate.projectNumber).isEqualTo(entity.projectNumber)
    assertThat(aggregate.title).isEqualTo(entity.title)
    aggregate.projectAddress!!.apply {
      assertThat(city).isEqualTo(entity.projectAddress!!.city)
      assertThat(houseNumber).isEqualTo(entity.projectAddress!!.houseNumber)
      assertThat(street).isEqualTo(entity.projectAddress!!.street)
      assertThat(zipCode).isEqualTo(entity.projectAddress!!.zipCode)
    }
    if (aggregate.category == null) {
      assertThat(entity.category).isNull()
    } else {
      assertThat(aggregate.category.name).isEqualTo(entity.category!!.name)
    }
  }

  companion object {
    var PROJECT_RESPONSE_FIELD_DESCRIPTORS: List<FieldDescriptor> =
        listOf(
            *ABSTRACT_RESOURCE_FIELD_DESCRIPTORS,
            fieldWithPath("client").description("Name of the project client"),
            fieldWithPath("description").description("Description of the project"),
            fieldWithPath("end").type("Date").description("End date of the project"),
            fieldWithPath("start").type("Date").description("Start date of the project"),
            fieldWithPath("projectNumber").description("Number of the project"),
            fieldWithPath("participants").description("Number of the project participants"),
            fieldWithPath("title").description("Title of the project"),
            fieldWithPath("category")
                .description(
                    "Category of the project (Possible values are: " +
                        StringUtils.join(ProjectCategoryEnum.values(), ", " + "") +
                        ")"),
            fieldWithPath("company.displayName")
                .description("Name of the company of the project creator"),
            fieldWithPath("company.id")
                .description("Identifier of the company of the project creator"),
            fieldWithPath("address.city").description("City of the project's address"),
            fieldWithPath("address.houseNumber")
                .description("House number of the project's address"),
            fieldWithPath("address.street").description("Street of the project's address"),
            fieldWithPath("address.zipCode").description("Zip code of the project's address"),
            fieldWithPath("constructionSiteManager")
                .optional()
                .type(OBJECT)
                .description("Construction site manager of the project"),
            fieldWithPath("constructionSiteManager.displayName")
                .optional()
                .type(STRING)
                .description("Name of the construction site manager of the project"),
            fieldWithPath("constructionSiteManager.position")
                .optional()
                .type(STRING)
                .description("Position of the construction site manager of the project"),
            subsectionWithPath("constructionSiteManager.phoneNumbers")
                .optional()
                .type(ARRAY)
                .description("Phone numbers of the construction site manager of the project"),
            subsectionWithPath("_links").ignored(),
            subsectionWithPath("_embedded")
                .optional()
                .description("Embedded resources")
                .type(OBJECT))

    private const val LINK_PARTICIPANTS_DESCRIPTION =
        "Link to the list of participants of the project"
    private const val LINK_COMPANIES_DESCRIPTION =
        "Link to the list of participant companies of the project"
    private const val LINK_TASKS_DESCRIPTION = "Link to the list of tasks of the project"
    private const val LINK_PROJECT_CRAFT_DESCRIPTION =
        "Link to the list of project crafts of the project"
    private const val LINK_PROJECT_WORKAREA_DESCRIPTION =
        "Link to the list of work areas of the project"
    private const val LINK_PROJECT_WORKDAY_CONFIGURATION_DESCRIPTION =
        "Link to the list of workday configurations of the project"
    private const val LINK_PROJECT_MILESTONE_DESCRIPTION =
        "Link to the list of milestones of the project"
    private const val LINK_CREATE_PROJECT_DESCRIPTION = "Link to create a new project"
    private const val LINK_UPDATE_PROJECT_DESCRIPTION = "Link to update a project"
    private const val LINK_DELETE_PROJECT_DESCRIPTION = "Link to delete a project"
    private const val LINK_RESCHEDULE_PROJECT_DESCRIPTION =
        "Optional link to reschedule data in the project"
    private const val LINK_EXPORT_DESCRIPTION =
        "Optional link indicating that data can be exported to file " +
            "(only available on the single-project endpoint)"
    private const val LINK_IMPORT_DESCRIPTION =
        "Optional link indicating that data can be import from file " +
            "(only available on the single-project endpoint)"
    private const val LINK_COPY_DESCRIPTION =
        "Optional link indicating that project data can be copied into an new project " +
            "(only available on the single-project endpoint)"
    private const val LINK_CALENDAR_CUSTOM_SORT_DESCRIPTION =
        "Optional link indicating that the custom calendar sorting feature is enabled for the project " +
            "and/or the user's company."
    private const val EXPECTED_PROJECT1_TITLE = "Project1"
    private const val EXPECTED_PROJECT2_TITLE = "Project2"
  }
}
