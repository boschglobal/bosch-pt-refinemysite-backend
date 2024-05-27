/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.milestone.facade.rest

import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.common.facade.rest.etag.ETag
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneListEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneListEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneListEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneListEventEnumAvro.ITEMADDED
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneTypeEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.event.SubmitMilestoneWithListDto
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMilestonesWithList
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.etag.from
import com.bosch.pt.iot.smartsite.project.milestone.domain.asMilestoneId
import com.bosch.pt.iot.smartsite.project.milestone.domain.asMilestoneListId
import com.bosch.pt.iot.smartsite.project.milestone.facade.rest.resource.request.FilterMilestoneListResource
import com.bosch.pt.iot.smartsite.project.milestone.facade.rest.resource.request.UpdateMilestoneResource
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.Milestone
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.MilestoneList
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.MilestoneTypeEnum.CRAFT
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.MilestoneTypeEnum.INVESTOR
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.MilestoneTypeEnum.PROJECT
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.asProjectCraftId
import com.bosch.pt.iot.smartsite.project.workarea.domain.asWorkAreaId
import java.time.LocalDate
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@EnableAllKafkaListeners
class MilestoneUpdateIntegrationTest : AbstractMilestoneIntegrationTest() {

  @Autowired lateinit var cut: MilestoneController

  @Autowired lateinit var milestoneSearchController: MilestoneSearchController

  private val projectIdentifier by lazy { getIdentifier("project").asProjectId() }

