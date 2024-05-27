/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.common.repository

object ArrayQueryUtils {

  /**
   * To convert a list/array to an array parameter in a prepared statement, a database connection
   * and database-system ( e.g. mysql) specific data-types have to be used/specified.
   *
   * Since this doesn't work with different databases (mysql, h2, etc.), a simpler but similar
   * approach is it, to generate the query string with the same amount of question marks as the
   * amount of elements in the list.
   *
   * This helper method takes a query string with a "%s" placeholder to be replaced. The collection
   * is taken to determine the amount of question marks.
   *
   * The query can be used in a prepared statement. Values can be set in the prepared statement
   * callback by calling (idx is 1 based):
   *
   * ps.setString(idx + 1, String.valueOf(collection.get(i))).
   *
   * @param query the query with a "%s" placeholder
   * @param collection the collection to use to get the amount of question marks
   * @return the query with question marks
   */
  fun createArrayQuery(query: String, collection: Collection<*>): String {
    require(query.contains("%s")) { "Query requires a placeholder to be replaced" }

    // Add question marks
    val parameters = StringBuilder()
    for (i in collection.indices) {
      parameters.append("?")
      if (i < collection.size - 1) {
        parameters.append(",")
      }
    }

    return String.format(query, parameters)
  }
}
