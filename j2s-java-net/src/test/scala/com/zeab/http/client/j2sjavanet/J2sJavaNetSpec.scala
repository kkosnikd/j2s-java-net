package com.zeab.http.client.j2sjavanet

import com.zeab.http.seed.HttpSeed
import org.scalatest.FunSpec

class J2sJavaNetSpec extends FunSpec{

  describe("J2sJavaNet Http Client") {
    val response = HttpClient.Seed(HttpSeed("http://google.com")).toHttpResponse
    it("should return a http response") {
      assert(response.isRight)
    }
  }

}
