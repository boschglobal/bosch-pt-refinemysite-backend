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
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.etag.from
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaId
import com.bosch.pt.iot.smartsite.project.workarea.domain.asWorkAreaId
import com.bosch.pt.iot.smartsite.project.workarea.facade.rest.resource.request.UpdateWorkAreaResource
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.AccessDeniedException

@EnableAllKafkaListeners
class WorkAreaUpdateIntegrationTest : AbstractWorkAreaIntegrationTest() {

  @Autowired private lateinit var cut: WorkAreaController

  private val workArea by lazy {
    repositories.findWorkArea(getIdentifier("workArea").asWorkAreaId())!!
  }
  private val workAreaTwo by lazy {
    repositories.findWorkArea(getIdentifier("workAreaTwo").asWorkAreaId())!!
  }

  @Test
  fun `verify update of a non-existing work area fails`() {
    assertThatThrownBy {
          cut.updateWorkArea(
              WorkAreaId(), UpdateWorkAreaResource("Some name"), ETag.from(workArea.version))
        }
        .isInstanceOf(AccessDeniedException::class.java)

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify update with wrong work area etag fails`() {
    assertThatThrownBy {
          cut.updateWorkArea(
              workArea.identifier, UpdateWorkAreaResource("Some name"), ETag.from("10"))
        }
        .isInstanceOf(EntityOutdatedException::class.java)

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify update project craft name with used name fails`() {
    assertThatThrownBy {
          cut.updateWorkArea(
              workArea.identifier,
              UpdateWorkAreaResource(workAreaTwo.name),
              ETag.from(workArea.version))
        }
        .isInstanceOf(PreconditionViolationException::class.java)
        .withFailMessage("Working area is already in use")

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify update project craft name different capitalization succeeds`() {

    cut.updateWorkArea(
        workArea.identifier,
        UpdateWorkAreaResource(workArea.name.lowercase()),
        ETag.from(workArea.version))

    assertThat(workArea.name).isEqualTo(workArea.name.lowercase())
  }
}