  @Test
  fun `verify update craft milestone fails without craft`() {
    val updateMilestoneResource =
        UpdateMilestoneResource(type = CRAFT, name = "Test", date = LocalDate.now(), header = true)

    assertThatExceptionOfType(PreconditionViolationException::class.java).isThrownBy {
      cut.update(
          milestoneId = getIdentifier("milestone").asMilestoneId(),
          updateMilestoneResource = updateMilestoneResource,
          eTag = ETag.from(0L))
    }

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify craft is ignored when updating project milestone`() {
    val updateMilestoneResource =
        UpdateMilestoneResource(
            type = PROJECT,
            name = "Test",
            date = LocalDate.now(),
            header = true,
            craftId = getIdentifier("projectCraft").asProjectCraftId())

    val response =
        cut.update(
            milestoneId = getIdentifier("milestone").asMilestoneId(),
            updateMilestoneResource = updateMilestoneResource,
            eTag = ETag.from(0L))

    assertThat(response.body!!.craft).isNull()
  }

  @Test
  fun `verify craft is ignored when updating investor milestone`() {
    val updateMilestoneResource =
        UpdateMilestoneResource(
            type = INVESTOR,
            name = "Test",
            date = LocalDate.now(),
            header = true,
            craftId = getIdentifier("projectCraft").asProjectCraftId())

    val response =
        cut.update(
            milestoneId = getIdentifier("milestone").asMilestoneId(),
            updateMilestoneResource = updateMilestoneResource,
            eTag = ETag.from(0L))

    assertThat(response.body!!.craft).isNull()
  }

  @Test
  fun `verify work area is ignored when updating header milestone`() {
    val updateMilestoneResource =
        UpdateMilestoneResource(
            type = INVESTOR,
            name = "Test",
            date = LocalDate.now(),
            header = true,
            workAreaId = getIdentifier("workArea").asWorkAreaId())

    val response =
        cut.update(
            milestoneId = getIdentifier("milestone").asMilestoneId(),
            updateMilestoneResource = updateMilestoneResource,
            eTag = ETag.from(0L))

    assertThat(response.body!!.workArea).isNull()
  }

  @Nested
  @Suppress("ClassName")
  inner class `Verify milestone is` {

    private lateinit var list1: MilestoneList
    private lateinit var list2: MilestoneList
    private lateinit var list3: MilestoneList

    private lateinit var list1Milestone0: Milestone
    private lateinit var list1Milestone1: Milestone
    private lateinit var list1Milestone2: Milestone
    private lateinit var list2Milestone0: Milestone
    private lateinit var list2Milestone1: Milestone
    private lateinit var list2Milestone2: Milestone
    private lateinit var list3Milestone0: Milestone

    private lateinit var list1Filter: FilterMilestoneListResource
    private lateinit var list2Filter: FilterMilestoneListResource
    private lateinit var list3Filter: FilterMilestoneListResource

    @BeforeEach
    fun setupLists() {
      val date1 = LocalDate.now().plusDays(2)
      list1Filter = defaultFilter.copy(from = date1, to = date1)
      eventStreamGenerator.submitMilestonesWithList(
          listReference = "list1",
          date = date1,
          milestones =
              listOf(
                  SubmitMilestoneWithListDto(type = MilestoneTypeEnumAvro.INVESTOR),
                  SubmitMilestoneWithListDto(
                      type = MilestoneTypeEnumAvro.CRAFT, craft = getByReference("projectCraft")),
                  SubmitMilestoneWithListDto(type = MilestoneTypeEnumAvro.PROJECT),
              ))

      list1Milestone0 =
          repositories.findMilestoneWithDetails(getIdentifier("list1M0").asMilestoneId())
      list1Milestone1 =
          repositories.findMilestoneWithDetails(getIdentifier("list1M1").asMilestoneId())
      list1Milestone2 =
          repositories.findMilestoneWithDetails(getIdentifier("list1M2").asMilestoneId())
      list1 = repositories.findMilestoneList(getIdentifier("list1").asMilestoneListId())!!

      val date2 = LocalDate.now().plusDays(4)
      list2Filter = defaultFilter.copy(from = date2, to = date2)
      eventStreamGenerator.submitMilestonesWithList(
          listReference = "list2",
          date = date2,
          milestones =
              listOf(
                  SubmitMilestoneWithListDto(type = MilestoneTypeEnumAvro.INVESTOR),
                  SubmitMilestoneWithListDto(
                      type = MilestoneTypeEnumAvro.CRAFT, craft = getByReference("projectCraft")),
                  SubmitMilestoneWithListDto(type = MilestoneTypeEnumAvro.PROJECT),
              ))

      list2Milestone0 =
          repositories.findMilestoneWithDetails(getIdentifier("list2M0").asMilestoneId())
      list2Milestone1 =
          repositories.findMilestoneWithDetails(getIdentifier("list2M1").asMilestoneId())
      list2Milestone2 =
          repositories.findMilestoneWithDetails(getIdentifier("list2M2").asMilestoneId())
      list2 = repositories.findMilestoneListWithDetails(getIdentifier("list2").asMilestoneListId())

      val date3 = LocalDate.now().plusDays(6)
      list3Filter = defaultFilter.copy(from = date3, to = date3)
      eventStreamGenerator.submitMilestonesWithList(
          listReference = "list3",
          date = date3,
          milestones = listOf(SubmitMilestoneWithListDto(type = MilestoneTypeEnumAvro.INVESTOR)))

      list3Milestone0 =
          repositories.findMilestoneWithDetails(getIdentifier("list3M0").asMilestoneId())
      list3 = repositories.findMilestoneList(getIdentifier("list3").asMilestoneListId())!!

      projectEventStoreUtils.reset()
    }

    @Test
    fun `moved to a new empty list`() {

      val newDate = LocalDate.now().plusDays(10)

      val updateMilestoneResource =
          UpdateMilestoneResource(
              type = list1Milestone2.type,
              name = list1Milestone2.name,
              date = newDate,
              header = list1Milestone2.header,
              workAreaId = list1Milestone2.workArea?.identifier)

      cut.update(
          milestoneId = list1Milestone2.identifier,
          updateMilestoneResource = updateMilestoneResource,
          eTag = ETag.from(0L))

      milestoneSearchController
          .search(projectIdentifier, list1Filter, DEFAULT_SORTING)
          .body!!
          .items
          .also { items ->
            assertThat(items).hasSize(2)
            assertThat(items[0].name).isEqualTo(list1Milestone0.name)
            assertThat(items[0].id).isEqualTo(list1Milestone0.identifier.toUuid())
            assertThat(items[1].name).isEqualTo(list1Milestone1.name)
            assertThat(items[1].id).isEqualTo(list1Milestone1.identifier.toUuid())
          }

      val newListFilter = defaultFilter.copy(from = newDate, to = newDate)

      milestoneSearchController
          .search(projectIdentifier, newListFilter, DEFAULT_SORTING)
          .body!!
          .items
          .also { items ->
            assertThat(items).hasSize(1)
            assertThat(items[0].name).isEqualTo(list1Milestone2.name)
            assertThat(items[0].id).isEqualTo(list1Milestone2.identifier.toUuid())
          }

      projectEventStoreUtils.verifyContainsInSequence(
          listOf(
              MilestoneEventAvro::class.java,
              MilestoneListEventAvro::class.java,
              MilestoneListEventAvro::class.java))
      projectEventStoreUtils.verifyContains(
          MilestoneEventAvro::class.java, MilestoneEventEnumAvro.UPDATED, 1, false)
      projectEventStoreUtils.verifyContains(
          MilestoneListEventAvro::class.java, MilestoneListEventEnumAvro.ITEMREMOVED, 1, false)
      projectEventStoreUtils.verifyContains(MilestoneListEventAvro::class.java, CREATED, 1, false)
    }

    @Test
    fun `moved to a new empty list and old list is deleted because it is empty`() {

      val newDate = LocalDate.now().plusDays(10)

      val updateMilestoneResource =
          UpdateMilestoneResource(
              type = list3Milestone0.type,
              name = list3Milestone0.name,
              date = newDate,
              header = list3Milestone0.header,
              workAreaId = list3Milestone0.workArea?.identifier)

      cut.update(
          milestoneId = list3Milestone0.identifier,
          updateMilestoneResource = updateMilestoneResource,
          eTag = ETag.from(0L))

      milestoneSearchController
          .search(projectIdentifier, list3Filter, DEFAULT_SORTING)
          .body!!
          .items
          .also { items -> assertThat(items).isEmpty() }

      val newListFilter = defaultFilter.copy(from = newDate, to = newDate)

      milestoneSearchController
          .search(projectIdentifier, newListFilter, DEFAULT_SORTING)
          .body!!
          .items
          .also { items ->
            assertThat(items).hasSize(1)
            assertThat(items[0].name).isEqualTo(list3Milestone0.name)
            assertThat(items[0].id).isEqualTo(list3Milestone0.identifier.toUuid())
          }

      projectEventStoreUtils.verifyContainsInSequence(
          listOf(
              MilestoneEventAvro::class.java,
              MilestoneListEventAvro::class.java,
              MilestoneListEventAvro::class.java))
      projectEventStoreUtils.verifyContains(
          MilestoneEventAvro::class.java, MilestoneEventEnumAvro.UPDATED, 1, false)
      projectEventStoreUtils.verifyContains(
          MilestoneListEventAvro::class.java, MilestoneListEventEnumAvro.DELETED, 1, false)
      projectEventStoreUtils.verifyContains(MilestoneListEventAvro::class.java, CREATED, 1, false)
    }

    @Test
    fun `moved to defined position at another existing list`() {

      val updateMilestoneResource =
          UpdateMilestoneResource(
              type = list1Milestone2.type,
              name = list1Milestone2.name,
              date = list2Milestone2.date,
              header = list2Milestone2.header,
              workAreaId = list2Milestone2.workArea?.identifier,
              position = 2)

      cut.update(
          milestoneId = list1Milestone2.identifier,
          updateMilestoneResource = updateMilestoneResource,
          eTag = ETag.from(0L))

      milestoneSearchController
          .search(projectIdentifier, list1Filter, DEFAULT_SORTING)
          .body!!
          .items
          .also { items ->
            assertThat(items).hasSize(2)
            assertThat(items[0].name).isEqualTo(list1Milestone0.name)
            assertThat(items[0].id).isEqualTo(list1Milestone0.identifier.toUuid())
            assertThat(items[1].name).isEqualTo(list1Milestone1.name)
            assertThat(items[1].id).isEqualTo(list1Milestone1.identifier.toUuid())
          }

      milestoneSearchController
          .search(projectIdentifier, list2Filter, DEFAULT_SORTING)
          .body!!
          .items
          .also { items ->
            assertThat(items).hasSize(4)
            assertThat(items[0].name).isEqualTo(list2Milestone0.name)
            assertThat(items[0].id).isEqualTo(list2Milestone0.identifier.toUuid())
            assertThat(items[1].name).isEqualTo(list2Milestone1.name)
            assertThat(items[1].id).isEqualTo(list2Milestone1.identifier.toUuid())
            assertThat(items[2].name).isEqualTo(list1Milestone2.name)
            assertThat(items[2].id).isEqualTo(list1Milestone2.identifier.toUuid())
            assertThat(items[3].name).isEqualTo(list2Milestone2.name)
            assertThat(items[3].id).isEqualTo(list2Milestone2.identifier.toUuid())
          }

      projectEventStoreUtils.verifyContainsInSequence(
          listOf(
              MilestoneEventAvro::class.java,
              MilestoneListEventAvro::class.java,
              MilestoneListEventAvro::class.java))
      projectEventStoreUtils.verifyContains(
          MilestoneListEventAvro::class.java, MilestoneListEventEnumAvro.ITEMREMOVED, 1, false)
      projectEventStoreUtils.verifyContains(
          MilestoneEventAvro::class.java, MilestoneEventEnumAvro.UPDATED, 1, false)
      projectEventStoreUtils.verifyContains(MilestoneListEventAvro::class.java, ITEMADDED, 1, false)
    }

    @Test
    fun `moved to default position (top) of another existing list`() {

      val updateMilestoneResource =
          UpdateMilestoneResource(
              type = list1Milestone2.type,
              name = list1Milestone2.name,
              date = list2Milestone2.date,
              header = list2Milestone2.header,
              workAreaId = list2Milestone2.workArea?.identifier)

      cut.update(
          milestoneId = list1Milestone2.identifier,
          updateMilestoneResource = updateMilestoneResource,
          eTag = ETag.from(0L))

      milestoneSearchController
          .search(projectIdentifier, list1Filter, DEFAULT_SORTING)
          .body!!
          .items
          .also { items ->
            assertThat(items).hasSize(2)
            assertThat(items[0].name).isEqualTo(list1Milestone0.name)
            assertThat(items[0].id).isEqualTo(list1Milestone0.identifier.toUuid())
            assertThat(items[1].name).isEqualTo(list1Milestone1.name)
            assertThat(items[1].id).isEqualTo(list1Milestone1.identifier.toUuid())
          }

      milestoneSearchController
          .search(projectIdentifier, list2Filter, DEFAULT_SORTING)
          .body!!
          .items
          .also { items ->
            assertThat(items).hasSize(4)
            assertThat(items[0].name).isEqualTo(list1Milestone2.name)
            assertThat(items[0].id).isEqualTo(list1Milestone2.identifier.toUuid())
            assertThat(items[1].name).isEqualTo(list2Milestone0.name)
            assertThat(items[1].id).isEqualTo(list2Milestone0.identifier.toUuid())
            assertThat(items[2].name).isEqualTo(list2Milestone1.name)
            assertThat(items[2].id).isEqualTo(list2Milestone1.identifier.toUuid())
            assertThat(items[3].name).isEqualTo(list2Milestone2.name)
            assertThat(items[3].id).isEqualTo(list2Milestone2.identifier.toUuid())
          }

      projectEventStoreUtils.verifyContainsInSequence(
          listOf(
              MilestoneEventAvro::class.java,
              MilestoneListEventAvro::class.java,
              MilestoneListEventAvro::class.java))
      projectEventStoreUtils.verifyContains(
          MilestoneListEventAvro::class.java, MilestoneListEventEnumAvro.ITEMREMOVED, 1, false)
      projectEventStoreUtils.verifyContains(
          MilestoneEventAvro::class.java, MilestoneEventEnumAvro.UPDATED, 1, false)
      projectEventStoreUtils.verifyContains(MilestoneListEventAvro::class.java, ITEMADDED, 1, false)
    }

    @Test
    fun `moved to defined position inside same list`() {
      val updateMilestoneResource =
          UpdateMilestoneResource(
              type = list1Milestone2.type,
              name = list1Milestone2.name,
              date = list1Milestone2.date,
              header = list1Milestone2.header,
              workAreaId = list1Milestone2.workArea?.identifier,
              position = 1)

      cut.update(
          milestoneId = list1Milestone2.identifier,
          updateMilestoneResource = updateMilestoneResource,
          eTag = ETag.from(0L))

      milestoneSearchController
          .search(projectIdentifier, list1Filter, DEFAULT_SORTING)
          .body!!
          .items
          .also { items ->
            assertThat(items).hasSize(3)
            assertThat(items[0].name).isEqualTo(list1Milestone0.name)
            assertThat(items[0].id).isEqualTo(list1Milestone0.identifier.toUuid())
            assertThat(items[1].name).isEqualTo(list1Milestone2.name)
            assertThat(items[1].id).isEqualTo(list1Milestone2.identifier.toUuid())
            assertThat(items[2].name).isEqualTo(list1Milestone1.name)
            assertThat(items[2].id).isEqualTo(list1Milestone1.identifier.toUuid())
          }

      projectEventStoreUtils.verifyContains(
          MilestoneListEventAvro::class.java, MilestoneListEventEnumAvro.REORDERED, 1)
    }

    @Test
    fun `not moved inside same list if position is not defined`() {
      val updateMilestoneResource =
          UpdateMilestoneResource(
              type = list1Milestone2.type,
              name = list1Milestone2.name,
              date = list1Milestone2.date,
              header = list1Milestone2.header,
              workAreaId = list1Milestone2.workArea?.identifier)

      cut.update(
          milestoneId = list1Milestone2.identifier,
          updateMilestoneResource = updateMilestoneResource,
          eTag = ETag.from(0L))

      milestoneSearchController
          .search(projectIdentifier, list1Filter, DEFAULT_SORTING)
          .body!!
          .items
          .also { items ->
            assertThat(items).hasSize(3)
            assertThat(items[0].name).isEqualTo(list1Milestone0.name)
            assertThat(items[0].id).isEqualTo(list1Milestone0.identifier.toUuid())
            assertThat(items[1].name).isEqualTo(list1Milestone1.name)
            assertThat(items[1].id).isEqualTo(list1Milestone1.identifier.toUuid())
            assertThat(items[2].name).isEqualTo(list1Milestone2.name)
            assertThat(items[2].id).isEqualTo(list1Milestone2.identifier.toUuid())
          }

      projectEventStoreUtils.verifyEmpty()
    }

    @Test
    fun `not moved inside same list when already at defined position`() {
      val updateMilestoneResource =
          UpdateMilestoneResource(
              type = list1Milestone2.type,
              name = list1Milestone2.name,
              date = list1Milestone2.date,
              header = list1Milestone2.header,
              workAreaId = list1Milestone2.workArea?.identifier,
              position = 2)

      cut.update(
          milestoneId = list1Milestone2.identifier,
          updateMilestoneResource = updateMilestoneResource,
          eTag = ETag.from(0L))

      milestoneSearchController
          .search(projectIdentifier, list1Filter, DEFAULT_SORTING)
          .body!!
          .items
          .also { items ->
            assertThat(items).hasSize(3)
            assertThat(items[0].name).isEqualTo(list1Milestone0.name)
            assertThat(items[0].id).isEqualTo(list1Milestone0.identifier.toUuid())
            assertThat(items[1].name).isEqualTo(list1Milestone1.name)
            assertThat(items[1].id).isEqualTo(list1Milestone1.identifier.toUuid())
            assertThat(items[2].name).isEqualTo(list1Milestone2.name)
            assertThat(items[2].id).isEqualTo(list1Milestone2.identifier.toUuid())
          }

      projectEventStoreUtils.verifyEmpty()
    }
  }
}
