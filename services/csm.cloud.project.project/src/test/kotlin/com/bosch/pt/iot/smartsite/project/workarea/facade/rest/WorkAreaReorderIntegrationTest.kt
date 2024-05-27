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
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.etag.toEtag
import com.bosch.pt.iot.smartsite.common.i18n.Key.WORK_AREA_VALIDATION_ERROR_INVALID_POSITION
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaId
import com.bosch.pt.iot.smartsite.project.workarea.domain.asWorkAreaId
import com.bosch.pt.iot.smartsite.project.workarea.domain.asWorkAreaListId
import com.bosch.pt.iot.smartsite.project.workarea.facade.rest.resource.request.UpdateWorkAreaListResource
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.AccessDeniedException

@EnableAllKafkaListeners
class WorkAreaReorderIntegrationTest : AbstractWorkAreaIntegrationTest() {

  @Autowired private lateinit var cut: WorkAreaController

  private val workArea by lazy {
    repositories.findWorkArea(getIdentifier("workArea").asWorkAreaId())!!
  }
  private val workAreaTwo by lazy {
    repositories.findWorkArea(getIdentifier("workAreaTwo").asWorkAreaId())!!
  }
  private val workAreaThree by lazy {
    repositories.findWorkArea(getIdentifier("workAreaThree").asWorkAreaId())!!
  }
  private val workAreaList by lazy {
    repositories.findWorkAreaList(getIdentifier("workAreaList").asWorkAreaListId())!!
  }

  @Test
  fun `verify reorder workArea position`() {
    val workAreaListResource =
        cut.updateWorkAreaList(
                UpdateWorkAreaListResource(workArea.identifier, 2), workAreaList.toEtag())
            .body!!

    assertThat(workAreaListResource).isNotNull
    assertThat(workAreaListResource.workAreas).isNotNull
    assertThat(workAreaListResource.workAreas.size).isEqualTo(3)
    assertThat(workAreaListResource.workAreas)
        .extracting<WorkAreaId> { it.id.asWorkAreaId() }
        .containsExactly(workAreaTwo.identifier, workArea.identifier, workAreaThree.identifier)
  }

  @Test
  fun `verify reorder workArea position when set from top to bottom`() {
    val workAreaListResource =
        cut.updateWorkAreaList(
                UpdateWorkAreaListResource(workArea.identifier, 3), workAreaList.toEtag())
            .body!!

    assertThat(workAreaListResource).isNotNull
    assertThat(workAreaListResource.workAreas).isNotNull
    assertThat(workAreaListResource.workAreas.size).isEqualTo(3)
    assertThat(workAreaListResource.workAreas)
        .extracting<WorkAreaId> { it.id.asWorkAreaId() }
        .containsExactly(workAreaTwo.identifier, workAreaThree.identifier, workArea.identifier)
  }

  @Test
  fun `verify reorder workArea position when set from bottom to top`() {
    val workAreaListResource =
        cut.updateWorkAreaList(
                UpdateWorkAreaListResource(workAreaThree.identifier, 1), workAreaList.toEtag())
            .body!!

    assertThat(workAreaListResource).isNotNull
    assertThat(workAreaListResource.workAreas).isNotNull
    assertThat(workAreaListResource.workAreas.size).isEqualTo(3)
    assertThat(workAreaListResource.workAreas)
        .extracting<WorkAreaId> { it.id.asWorkAreaId() }
        .containsExactly(workAreaThree.identifier, workArea.identifier, workAreaTwo.identifier)
  }

  @Test
  fun `verify reorder of a non-existing work area fails`() {
    assertThatThrownBy {
          cut.updateWorkAreaList(
              UpdateWorkAreaListResource(WorkAreaId(), 1), ETag.from(workAreaList.version))
        }
        .isInstanceOf(AccessDeniedException::class.java)

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify reorder with wrong work area list etag fails`() {
    assertThatThrownBy {
          cut.updateWorkAreaList(
              UpdateWorkAreaListResource(workArea.identifier, 3), ETag.from("25"))
        }
        .isInstanceOf(EntityOutdatedException::class.java)

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify reorder with position -1 fails`() {
    assertThatThrownBy {
          cut.updateWorkAreaList(
              UpdateWorkAreaListResource(workAreaThree.identifier, -1), workAreaList.toEtag())
        }
        .usingRecursiveComparison()
        .withStrictTypeChecking()
        .isEqualTo(PreconditionViolationException(WORK_AREA_VALIDATION_ERROR_INVALID_POSITION))

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify reorder with position too big fails`() {
    assertThatThrownBy {
          cut.updateWorkAreaList(
              UpdateWorkAreaListResource(workArea.identifier, 25), workAreaList.toEtag())
        }
        .usingRecursiveComparison()
        .withStrictTypeChecking()
        .isEqualTo(PreconditionViolationException(WORK_AREA_VALIDATION_ERROR_INVALID_POSITION))

    projectEventStoreUtils.verifyEmpty()
  }
}
