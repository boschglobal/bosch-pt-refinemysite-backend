/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.milestone.facade.rest

import com.bosch.pt.csm.cloud.common.extensions.toEpochMilli
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.common.transaction.messages.BatchOperationFinishedEventAvro
import com.bosch.pt.csm.cloud.common.transaction.messages.BatchOperationStartedEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum
import com.bosch.pt.csm.cloud.projectmanagement.milestone.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneListEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneTypeEnumAvro.CRAFT
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneTypeEnumAvro.INVESTOR
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneTypeEnumAvro.PROJECT
import com.bosch.pt.csm.cloud.projectmanagement.project.event.SubmitMilestoneWithListDto
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMilestonesWithList
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractIntegrationTestV2
import com.bosch.pt.iot.smartsite.project.milestone.command.service.MilestoneRescheduleService
import com.bosch.pt.iot.smartsite.project.milestone.domain.asMilestoneId
import com.bosch.pt.iot.smartsite.project.milestone.domain.asMilestoneListId
import com.bosch.pt.iot.smartsite.project.milestone.shared.dto.MilestoneRescheduleResult
import com.bosch.pt.iot.smartsite.project.milestone.shared.dto.SearchMilestonesDto
import com.bosch.pt.iot.smartsite.project.milestone.shared.dto.TypesFilterDto
import com.bosch.pt.iot.smartsite.project.milestone.shared.dto.WorkAreaFilterDto
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.MilestoneTypeEnum
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.ProjectCraftId
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.asProjectCraftId
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaIdOrEmpty
import com.bosch.pt.iot.smartsite.project.workarea.domain.asWorkAreaId
import java.time.LocalDate.now
import java.time.LocalDateTime
import org.assertj.core.api.AbstractLongAssert
import org.assertj.core.api.AbstractStringAssert
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@EnableAllKafkaListeners
class MilestoneRescheduleServiceIntegrationTest : AbstractIntegrationTestV2() {

  @Autowired lateinit var cut: MilestoneRescheduleService

  private val existingWorkArea by lazy {
    repositories.findWorkArea(getIdentifier("workArea").asWorkAreaId())!!
  }

  private val existingProjectCraft by lazy {
    repositories.findProjectCraft(getIdentifier("projectCraft").asProjectCraftId())!!
  }

  private val existingMilestone by lazy {
    repositories.findMilestoneWithDetails(getIdentifier("milestone").asMilestoneId())
  }

  @BeforeEach
  fun setupLists() {
    eventStreamGenerator.setupDatasetTestData()
    projectEventStoreUtils.reset()

    setAuthentication(getIdentifier("userCsm2"))
  }

  @Test
  fun `reschedule of all project milestones if no other filter criteria`() {
    val date = now()

    eventStreamGenerator.submitMilestonesWithList(
        listReference = "list1",
        date = date.plusDays(2),
        milestones =
            listOf(
                SubmitMilestoneWithListDto(type = INVESTOR),
                SubmitMilestoneWithListDto(type = CRAFT, craft = getByReference("projectCraft")),
                SubmitMilestoneWithListDto(type = PROJECT),
            ))
    eventStreamGenerator.submitMilestonesWithList(
        listReference = "list2",
        date = date.plusDays(4),
        milestones = listOf(SubmitMilestoneWithListDto(type = INVESTOR)))
    projectEventStoreUtils.reset()

    val searchDto = SearchMilestonesDto(projectIdentifier = getIdentifier("project").asProjectId())

    val validation = cut.validateMilestones(searchDto)
    val reschedule = cut.rescheduleMilestones(2, searchDto)

    val (list1Milestone0, list1Milestone1, list1Milestone2, list2Milestone0) =
        findMilestonesInOrder("list1M0", "list1M1", "list1M2", "list2M0")

    // assert result
    verifyRescheduleMatchValidation(reschedule, validation)
    assertThat(reschedule.successful)
        .containsExactlyInAnyOrder(
            list1Milestone0.identifier,
            list1Milestone1.identifier,
            list1Milestone2.identifier,
            list2Milestone0.identifier,
            existingMilestone.identifier)

    // assert all shifted
    assertThat(list1Milestone0.date).isEqualTo(date.plusDays(4))
    assertThat(list1Milestone1.date).isEqualTo(date.plusDays(4))
    assertThat(list1Milestone2.date).isEqualTo(date.plusDays(4))
    assertThat(list2Milestone0.date).isEqualTo(date.plusDays(6))

    // assert that events sent (3 extra for the test setup milestone and milestone list)
    projectEventStoreUtils.verifyContainsInSequence(
        BatchOperationStartedEventAvro::class.java,
        MilestoneEventAvro::class.java,
        MilestoneListEventAvro::class.java,
        MilestoneListEventAvro::class.java,
        MilestoneEventAvro::class.java,
        MilestoneListEventAvro::class.java,
        MilestoneListEventAvro::class.java,
        MilestoneEventAvro::class.java,
        MilestoneListEventAvro::class.java,
        MilestoneListEventAvro::class.java,
        MilestoneEventAvro::class.java,
        MilestoneListEventAvro::class.java,
        MilestoneListEventAvro::class.java,
        MilestoneEventAvro::class.java,
        MilestoneListEventAvro::class.java,
        MilestoneListEventAvro::class.java,
        BatchOperationFinishedEventAvro::class.java)

    projectEventStoreUtils
        .verifyContainsAndGet(BatchOperationStartedEventAvro::class.java, null, false)
        .also {
          assertThat(it.auditingInformation.date).isCloseToNow()
          assertThat(it.auditingInformation.user).isIdentifierOf("userCsm2")
        }

    projectEventStoreUtils
        .verifyContainsAndGet(BatchOperationFinishedEventAvro::class.java, null, false)
        .also {
          assertThat(it.auditingInformation.date).isCloseToNow()
          assertThat(it.auditingInformation.user).isIdentifierOf("userCsm2")
        }

    // one extra for the test setup milestone
    projectEventStoreUtils
        .verifyContainsAndGet(
            MilestoneEventAvro::class.java, MilestoneEventEnumAvro.UPDATED, 5, false)
        .also { verifyUpdateAggregate(it) }
  }

