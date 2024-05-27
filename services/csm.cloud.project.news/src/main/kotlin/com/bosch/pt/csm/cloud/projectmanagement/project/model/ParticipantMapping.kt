/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.projectmanagement.project.model

import java.util.UUID
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import com.bosch.pt.csm.cloud.common.api.AbstractPersistable

@Entity
@Table(
    indexes =
        [
            Index(
                name = "IX_ParMap_ProjectRoleCompany",
                columnList = "project_identifier,participant_role,company_identifier"),
            Index(
                name = "IX_ParMap_ProjIdenUserIdent",
                columnList = "project_identifier,user_identifier",
                unique = true)])
class ParticipantMapping(
    @Column(name = "PROJECT_IDENTIFIER") var projectIdentifier: UUID,
    @Column(name = "COMPANY_IDENTIFIER") var companyIdentifier: UUID,
    @Column(name = "PARTICIPANT_ROLE") var participantRole: String,
    @Column(name = "USER_IDENTIFIER") var userIdentifier: UUID
) : AbstractPersistable<Long>()
