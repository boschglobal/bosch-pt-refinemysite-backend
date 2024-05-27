/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.participant.query

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.api.asUserId
import com.bosch.pt.csm.cloud.common.extensions.toEpochMilli
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitCompany
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantStatusEnumAvro.ACTIVE
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantStatusEnumAvro.INACTIVE
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUserAndActivate
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractIntegrationTestV2
import com.bosch.pt.iot.smartsite.project.participant.asParticipantId
import com.bosch.pt.iot.smartsite.project.participant.shared.model.Participant
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import java.time.LocalDateTime.now
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

/**
 * This test was specifically added to test methods that are only used by the resource factories. It
 * tests the filter behaviour when multiple participants for the same user exist.
 *
 * WARNING: <br></br> The tests have to run with disabled hibernate listeners to set the
 * lastModifiedDate manually. If the hibernate event listeners (audit listener) are enabled, then
 * the audit parameters are overwritten and the tests don't work as expected anymore.
 */
@DisplayName("Test Participant Search Service")
@EnableAllKafkaListeners
class ParticipantQueryServiceIntegrationTest : AbstractIntegrationTestV2() {

  @Autowired private lateinit var cut: ParticipantQueryService

  private val company1AggregateIdentifier by lazy { getByReference("company1") }
  private val company2AggregateIdentifier by lazy { getByReference("company2") }
  private val company3AggregateIdentifier by lazy { getByReference("company3") }
  private val company4AggregateIdentifier by lazy { getByReference("company4") }
  private val participantIdentifier by lazy { getIdentifier("participant2").asParticipantId() }
  private val projectIdentifier by lazy { getIdentifier("project").asProjectId() }
  private val userIdentifier by lazy { getIdentifier("testUser").asUserId() }

  @BeforeEach
  fun setup() {
    eventStreamGenerator
        .submitUserAndActivate("testUser")
        .also { eventStreamGenerator ->
          for (i in 1..4) {
            eventStreamGenerator.submitCompany("company$i") {
              it.auditingInformationBuilder.createdDate = now().minusDays(10).toEpochMilli()
              it.auditingInformationBuilder.lastModifiedDate = now().minusDays(5).toEpochMilli()
            }
          }
        }
        .submitProject("project") {
          it.auditingInformationBuilder.createdDate = now().minusDays(10).toEpochMilli()
          it.auditingInformationBuilder.lastModifiedDate = now().minusDays(5).toEpochMilli()
        }
  }

  @Nested
  @DisplayName(
      "Verify that correct participant is filtered when multiple inactive participants exist")
  inner class FilterMultipleInactiveParticipants {

    @BeforeEach
    fun init() {
      eventStreamGenerator
          .submitParticipantG3("participant1") {
            it.company = company1AggregateIdentifier
            it.status = INACTIVE
            it.auditingInformationBuilder.createdDate = now().minusDays(10).toEpochMilli()
            it.auditingInformationBuilder.lastModifiedDate = now().toEpochMilli()
          }
          .submitParticipantG3("participant2") {
            it.company = company2AggregateIdentifier
            it.status = INACTIVE
            it.auditingInformationBuilder.createdDate = now().minusDays(10).toEpochMilli()
            it.auditingInformationBuilder.lastModifiedDate = now().plusDays(2).toEpochMilli()
          }
          .submitParticipantG3("participant3") {
            it.company = company3AggregateIdentifier
            it.status = INACTIVE
            it.auditingInformationBuilder.createdDate = now().minusDays(10).toEpochMilli()
            it.auditingInformationBuilder.lastModifiedDate = now().plusDays(1).toEpochMilli()
          }
    }

    @DisplayName("method for multiple projects")
    @Test
    fun verifyMethodForMultipleProjects() {
      val participants: Map<UserId, Map<ProjectId, Participant>> =
          cut.findActiveAndInactiveParticipants(setOf(userIdentifier), setOf(projectIdentifier))

      assertThat(participants[userIdentifier]!![projectIdentifier]!!.identifier)
          .isEqualTo(participantIdentifier)
    }

    @DisplayName("method for single project")
    @Test
    fun verifyMethodForSingleProject() {
      val participants =
          cut.findActiveAndInactiveParticipants(projectIdentifier, setOf(userIdentifier))

      assertThat(participants[userIdentifier]!!.identifier).isEqualTo(participantIdentifier)
    }
  }

  @Nested
  @DisplayName(
      "Verify that correct participant is filtered when active and inactive participants exist")
  inner class FilterActiveAndInactiveParticipants {

    @BeforeEach
    fun init() {
      eventStreamGenerator
          .submitParticipantG3("participant1") {
            it.company = company1AggregateIdentifier
            it.status = INACTIVE
            it.auditingInformationBuilder.createdDate = now().minusDays(10).toEpochMilli()
            it.auditingInformationBuilder.lastModifiedDate = now().toEpochMilli()
          }
          .submitParticipantG3("participant2") {
            it.company = company2AggregateIdentifier
            it.status = ACTIVE
            it.auditingInformationBuilder.createdDate = now().minusDays(10).toEpochMilli()
            it.auditingInformationBuilder.lastModifiedDate = now().plusDays(2).toEpochMilli()
          }
          .submitParticipantG3("participant3") {
            it.company = company3AggregateIdentifier
            it.status = INACTIVE
            it.auditingInformationBuilder.createdDate = now().minusDays(10).toEpochMilli()
            it.auditingInformationBuilder.lastModifiedDate = now().plusDays(3).toEpochMilli()
          }
          .submitParticipantG3("participant4") {
            it.company = company4AggregateIdentifier
            it.status = INACTIVE
            it.auditingInformationBuilder.createdDate = now().minusDays(10).toEpochMilli()
            it.auditingInformationBuilder.lastModifiedDate = now().plusDays(1).toEpochMilli()
          }
    }

    @DisplayName("method for multiple projects")
    @Test
    fun verifyMethodForMultipleProjects() {
      val participants: Map<UserId, Map<ProjectId, Participant>> =
          cut.findActiveAndInactiveParticipants(setOf(userIdentifier), setOf(projectIdentifier))

      assertThat(participants[userIdentifier]!![projectIdentifier]!!.identifier)
          .isEqualTo(participantIdentifier)
    }

    @DisplayName("method for single project")
    @Test
    fun verifyMethodForSingleProject() {
      val participants =
          cut.findActiveAndInactiveParticipants(projectIdentifier, setOf(userIdentifier))

      assertThat(participants[userIdentifier]!!.identifier).isEqualTo(participantIdentifier)
    }
  }
}