  @Test
  fun `reschedule milestones by INVESTOR type`() {
    val date = now()

    eventStreamGenerator.submitMilestonesWithList(
        listReference = "list1",
        date = date.plusDays(2),
        milestones =
            listOf(
                SubmitMilestoneWithListDto(type = INVESTOR),
                SubmitMilestoneWithListDto(type = CRAFT, craft = getByReference("projectCraft")),
                SubmitMilestoneWithListDto(type = PROJECT),
            ))
    eventStreamGenerator.submitMilestonesWithList(
        listReference = "list2",
        date = date.plusDays(4),
        milestones = listOf(SubmitMilestoneWithListDto(type = INVESTOR)))
    projectEventStoreUtils.reset()

    val searchDto =
        SearchMilestonesDto(
            projectIdentifier = getIdentifier("project").asProjectId(),
            typesFilter = TypesFilterDto(types = setOf(MilestoneTypeEnum.INVESTOR)))

    val validation = cut.validateMilestones(searchDto)
    val reschedule = cut.rescheduleMilestones(2, searchDto)

    val (list1Milestone0, list1Milestone1, list1Milestone2, list2Milestone0) =
        findMilestonesInOrder("list1M0", "list1M1", "list1M2", "list2M0")

    // assert result
    verifyRescheduleMatchValidation(reschedule, validation)
    assertThat(reschedule.successful)
        .containsExactlyInAnyOrder(list1Milestone0.identifier, list2Milestone0.identifier)

    // assert list1m0 and list2m0 shifted
    assertThat(list1Milestone0.date).isEqualTo(date.plusDays(4))
    assertThat(list2Milestone0.date).isEqualTo(date.plusDays(6))

    // assert list1m1 and list1m2 not shifted
    assertThat(list1Milestone1.date).isEqualTo(date.plusDays(2))
    assertThat(list1Milestone2.date).isEqualTo(date.plusDays(2))

    // assert that events sent
    projectEventStoreUtils.verifyContainsInSequence(
        BatchOperationStartedEventAvro::class.java,
        MilestoneEventAvro::class.java,
        MilestoneListEventAvro::class.java,
        MilestoneListEventAvro::class.java,
        MilestoneEventAvro::class.java,
        MilestoneListEventAvro::class.java,
        MilestoneListEventAvro::class.java,
        BatchOperationFinishedEventAvro::class.java)

    projectEventStoreUtils
        .verifyContainsAndGet(BatchOperationStartedEventAvro::class.java, null, false)
        .also {
          assertThat(it.auditingInformation.date).isCloseToNow()
          assertThat(it.auditingInformation.user).isIdentifierOf("userCsm2")
        }

    projectEventStoreUtils
        .verifyContainsAndGet(BatchOperationFinishedEventAvro::class.java, null, false)
        .also {
          assertThat(it.auditingInformation.date).isCloseToNow()
          assertThat(it.auditingInformation.user).isIdentifierOf("userCsm2")
        }

    projectEventStoreUtils
        .verifyContainsAndGet(
            MilestoneEventAvro::class.java, MilestoneEventEnumAvro.UPDATED, 2, false)
        .also { verifyUpdateAggregate(it) }
  }

