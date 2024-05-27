/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.company.model

import com.bosch.pt.iot.smartsite.common.i18n.Key.EMPLOYEE_ROLE_ENUM_CSM
import com.bosch.pt.iot.smartsite.common.i18n.Key.EMPLOYEE_ROLE_ENUM_FM
import com.bosch.pt.iot.smartsite.common.i18n.Key.EMPLOYEE_ROLE_ENUM_RP

enum class EmployeeRoleEnum(val i18nKey: String) {

  /** Company admin role. */
  CA(""),

  /** Company representative role. */
  CR(EMPLOYEE_ROLE_ENUM_RP),

  /** Construction site manager role. */
  CSM(EMPLOYEE_ROLE_ENUM_CSM),

  /** Foreman role. */
  FM(EMPLOYEE_ROLE_ENUM_FM)
}
