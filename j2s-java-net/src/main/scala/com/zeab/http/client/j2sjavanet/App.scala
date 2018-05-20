package com.zeab.http.client.j2sjavanet

import com.zeab.http.seed.HttpSeedMetaData
import com.zeab.http.seed.builder.Seed
import com.zeab.http.urlstring.builder.UrlStringBuilder

object App extends App{


  val data = Some(HttpSeedMetaData.bearerMeta("xoxb-318035090016-a2TraU0oXlusie7eYYGzNYyE"))

  val kk =    Seed
    .addUrl(
      UrlStringBuilder
        .https("slack.com")
        .hostParameter("api")
        .hostParameter("rtm.connect").toString)
    .addMetaData(data)
    .addMetaDataCommonName("Get-SlackRTMConnection")
    .toHttpSeed

  val gff = Seed.addUrl("https://slack.com/api/rtm.connect").addMetaData(data).toHttpSeed

  val hg = HttpClient.Seed(kk).toHttpResponse

  println(hg)
}

//https://slack.com/api/rtm.connect
//https://slack.com/api/rtm.connect