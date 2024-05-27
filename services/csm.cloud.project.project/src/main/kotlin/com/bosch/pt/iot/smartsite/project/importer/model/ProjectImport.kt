/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.importer.model

import com.bosch.pt.csm.cloud.common.api.LocalEntity
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import jakarta.persistence.AttributeOverride
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.Version
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(
    name = "project_import",
    indexes =
        [
            Index(
                name = "UK_ProjectImport_ProjectIdentifier",
                columnList = "projectIdentifier",
                unique = true),
            Index(name = "IX_ProjectImport_CreatedDate", columnList = "createdDate")])
class ProjectImport(
    @AttributeOverride(
        name = "identifier", column = Column(name = "projectIdentifier", nullable = false))
    val projectIdentifier: ProjectId,
    @Column(nullable = false) var blobName: String,
    @Column(nullable = false, columnDefinition = "varchar(255)")
    @Enumerated(EnumType.STRING)
    var status: ProjectImportStatus,
    @Column(nullable = false) val createdDate: LocalDateTime,
    var readWorkingAreasHierarchically: Boolean? = null,
    var craftColumn: String? = null,
    var craftColumnFieldType: String? = null,
    var workAreaColumn: String? = null,
    var workAreaColumnFieldType: String? = null,
    var jobId: UUID? = null
) : LocalEntity<Long>() {

  /** Version for optimistic locking. */
  @field:NotNull @Version @Column(nullable = false) var version: Long? = null
}
