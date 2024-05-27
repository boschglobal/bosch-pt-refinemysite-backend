/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.quickfilter.boundary

import com.bosch.pt.csm.cloud.common.facade.rest.etag.ETag
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkArea
import com.bosch.pt.iot.smartsite.application.security.AuthorizationTestUtils.doWithAuthorization
import com.bosch.pt.iot.smartsite.common.authorization.AbstractAuthorizationIntegrationTestV2
import com.bosch.pt.iot.smartsite.common.facade.rest.UserTypeAccess
import com.bosch.pt.iot.smartsite.common.facade.rest.UserTypeAccess.Companion.createGrantedGroup
import com.bosch.pt.iot.smartsite.project.milestone.shared.dto.SearchMilestonesDto
import com.bosch.pt.iot.smartsite.project.participant.asParticipantId
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.quickfilter.boundary.dto.QuickFilterDto
import com.bosch.pt.iot.smartsite.project.quickfilter.domain.QuickFilterId
import com.bosch.pt.iot.smartsite.project.quickfilter.domain.asQuickFilterId
import com.bosch.pt.iot.smartsite.project.quickfilter.model.MilestoneCriteria
import com.bosch.pt.iot.smartsite.project.quickfilter.model.QuickFilter
import com.bosch.pt.iot.smartsite.project.quickfilter.model.TaskCriteria
import com.bosch.pt.iot.smartsite.project.quickfilter.repository.QuickFilterRepository
import com.bosch.pt.iot.smartsite.project.task.shared.repository.dto.SearchTasksDto
import java.time.LocalDate
import java.util.UUID
import java.util.stream.Stream
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

open class QuickFilterServiceAuthorizationIntegrationTest(@Autowired val cut: QuickFilterService) :
    AbstractAuthorizationIntegrationTestV2() {

  @Autowired private lateinit var quickFilterRepository: QuickFilterRepository

  private val projectIdentifier by lazy { getIdentifier("project").asProjectId() }
  private val participantCsmIdentifier by lazy { getIdentifier("participantCsm").asParticipantId() }
  private lateinit var quickFilter: QuickFilter
  private lateinit var quickFilterDto: QuickFilterDto

  @BeforeEach
  fun init() {
    eventStreamGenerator.submitProjectCraftG2().submitWorkArea()
    quickFilter =
        QuickFilter(
            identifier = QuickFilterId(),
            name = "Quick Filter 1",
            projectIdentifier = projectIdentifier,
            participantIdentifier = participantCsmIdentifier,
            milestoneCriteria =
                MilestoneCriteria(from = LocalDate.now(), to = LocalDate.now().plusDays(14)),
            taskCriteria = TaskCriteria(from = LocalDate.now(), to = LocalDate.now().plusDays(14)))

    quickFilterDto =
        QuickFilterDto(
            name = "Test",
            highlight = false,
            useMilestoneCriteria = false,
            useTaskCriteria = false,
            taskCriteria = SearchTasksDto(projectIdentifier),
            milestoneCriteria = SearchMilestonesDto(projectIdentifier),
            projectRef = projectIdentifier)
  }

  @ParameterizedTest
  @MethodSource("allActiveParticipantsWithAccess")
  fun `Find quick filters is granted for`(userType: UserTypeAccess) {
    checkAccessWith(userType) { cut.findAllForCurrentUser(projectIdentifier) }
  }

  @ParameterizedTest
  @MethodSource("creatorAccess")
  fun `Update quick filter authorized`(userType: UserTypeAccess) {
    doWithAuthorization(userCsm, false) { quickFilterRepository.save(quickFilter) }

    checkAccessWith(userType) {
      cut.update(identifier = quickFilter.identifier, quickFilterDto, etag = ETag.from("0"))
    }
  }

  @ParameterizedTest
  @MethodSource("noOneWithAccess")
  fun `Update quick filter not authorized if the quick filter identifier is not existent`(
      userType: UserTypeAccess
  ) {
    checkAccessWith(userType) {
      cut.update(identifier = QuickFilterId(), quickFilterDto, etag = ETag.from("0"))
    }
  }

  @ParameterizedTest
  @MethodSource("noOneWithAccess")
  fun `Update quick filter not authorized if the project identifier is not existent`(
      userType: UserTypeAccess
  ) {
    doWithAuthorization(userCsm, false) {
      quickFilterRepository.save(quickFilter.copy(projectIdentifier = ProjectId()))
    }

    checkAccessWith(userType) {
      cut.update(identifier = quickFilter.identifier, quickFilterDto, etag = ETag.from("0"))
    }
  }

  @ParameterizedTest
  @MethodSource("creatorAccess")
  fun `Delete quick filter authorized`(userType: UserTypeAccess) {
    var identifier: UUID? = null
    doWithAuthorization(userCsm, false) {
      identifier = quickFilterRepository.save(quickFilter).identifier.identifier
    }

    checkAccessWith(userType) { cut.delete(identifier!!.asQuickFilterId(), projectIdentifier) }
  }

  @ParameterizedTest
  @MethodSource("noOneWithAccess")
  fun `Delete quick filter not authorized identifier is not exist`(userType: UserTypeAccess) {
    doWithAuthorization(userCsm, false) {
      quickFilterRepository.save(quickFilter).identifier.identifier
    }

    checkAccessWith(userType) { cut.delete(QuickFilterId(), projectIdentifier) }
  }

  @ParameterizedTest
  @MethodSource("noOneWithAccess")
  fun `Delete quick filter not authorized project identifier is not exist`(
      userType: UserTypeAccess
  ) {
    var identifier: UUID? = null
    doWithAuthorization(userCsm, false) {
      identifier = quickFilterRepository.save(quickFilter).identifier.identifier
    }

    checkAccessWith(userType) { cut.delete(identifier!!.asQuickFilterId(), ProjectId()) }
  }

  companion object {

    @JvmStatic
    fun creatorAccess(): Stream<UserTypeAccess> = createGrantedGroup(userTypes, setOf(CSM))
  }
}
