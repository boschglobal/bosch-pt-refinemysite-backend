/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.common.repository

import org.springframework.data.domain.Sort
import org.springframework.data.domain.Sort.Direction.ASC
import org.springframework.data.domain.Sort.Direction.DESC

/** Class containing constant expressions related to repositories. */
object RepositoryConstants {

  /** Default company sorting. */
  val DEFAULT_COMPANY_SORTING = Sort.by(ASC, "name")

  /** Default attachment sorting. */
  val DEFAULT_ATTACHMENT_SORTING =
      Sort.by(
          listOf(
              Sort.Order(DESC, "createdDate"),
              Sort.Order(ASC, "fileName"),
              Sort.Order(ASC, "identifier")))

  /** Default bulk attachment sorting. */
  val DEFAULT_BULK_ATTACHMENT_SORTING =
      Sort.by(
          listOf(
              Sort.Order(DESC, "task.identifier"),
              Sort.Order(DESC, "createdDate"),
              Sort.Order(ASC, "fileName"),
              Sort.Order(ASC, "identifier")))
}
