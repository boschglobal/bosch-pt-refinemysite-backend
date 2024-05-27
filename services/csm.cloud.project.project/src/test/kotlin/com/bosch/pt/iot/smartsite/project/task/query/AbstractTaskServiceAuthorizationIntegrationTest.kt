/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.task.query

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.PROJECT
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskStatusEnumAvro
import com.bosch.pt.iot.smartsite.application.security.AuthorizationTestUtils.doWithAuthorization
import com.bosch.pt.iot.smartsite.common.authorization.AbstractAuthorizationIntegrationTestV2
import com.bosch.pt.iot.smartsite.project.task.domain.asTaskId
import org.junit.jupiter.api.BeforeEach

abstract class AbstractTaskServiceAuthorizationIntegrationTest :
    AbstractAuthorizationIntegrationTestV2() {

  protected val taskUnassigned by lazy {
    repositories.findTaskWithDetails(getIdentifier("taskUnassigned").asTaskId())!!
  }

  protected val taskAssigned by lazy {
    repositories.findTaskWithDetails(getIdentifier("taskAssigned").asTaskId())!!
  }

  @BeforeEach
  fun initAbstractTaskServiceAuthorizationIntegrationTest() {
    // Only the csm users can add project crafts to a project
    eventStreamGenerator
        .setUserContext("userCsm")
        .setLastIdentifierForType(PROJECT.value, getByReference("project"))
        .submitProjectCraftG2()
        .setUserContext("userCreator")

    doWithAuthorization(userCreator) {
      eventStreamGenerator
          .submitTask("taskUnassigned") { it.status = TaskStatusEnumAvro.DRAFT }
          .submitTask("taskAssigned") {
            it.status = TaskStatusEnumAvro.DRAFT
            it.assignee = getByReference("participantFmAssignee")
          }
    }
  }
}
