/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest

import com.bosch.pt.csm.cloud.common.facade.rest.CustomLinkBuilderFactory
import com.bosch.pt.csm.cloud.common.util.HttpTestUtils.setFakeUrlWithApiVersion
import com.bosch.pt.iot.smartsite.application.SmartSiteMockKTest
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.authorization.ProjectAuthorizationComponent
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response.TaskListResource
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response.factory.TaskListResourceFactory
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response.factory.TaskResourceFactoryHelper
import com.bosch.pt.iot.smartsite.project.task.shared.model.Task
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

@SmartSiteMockKTest
class TaskListResourceFactoryTest {

  @Suppress("Unused", "UnusedPrivateMember")
  @RelaxedMockK
  private lateinit var taskListResource: TaskListResource

  @Suppress("Unused", "UnusedPrivateMember") @RelaxedMockK private lateinit var tasks: Page<Task>

  @Suppress("Unused", "UnusedPrivateMember") @MockK private lateinit var pageable: Pageable

  @Suppress("Unused", "UnusedPrivateMember")
  @RelaxedMockK
  private lateinit var taskResourceFactoryHelper: TaskResourceFactoryHelper

  @Suppress("Unused", "UnusedPrivateMember")
  @RelaxedMockK
  private lateinit var linkFactory: CustomLinkBuilderFactory

  @Suppress("Unused", "UnusedPrivateMember")
  @MockK
  private lateinit var projectAuthorizationComponent: ProjectAuthorizationComponent

  @InjectMockKs lateinit var cut: TaskListResourceFactory

  @BeforeEach
  fun mockApiVersioning() {
    setFakeUrlWithApiVersion()
  }

  @Test
  fun `task list without project id`() {
    val resource = cut.build(tasks, pageable, null)
    assertThat(resource.links).hasSize(0)
  }

  @Test
  fun `task list with project id but no permissions`() {

    every { projectAuthorizationComponent.hasCreateTaskPermissionOnProject(any()) }.returns(false)
    every { projectAuthorizationComponent.hasAssignPermissionOnProject(any()) }.returns(false)
    every { projectAuthorizationComponent.hasOpenPermissionOnProject(any()) }.returns(false)

    val resource = cut.build(tasks, pageable, ProjectId())
    assertThat(resource.links).hasSize(0)
  }
}
