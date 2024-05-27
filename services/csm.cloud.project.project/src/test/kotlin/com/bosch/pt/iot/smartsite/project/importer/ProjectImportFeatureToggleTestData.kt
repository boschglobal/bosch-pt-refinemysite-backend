/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.importer

import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator
import com.bosch.pt.csm.cloud.companymanagement.common.CompanymanagementAggregateTypeEnum.COMPANY
import com.bosch.pt.csm.cloud.featuretogglemanagement.common.SubjectTypeEnum
import com.bosch.pt.csm.cloud.featuretogglemanagement.feature.event.addSubjectToWhitelistOfFeature
import com.bosch.pt.csm.cloud.featuretogglemanagement.feature.event.createFeature
import com.bosch.pt.iot.smartsite.featuretoggle.query.FeatureEnum.PROJECT_IMPORT
import java.util.UUID

fun EventStreamGenerator.submitProjectImportFeatureToggle(
    companyId: UUID =
        requireNotNull(getContext().lastIdentifierPerType[COMPANY.value]).identifier.toUUID()
) =
    createFeature(asReference = "projectImport") { it.featureName = PROJECT_IMPORT.name }
        .addSubjectToWhitelistOfFeature(asReference = "projectImport") {
          it.featureName = PROJECT_IMPORT.name
          it.subjectRef = companyId.toString()
          it.type = SubjectTypeEnum.COMPANY.name
        }
        .run { this }