  @Test
  fun `reschedule milestones by CRAFT type`() {
    val date = now()

    eventStreamGenerator.submitMilestonesWithList(
        listReference = "list1",
        date = date.plusDays(2),
        milestones =
            listOf(
                SubmitMilestoneWithListDto(type = INVESTOR),
                SubmitMilestoneWithListDto(type = CRAFT, craft = getByReference("projectCraft")),
                SubmitMilestoneWithListDto(type = PROJECT),
            ))
    eventStreamGenerator.submitMilestonesWithList(
        listReference = "list2",
        date = date.plusDays(4),
        milestones = listOf(SubmitMilestoneWithListDto(type = INVESTOR)))
    projectEventStoreUtils.reset()

    val searchDto =
        SearchMilestonesDto(
            projectIdentifier = getIdentifier("project").asProjectId(),
            typesFilter = TypesFilterDto(types = setOf(MilestoneTypeEnum.CRAFT)))

    val validation = cut.validateMilestones(searchDto)
    val reschedule = cut.rescheduleMilestones(2, searchDto)

    val (list1Milestone0, list1Milestone1, list1Milestone2, list2Milestone0) =
        findMilestonesInOrder("list1M0", "list1M1", "list1M2", "list2M0")

    // assert result
    verifyRescheduleMatchValidation(reschedule, validation)
    assertThat(reschedule.successful).containsExactlyInAnyOrder(list1Milestone1.identifier)

    // assert list1m1 shifted
    assertThat(list1Milestone1.date).isEqualTo(date.plusDays(4))

    // assert list1m0, list1m2 and list2m0 not shifted
    assertThat(list1Milestone0.date).isEqualTo(date.plusDays(2))
    assertThat(list1Milestone2.date).isEqualTo(date.plusDays(2))
    assertThat(list2Milestone0.date).isEqualTo(date.plusDays(4))

    // assert that events sent
    projectEventStoreUtils.verifyContainsInSequence(
        BatchOperationStartedEventAvro::class.java,
        MilestoneEventAvro::class.java,
        MilestoneListEventAvro::class.java,
        MilestoneListEventAvro::class.java,
        BatchOperationFinishedEventAvro::class.java)

    projectEventStoreUtils
        .verifyContainsAndGet(BatchOperationStartedEventAvro::class.java, null, false)
        .also {
          assertThat(it.auditingInformation.date).isCloseToNow()
          assertThat(it.auditingInformation.user).isIdentifierOf("userCsm2")
        }

    projectEventStoreUtils
        .verifyContainsAndGet(BatchOperationFinishedEventAvro::class.java, null, false)
        .also {
          assertThat(it.auditingInformation.date).isCloseToNow()
          assertThat(it.auditingInformation.user).isIdentifierOf("userCsm2")
        }

    projectEventStoreUtils
        .verifyContainsAndGet(
            MilestoneEventAvro::class.java, MilestoneEventEnumAvro.UPDATED, 1, false)
        .also { verifyUpdateAggregate(it) }
  }

  @Test
  fun `reschedule milestones by PROJECT type`() {
    val date = now()

    eventStreamGenerator.submitMilestonesWithList(
        listReference = "list1",
        date = date.plusDays(2),
        milestones =
            listOf(
                SubmitMilestoneWithListDto(type = INVESTOR),
                SubmitMilestoneWithListDto(type = CRAFT, craft = getByReference("projectCraft")),
                SubmitMilestoneWithListDto(type = PROJECT),
            ))
    eventStreamGenerator.submitMilestonesWithList(
        listReference = "list2",
        date = date.plusDays(4),
        milestones = listOf(SubmitMilestoneWithListDto(type = INVESTOR)))
    projectEventStoreUtils.reset()

    val searchDto =
        SearchMilestonesDto(
            projectIdentifier = getIdentifier("project").asProjectId(),
            typesFilter = TypesFilterDto(types = setOf(MilestoneTypeEnum.PROJECT)))

    val validation = cut.validateMilestones(searchDto)
    val reschedule = cut.rescheduleMilestones(2, searchDto)

    val (list1Milestone0, list1Milestone1, list1Milestone2, list2Milestone0) =
        findMilestonesInOrder("list1M0", "list1M1", "list1M2", "list2M0")

    // assert result
    verifyRescheduleMatchValidation(reschedule, validation)
    assertThat(reschedule.successful)
        .containsExactlyInAnyOrder(list1Milestone2.identifier, existingMilestone.identifier)

    // assert list1m2 shifted
    assertThat(list1Milestone2.date).isEqualTo(date.plusDays(4))

    // assert list1m0, list1m1 and list2m0 not shifted
    assertThat(list1Milestone0.date).isEqualTo(date.plusDays(2))
    assertThat(list1Milestone1.date).isEqualTo(date.plusDays(2))
    assertThat(list2Milestone0.date).isEqualTo(date.plusDays(4))

    // assert that events sent (3 extra for the test setup milestone and milestone list)
    projectEventStoreUtils.verifyContainsInSequence(
        BatchOperationStartedEventAvro::class.java,
        MilestoneEventAvro::class.java,
        MilestoneListEventAvro::class.java,
        MilestoneListEventAvro::class.java,
        MilestoneEventAvro::class.java,
        MilestoneListEventAvro::class.java,
        MilestoneListEventAvro::class.java,
        BatchOperationFinishedEventAvro::class.java)

    projectEventStoreUtils
        .verifyContainsAndGet(BatchOperationStartedEventAvro::class.java, null, false)
        .also {
          assertThat(it.auditingInformation.date).isCloseToNow()
          assertThat(it.auditingInformation.user).isIdentifierOf("userCsm2")
        }

    projectEventStoreUtils
        .verifyContainsAndGet(BatchOperationFinishedEventAvro::class.java, null, false)
        .also {
          assertThat(it.auditingInformation.date).isCloseToNow()
          assertThat(it.auditingInformation.user).isIdentifierOf("userCsm2")
        }

    // one extra for the test setup milestone
    projectEventStoreUtils
        .verifyContainsAndGet(
            MilestoneEventAvro::class.java, MilestoneEventEnumAvro.UPDATED, 2, false)
        .also { verifyUpdateAggregate(it) }
  }

