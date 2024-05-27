/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.companymanagement.employee.messages

fun EmployeeEventAvro.getCompanyIdentifier() = getAggregate().getCompanyIdentifier()

fun EmployeeEventAvro.getUserIdentifier() = getAggregate().getUserIdentifier()

fun EmployeeEventAvro.getIdentifier() = getAggregate().getIdentifier()

fun EmployeeEventAvro.getVersion() = getAggregate().getVersion()

fun EmployeeEventAvro.getCreatedDate() = getAggregate().getCreatedDate()

fun EmployeeEventAvro.getCreatedByUserIdentifier() = getAggregate().getCreatedByUserIdentifier()

fun EmployeeEventAvro.getLastModifiedDate() = getAggregate().getLastModifiedDate()

fun EmployeeEventAvro.getLastModifiedByUserIdentifier() =
    getAggregate().getLastModifiedByUserIdentifier()

fun EmployeeEventAvro.buildAggregateIdentifier() = getAggregate().buildAggregateIdentifier()
