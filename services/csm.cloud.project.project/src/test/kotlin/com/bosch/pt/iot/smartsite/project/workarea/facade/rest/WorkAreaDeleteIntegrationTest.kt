/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.workarea.facade.rest

import com.bosch.pt.csm.cloud.common.command.exceptions.EntityOutdatedException
import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.common.facade.rest.etag.ETag
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMilestone
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkArea
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaEventEnumAvro
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.etag.from
import com.bosch.pt.iot.smartsite.common.i18n.Key.WORK_AREA_VALIDATION_ERROR_WORK_AREA_IN_USE
import com.bosch.pt.iot.smartsite.common.i18n.Key.WORK_AREA_VALIDATION_ERROR_WORK_AREA_IS_PARENT
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaId
import com.bosch.pt.iot.smartsite.project.workarea.domain.asWorkAreaId
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.AccessDeniedException

@EnableAllKafkaListeners
class WorkAreaDeleteIntegrationTest : AbstractWorkAreaIntegrationTest() {

  @Autowired private lateinit var cut: WorkAreaController

  private val workArea by lazy {
    repositories.findWorkArea(getIdentifier("workArea").asWorkAreaId())!!
  }
  private val workAreaTwo by lazy {
    repositories.findWorkArea(getIdentifier("workAreaTwo").asWorkAreaId())!!
  }

  @Test
  fun `verify delete with a non-existing work area fails`() {
    assertThatThrownBy { cut.deleteWorkArea(WorkAreaId(), ETag.from(workArea.version)) }
        .isInstanceOf(AccessDeniedException::class.java)

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify delete with wrong etag fails`() {
    assertThatThrownBy { cut.deleteWorkArea(workArea.identifier, ETag.from("25")) }
        .isInstanceOf(EntityOutdatedException::class.java)

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify delete work area which is parent of another work area fails`() {
    eventStreamGenerator.submitWorkArea(
        "workAreaThree", eventType = WorkAreaEventEnumAvro.UPDATED) {
          it.parent = getIdentifier("workAreaTwo").toString()
        }

    projectEventStoreUtils.reset()

    assertThatThrownBy {
          cut.deleteWorkArea(workAreaTwo.identifier, ETag.from(workAreaTwo.version))
        }
        .usingRecursiveComparison()
        .withStrictTypeChecking()
        .isEqualTo(PreconditionViolationException(WORK_AREA_VALIDATION_ERROR_WORK_AREA_IS_PARENT))

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify delete work area in use for a task fails`() {
    eventStreamGenerator.submitTask("anotherTask") { it.workarea = getByReference("workAreaTwo") }

    projectEventStoreUtils.reset()

    assertThatThrownBy {
          cut.deleteWorkArea(workAreaTwo.identifier, ETag.from(workAreaTwo.version))
        }
        .usingRecursiveComparison()
        .withStrictTypeChecking()
        .isEqualTo(PreconditionViolationException(WORK_AREA_VALIDATION_ERROR_WORK_AREA_IN_USE))

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify delete work area in use for a milestone fails`() {
    eventStreamGenerator.submitMilestone("anotherMilestone") {
      it.workarea = getByReference("workAreaTwo")
    }

    projectEventStoreUtils.reset()

    assertThatThrownBy {
          cut.deleteWorkArea(workAreaTwo.identifier, ETag.from(workAreaTwo.version))
        }
        .usingRecursiveComparison()
        .withStrictTypeChecking()
        .isEqualTo(PreconditionViolationException(WORK_AREA_VALIDATION_ERROR_WORK_AREA_IN_USE))

    projectEventStoreUtils.verifyEmpty()
  }
}
