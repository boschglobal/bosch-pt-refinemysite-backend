/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.featuretoggle.query

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.featuretogglemanagement.common.SubjectTypeEnum.COMPANY
import com.bosch.pt.csm.cloud.featuretogglemanagement.common.SubjectTypeEnum.PROJECT
import com.bosch.pt.csm.cloud.featuretogglemanagement.feature.event.addSubjectToWhitelistOfFeature
import com.bosch.pt.csm.cloud.featuretogglemanagement.feature.event.createFeature
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitTestAdminUserAndActivate
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractIntegrationTestV2
import com.bosch.pt.iot.smartsite.featuretoggle.query.FeatureEnum.PROJECT_IMPORT
import com.bosch.pt.iot.smartsite.project.daycard.facade.rest.submitInitProjectData
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.project.shared.model.Project
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.AccessDeniedException

@EnableAllKafkaListeners
class FeatureQueryServiceIntegrationTest : AbstractIntegrationTestV2() {

  @Autowired private lateinit var featureQueryService: FeatureQueryService

  @Test
  fun `verify feature toggle is active for company feature type`() {
    initBasicTestData()

    // Generate test specific data
    eventStreamGenerator
        // Delete project craft to have an empty project - required for feature toggle check
        .submitProjectCraftG2(eventType = DELETED)
        .createFeature("feature1") { it.featureName = PROJECT_IMPORT.name }
        .addSubjectToWhitelistOfFeature("feature1") {
          it.featureName = PROJECT_IMPORT.name
          it.subjectRef = getIdentifier("company").toString()
          it.type = COMPANY.name
        }

    // Check feature toggle
    val project = repositories.findProject(getIdentifier("project").asProjectId())
    assertThat(featureQueryService.isFeatureEnabled(PROJECT_IMPORT, project.getProjectId())).isTrue
  }

  @Test
  fun `verify feature toggle is inactive for company feature type`() {
    initBasicTestData()

    // Generate test specific data
    eventStreamGenerator
        // Delete project craft to have an empty project - required for feature toggle check
        .submitProjectCraftG2(eventType = DELETED)
        .createFeature("feature1") { it.featureName = PROJECT_IMPORT.name }

    // Check feature toggle
    val project = repositories.findProject(getIdentifier("project").asProjectId())
    assertThat(featureQueryService.isFeatureEnabled(PROJECT_IMPORT, project.getProjectId())).isFalse
  }

  @Test
  fun `verify feature toggle is active for project subject type `() {
    initBasicTestData()

    // Generate test specific data
    eventStreamGenerator
        // Delete project craft to have an empty project - required for feature toggle check
        .submitProjectCraftG2(eventType = DELETED)
        .createFeature("feature1") { it.featureName = PROJECT_IMPORT.name }
        .addSubjectToWhitelistOfFeature("feature1") {
          it.featureName = PROJECT_IMPORT.name
          it.subjectRef = getIdentifier("project").toString()
          it.type = PROJECT.name
        }

    // Check feature toggle
    val project = repositories.findProject(getIdentifier("project").asProjectId())
    assertThat(featureQueryService.isFeatureEnabled(PROJECT_IMPORT, project.getProjectId())).isTrue
  }

  @Test
  fun `verify feature toggle is inactive for project subject type `() {
    initBasicTestData()

    // Generate test specific data
    eventStreamGenerator
        // Delete project craft to have an empty project - required for feature toggle check
        .submitProjectCraftG2(eventType = DELETED)
        .createFeature("feature1") { it.featureName = PROJECT_IMPORT.name }

    // Check feature toggle
    val project = repositories.findProject(getIdentifier("project").asProjectId())
    assertThat(featureQueryService.isFeatureEnabled(PROJECT_IMPORT, project.getProjectId())).isFalse
  }

  @Test
  fun `verify feature toggle is inactive for users not assigned to a company`() {
    // Generate test specific data
    eventStreamGenerator
        .submitTestAdminUserAndActivate()
        .submitProject()
        .submitUser("userWithoutEmployee")

    setAuthentication("userWithoutEmployee")

    // Check feature toggle
    val project = repositories.findProject(getIdentifier("project").asProjectId())

    assertThatExceptionOfType(AccessDeniedException::class.java).isThrownBy {
      featureQueryService.isFeatureEnabled(PROJECT_IMPORT, project.getProjectId())
    }
  }

  @Test
  fun `verify feature toggle is inactive for unknown feature`() {
    initBasicTestData()

    // Check feature toggle
    val project = repositories.findProject(getIdentifier("project").asProjectId())
    assertThat(featureQueryService.isFeatureEnabled(PROJECT_IMPORT, project.getProjectId())).isFalse
  }

  private fun initBasicTestData() {
    eventStreamGenerator.submitInitProjectData()
    setAuthentication("userCsm1")
  }

  private fun Project?.getProjectId(): ProjectId = checkNotNull(this).identifier
}
