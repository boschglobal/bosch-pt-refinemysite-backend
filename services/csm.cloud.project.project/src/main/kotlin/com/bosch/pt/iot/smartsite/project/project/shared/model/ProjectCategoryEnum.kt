/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.project.shared.model

import com.bosch.pt.iot.smartsite.common.i18n.Key.PROJECT_CATEGORY_ENUM_NB
import com.bosch.pt.iot.smartsite.common.i18n.Key.PROJECT_CATEGORY_ENUM_OB
import com.bosch.pt.iot.smartsite.common.i18n.Key.PROJECT_CATEGORY_ENUM_RB

/** Project categories. */
enum class ProjectCategoryEnum

/**
 * Constructor to initialize [ProjectCategoryEnum] with i18n key.
 *
 * @param i18nKey the key to initialize enumeration value with
 */
constructor(val i18nKey: String) {
  /** New Building. */
  NB(PROJECT_CATEGORY_ENUM_NB),

  /** Old Building. */
  OB(PROJECT_CATEGORY_ENUM_OB),

  /** Reconstruction Building. */
  RB(PROJECT_CATEGORY_ENUM_RB);

  companion object {
    /** Valid enum values for documentation. */
    const val ENUM_VALUES = "NB,OB,RB"
  }
}
