/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.milestone.facade.rest

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneTypeEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.event.SubmitMilestoneWithListDto
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMilestonesWithList
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractApiDocumentationTestV2
import com.bosch.pt.iot.smartsite.project.milestone.domain.asMilestoneId
import com.bosch.pt.iot.smartsite.project.milestone.domain.asMilestoneListId
import com.bosch.pt.iot.smartsite.project.milestone.facade.rest.resource.request.FilterMilestoneListResource
import com.bosch.pt.iot.smartsite.project.milestone.facade.rest.resource.request.FilterMilestoneListResource.TypesFilter
import com.bosch.pt.iot.smartsite.project.milestone.facade.rest.resource.request.FilterMilestoneListResource.WorkAreaFilter
import com.bosch.pt.iot.smartsite.project.milestone.facade.rest.resource.response.MilestoneResource
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.MilestoneTypeEnum.CRAFT
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.MilestoneTypeEnum.INVESTOR
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.MilestoneTypeEnum.PROJECT
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.asProjectCraftId
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaIdOrEmpty
import com.bosch.pt.iot.smartsite.project.workarea.domain.asWorkAreaId
import java.time.LocalDate.now
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Pageable.unpaged
import org.springframework.hateoas.MediaTypes
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@EnableAllKafkaListeners
class MilestoneSearchIntegrationTest : AbstractApiDocumentationTestV2() {

  @Autowired lateinit var cut: MilestoneSearchController

  private val projectIdentifier by lazy { getIdentifier("project").asProjectId() }
  private val workArea by lazy {
    repositories.findWorkArea(getIdentifier("workArea").asWorkAreaId())!!
  }
  private val projectCraft by lazy {
    repositories.findProjectCraft(getIdentifier("projectCraft").asProjectCraftId())!!
  }
  private val projectCraft2 by lazy {
    repositories.findProjectCraft(getIdentifier("projectCraft2").asProjectCraftId())!!
  }
  private val projectCraft3 by lazy {
    repositories.findProjectCraft(getIdentifier("projectCraft3").asProjectCraftId())!!
  }
  private val investorMilestoneWithWorkArea by lazy {
    repositories.findMilestone(getIdentifier("milestoneListInvestorM0").asMilestoneId())!!
  }
  private val craftMilestone3 by lazy {
    repositories.findMilestone(getIdentifier("milestoneListCrafts2M1").asMilestoneId())!!
  }
  private val milestoneListInvestor by lazy {
    repositories.findMilestoneList(getIdentifier("milestoneListInvestor").asMilestoneListId())!!
  }
  private val milestoneListWith2CraftMilestones by lazy {
    repositories.findMilestoneList(getIdentifier("milestoneListCrafts2").asMilestoneListId())!!
  }

  @BeforeEach
  fun setup() {
    val now = now()
    eventStreamGenerator
        // this will also submit one PROJECT milestone in the header section of the calendar
        .setupDatasetTestData()
        .submitProjectCraftG2(asReference = "projectCraft2")
        .submitProjectCraftG2(asReference = "projectCraft3")
        // submit INVESTOR milestone in the working area section of the calendar
        .submitMilestonesWithList(
            listReference = "milestoneListInvestor",
            date = now.minusDays(10),
            header = false,
            workArea = "workArea",
            milestones = listOf(SubmitMilestoneWithListDto(type = MilestoneTypeEnumAvro.INVESTOR)))
        // submit CRAFT milestone in the "without working area" section of the calendar
        .submitMilestonesWithList(
            listReference = "milestoneListCrafts1",
            date = now.plusDays(10),
            header = false,
            workArea = null,
            milestones =
                listOf(
                    SubmitMilestoneWithListDto(
                        type = MilestoneTypeEnumAvro.CRAFT,
                        craft = getByReference("projectCraft"))))
        // submit milestone in the header section of the calendar
        .submitMilestonesWithList(
            listReference = "milestoneListCrafts2",
            date = now.plusDays(10),
            header = true,
            milestones =
                listOf(
                    SubmitMilestoneWithListDto(
                        type = MilestoneTypeEnumAvro.CRAFT,
                        craft = getByReference("projectCraft2")),
                    SubmitMilestoneWithListDto(
                        type = MilestoneTypeEnumAvro.CRAFT,
                        craft = getByReference("projectCraft3"))))

    setAuthentication(getIdentifier("userCsm2"))
  }

