/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.featuretoggle.whitelist.facade.rest.resource.request

import com.bosch.pt.csm.cloud.featuretogglemanagement.common.SubjectTypeEnum

data class CreateWhitelistedSubjectResource(val type: SubjectTypeEnum)
