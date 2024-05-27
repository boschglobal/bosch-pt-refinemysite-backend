/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.featuretoggle.whitelist.command.api

import com.bosch.pt.csm.cloud.featuretogglemanagement.common.SubjectTypeEnum
import java.util.UUID

data class AddSubjectToWhitelistCommand(
    val featureName: String,
    val subjectRef: UUID,
    val type: SubjectTypeEnum,
)

data class DeleteSubjectFromWhitelistCommand(
    val featureName: String,
    val subjectRef: UUID,
)
