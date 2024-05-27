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
import com.bosch.pt.iot.smartsite.common.i18n.Key.WORK_AREA_VALIDATION_ERROR_USED_NAME
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaId
import com.bosch.pt.iot.smartsite.project.workarea.domain.asWorkAreaId
import com.bosch.pt.iot.smartsite.project.workarea.domain.asWorkAreaListId
import com.bosch.pt.iot.smartsite.project.workarea.facade.rest.resource.request.CreateWorkAreaResource
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.AccessDeniedException

@EnableAllKafkaListeners
class WorkAreaCreateIntegrationTest : AbstractWorkAreaIntegrationTest() {

  @Autowired private lateinit var cut: WorkAreaController

  private val project by lazy { repositories.findProject(getIdentifier("project").asProjectId())!! }
  private val projectTwo by lazy {
    repositories.findProject(getIdentifier("projectTwo").asProjectId())!!
  }
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
  fun `verify workArea is created in a correct position when not passing a position`() {
    val identifier = WorkAreaId()
    val workAreaListResource =
        cut.createWorkArea(
                identifier,
                CreateWorkAreaResource(project.identifier, "workAreaFour"),
                workAreaList.toEtag())
            .body!!

    assertThat(workAreaListResource).isNotNull
    assertThat(workAreaListResource.workAreas).isNotNull
    assertThat(workAreaListResource.workAreas.size).isEqualTo(4)
    assertThat(workAreaListResource.workAreas)
        .extracting<WorkAreaId> { it.id.asWorkAreaId() }
        .containsExactly(
            workArea.identifier, workAreaTwo.identifier, workAreaThree.identifier, identifier)
  }

  @Test
  fun `verify create with a non-existing project fails`() {
    assertThatThrownBy {
          cut.createWorkArea(
              WorkAreaId(),
              CreateWorkAreaResource(ProjectId(), "Some name", 1),
              ETag.from(workAreaList.version))
        }
        .isInstanceOf(AccessDeniedException::class.java)

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify create with a non-existing work area list fails`() {
    assertThatThrownBy {
          cut.createWorkArea(
              WorkAreaId(),
              CreateWorkAreaResource(projectTwo.identifier, "Some name", 1),
              ETag.from("0"))
        }
        .isInstanceOf(AccessDeniedException::class.java)

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify create with an existing work area name fails`() {
    assertThatThrownBy {
          cut.createWorkArea(
              WorkAreaId(),
              CreateWorkAreaResource(project.identifier, workAreaTwo.name, 1),
              ETag.from(workAreaList.version))
        }
        .usingRecursiveComparison()
        .withStrictTypeChecking()
        .isEqualTo(PreconditionViolationException(WORK_AREA_VALIDATION_ERROR_USED_NAME))

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify create with a work area with different capitalization fails`() {
    assertThatThrownBy {
          cut.createWorkArea(
              WorkAreaId(),
              CreateWorkAreaResource(project.identifier, workArea.name.uppercase(), 1),
              ETag.from(workAreaList.version))
        }
        .usingRecursiveComparison()
        .withStrictTypeChecking()
        .isEqualTo(PreconditionViolationException(WORK_AREA_VALIDATION_ERROR_USED_NAME))

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify create with wrong work area list etag fails`() {
    assertThatThrownBy {
          cut.createWorkArea(
              WorkAreaId(),
              CreateWorkAreaResource(project.identifier, "workAreaFour", 1),
              ETag.from("0"))
        }
        .isInstanceOf(EntityOutdatedException::class.java)
  }

  @Test
  fun `verify creation with position -1 fails`() {
    assertThatThrownBy {
          cut.createWorkArea(
              WorkAreaId(),
              CreateWorkAreaResource(project.identifier, "workAreaFour", -1),
              workAreaList.toEtag())
        }
        .usingRecursiveComparison()
        .withStrictTypeChecking()
        .isEqualTo(PreconditionViolationException(WORK_AREA_VALIDATION_ERROR_INVALID_POSITION))

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify creation with position too big fails`() {
    assertThatThrownBy {
          cut.createWorkArea(
              WorkAreaId(),
              CreateWorkAreaResource(project.identifier, "workAreaFour", 25),
              workAreaList.toEtag())
        }
        .usingRecursiveComparison()
        .withStrictTypeChecking()
        .isEqualTo(PreconditionViolationException(WORK_AREA_VALIDATION_ERROR_INVALID_POSITION))

    projectEventStoreUtils.verifyEmpty()
  }
}
