/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.workarea.facade.rest

import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaId
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.AccessDeniedException

@EnableAllKafkaListeners
class WorkAreaSearchIntegrationTest : AbstractWorkAreaIntegrationTest() {

  @Autowired private lateinit var cut: WorkAreaController

  @Test
  fun `verify find with a non-existing work area fails`() {
    assertThatThrownBy { cut.findOneByIdentifier(WorkAreaId()) }
        .isInstanceOf(AccessDeniedException::class.java)

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify find with a non-existing project fails`() {
    assertThatThrownBy { cut.findAllByProjectIdentifier(ProjectId()) }
        .isInstanceOf(AccessDeniedException::class.java)

    projectEventStoreUtils.verifyEmpty()
  }
}
