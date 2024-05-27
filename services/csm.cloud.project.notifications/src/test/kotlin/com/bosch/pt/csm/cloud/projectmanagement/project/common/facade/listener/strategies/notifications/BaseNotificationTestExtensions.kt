/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator
import com.bosch.pt.csm.cloud.companymanagement.common.CompanymanagementAggregateTypeEnum
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.BaseNotificationTest.Companion.COMPANY
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.BaseNotificationTest.Companion.CR_PARTICIPANT
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.BaseNotificationTest.Companion.CR_USER
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.BaseNotificationTest.Companion.CSM_PARTICIPANT
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.BaseNotificationTest.Companion.CSM_USER
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.BaseNotificationTest.Companion.FM_PARTICIPANT
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.BaseNotificationTest.Companion.FM_USER
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.BaseNotificationTest.Companion.OTHER_CR_PARTICIPANT
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.BaseNotificationTest.Companion.OTHER_CR_USER
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.BaseNotificationTest.Companion.OTHER_CSM_PARTICIPANT
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.BaseNotificationTest.Companion.OTHER_CSM_USER
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskStatusEnumAvro

fun EventStreamGenerator.submitCsmParticipant(): EventStreamGenerator {
  setLastIdentifierForType(
      CompanymanagementAggregateTypeEnum.COMPANY.value, getByReference(COMPANY))
  submitParticipantG3(asReference = CSM_PARTICIPANT) {
    it.user = getByReference(CSM_USER)
    it.role = ParticipantRoleEnumAvro.CSM
  }
  return this
}

fun EventStreamGenerator.submitOtherCsmParticipant(): EventStreamGenerator {
  setLastIdentifierForType(
      CompanymanagementAggregateTypeEnum.COMPANY.value, getByReference(COMPANY))
  submitParticipantG3(asReference = OTHER_CSM_PARTICIPANT) {
    it.user = getByReference(OTHER_CSM_USER)
    it.role = ParticipantRoleEnumAvro.CSM
  }
  return this
}

fun EventStreamGenerator.submitCrParticipant(): EventStreamGenerator {
  setLastIdentifierForType(
      CompanymanagementAggregateTypeEnum.COMPANY.value, getByReference(COMPANY))
  submitParticipantG3(asReference = CR_PARTICIPANT) {
    it.user = getByReference(CR_USER)
    it.role = ParticipantRoleEnumAvro.CR
  }
  return this
}

fun EventStreamGenerator.submitOtherCrParticipant(): EventStreamGenerator {
  setLastIdentifierForType(
      CompanymanagementAggregateTypeEnum.COMPANY.value, getByReference(COMPANY))
  submitParticipantG3(asReference = OTHER_CR_PARTICIPANT) {
    it.user = getByReference(OTHER_CR_USER)
    it.role = ParticipantRoleEnumAvro.CR
  }
  return this
}

fun EventStreamGenerator.submitFmParticipant(): EventStreamGenerator {
  setLastIdentifierForType(
      CompanymanagementAggregateTypeEnum.COMPANY.value, getByReference(COMPANY))
  submitParticipantG3(asReference = FM_PARTICIPANT) {
    it.user = getByReference(FM_USER)
    it.role = ParticipantRoleEnumAvro.FM
  }
  return this
}

fun EventStreamGenerator.submitTaskAsFm(): EventStreamGenerator {
  submitTask(auditUserReference = FM_USER) {
    it.assignee = getByReference(FM_PARTICIPANT)
    it.status = TaskStatusEnumAvro.OPEN
  }
  return this
}

fun EventStreamGenerator.submitTaskAsCr(): EventStreamGenerator {
  submitTask(auditUserReference = CR_USER) {
    it.assignee = getByReference(CR_PARTICIPANT)
    it.status = TaskStatusEnumAvro.OPEN
  }
  return this
}

fun EventStreamGenerator.submitTaskAsCsm(): EventStreamGenerator {
  submitTask(auditUserReference = CSM_USER) {
    it.assignee = getByReference(CSM_PARTICIPANT)
    it.status = TaskStatusEnumAvro.OPEN
  }
  return this
}