  @Test
  fun `reschedule milestones by project craft`() {
    val date = now()

    eventStreamGenerator.submitMilestonesWithList(
        listReference = "list1",
        date = date.plusDays(2),
        milestones =
            listOf(
                SubmitMilestoneWithListDto(type = INVESTOR),
                SubmitMilestoneWithListDto(type = CRAFT, craft = getByReference("projectCraft")),
                SubmitMilestoneWithListDto(type = PROJECT),
            ))
    eventStreamGenerator.submitMilestonesWithList(
        listReference = "list2",
        date = date.plusDays(4),
        milestones = listOf(SubmitMilestoneWithListDto(type = INVESTOR)))
    projectEventStoreUtils.reset()

    val searchDto =
        SearchMilestonesDto(
            projectIdentifier = getIdentifier("project").asProjectId(),
            typesFilter = TypesFilterDto(craftIdentifiers = setOf(existingProjectCraft.identifier)))

    val validation = cut.validateMilestones(searchDto)
    val reschedule = cut.rescheduleMilestones(2, searchDto)

    val (list1Milestone1) = findMilestonesInOrder("list1M1")

    // assert result
    verifyRescheduleMatchValidation(reschedule, validation)
    assertThat(reschedule.successful).containsExactlyInAnyOrder(list1Milestone1.identifier)

    // assert all shifted:
    // - the one that as a CRAFT it matches
    // - and the other are shifted since the filter accepts all the ones without crafts
    assertThat(list1Milestone1.date).isEqualTo(date.plusDays(4))

    // assert that events sent (3 extra for the test setup milestone and milestone list)
    projectEventStoreUtils.verifyContainsInSequence(
        BatchOperationStartedEventAvro::class.java,
        MilestoneEventAvro::class.java,
        MilestoneListEventAvro::class.java,
        MilestoneListEventAvro::class.java,
        BatchOperationFinishedEventAvro::class.java)

    projectEventStoreUtils
        .verifyContainsAndGet(BatchOperationStartedEventAvro::class.java, null, false)
        .also {
          assertThat(it.auditingInformation.date).isCloseToNow()
          assertThat(it.auditingInformation.user).isIdentifierOf("userCsm2")
        }

    projectEventStoreUtils
        .verifyContainsAndGet(BatchOperationFinishedEventAvro::class.java, null, false)
        .also {
          assertThat(it.auditingInformation.date).isCloseToNow()
          assertThat(it.auditingInformation.user).isIdentifierOf("userCsm2")
        }

    // one extra for the test setup milestone
    projectEventStoreUtils
        .verifyContainsAndGet(
            MilestoneEventAvro::class.java, MilestoneEventEnumAvro.UPDATED, 1, false)
        .also { verifyUpdateAggregate(it) }
  }

  @Test
  fun `reschedule milestones by work area`() {
    val date = now()

    eventStreamGenerator.submitMilestonesWithList(
        listReference = "list1",
        date = date.plusDays(2),
        workArea = "workArea",
        milestones =
            listOf(
                SubmitMilestoneWithListDto(type = INVESTOR),
                SubmitMilestoneWithListDto(type = CRAFT, craft = getByReference("projectCraft")),
                SubmitMilestoneWithListDto(type = PROJECT),
            ))
    eventStreamGenerator.submitMilestonesWithList(
        listReference = "list2",
        date = date.plusDays(4),
        milestones = listOf(SubmitMilestoneWithListDto(type = INVESTOR)))
    projectEventStoreUtils.reset()

    val searchDto =
        SearchMilestonesDto(
            projectIdentifier = getIdentifier("project").asProjectId(),
            workAreas =
                WorkAreaFilterDto(
                    workAreaIdentifiers = setOf(WorkAreaIdOrEmpty(existingWorkArea.identifier))))

    val validation = cut.validateMilestones(searchDto)
    val reschedule = cut.rescheduleMilestones(2, searchDto)

    val (list1Milestone0, list1Milestone1, list1Milestone2, list2Milestone0) =
        findMilestonesInOrder("list1M0", "list1M1", "list1M2", "list2M0")

    // assert result
    verifyRescheduleMatchValidation(reschedule, validation)
    assertThat(reschedule.successful)
        .containsExactlyInAnyOrder(
            list1Milestone0.identifier, list1Milestone1.identifier, list1Milestone2.identifier)

    // assert all except the one without workArea (list2m0) are shifted:
    assertThat(list1Milestone0.date).isEqualTo(date.plusDays(4))
    assertThat(list1Milestone1.date).isEqualTo(date.plusDays(4))
    assertThat(list1Milestone2.date).isEqualTo(date.plusDays(4))

    assertThat(list2Milestone0.date).isEqualTo(date.plusDays(4))

    // assert that events sent
    projectEventStoreUtils.verifyContainsInSequence(
        BatchOperationStartedEventAvro::class.java,
        MilestoneEventAvro::class.java,
        MilestoneListEventAvro::class.java,
        MilestoneListEventAvro::class.java,
        MilestoneEventAvro::class.java,
        MilestoneListEventAvro::class.java,
        MilestoneListEventAvro::class.java,
        MilestoneEventAvro::class.java,
        MilestoneListEventAvro::class.java,
        MilestoneListEventAvro::class.java,
        BatchOperationFinishedEventAvro::class.java)

    projectEventStoreUtils
        .verifyContainsAndGet(BatchOperationStartedEventAvro::class.java, null, false)
        .also {
          assertThat(it.auditingInformation.date).isCloseToNow()
          assertThat(it.auditingInformation.user).isIdentifierOf("userCsm2")
        }

    projectEventStoreUtils
        .verifyContainsAndGet(BatchOperationFinishedEventAvro::class.java, null, false)
        .also {
          assertThat(it.auditingInformation.date).isCloseToNow()
          assertThat(it.auditingInformation.user).isIdentifierOf("userCsm2")
        }

    projectEventStoreUtils
        .verifyContainsAndGet(
            MilestoneEventAvro::class.java, MilestoneEventEnumAvro.UPDATED, 3, false)
        .also { verifyUpdateAggregate(it) }
  }