  @Test
  fun `verify search milestone with filter for milestone type`() {
    val filterMilestoneListResource =
        FilterMilestoneListResource(types = TypesFilter(types = setOf(CRAFT)))

    val milestoneList =
        cut.search(projectIdentifier, filterMilestoneListResource, unpaged()).body!!.items

    assertThat(milestoneList).hasSize(3)
    assertThat(milestoneList[0].type).isEqualTo(CRAFT)
    assertThat(milestoneList[1].type).isEqualTo(CRAFT)
    assertThat(milestoneList[2].type).isEqualTo(CRAFT)
  }

  @Test
  fun `verify search milestone with filter for milestone header true (old API)`() {
    val filterMilestoneListResource =
        FilterMilestoneListResource(workAreas = WorkAreaFilter(header = true))

    val milestoneList =
        cut.search(projectIdentifier, filterMilestoneListResource, unpaged()).body!!.items

    assertThat(milestoneList).hasSize(3)
    assertThat(milestoneList[0].header).isTrue
    assertThat(milestoneList[1].header).isTrue
    assertThat(milestoneList[2].header).isTrue
  }

  @Test
  fun `verify search milestone with filter for milestone header true (new API)`() {
    val filterMilestoneListResource =
        FilterMilestoneListResource(workAreas = WorkAreaFilter(header = true))

    val milestoneList =
        cut.search(projectIdentifier, filterMilestoneListResource, unpaged()).body!!.items

    assertThat(milestoneList).hasSize(3)
    assertThat(milestoneList[0].header).isTrue
    assertThat(milestoneList[1].header).isTrue
    assertThat(milestoneList[2].header).isTrue
  }

  // we treat "header == false" as "header == null" because we don't want to introduce exclusion
  // criteria into the search. So far, all criteria in the search are inclusion criteria. Changing
  // this is not needed so far and would introduce unneccessary complexity.
  @Test
  fun `verify search milestone with filter for milestone header false`() {
    val filterMilestoneListResource =
        FilterMilestoneListResource(workAreas = WorkAreaFilter(header = false))

    val milestoneList =
        cut.search(projectId = projectIdentifier, filterMilestoneListResource, unpaged())
            .body!!
            .items

    // expect all milestones to be returned regardless of being in the header or not
    assertThat(milestoneList).hasSize(5)
  }

  @Test
  fun `verify search milestone with filter for milestone header null`() {
    val filterMilestoneListResource =
        FilterMilestoneListResource(workAreas = WorkAreaFilter(header = null))

    val milestoneList =
        cut.search(projectIdentifier, filterMilestoneListResource, unpaged()).body!!.items

    // expect all milestones to be returned regardless of being in the header or not
    assertThat(milestoneList).hasSize(5)
  }

  @Test
  fun `verify search milestone with filter for work area and milestone header`() {
    val filterMilestoneListResource =
        FilterMilestoneListResource(
            workAreas =
                WorkAreaFilter(
                    header = true, workAreaIds = setOf(WorkAreaIdOrEmpty(workArea.identifier))))

    val milestoneList =
        cut.search(projectIdentifier, filterMilestoneListResource, unpaged()).body!!.items

    assertThat(milestoneList).hasSize(4)
    assertThat(milestoneList.filter { it.isWorkAreaMilestone() }).hasSize(1)
    assertThat(milestoneList.filter { it.isHeaderMilestone() }).hasSize(3)
  }

