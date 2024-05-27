/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.exporter.facade.job.submitter

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.featuretogglemanagement.common.SubjectTypeEnum.COMPANY
import com.bosch.pt.csm.cloud.featuretogglemanagement.feature.event.addSubjectToWhitelistOfFeature
import com.bosch.pt.csm.cloud.featuretogglemanagement.feature.event.createFeature
import com.bosch.pt.iot.smartsite.common.authorization.AbstractAuthorizationIntegrationTestV2
import com.bosch.pt.iot.smartsite.common.facade.rest.UserTypeAccess
import com.bosch.pt.iot.smartsite.featuretoggle.query.FeatureEnum.PROJECT_EXPORT
import com.bosch.pt.iot.smartsite.project.exporter.api.ProjectExportFormatEnum.MS_PROJECT_XML
import com.bosch.pt.iot.smartsite.project.exporter.api.ProjectExportParameters
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

class ProjectExportJobSubmitterAuthorizationIntegrationTest :
    AbstractAuthorizationIntegrationTestV2() {

  @Autowired private lateinit var cut: ProjectExportJobSubmitter

  @BeforeEach
  fun init() {
    eventStreamGenerator
        .createFeature(asReference = "projectExport") { it.featureName = PROJECT_EXPORT.name }
        .addSubjectToWhitelistOfFeature(asReference = "projectExport") {
          it.type = COMPANY.name
          it.subjectRef = getIdentifier("company").toString()
          it.featureName = PROJECT_EXPORT.name
        }
        .addSubjectToWhitelistOfFeature(asReference = "projectExport") {
          it.type = COMPANY.name
          it.subjectRef = getIdentifier("otherCompany").toString()
          it.featureName = PROJECT_EXPORT.name
        }
  }

  @ParameterizedTest
  @MethodSource("allActiveParticipantsWithAccess")
  fun `verify export authorized`(userType: UserTypeAccess) {

    val exportParameters =
        ProjectExportParameters(MS_PROJECT_XML, includeMilestones = false, includeComments = false)

    checkAccessWith(userType) { cut.enqueueExportJob(getIdentifier("project"), exportParameters) }
  }
}