  @Test
  fun `reschedule milestones by header`() {
    val date = now()

    eventStreamGenerator.submitMilestonesWithList(
        listReference = "list1",
        date = date.plusDays(2),
        header = true,
        milestones =
            listOf(
                SubmitMilestoneWithListDto(type = INVESTOR),
                SubmitMilestoneWithListDto(type = CRAFT, craft = getByReference("projectCraft")),
                SubmitMilestoneWithListDto(type = PROJECT),
            ))
    eventStreamGenerator.submitMilestonesWithList(
        listReference = "list2",
        date = date.plusDays(4),
        milestones = listOf(SubmitMilestoneWithListDto(type = INVESTOR)))
    projectEventStoreUtils.reset()

    val searchDto =
        SearchMilestonesDto(
            projectIdentifier = getIdentifier("project").asProjectId(),
            workAreas = WorkAreaFilterDto(header = true))

    val validation = cut.validateMilestones(searchDto)
    val reschedule = cut.rescheduleMilestones(2, searchDto)

    val (list1Milestone0, list1Milestone1, list1Milestone2, list2Milestone0) =
        findMilestonesInOrder("list1M0", "list1M1", "list1M2", "list2M0")

    // assert result
    verifyRescheduleMatchValidation(reschedule, validation)
    assertThat(reschedule.successful)
        .containsExactlyInAnyOrder(
            list1Milestone0.identifier,
            list1Milestone1.identifier,
            list1Milestone2.identifier,
            existingMilestone.identifier)

    // assert all except the one without header true (list2m0) are shifted:
    assertThat(list1Milestone0.date).isEqualTo(date.plusDays(4))
    assertThat(list1Milestone1.date).isEqualTo(date.plusDays(4))
    assertThat(list1Milestone2.date).isEqualTo(date.plusDays(4))

    assertThat(list2Milestone0.date).isEqualTo(date.plusDays(4))

    // assert that events sent (3 extra for the test setup milestone and milestone list)
    projectEventStoreUtils.verifyContainsInSequence(
        BatchOperationStartedEventAvro::class.java,
        MilestoneEventAvro::class.java,
        MilestoneListEventAvro::class.java,
        MilestoneListEventAvro::class.java,
        MilestoneEventAvro::class.java,
        MilestoneListEventAvro::class.java,
        MilestoneListEventAvro::class.java,
        MilestoneEventAvro::class.java,
        MilestoneListEventAvro::class.java,
        MilestoneListEventAvro::class.java,
        MilestoneEventAvro::class.java,
        MilestoneListEventAvro::class.java,
        MilestoneListEventAvro::class.java,
        BatchOperationFinishedEventAvro::class.java)

    projectEventStoreUtils
        .verifyContainsAndGet(BatchOperationStartedEventAvro::class.java, null, false)
        .also {
          assertThat(it.auditingInformation.date).isCloseToNow()
          assertThat(it.auditingInformation.user).isIdentifierOf("userCsm2")
        }

    projectEventStoreUtils
        .verifyContainsAndGet(BatchOperationFinishedEventAvro::class.java, null, false)
        .also {
          assertThat(it.auditingInformation.date).isCloseToNow()
          assertThat(it.auditingInformation.user).isIdentifierOf("userCsm2")
        }

    // one extra for the test setup milestone
    projectEventStoreUtils
        .verifyContainsAndGet(
            MilestoneEventAvro::class.java, MilestoneEventEnumAvro.UPDATED, 4, false)
        .also { verifyUpdateAggregate(it) }
  }

