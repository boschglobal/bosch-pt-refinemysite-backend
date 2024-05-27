/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.company.company.command.api

import com.bosch.pt.csm.company.company.CompanyId
import com.bosch.pt.csm.company.company.PostBoxAddressVo
import com.bosch.pt.csm.company.company.StreetAddressVo

@Suppress("UnnecessaryAbstractClass") abstract class CompanyCommand(open val identifier: CompanyId)

data class CreateCompanyCommand(
    val identifier: CompanyId? = null,
    val name: String,
    val streetAddress: StreetAddressVo? = null,
    val postBoxAddress: PostBoxAddressVo? = null
)

data class UpdateCompanyCommand(
    override val identifier: CompanyId,
    val version: Long,
    val name: String,
    val streetAddress: StreetAddressVo? = null,
    val postBoxAddress: PostBoxAddressVo? = null
) : CompanyCommand(identifier)

data class DeleteCompanyCommand(override val identifier: CompanyId) : CompanyCommand(identifier)