  @Test
  fun `verify search milestone with filter for time range start date`() {
    val startDate = now()
    val filterMilestoneListResource = FilterMilestoneListResource(from = startDate)

    val milestoneList =
        cut.search(projectIdentifier, filterMilestoneListResource, unpaged()).body!!.items

    assertThat(milestoneList).hasSize(4)
    assertThat(milestoneList[0].date).isAfterOrEqualTo(startDate)
    assertThat(milestoneList[1].date).isAfterOrEqualTo(startDate)
  }

  @Test
  fun `verify search milestone with filter for time range end date`() {
    val endDate = now()
    val filterMilestoneListResource = FilterMilestoneListResource(to = endDate)

    val milestoneList =
        cut.search(projectIdentifier, filterMilestoneListResource, unpaged()).body!!.items

    assertThat(milestoneList).hasSize(2)
    assertThat(milestoneList[0].date).isBeforeOrEqualTo(endDate)
    assertThat(milestoneList[1].date).isBeforeOrEqualTo(endDate)
  }

  @Test
  fun `verify search milestone with filter for time range`() {
    val startDate = now().minusDays(1)
    val endDate = now().plusDays(1)
    val filterMilestoneListResource = FilterMilestoneListResource(from = startDate, to = endDate)

    val milestoneList =
        cut.search(projectIdentifier, filterMilestoneListResource, unpaged()).body!!.items

    assertThat(milestoneList).hasSize(1)
    assertThat(milestoneList.single().date).isBetween(startDate, endDate)
  }

  @Test
  fun `verify search milestone with filter when rangeStartDate is null`() {
    val endDate = now()
    val filterMilestoneListResource = FilterMilestoneListResource(from = null, to = endDate)

    val milestoneList =
        cut.search(projectIdentifier, filterMilestoneListResource, unpaged()).body!!.items

    assertThat(milestoneList).hasSize(2)
    assertThat(milestoneList[0].date).isBeforeOrEqualTo(endDate)
    assertThat(milestoneList[1].date).isBeforeOrEqualTo(endDate)
  }

  @Test
  fun `verify search milestone with filter when rangeEndDate is null`() {
    val startDate = now()
    val filterMilestoneListResource = FilterMilestoneListResource(from = startDate, to = null)

    val milestoneList =
        cut.search(projectIdentifier, filterMilestoneListResource, unpaged()).body!!.items

    assertThat(milestoneList).hasSize(4)
    assertThat(milestoneList[0].date).isAfterOrEqualTo(startDate)
    assertThat(milestoneList[1].date).isAfterOrEqualTo(startDate)
    assertThat(milestoneList[2].date).isAfterOrEqualTo(startDate)
    assertThat(milestoneList[3].date).isAfterOrEqualTo(startDate)
  }

  @Test
  fun `verify search milestone with filter when rangeStartDate and rangeEndDate are null`() {
    val filterMilestoneListResource = FilterMilestoneListResource(from = null, to = null)

    val milestoneList =
        cut.search(projectIdentifier, filterMilestoneListResource, unpaged()).body!!.items

    assertThat(milestoneList).hasSize(5)
  }

  @Test
  fun `verify search milestone with filter for type investor(client)`() {
    val filterMilestoneListResource =
        FilterMilestoneListResource(types = TypesFilter(types = setOf(INVESTOR)))

    val milestoneList =
        cut.search(projectIdentifier, filterMilestoneListResource, unpaged()).body!!.items

    assertThat(milestoneList.filter { it.type == INVESTOR }).hasSize(1)
  }

  @Test
  fun `verify search milestone with filter for type project`() {
    val filterMilestoneListResource =
        FilterMilestoneListResource(types = TypesFilter(types = setOf(PROJECT)))

    val milestoneList =
        cut.search(projectIdentifier, filterMilestoneListResource, unpaged()).body!!.items

    assertThat(milestoneList.filter { it.type == PROJECT }).hasSize(1)
  }

