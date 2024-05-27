/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.statistics.model

data class RfvDto(
    val key: DayCardReasonVarianceEnum,
    val active: Boolean,
    val name: String? = null
)
