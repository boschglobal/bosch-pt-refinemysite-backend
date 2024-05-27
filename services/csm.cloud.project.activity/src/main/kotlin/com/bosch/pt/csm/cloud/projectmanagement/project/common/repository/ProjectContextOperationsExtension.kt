/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.common.repository

import datadog.trace.api.Trace
import java.util.UUID

interface ProjectContextOperationsExtension<T> {

  @Trace fun findLatest(identifier: UUID, projectIdentifier: UUID): T?

  @Trace fun find(identifier: UUID, version: Long, projectIdentifier: UUID): T?

  @Trace fun delete(identifier: UUID, projectIdentifier: UUID)

  @Trace fun deleteByVersion(identifier: UUID, version: Long, projectIdentifier: UUID)
}
