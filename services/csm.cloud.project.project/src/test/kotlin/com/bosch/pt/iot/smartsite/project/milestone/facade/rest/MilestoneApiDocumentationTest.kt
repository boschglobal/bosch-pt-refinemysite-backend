/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.milestone.facade.rest

import com.bosch.pt.csm.cloud.common.extensions.toLocalDateByMillis
import com.bosch.pt.csm.cloud.common.facade.rest.resource.request.BatchRequestResource
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.MILESTONE
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.MILESTONELIST
import com.bosch.pt.csm.cloud.projectmanagement.milestone.message.getVersion
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneListEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneListEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMilestone
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMilestoneList
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkArea
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractApiDocumentationTestV2
import com.bosch.pt.iot.smartsite.common.facade.rest.ApiDocumentationSnippets.ABSTRACT_RESOURCE_FIELD_DESCRIPTORS
import com.bosch.pt.iot.smartsite.common.facade.rest.ApiDocumentationSnippets.CRAFT_REFERENCE_FIELD_DESCRIPTORS
import com.bosch.pt.iot.smartsite.common.facade.rest.ApiDocumentationSnippets.CREATOR_PARTICIPANT_REFERENCE_FIELD_DESCRIPTORS
import com.bosch.pt.iot.smartsite.common.facade.rest.ApiDocumentationSnippets.ETAG_HEADER
import com.bosch.pt.iot.smartsite.common.facade.rest.ApiDocumentationSnippets.ETAG_HEADER_DESCRIPTOR
import com.bosch.pt.iot.smartsite.common.facade.rest.ApiDocumentationSnippets.LOCATION_HEADER_DESCRIPTOR
import com.bosch.pt.iot.smartsite.common.facade.rest.ApiDocumentationSnippets.PROJECT_REFERENCE_FIELD_DESCRIPTORS
import com.bosch.pt.iot.smartsite.common.facade.rest.ApiDocumentationSnippets.WORK_AREA_REFERENCE_FIELD_DESCRIPTORS
import com.bosch.pt.iot.smartsite.common.facade.rest.ConstrainedFields
import com.bosch.pt.iot.smartsite.common.facade.rest.CustomMockMvcResultMatchers.hasIdentifierAndVersion
import com.bosch.pt.iot.smartsite.common.facade.rest.CustomMockMvcResultMatchers.isCreatedBy
import com.bosch.pt.iot.smartsite.common.facade.rest.CustomMockMvcResultMatchers.isLastModifiedBy
import com.bosch.pt.iot.smartsite.project.milestone.domain.asMilestoneId
import com.bosch.pt.iot.smartsite.project.milestone.facade.rest.MilestoneController.Companion.PATH_VARIABLE_MILESTONE_ID
import com.bosch.pt.iot.smartsite.project.milestone.facade.rest.MilestoneController.Companion.PATH_VARIABLE_PROJECT_ID
import com.bosch.pt.iot.smartsite.project.milestone.facade.rest.resource.request.CreateMilestoneResource
import com.bosch.pt.iot.smartsite.project.milestone.facade.rest.resource.request.UpdateMilestoneResource
import com.bosch.pt.iot.smartsite.project.milestone.facade.rest.resource.response.MilestoneResource.Companion.LINK_MILESTONE_DELETE
import com.bosch.pt.iot.smartsite.project.milestone.facade.rest.resource.response.MilestoneResource.Companion.LINK_MILESTONE_UPDATE
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.MilestoneTypeEnum.CRAFT
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.MilestoneTypeEnum.PROJECT
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.asProjectCraftId
import com.bosch.pt.iot.smartsite.project.workarea.domain.asWorkAreaId
import com.bosch.pt.iot.smartsite.util.getIdentifier
import java.time.LocalDate
import java.util.UUID
import java.util.UUID.randomUUID
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.matchesRegex
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.hateoas.MediaTypes.HAL_JSON_VALUE
import org.springframework.http.HttpHeaders.LOCATION
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
import org.springframework.restdocs.payload.JsonFieldType.BOOLEAN
import org.springframework.restdocs.payload.JsonFieldType.NUMBER
import org.springframework.restdocs.payload.JsonFieldType.STRING
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.restdocs.snippet.Attributes.key
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

// TODO: [SMAR-12593] Replace with test-type specific annotations
@EnableAllKafkaListeners
class MilestoneApiDocumentationTest : AbstractApiDocumentationTestV2() {