  @Test
  fun `reschedule milestones by start date`() {
    val date = now()

    eventStreamGenerator.submitMilestonesWithList(
        listReference = "list1",
        date = date,
        milestones =
            listOf(
                SubmitMilestoneWithListDto(type = INVESTOR),
                SubmitMilestoneWithListDto(type = CRAFT, craft = getByReference("projectCraft")),
                SubmitMilestoneWithListDto(type = PROJECT),
            ))
    eventStreamGenerator.submitMilestonesWithList(
        listReference = "list2",
        date = date.minusDays(2),
        milestones = listOf(SubmitMilestoneWithListDto(type = INVESTOR)))
    projectEventStoreUtils.reset()

    val searchDto =
        SearchMilestonesDto(
            projectIdentifier = getIdentifier("project").asProjectId(), from = now())

    val validation = cut.validateMilestones(searchDto)
    val reschedule = cut.rescheduleMilestones(2, searchDto)

    val (list1Milestone0, list1Milestone1, list1Milestone2, list2Milestone0) =
        findMilestonesInOrder("list1M0", "list1M1", "list1M2", "list2M0")

    // assert result
    verifyRescheduleMatchValidation(reschedule, validation)
    assertThat(reschedule.successful)
        .containsExactlyInAnyOrder(
            list1Milestone0.identifier,
            list1Milestone1.identifier,
            list1Milestone2.identifier,
            existingMilestone.identifier)

    // assert list1 shifted
    assertThat(list1Milestone0.date).isEqualTo(date.plusDays(2))
    assertThat(list1Milestone1.date).isEqualTo(date.plusDays(2))
    assertThat(list1Milestone2.date).isEqualTo(date.plusDays(2))

    // assert list2 not shifted
    assertThat(list2Milestone0.date).isEqualTo(date.minusDays(2))

    // assert that events sent (3 extra for the test setup milestone and milestone list)
    projectEventStoreUtils.verifyContainsInSequence(
        BatchOperationStartedEventAvro::class.java,
        MilestoneEventAvro::class.java,
        MilestoneListEventAvro::class.java,
        MilestoneListEventAvro::class.java,
        MilestoneEventAvro::class.java,
        MilestoneListEventAvro::class.java,
        MilestoneListEventAvro::class.java,
        MilestoneEventAvro::class.java,
        MilestoneListEventAvro::class.java,
        MilestoneListEventAvro::class.java,
        MilestoneEventAvro::class.java,
        MilestoneListEventAvro::class.java,
        MilestoneListEventAvro::class.java,
        BatchOperationFinishedEventAvro::class.java)

    projectEventStoreUtils
        .verifyContainsAndGet(BatchOperationStartedEventAvro::class.java, null, false)
        .also {
          assertThat(it.auditingInformation.date).isCloseToNow()
          assertThat(it.auditingInformation.user).isIdentifierOf("userCsm2")
        }

    projectEventStoreUtils
        .verifyContainsAndGet(BatchOperationFinishedEventAvro::class.java, null, false)
        .also {
          assertThat(it.auditingInformation.date).isCloseToNow()
          assertThat(it.auditingInformation.user).isIdentifierOf("userCsm2")
        }

    // one extra for the test setup milestone
    projectEventStoreUtils
        .verifyContainsAndGet(
            MilestoneEventAvro::class.java, MilestoneEventEnumAvro.UPDATED, 4, false)
        .also { verifyUpdateAggregate(it) }
  }

  @Test
  fun `reschedule milestones by end date`() {
    val date = now()

    eventStreamGenerator.submitMilestonesWithList(
        listReference = "list1",
        date = date,
        milestones =
            listOf(
                SubmitMilestoneWithListDto(type = INVESTOR),
                SubmitMilestoneWithListDto(type = CRAFT, craft = getByReference("projectCraft")),
                SubmitMilestoneWithListDto(type = PROJECT),
            ))
    eventStreamGenerator.submitMilestonesWithList(
        listReference = "list2",
        date = date.minusDays(2),
        milestones = listOf(SubmitMilestoneWithListDto(type = INVESTOR)))
    projectEventStoreUtils.reset()

    val searchDto =
        SearchMilestonesDto(
            projectIdentifier = getIdentifier("project").asProjectId(), to = now().minusDays(1))

    val validation = cut.validateMilestones(searchDto)
    val reschedule = cut.rescheduleMilestones(2, searchDto)

    val (list1Milestone0, list1Milestone1, list1Milestone2, list2Milestone0) =
        findMilestonesInOrder("list1M0", "list1M1", "list1M2", "list2M0")

    // assert result
    verifyRescheduleMatchValidation(reschedule, validation)
    assertThat(reschedule.successful).containsExactlyInAnyOrder(list2Milestone0.identifier)

    // assert list1 not shifted
    assertThat(list1Milestone0.date).isEqualTo(date)
    assertThat(list1Milestone1.date).isEqualTo(date)
    assertThat(list1Milestone2.date).isEqualTo(date)

    // assert list2 shifted
    assertThat(list2Milestone0.date).isEqualTo(date)

    // assert that events sent
    projectEventStoreUtils.verifyContainsInSequence(
        BatchOperationStartedEventAvro::class.java,
        MilestoneEventAvro::class.java,
        MilestoneListEventAvro::class.java,
        MilestoneListEventAvro::class.java,
        BatchOperationFinishedEventAvro::class.java)

    projectEventStoreUtils
        .verifyContainsAndGet(BatchOperationStartedEventAvro::class.java, null, false)
        .also {
          assertThat(it.auditingInformation.date).isCloseToNow()
          assertThat(it.auditingInformation.user).isIdentifierOf("userCsm2")
        }

    projectEventStoreUtils
        .verifyContainsAndGet(BatchOperationFinishedEventAvro::class.java, null, false)
        .also {
          assertThat(it.auditingInformation.date).isCloseToNow()
          assertThat(it.auditingInformation.user).isIdentifierOf("userCsm2")
        }

    projectEventStoreUtils
        .verifyContainsAndGet(
            MilestoneEventAvro::class.java, MilestoneEventEnumAvro.UPDATED, 1, false)
        .also { verifyUpdateAggregate(it) }
  }

