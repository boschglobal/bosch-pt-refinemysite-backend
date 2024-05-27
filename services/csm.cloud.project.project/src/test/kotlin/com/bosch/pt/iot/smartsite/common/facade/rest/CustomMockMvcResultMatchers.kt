/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.common.facade.rest

import com.bosch.pt.iot.smartsite.project.project.shared.model.Project
import com.bosch.pt.iot.smartsite.user.model.User
import java.util.Locale
import java.util.UUID
import org.springframework.context.MessageSource
import org.springframework.test.web.servlet.ResultMatcher
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath

object CustomMockMvcResultMatchers {

  fun errorMessageWithKey(key: String, messageSource: MessageSource): ResultMatcher =
      jsonPath("$.message").value(messageSource.getMessage(key, arrayOf(), Locale.ENGLISH))

  fun hasIdentifierAndVersion() = arrayOf(jsonPath("$.id").exists(), jsonPath("$.version").value(0))

  fun hasIdentifierAndVersion(index: Int) =
      arrayOf(
          jsonPath("$.items").exists(),
          jsonPath("$.items[$index].id").exists(),
          jsonPath("$.items[$index].version").value(0))

  fun hasIdentifierAndVersion(identifier: UUID? = null, version: Long = 0) =
      arrayOf(
          if (identifier != null) jsonPath("$.id").value(identifier.toString())
          else jsonPath("$.id").exists(),
          jsonPath("$.version").value(version))

  fun hasIdentifierAndVersion(identifier: UUID, version: Long = 0, index: Int) =
      arrayOf(
          jsonPath("$.items[$index].id").value(identifier.toString()),
          jsonPath("$.items[$index].version").value(version))

  fun isCreatedBy(user: User) =
      arrayOf(
          jsonPath("$.createdBy.id").value(user.identifier.toString()),
          jsonPath("$.createdBy.displayName").value(user.getDisplayName()),
          jsonPath("$.createdDate").exists())

  fun isCreatedBy(user: User, index: Int) =
      arrayOf(
          jsonPath("$.items[$index].createdBy.id").value(user.identifier.toString()),
          jsonPath("$.items[$index].createdBy.displayName").value(user.getDisplayName()),
          jsonPath("$.items[$index].createdDate").exists())

  fun isLastModifiedBy(user: User) =
      arrayOf(
          jsonPath("$.lastModifiedBy.id").value(user.identifier.toString()),
          jsonPath("$.lastModifiedBy.displayName").value(user.getDisplayName()),
          jsonPath("$.lastModifiedDate").exists())

  fun isLastModifiedBy(user: User, index: Int) =
      arrayOf(
          jsonPath("$.items[$index].lastModifiedBy.id").value(user.identifier.toString()),
          jsonPath("$.items[$index].lastModifiedBy.displayName").value(user.getDisplayName()),
          jsonPath("$.items[$index].lastModifiedDate").exists())

  fun hasReference(project: Project) =
      arrayOf(
          jsonPath("$.project.id").value(project.identifier.toString()),
          jsonPath("$.project.displayName").value(project.getDisplayName()))

  fun isPage(pageNumber: Int, pageSize: Int, totalPages: Int, totalElements: Int) =
      arrayOf(
          jsonPath("$.pageNumber").value(pageNumber),
          jsonPath("$.pageSize").value(pageSize),
          jsonPath("$.totalPages").value(totalPages),
          jsonPath("$.totalElements").value(totalElements),
          jsonPath("$._links.next.href").exists(),
          jsonPath("$._links.prev.href").exists())

  fun isSlice(pageNumber: Int, pageSize: Int) =
      arrayOf(
          jsonPath("$.pageNumber").value(pageNumber),
          jsonPath("$.pageSize").value(pageSize),
          jsonPath("$._links.next.href").exists(),
          jsonPath("$._links.prev.href").exists())
}