  private val project by lazy { repositories.findProject(getIdentifier("project").asProjectId())!! }
  private val milestone by lazy {
    repositories.findMilestoneWithDetails(getIdentifier("milestone").asMilestoneId())
  }
  private val craft by lazy {
    repositories.findProjectCraft(getIdentifier("projectCraft").asProjectCraftId())!!
  }
  private val workArea by lazy {
    repositories.findWorkArea(getIdentifier("workArea").asWorkAreaId())!!
  }
  private val userCsm1 by lazy { repositories.findUser(getIdentifier("userCsm1"))!! }
  private val participantCsm1 by lazy {
    repositories.findParticipant(getIdentifier("participantCsm1"))!!
  }
  private val testUser by lazy { repositories.findUser(getIdentifier("userCsm2"))!! }
  private val participantCsm2 by lazy {
    repositories.findParticipant(getIdentifier("participantCsm2"))!!
  }

  @BeforeEach
  fun setup() {
    eventStreamGenerator.setupDatasetTestData()

    setAuthentication(getIdentifier("userCsm2"))
    projectEventStoreUtils.reset()
  }

  @Test
  fun `verify and document create milestone`() {
    val createMilestoneResource =
        CreateMilestoneResource(
            projectId = project.identifier,
            name = "Shell completed",
            type = CRAFT,
            date = LocalDate.now(),
            header = false,
            craftId = craft.identifier,
            workAreaId = workArea.identifier)

    this.mockMvc
        .perform(
            requestBuilder(post(latestVersionOf("/projects/milestones")), createMilestoneResource))
        .andExpectAll(
            status().isCreated,
            header().string(ETAG_HEADER, "\"0\""),
            header()
                .string(
                    LOCATION,
                    matchesRegex(".*${latestVersionOf("/projects/milestones")}/[-a-z\\d]{36}\$")),
            *hasIdentifierAndVersion(),
            *isCreatedBy(testUser),
            *isLastModifiedBy(testUser),
            jsonPath("$.name").value(createMilestoneResource.name),
            jsonPath("$.type").value(createMilestoneResource.type.name),
            jsonPath("$.date").value(createMilestoneResource.date.toString()),
            jsonPath("$.header").value(createMilestoneResource.header),
            jsonPath("$.project.id").value(project.identifier.toString()),
            jsonPath("$.project.displayName").value(project.getDisplayName()),
            jsonPath("$.craft.id").value(craft.identifier.toString()),
            jsonPath("$.craft.displayName").value(craft.getDisplayName()),
            jsonPath("$.workArea.id").value(workArea.identifier.toString()),
            jsonPath("$.workArea.displayName").value(workArea.getDisplayName()),
            jsonPath("$.creator.id").value(participantCsm2.identifier.toString()),
            jsonPath("$.creator.displayName").value(testUser.getDisplayName()),
            jsonPath("$.creator.picture").isNotEmpty,
            jsonPath("$.position").value(0),
            jsonPath("$._links.update").exists(),
            jsonPath("$._links.delete").exists(),
        )
        .andDo(
            document(
                "milestones/document-create-milestone",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestFields(CREATE_MILESTONE_REQUEST_FIELD_DESCRIPTORS),
                responseFields(MILESTONE_RESPONSE_FIELD_DESCRIPTORS),
                responseHeaders(ETAG_HEADER_DESCRIPTOR, LOCATION_HEADER_DESCRIPTOR),
                links(MILESTONE_LINK_DESCRIPTORS)))

    val milestoneIdentifier: UUID

    projectEventStoreUtils.verifyContainsInSequence(
        listOf(MilestoneEventAvro::class.java, MilestoneListEventAvro::class.java))

    projectEventStoreUtils
        .verifyContainsAndGet(
            MilestoneEventAvro::class.java, MilestoneEventEnumAvro.CREATED, 1, false)
        .also {
          it.first().aggregate.also { aggregate ->
            validateCreatedAggregateAuditInfoAndAggregateIdentifier(aggregate, MILESTONE, testUser)
            assertThat(aggregate.name).isEqualTo(createMilestoneResource.name)
            assertThat(aggregate.type.name).isEqualTo(createMilestoneResource.type.name)
            assertThat(aggregate.date.toLocalDateByMillis()).isEqualTo(createMilestoneResource.date)
            assertThat(aggregate.header).isEqualTo(createMilestoneResource.header)
            assertThat(aggregate.description).isEqualTo(createMilestoneResource.description)
            assertThat(aggregate.craft.identifier)
                .isEqualTo(createMilestoneResource.craftId.toString())
            assertThat(aggregate.workarea.identifier)
                .isEqualTo(createMilestoneResource.workAreaId.toString())
            assertThat(aggregate.project.identifier)
                .isEqualTo(createMilestoneResource.projectId.toString())
            milestoneIdentifier = aggregate.getIdentifier()
          }
        }

    projectEventStoreUtils
        .verifyContainsAndGet(
            MilestoneListEventAvro::class.java, MilestoneListEventEnumAvro.CREATED, 1, false)
        .also {
          it.first().aggregate.also { aggregate ->
            validateCreatedAggregateAuditInfoAndAggregateIdentifier(
                aggregate, MILESTONELIST, testUser)
            assertThat(aggregate.project.identifier)
                .isEqualTo(createMilestoneResource.projectId.toString())
            assertThat(aggregate.date.toLocalDateByMillis()).isEqualTo(createMilestoneResource.date)
            assertThat(aggregate.header).isEqualTo(createMilestoneResource.header)
            assertThat(aggregate.workarea.identifier)
                .isEqualTo(createMilestoneResource.workAreaId.toString())
            aggregate.milestones.first().also { milestone ->
              assertThat(milestone.identifier).isEqualTo(milestoneIdentifier.toString())
              assertThat(milestone.type).isEqualTo(MILESTONE.value)
            }
          }
        }
  }

