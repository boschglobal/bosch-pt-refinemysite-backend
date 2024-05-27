/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.relation.boundary

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMilestone
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitRelation
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.relation.messages.RelationTypeEnumAvro
import com.bosch.pt.iot.smartsite.common.authorization.AbstractAuthorizationIntegrationTestV2
import com.bosch.pt.iot.smartsite.common.facade.rest.UserTypeAccess
import com.bosch.pt.iot.smartsite.project.milestone.domain.asMilestoneId
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.relation.boundary.dto.RelationDto
import com.bosch.pt.iot.smartsite.project.relation.boundary.dto.RelationDto.RelationElementDto
import com.bosch.pt.iot.smartsite.project.relation.model.RelationElementTypeEnum.MILESTONE
import com.bosch.pt.iot.smartsite.project.relation.model.RelationElementTypeEnum.TASK
import com.bosch.pt.iot.smartsite.project.relation.model.RelationTypeEnum.PART_OF
import com.bosch.pt.iot.smartsite.project.task.domain.asTaskId
import java.util.UUID.randomUUID
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

class RelationServiceAuthorizationIntegrationTest : AbstractAuthorizationIntegrationTestV2() {

  @Autowired private lateinit var cut: RelationService

  private val task by lazy { repositories.findTaskWithDetails(getIdentifier("task").asTaskId())!! }
  private val milestone by lazy {
    repositories.findMilestone(getIdentifier("milestone").asMilestoneId())!!
  }

  @BeforeEach
  fun init() {
    // Only the csm users can add project crafts to a project
    eventStreamGenerator
        .setUserContext("userCsm")
        .submitProjectCraftG2()
        .setUserContext("userCreator")
        .submitTask()
        .submitMilestone()
  }

  @ParameterizedTest
  @MethodSource("allActiveParticipantsWithAccess")
  fun `verify create relation is authorized for`(userType: UserTypeAccess) =
      checkAccessWith(userType) {
        cut.create(
            RelationDto(
                PART_OF,
                RelationElementDto(task.identifier.toUuid(), TASK),
                RelationElementDto(milestone.identifier.toUuid(), MILESTONE)),
            project.identifier)
      }

  @ParameterizedTest
  @MethodSource("noOneWithAccess")
  fun `verify create relation for non-existing project is denied for`(userType: UserTypeAccess) =
      checkAccessWith(userType) {
        cut.create(
            RelationDto(
                PART_OF,
                RelationElementDto(task.identifier.toUuid(), TASK),
                RelationElementDto(milestone.identifier.toUuid(), MILESTONE)),
            ProjectId())
      }

  @ParameterizedTest
  @MethodSource("allActiveParticipantsWithAccess")
  fun `verify batch create relations is authorized for`(userType: UserTypeAccess) =
      checkAccessWith(userType) {
        cut.createBatch(
            setOf(
                RelationDto(
                    PART_OF,
                    RelationElementDto(task.identifier.toUuid(), TASK),
                    RelationElementDto(milestone.identifier.toUuid(), MILESTONE))),
            project.identifier)
      }

  @ParameterizedTest
  @MethodSource("noOneWithAccess")
  fun `verify batch create relations for non-existing project is denied for`(
      userType: UserTypeAccess
  ) =
      checkAccessWith(userType) {
        cut.createBatch(
            setOf(
                RelationDto(
                    PART_OF,
                    RelationElementDto(task.identifier.toUuid(), TASK),
                    RelationElementDto(milestone.identifier.toUuid(), MILESTONE))),
            ProjectId())
      }

  @ParameterizedTest
  @MethodSource("allActiveParticipantsWithAccess")
  fun `verify find relation is authorized for`(userType: UserTypeAccess) {
    createRelation()

    checkAccessWith(userType) { cut.find(getIdentifier("relation"), project.identifier) }
  }

  @ParameterizedTest
  @MethodSource("noOneWithAccess")
  fun `verify find relation for non-existing project is denied for`(userType: UserTypeAccess) =
      checkAccessWith(userType) { cut.find(randomUUID(), ProjectId()) }

  @ParameterizedTest
  @MethodSource("allActiveParticipantsWithAccess")
  fun `verify batch find relations is authorized for`(userType: UserTypeAccess) {
    createRelation()

    checkAccessWith(userType) {
      cut.findBatch(setOf(getIdentifier("relation")), project.identifier)
    }
  }

  @ParameterizedTest
  @MethodSource("noOneWithAccess")
  fun `verify batch find relations for non-existing project is denied for`(
      userType: UserTypeAccess
  ) = checkAccessWith(userType) { cut.findBatch(setOf(randomUUID()), ProjectId()) }

  @ParameterizedTest
  @MethodSource("csmAndCrAndCreatorWithAccess")
  fun `verify delete relation is authorized for`(userType: UserTypeAccess) {
    createRelation()

    checkAccessWith(userType) { cut.delete(getIdentifier("relation"), project.identifier) }
  }

  @ParameterizedTest
  @MethodSource("noOneWithAccess")
  fun `verify delete relation for non-existing identifier is denied for`(userType: UserTypeAccess) =
      checkAccessWith(userType) { cut.delete(randomUUID(), project.identifier) }

  @ParameterizedTest
  @MethodSource("noOneWithAccess")
  fun `verify delete relation for non-existing project is denied for`(userType: UserTypeAccess) {
    createRelation()

    checkAccessWith(userType) { cut.delete(getIdentifier("relation"), ProjectId()) }
  }

  private fun createRelation() =
      eventStreamGenerator.setUserContext("userCreator").submitRelation {
        it.type = RelationTypeEnumAvro.PART_OF
        it.source = getByReference("task")
        it.target = getByReference("milestone")
      }
}
