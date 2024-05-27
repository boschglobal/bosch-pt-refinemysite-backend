/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.statistics.model

import com.bosch.pt.csm.cloud.common.api.AbstractPersistable
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(
    indexes =
        [
            Index(
                name = "IX_ParMap_ProjectRole", columnList = "project_identifier,participant_role"),
            Index(
                name = "IX_ParMap_ProjIdCompIdUserId",
                columnList = "project_identifier,user_identifier,company_identifier",
                unique = true)])
class ParticipantMapping(
    @Column(name = "participant_identifier") var participantIdentifier: UUID,
    @Column(name = "project_identifier") var projectIdentifier: UUID,
    @Column(name = "participant_role") var participantRole: String,
    @Column(name = "user_identifier") var userIdentifier: UUID,
    @Column(name = "company_identifier") var companyIdentifier: UUID,
    var active: Boolean = true
) : AbstractPersistable<Long>()
