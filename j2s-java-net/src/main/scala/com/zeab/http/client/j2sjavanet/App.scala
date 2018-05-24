package com.zeab.http.client.j2sjavanet

import com.zeab.http.seed.HttpSeed

object App extends App{

  println(HttpClient.Seed(HttpSeed("http://google.com")).toHttpResponse)

  println(HttpClient.Seed(HttpSeed("http://google.com")).retryHttpResponse(201, 1))



}