  @Test
  fun `verify search milestone with filter for type craft`() {
    val filterMilestoneListResource =
        FilterMilestoneListResource(types = TypesFilter(types = setOf(CRAFT)))

    val milestoneList =
        cut.search(projectIdentifier, filterMilestoneListResource, unpaged()).body!!.items

    assertThat(milestoneList).hasSize(3)
    assertThat(milestoneList.filter { it.type == CRAFT }).hasSize(3)
  }

  @Test
  fun `verify search milestone with filter for type craft and craft id`() {
    val filterMilestoneListResource =
        FilterMilestoneListResource(
            types =
                TypesFilter(types = setOf(CRAFT), projectCraftIds = setOf(projectCraft.identifier)))

    val milestoneList =
        cut.search(projectIdentifier, filterMilestoneListResource, unpaged()).body!!.items

    assertThat(milestoneList).hasSize(1)
    assertThat(milestoneList.single { it.type == CRAFT }.craft!!.identifier.asProjectCraftId())
        .isEqualTo(projectCraft.identifier)
  }

  @Test
  fun `verify search milestone with filter for type investor(client), type project, type craft and craft id`() {
    val filterMilestoneListResource =
        FilterMilestoneListResource(
            types =
                TypesFilter(
                    types = setOf(INVESTOR, PROJECT, CRAFT),
                    projectCraftIds = setOf(projectCraft.identifier)))

    val milestoneList =
        cut.search(projectIdentifier, filterMilestoneListResource, unpaged()).body!!.items

    assertThat(milestoneList).hasSize(3)
    assertThat(milestoneList.filter { it.type == PROJECT }).hasSize(1)
    assertThat(milestoneList.filter { it.type == INVESTOR }).hasSize(1)
    assertThat(milestoneList.single { it.type == CRAFT }.craft!!.identifier.asProjectCraftId())
        .isEqualTo(projectCraft.identifier)
  }

  @Test
  fun `verify search milestone with filter for type investor(client), type project, type craft and all craft ids`() {
    val filterMilestoneListResource =
        FilterMilestoneListResource(
            types =
                TypesFilter(
                    types = setOf(INVESTOR, PROJECT, CRAFT),
                    projectCraftIds =
                        setOf(
                            projectCraft.identifier,
                            projectCraft2.identifier,
                            projectCraft3.identifier)))

    val milestoneList =
        cut.search(projectIdentifier, filterMilestoneListResource, unpaged()).body!!.items

    assertThat(milestoneList).hasSize(5)
    assertThat(milestoneList.filter { it.type == PROJECT }).hasSize(1)
    assertThat(milestoneList.filter { it.type == INVESTOR }).hasSize(1)
    assertThat(milestoneList.filter { it.type == CRAFT }).hasSize(3)
  }

  @Test
  fun `verify search milestone with empty types filter to get all milestones`() {
    val filterMilestoneListResource = FilterMilestoneListResource(types = TypesFilter())

    val milestoneList =
        cut.search(projectIdentifier, filterMilestoneListResource, unpaged()).body!!.items

    assertThat(milestoneList).hasSize(5)
    assertThat(milestoneList.filter { it.type == PROJECT }).hasSize(1)
    assertThat(milestoneList.filter { it.type == INVESTOR }).hasSize(1)
    assertThat(milestoneList.filter { it.type == CRAFT }).hasSize(3)
  }

  @Test
  fun `verify search milestone with filter for work areas`() {
    val filterMilestoneListResource =
        FilterMilestoneListResource(
            workAreas = WorkAreaFilter(workAreaIds = setOf(WorkAreaIdOrEmpty(workArea.identifier))))

    val milestoneList =
        cut.search(projectIdentifier, filterMilestoneListResource, unpaged()).body!!.items

    assertThat(milestoneList).hasSize(1)
    assertThat(milestoneList.single().id)
        .isEqualTo(investorMilestoneWithWorkArea.identifier.toUuid())
    assertThat(milestoneList.single().workArea!!.identifier).isEqualTo(workArea.identifier.toUuid())
  }