  @Test
  fun `verify and document create milestone with identifier`() {
    val milestoneIdentifier = randomUUID()
    val createMilestoneResource =
        CreateMilestoneResource(
            projectId = project.identifier,
            name = "Shell completed",
            type = PROJECT,
            date = LocalDate.now(),
            header = true)

    this.mockMvc
        .perform(
            requestBuilder(
                post(latestVersionOf("/projects/milestones/{milestoneId}"), milestoneIdentifier),
                createMilestoneResource))
        .andExpectAll(
            status().isCreated,
            header().string(ETAG_HEADER, "\"0\""),
            header()
                .string(
                    LOCATION,
                    matchesRegex(
                        ".*${latestVersionOf("/projects/milestones")}/$milestoneIdentifier$")),
            jsonPath("$._links.update").exists(),
            jsonPath("$._links.delete").exists(),
        )
        .andDo(
            document(
                "milestones/document-create-milestone-with-identifier",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(MILESTONE_ID_PATH_PARAMETER_DESCRIPTOR),
                requestFields(CREATE_MILESTONE_REQUEST_FIELD_DESCRIPTORS),
                responseFields(MILESTONE_RESPONSE_FIELD_DESCRIPTORS),
                responseHeaders(ETAG_HEADER_DESCRIPTOR, LOCATION_HEADER_DESCRIPTOR),
                links(MILESTONE_LINK_DESCRIPTORS)))

    projectEventStoreUtils.verifyContainsInSequence(
        listOf(MilestoneEventAvro::class.java, MilestoneListEventAvro::class.java))

    projectEventStoreUtils
        .verifyContainsAndGet(
            MilestoneEventAvro::class.java, MilestoneEventEnumAvro.CREATED, 1, false)
        .also {
          it.first().aggregate.also { aggregate ->
            validateCreatedAggregateAuditInfoAndAggregateIdentifier(aggregate, MILESTONE, testUser)
            assertThat(aggregate.aggregateIdentifier.type).isEqualTo(MILESTONE.toString())
            assertThat(aggregate.aggregateIdentifier.identifier)
                .isEqualTo(milestoneIdentifier.toString())
            assertThat(aggregate.aggregateIdentifier.version).isEqualTo(0)
          }
        }
  }

