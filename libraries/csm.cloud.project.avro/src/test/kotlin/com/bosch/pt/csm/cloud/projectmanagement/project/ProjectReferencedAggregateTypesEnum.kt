/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project

enum class ProjectReferencedAggregateTypesEnum(val value: String) {
  @Deprecated("Can be removed with participant generation 1") EMPLOYEE("EMPLOYEE"),
  COMPANY("COMPANY"),
  USER("USER")
}