  @Test
  fun `verify search milestone with filter for EMPTY work area`() {
    val filterMilestoneListResource =
        FilterMilestoneListResource(
            workAreas = WorkAreaFilter(workAreaIds = setOf(WorkAreaIdOrEmpty())))

    val milestoneList =
        cut.search(projectIdentifier, filterMilestoneListResource, unpaged()).body!!.items

    assertThat(milestoneList).hasSize(1)
    assertThat(milestoneList.single().isEmptyWorkAreaMilestone()).isTrue
  }

  @Test
  fun `verify search milestone with filter for EMPTY work area and defined work area`() {
    val filterMilestoneListResource =
        FilterMilestoneListResource(
            workAreas =
                WorkAreaFilter(
                    workAreaIds =
                        setOf(WorkAreaIdOrEmpty(), WorkAreaIdOrEmpty(workArea.identifier))))

    val milestoneList =
        cut.search(projectIdentifier, filterMilestoneListResource, unpaged()).body!!.items

    assertThat(milestoneList.filter { it.isEmptyWorkAreaMilestone() }).hasSize(1)
    assertThat(milestoneList.filter { it.isWorkAreaMilestone() }).hasSize(1)
  }

  @Test
  fun `verify search milestone with filter for milestone list`() {
    val filterMilestoneListResource =
        FilterMilestoneListResource(
            milestoneListIds =
                setOf(
                    milestoneListInvestor.identifier, milestoneListWith2CraftMilestones.identifier))

    val milestoneList =
        cut.search(projectIdentifier, filterMilestoneListResource, unpaged()).body!!.items

    assertThat(milestoneList.size).isEqualTo(3)
    assertThat(milestoneList.filter { it.type == CRAFT }).hasSize(2)
    assertThat(milestoneList.filter { it.type == INVESTOR }).hasSize(1)
  }

  @Test
  fun `verify search milestone with filter for milestone list and craft`() {
    val filterMilestoneListResource =
        FilterMilestoneListResource(
            milestoneListIds = setOf(milestoneListWith2CraftMilestones.identifier),
            types = TypesFilter(projectCraftIds = setOf(projectCraft3.identifier)))

    val milestoneList =
        cut.search(projectIdentifier, filterMilestoneListResource, unpaged()).body!!.items

    assertThat(milestoneList.size).isEqualTo(1)
    assertThat(milestoneList.single().id).isEqualTo(craftMilestone3.identifier.toUuid())
    assertThat(milestoneList.single().position).isEqualTo(1)
  }

  @Test
  fun `verify links for fm participant`() {
    setAuthentication(getIdentifier("user"))
    mockMvc
        .perform(
            requestBuilder(
                post(latestVersionOf("/projects/{projectId}/milestones/search"), projectIdentifier)
                    .param("sort", "date")
                    .param("size", "1")
                    .param("page", "0"),
                FilterMilestoneListResource(types = TypesFilter(types = setOf(INVESTOR)))))
        .andExpectAll(
            status().isOk,
            content().contentType(MediaTypes.HAL_JSON_VALUE),
            jsonPath("$.items.length()").value(1),
            jsonPath("$.items[0]._links.update").doesNotExist(),
            jsonPath("$.items[0]._links.delete").doesNotExist(),
            jsonPath("$._links.createCraftMilestone").exists(),
            jsonPath("$._links.createInvestorMilestone").doesNotExist(),
            jsonPath("$._links.createProjectMilestone").doesNotExist())
  }

  private fun MilestoneResource.isHeaderMilestone() = workArea == null && header

  private fun MilestoneResource.isEmptyWorkAreaMilestone() = workArea == null && !header

  private fun MilestoneResource.isWorkAreaMilestone() = workArea != null
}