  @Test
  fun `reschedule milestones by all dates in range`() {
    val date = now()

    eventStreamGenerator.submitMilestonesWithList(
        listReference = "list1",
        date = date,
        milestones =
            listOf(
                SubmitMilestoneWithListDto(type = INVESTOR),
                SubmitMilestoneWithListDto(type = CRAFT, craft = getByReference("projectCraft")),
                SubmitMilestoneWithListDto(type = PROJECT),
            ))
    eventStreamGenerator.submitMilestonesWithList(
        listReference = "list2",
        date = date.minusDays(2),
        milestones = listOf(SubmitMilestoneWithListDto(type = INVESTOR)))
    projectEventStoreUtils.reset()

    val searchDto =
        SearchMilestonesDto(
            projectIdentifier = getIdentifier("project").asProjectId(),
            from = now().minusDays(3),
            to = now().plusDays(3))

    val validation = cut.validateMilestones(searchDto)
    val reschedule = cut.rescheduleMilestones(2, searchDto)

    val (list1Milestone0, list1Milestone1, list1Milestone2, list2Milestone0) =
        findMilestonesInOrder("list1M0", "list1M1", "list1M2", "list2M0")

    // assert result
    verifyRescheduleMatchValidation(reschedule, validation)
    assertThat(reschedule.successful)
        .containsExactlyInAnyOrder(
            list1Milestone0.identifier,
            list1Milestone1.identifier,
            list1Milestone2.identifier,
            list2Milestone0.identifier,
            existingMilestone.identifier)

    // assert all shifted
    assertThat(list1Milestone0.date).isEqualTo(date.plusDays(2))
    assertThat(list1Milestone1.date).isEqualTo(date.plusDays(2))
    assertThat(list1Milestone2.date).isEqualTo(date.plusDays(2))
    assertThat(list2Milestone0.date).isEqualTo(date)

    // assert that events sent (3 extra for the test setup milestone and milestone list)
    projectEventStoreUtils.verifyContainsInSequence(
        BatchOperationStartedEventAvro::class.java,
        MilestoneEventAvro::class.java,
        MilestoneListEventAvro::class.java,
        MilestoneListEventAvro::class.java,
        MilestoneEventAvro::class.java,
        MilestoneListEventAvro::class.java,
        MilestoneListEventAvro::class.java,
        MilestoneEventAvro::class.java,
        MilestoneListEventAvro::class.java,
        MilestoneListEventAvro::class.java,
        MilestoneEventAvro::class.java,
        MilestoneListEventAvro::class.java,
        MilestoneListEventAvro::class.java,
        MilestoneEventAvro::class.java,
        MilestoneListEventAvro::class.java,
        MilestoneListEventAvro::class.java,
        BatchOperationFinishedEventAvro::class.java)

    projectEventStoreUtils
        .verifyContainsAndGet(BatchOperationStartedEventAvro::class.java, null, false)
        .also {
          assertThat(it.auditingInformation.date).isCloseToNow()
          assertThat(it.auditingInformation.user).isIdentifierOf("userCsm2")
        }

    projectEventStoreUtils
        .verifyContainsAndGet(BatchOperationFinishedEventAvro::class.java, null, false)
        .also {
          assertThat(it.auditingInformation.date).isCloseToNow()
          assertThat(it.auditingInformation.user).isIdentifierOf("userCsm2")
        }

    // one extra for the test setup milestone
    projectEventStoreUtils
        .verifyContainsAndGet(
            MilestoneEventAvro::class.java, MilestoneEventEnumAvro.UPDATED, 5, false)
        .also { verifyUpdateAggregate(it) }
  }