  @Test
  fun `verify and document find one milestone`() {
    mockMvc
        .perform(
            requestBuilder(
                get(latestVersionOf("/projects/milestones/{milestoneId}"), milestone.identifier)))
        .andExpectAll(
            status().isOk,
            content().contentType(HAL_JSON_VALUE),
            jsonPath("$.name").value(milestone.name),
            jsonPath("$.type").value(milestone.type.name),
            jsonPath("$.date").value(milestone.date.toString()),
            jsonPath("$.header").value(milestone.header),
            jsonPath("$.project.id").value(project.identifier.toString()),
            jsonPath("$.project.displayName").value(project.getDisplayName()),
            jsonPath("$.craft.id").doesNotExist(),
            jsonPath("$.craft.displayName").doesNotExist(),
            jsonPath("$.workArea.id").doesNotExist(),
            jsonPath("$.workArea.displayName").doesNotExist(),
            jsonPath("$.creator.id").value(participantCsm1.identifier.toString()),
            jsonPath("$.creator.displayName").value(userCsm1.getDisplayName()),
            jsonPath("$.creator.picture").isNotEmpty,
        )
        .andDo(
            document(
                "milestones/document-get-milestone",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(MILESTONE_ID_PATH_PARAMETER_DESCRIPTOR),
                responseHeaders(ETAG_HEADER_DESCRIPTOR),
                responseFields(MILESTONE_RESPONSE_FIELD_DESCRIPTORS),
                links(MILESTONE_LINK_DESCRIPTORS)))

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify and document batch find of milestones`() {
    val batchRequestResource = BatchRequestResource(setOf(milestone.identifier.toUuid()))

    mockMvc
        .perform(
            requestBuilder(
                post(
                    latestVersionOf("/projects/{projectId}/milestones/batch/find"),
                    project.identifier),
                batchRequestResource))
        .andExpectAll(
            status().isOk,
            content().contentType(HAL_JSON_VALUE),
            jsonPath("$.items.length()").value(1),
            jsonPath("$.items[0].name").value(milestone.name),
            jsonPath("$.items[0].type").value(milestone.type.name),
            jsonPath("$.items[0].date").value(milestone.date.toString()),
            jsonPath("$.items[0].header").value(milestone.header),
            jsonPath("$.items[0].project.id").value(project.identifier.toString()),
            jsonPath("$.items[0].project.displayName").value(project.getDisplayName()),
            jsonPath("$.items[0].craft.id").doesNotExist(),
            jsonPath("$.items[0].craft.displayName").doesNotExist(),
            jsonPath("$.items[0].workArea.id").doesNotExist(),
            jsonPath("$.items[0].workArea.displayName").doesNotExist(),
            jsonPath("$.items[0].creator.id").value(participantCsm1.identifier.toString()),
            jsonPath("$.items[0].creator.displayName").value(userCsm1.getDisplayName()),
            jsonPath("$.items[0].creator.picture").isNotEmpty,
        )
        .andDo(
            document(
                "milestones/document-batch-find-milestones",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(PROJECT_ID_PATH_PARAMETER_DESCRIPTOR),
                requestFields(BATCH_REQUEST_RESOURCE_FIELD_DESCRIPTORS),
                BATCH_FIND_RESPONSE_FIELDS))

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify and document update milestone`() {
    val updateMilestoneResource =
        UpdateMilestoneResource(
            name = "Shell completed",
            type = CRAFT,
            date = LocalDate.now(),
            header = false,
            craftId = craft.identifier,
            workAreaId = workArea.identifier)

    this.mockMvc
        .perform(
            requestBuilder(
                put(latestVersionOf("/projects/milestones/{milestoneId}"), milestone.identifier),
                updateMilestoneResource,
                0L))
        .andExpectAll(
            status().isOk,
            header().string(ETAG_HEADER, "\"1\""),
            *isCreatedBy(userCsm1),
            *isLastModifiedBy(testUser),
            jsonPath("$.name").value(updateMilestoneResource.name),
            jsonPath("$.type").value(updateMilestoneResource.type.name),
            jsonPath("$.date").value(updateMilestoneResource.date.toString()),
            jsonPath("$.header").value(updateMilestoneResource.header),
            jsonPath("$.project.id").value(project.identifier.toString()),
            jsonPath("$.project.displayName").value(project.getDisplayName()),
            jsonPath("$.craft.id").value(craft.identifier.toString()),
            jsonPath("$.craft.displayName").value(craft.getDisplayName()),
            jsonPath("$.workArea.id").value(workArea.identifier.toString()),
            jsonPath("$.workArea.displayName").value(workArea.getDisplayName()),
            jsonPath("$.creator.id").value(participantCsm1.identifier.toString()),
            jsonPath("$.creator.displayName").value(userCsm1.getDisplayName()),
            jsonPath("$.creator.picture").isNotEmpty,
            jsonPath("$._links.update").exists(),
            jsonPath("$._links.delete").exists(),
        )
        .andDo(
            document(
                "milestones/document-update-milestone",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(MILESTONE_ID_PATH_PARAMETER_DESCRIPTOR),
                requestFields(UPDATE_MILESTONE_REQUEST_FIELD_DESCRIPTORS),
                responseFields(MILESTONE_RESPONSE_FIELD_DESCRIPTORS),
                responseHeaders(ETAG_HEADER_DESCRIPTOR),
                links(MILESTONE_LINK_DESCRIPTORS)))

    projectEventStoreUtils.verifyContainsInSequence(
        listOf(
            MilestoneEventAvro::class.java,
            MilestoneListEventAvro::class.java,
            MilestoneListEventAvro::class.java))

    val updatedMilestone = repositories.findMilestone(getIdentifier("milestone").asMilestoneId())!!

    projectEventStoreUtils
        .verifyContainsAndGet(
            MilestoneEventAvro::class.java, MilestoneEventEnumAvro.UPDATED, 1, false)
        .also {
          it.first().aggregate.also { aggregate ->
            validateUpdatedAggregateAuditInfoAndAggregateIdentifier(
                aggregate, updatedMilestone, MILESTONE)
            assertThat(aggregate.name).isEqualTo(updateMilestoneResource.name)
            assertThat(aggregate.type.toString()).isEqualTo(updateMilestoneResource.type.name)
            assertThat(aggregate.date.toLocalDateByMillis()).isEqualTo(updateMilestoneResource.date)
            assertThat(aggregate.header).isEqualTo(updateMilestoneResource.header)
            assertThat(aggregate.description).isEqualTo(updateMilestoneResource.description)
            assertThat(aggregate.craft.identifier)
                .isEqualTo(updateMilestoneResource.craftId.toString())
            assertThat(aggregate.workarea.identifier)
                .isEqualTo(updateMilestoneResource.workAreaId.toString())
          }
        }

    projectEventStoreUtils.verifyContains(
        MilestoneListEventAvro::class.java, MilestoneListEventEnumAvro.DELETED, 1, false)

    projectEventStoreUtils
        .verifyContainsAndGet(
            MilestoneListEventAvro::class.java, MilestoneListEventEnumAvro.CREATED, 1, false)
        .also {
          it.first().aggregate.also { aggregate ->
            validateCreatedAggregateAuditInfoAndAggregateIdentifier(
                aggregate, MILESTONELIST, testUser)
            assertThat(aggregate.project.identifier)
                .isEqualTo(milestone.project.identifier.toString())
            assertThat(aggregate.date.toLocalDateByMillis()).isEqualTo(updateMilestoneResource.date)
            assertThat(aggregate.header).isEqualTo(updateMilestoneResource.header)
            assertThat(aggregate.workarea.identifier)
                .isEqualTo(updateMilestoneResource.workAreaId.toString())
            aggregate.milestones.first().also { milestoneReference ->
              assertThat(milestoneReference.identifier).isEqualTo(milestone.identifier.toString())
            }
          }
        }
  }

  @Test
  fun `verify and document delete milestone`() {
    eventStreamGenerator
        .submitWorkArea(asReference = "anotherWorkArea")
        .submitMilestone(asReference = "anotherMilestone") {
          it.workarea = getByReference("anotherWorkArea")
        }
        .submitMilestoneList(asReference = "anotherMilestoneList") {
          it.workarea = getByReference("anotherWorkArea")
          it.milestones = listOf(getByReference("anotherMilestone"))
        }

    projectEventStoreUtils.reset()

    val milestone = repositories.findMilestone(getIdentifier("anotherMilestone").asMilestoneId())!!

    this.mockMvc
        .perform(
            requestBuilder(
                delete(
                    latestVersionOf("/projects/milestones/{milestoneId}"),
                    getIdentifier("anotherMilestone")),
                null,
                0L))
        .andExpect(status().isNoContent)
        .andDo(
            document(
                "milestones/document-delete-milestone",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(MILESTONE_ID_PATH_PARAMETER_DESCRIPTOR)))

    projectEventStoreUtils.verifyContainsInSequence(
        listOf(MilestoneListEventAvro::class.java, MilestoneEventAvro::class.java))

    projectEventStoreUtils
        .verifyContainsAndGet(
            MilestoneListEventAvro::class.java, MilestoneListEventEnumAvro.DELETED, 1, false)
        .also {
          it.first().aggregate.also { ml ->
            assertThat(ml.getVersion()).isEqualTo(1)
            ml.milestones.first().also { ms ->
              assertThat(ms.identifier).isEqualTo(milestone.identifier.toString())
              assertThat(ms.version).isEqualTo(0)
            }
          }
        }

    projectEventStoreUtils
        .verifyContainsAndGet(MilestoneEventAvro::class.java, DELETED, 1, false)
        .also {
          it.first().aggregate.also { aggregate ->
            validateDeletedAggregateAuditInfoAndAggregateIdentifier(
                aggregate, milestone, MILESTONE, testUser)
            assertThat(aggregate.getIdentifier()).isEqualTo(milestone.identifier.toUuid())
            assertThat(aggregate.getVersion()).isEqualTo(1)
          }
        }
  }

  companion object {

    private const val LINK_UPDATE_DESCRIPTION = "Links to update a milestone"
    private const val LINK_DELETE_DESCRIPTION = "Link to delete a milestone"

    private val PROJECT_ID_PATH_PARAMETER_DESCRIPTOR =
        parameterWithName(PATH_VARIABLE_PROJECT_ID).description("ID of the project")

    private val MILESTONE_ID_PATH_PARAMETER_DESCRIPTOR =
        parameterWithName(PATH_VARIABLE_MILESTONE_ID).description("ID of the milestone")

    val MILESTONE_RESPONSE_FIELD_DESCRIPTORS =
        listOf(
            *ABSTRACT_RESOURCE_FIELD_DESCRIPTORS,
            fieldWithPath("name").description("Name of the milestone").type(STRING),
            fieldWithPath("type").description("Type of the milestone").type(STRING),
            fieldWithPath("date").description("Date of the milestone").type(STRING),
            fieldWithPath("header")
                .description("Indicator if milestone belongs to calender header")
                .type(BOOLEAN),
            fieldWithPath("description")
                .optional()
                .description("Description of the milestone")
                .type(STRING),
            fieldWithPath("position")
                .description("Position inside the list of milestones for the same calendar slot")
                .type(NUMBER),
            *PROJECT_REFERENCE_FIELD_DESCRIPTORS,
            *CRAFT_REFERENCE_FIELD_DESCRIPTORS,
            *WORK_AREA_REFERENCE_FIELD_DESCRIPTORS,
            *CREATOR_PARTICIPANT_REFERENCE_FIELD_DESCRIPTORS,
            subsectionWithPath("_links").ignored())

    private fun <T> buildCommonMilestoneFieldDescriptors(input: Class<T>): List<FieldDescriptor> =
        listOf(
            ConstrainedFields(input)
                .withPath("name")
                .description("Name of the milestone")
                .type(STRING),
            ConstrainedFields(input)
                .withPath("type")
                .description("Type of the milestone")
                .type(STRING),
            ConstrainedFields(input)
                .withPath("date")
                .description("Date of the milestone")
                .type(STRING),
            ConstrainedFields(input)
                .withPath("header")
                .description("Indicator if milestone belongs to calender header")
                .type(BOOLEAN),
            ConstrainedFields(input)
                .withPath("description")
                .description("Description of the milestone")
                .optional()
                .type(STRING),
            ConstrainedFields(input)
                .withPath("craftId")
                .description("ID of the referenced project craft")
                .attributes(key("constraints").value("Is only used if milestone type is CRAFT"))
                .optional()
                .type(STRING),
            ConstrainedFields(input)
                .withPath("workAreaId")
                .description("ID of the referenced work area")
                .attributes(key("constraints").value("Is only used if header is false"))
                .optional()
                .type(STRING),
            ConstrainedFields(input)
                .withPath("position")
                .description(
                    "This field can be used to sort milestones for the same date/header/workArea combination. " +
                        "If set, this milestone is inserted at the defined position in the list. " +
                        "The order is respected when milestones are searched.")
                .optional()
                .type(NUMBER))

    private val CREATE_MILESTONE_REQUEST_FIELD_DESCRIPTORS =
        listOf(
            ConstrainedFields(CreateMilestoneResource::class.java)
                .withPath("projectId")
                .description("ID of the referenced project")
                .type(STRING),
            *buildCommonMilestoneFieldDescriptors(CreateMilestoneResource::class.java)
                .toTypedArray())

    private val BATCH_FIND_RESPONSE_FIELDS =
        buildBatchItemsListResponseFields(MILESTONE_RESPONSE_FIELD_DESCRIPTORS)

    private val UPDATE_MILESTONE_REQUEST_FIELD_DESCRIPTORS =
        buildCommonMilestoneFieldDescriptors(UpdateMilestoneResource::class.java)

    private val MILESTONE_LINK_DESCRIPTORS =
        listOf(
            linkWithRel(LINK_MILESTONE_UPDATE).description(LINK_UPDATE_DESCRIPTION).optional(),
            linkWithRel(LINK_MILESTONE_DELETE).description(LINK_DELETE_DESCRIPTION).optional(),
        )
  }
}
