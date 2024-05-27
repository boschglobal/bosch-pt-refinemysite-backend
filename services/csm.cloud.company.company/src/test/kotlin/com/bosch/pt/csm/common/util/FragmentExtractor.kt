/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.common.util

import com.jayway.jsonpath.JsonPath
import io.mockk.every
import io.mockk.spyk
import org.mockito.Mockito
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.ResultMatcher

/** Performs matching against a JSON fragment as opposed to an entire JSON object. */
class FragmentExtractor
private constructor(private val fragmentPath: String, private val matcher: ResultMatcher) :
    ResultMatcher {

  override fun match(result: MvcResult) {
    val json = JsonPath.compile(fragmentPath).read<Any>(result.response.contentAsString)
    val response = Mockito.mock(MockHttpServletResponse::class.java)
    every { response.contentAsString } returns JsonPath.parse(json).jsonString()
    every { response.getContentAsString(any()) } returns JsonPath.parse(json).jsonString()

    val mvcResult = spyk(result)
    every { mvcResult.response } returns response
    matcher.match(mvcResult)
  }

  companion object {

    /**
     * Returns a matcher for a fragment of a JSON object.
     *
     * @param fragmentPath path to the fragment to extract
     * @param matcher which contains the expectations to be fulfilled by the extracted fragment
     * @return a matcher for a fragment of a JSON object.
     */
    fun extract(fragmentPath: String, matcher: ResultMatcher): ResultMatcher =
        FragmentExtractor(fragmentPath, matcher)
  }
}