  @Test
  fun `reschedule milestones by milestone list identifier`() {
    val date = now()

    eventStreamGenerator.submitMilestonesWithList(
        listReference = "list1",
        date = date,
        milestones =
            listOf(
                SubmitMilestoneWithListDto(type = INVESTOR),
                SubmitMilestoneWithListDto(type = CRAFT, craft = getByReference("projectCraft")),
                SubmitMilestoneWithListDto(type = PROJECT),
            ))
    eventStreamGenerator.submitMilestonesWithList(
        listReference = "list2",
        date = date.minusDays(2),
        milestones = listOf(SubmitMilestoneWithListDto(type = INVESTOR)))
    projectEventStoreUtils.reset()

    val searchDto =
        SearchMilestonesDto(
            projectIdentifier = getIdentifier("project").asProjectId(),
            milestoneListIdentifiers = setOf(getIdentifier("list2").asMilestoneListId()))

    val validation = cut.validateMilestones(searchDto)
    val reschedule = cut.rescheduleMilestones(2, searchDto)

    val (list1Milestone0, list1Milestone1, list1Milestone2, list2Milestone0) =
        findMilestonesInOrder("list1M0", "list1M1", "list1M2", "list2M0")

    // assert result
    verifyRescheduleMatchValidation(reschedule, validation)
    assertThat(reschedule.successful).containsExactlyInAnyOrder(list2Milestone0.identifier)

    // assert list1 not shifted
    assertThat(list1Milestone0.date).isEqualTo(date)
    assertThat(list1Milestone1.date).isEqualTo(date)
    assertThat(list1Milestone2.date).isEqualTo(date)

    // assert list2 shifted
    assertThat(list2Milestone0.date).isEqualTo(date)

    // assert that events sent
    projectEventStoreUtils.verifyContainsInSequence(
        BatchOperationStartedEventAvro::class.java,
        MilestoneEventAvro::class.java,
        MilestoneListEventAvro::class.java,
        MilestoneListEventAvro::class.java,
        BatchOperationFinishedEventAvro::class.java)

    projectEventStoreUtils
        .verifyContainsAndGet(BatchOperationStartedEventAvro::class.java, null, false)
        .also {
          assertThat(it.auditingInformation.date).isCloseToNow()
          assertThat(it.auditingInformation.user).isIdentifierOf("userCsm2")
        }

    projectEventStoreUtils
        .verifyContainsAndGet(BatchOperationFinishedEventAvro::class.java, null, false)
        .also {
          assertThat(it.auditingInformation.date).isCloseToNow()
          assertThat(it.auditingInformation.user).isIdentifierOf("userCsm2")
        }

    projectEventStoreUtils
        .verifyContainsAndGet(
            MilestoneEventAvro::class.java, MilestoneEventEnumAvro.UPDATED, 1, false)
        .also { verifyUpdateAggregate(it) }
  }

  @Test
  fun `reschedule no milestones none if filter matches nothing`() {
    val date = now()

    eventStreamGenerator.submitMilestonesWithList(
        listReference = "list1",
        date = date.plusDays(2),
        milestones =
            listOf(
                SubmitMilestoneWithListDto(type = INVESTOR),
                SubmitMilestoneWithListDto(type = CRAFT, craft = getByReference("projectCraft")),
                SubmitMilestoneWithListDto(type = PROJECT),
            ))
    eventStreamGenerator.submitMilestonesWithList(
        listReference = "list2",
        date = date.plusDays(4),
        milestones = listOf(SubmitMilestoneWithListDto(type = INVESTOR)))
    projectEventStoreUtils.reset()

    val searchDto =
        SearchMilestonesDto(
            projectIdentifier = getIdentifier("project").asProjectId(),
            TypesFilterDto(craftIdentifiers = setOf(ProjectCraftId())))

    val validation = cut.validateMilestones(searchDto)
    val reschedule = cut.rescheduleMilestones(2, searchDto)

    // assert result
    verifyRescheduleMatchValidation(reschedule, validation)
    assertThat(reschedule.successful).isEmpty()

    // assert that no event was sent
    projectEventStoreUtils.verifyEmpty()
  }

  private fun findMilestonesInOrder(vararg milestoneReferences: String) =
      repositories.milestoneRepository
          .findAllWithDetailsByIdentifierIn(
              milestoneReferences.map { getIdentifier(it).asMilestoneId() })
          .sortedBy { milestone ->
            // make sure returned milestones are in the same order as the milestoneReferences
            milestoneReferences
                .map { getIdentifier(it).asMilestoneId() }
                .indexOf(milestone.identifier)
          }

  private fun verifyUpdateAggregate(events: Collection<MilestoneEventAvro>) {
    for (event in events) {
      val aggregate = event.aggregate
      val milestone =
          repositories.findMilestoneWithDetails(aggregate.getIdentifier().asMilestoneId())
      with(aggregate) {
        validateUpdatedAggregateAuditInfoAndAggregateIdentifier(
            this, milestone, ProjectmanagementAggregateTypeEnum.MILESTONE)
        assertThat(aggregate.date).isEqualTo(milestone.date.toEpochMilli())
      }
    }
  }

  private fun verifyRescheduleMatchValidation(
      reschedule: MilestoneRescheduleResult,
      validation: MilestoneRescheduleResult
  ) {
    assertThat(reschedule.successful).containsExactlyInAnyOrderElementsOf(validation.successful)
    assertThat(reschedule.failed).containsExactlyInAnyOrderElementsOf(validation.failed)
  }

  private fun AbstractLongAssert<*>.isCloseToNow() =
      this.isCloseTo(LocalDateTime.now().toEpochMilli(), Offset.offset(10_000))

  private fun AbstractStringAssert<*>.isIdentifierOf(reference: String) =
      this.isEqualTo(getIdentifier(reference).toString())
}
