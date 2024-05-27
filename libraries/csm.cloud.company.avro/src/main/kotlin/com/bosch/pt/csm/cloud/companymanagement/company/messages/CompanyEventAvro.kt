/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.companymanagement.company.messages

fun CompanyEventAvro.getIdentifier() = getAggregate().getIdentifier()

fun CompanyEventAvro.getVersion() = getAggregate().getVersion()

fun CompanyEventAvro.getCreatedDate() = getAggregate().getCreatedDate()

fun CompanyEventAvro.getCreatedByUserIdentifier() = getAggregate().getCreatedByUserIdentifier()

fun CompanyEventAvro.getLastModifiedDate() = getAggregate().getLastModifiedDate()

fun CompanyEventAvro.getLastModifiedByUserIdentifier() =
    getAggregate().getLastModifiedByUserIdentifier()

fun CompanyEventAvro.buildAggregateIdentifier() = getAggregate().buildAggregateIdentifier()
