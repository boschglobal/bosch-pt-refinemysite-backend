/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.projectcraft.facade.rest

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftListEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftListEventEnumAvro
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractIntegrationTestV2
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.facade.rest.ProjectController
import com.bosch.pt.iot.smartsite.project.project.facade.rest.resource.request.SaveProjectResource
import com.bosch.pt.iot.smartsite.project.project.facade.rest.resource.response.ProjectAddressDto
import com.bosch.pt.iot.smartsite.project.project.shared.model.ProjectCategoryEnum.OB
import com.bosch.pt.iot.smartsite.project.projectcraft.util.ProjectCraftTestUtil.verifyCreatedAggregate
import java.time.LocalDate.now
import java.time.temporal.ChronoUnit
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@EnableAllKafkaListeners
class ProjectCraftListIntegrationTest : AbstractIntegrationTestV2() {

  @Autowired private lateinit var projectController: ProjectController

  private val testUser by lazy { repositories.findUser(getIdentifier("userCsm2"))!! }

  @BeforeEach
  fun init() {
    eventStreamGenerator.setupDatasetTestData()

    setAuthentication(getIdentifier("userCsm2"))
    projectEventStoreUtils.reset()
  }

  @Test
  fun `verify create project craft list succeeds when creating a project`() {
    val projectIdentifier = ProjectId()
    val newProjectResource =
        SaveProjectResource(
            client = "client",
            description = "description",
            start = now(),
            end = now().plus(1, ChronoUnit.DAYS),
            projectNumber = "projectNumber",
            title = "newCreatedProject",
            category = OB,
            address = ProjectAddressDto("city", "HN", "street", "ZC"))

    projectController.createProject(projectIdentifier, newProjectResource)

    projectEventStoreUtils
        .verifyContainsAndGet(
            ProjectCraftListEventAvro::class.java, ProjectCraftListEventEnumAvro.CREATED, 1, false)
        .also { verifyCreatedAggregate(it[0].aggregate, projectIdentifier, testUser) }
  }
}
