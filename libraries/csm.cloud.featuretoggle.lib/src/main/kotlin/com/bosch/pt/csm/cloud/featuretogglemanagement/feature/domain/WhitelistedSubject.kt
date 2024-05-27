/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.featuretogglemanagement.feature.domain

import com.bosch.pt.csm.cloud.featuretogglemanagement.common.SubjectTypeEnum

data class WhitelistedSubject(var subjectRef: String, var subjectType: SubjectTypeEnum)
