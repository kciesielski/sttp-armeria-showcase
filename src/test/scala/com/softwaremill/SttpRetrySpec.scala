package com.softwaremill

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.effect.unsafe.IORuntime
import org.mockserver.client.MockServerClient
import org.mockserver.integration.ClientAndServer
import org.mockserver.integration.ClientAndServer.startClientAndServer
import org.mockserver.matchers.Times
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.HttpResponse.response
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.SpanSugar.convertIntToGrainOfTime
import org.scalatest.{Assertion, BeforeAndAfterAll}
import sttp.client3.armeria.cats.ArmeriaCatsBackend
import sttp.client3.httpclient.fs2.HttpClientFs2Backend
import sttp.client3.{Response, SttpBackend, SttpBackendOptions, UriContext}
import sttp.model.StatusCode

import scala.concurrent.duration.FiniteDuration


class SttpRetrySpec extends AsyncFlatSpec with AsyncIOSpec with Matchers with BeforeAndAfterAll {

  val port = 8080
  val host = "localhost"
  var mockServer: ClientAndServer = null


  override protected def beforeAll(): Unit = {
    super.beforeAll()
    mockServer = startClientAndServer(port)
  }

  override protected def afterAll(): Unit = {
    mockServer.stop()
    super.afterAll()
  }

  def newBackend(): SttpBackend[IO, Any] =
    ArmeriaCatsBackend.usingDefaultClient()

  def newBackend2(): SttpBackend[IO, Any] = // Switch newBackend2 to newBackend and check how Armeria client behaves in this case
    HttpClientFs2Backend
      .resource[IO]()
      .allocated
      .unsafeRunSync()(IORuntime.global)
      ._1

  implicit class RetryTask[A](task: IO[A]) {
    def retry(
               delay: FiniteDuration = 100.millis,
               retriesLeft: Int = 5
             ): IO[A] =
      task
        .onError(e => IO.delay(println(s"Received error: $e, retries left = $retriesLeft")))
        .handleErrorWith { error =>
        if (retriesLeft == 3)
            retry(delay, retriesLeft - 1)
        else if (retriesLeft > 0)
          IO.sleep(delay) *> retry(delay, retriesLeft - 1)
        else
          IO.raiseError[A](error)
      }
  }

  def setRespondWithCode(code: Int, howManyTimes: Int ): Unit = {
      new MockServerClient(host, port)
        .when(
          request()
            .withPath("/api-test")
            .withMethod("GET"),
          Times.exactly(howManyTimes)
        )
        .respond(
          response()
            .withStatusCode(code)
        )
  }


  it should "call the HTTP server twice" in {
    // given
    setRespondWithCode(401, howManyTimes = 2)
    setRespondWithCode(400, howManyTimes = 1)
    setRespondWithCode(200, howManyTimes = 1)
    implicit val sttpBackend = newBackend()

    // when
    val result: IO[Response[Either[String, String]]] =
      sttp.client3.basicRequest
      .get(uri"http://$host:$port/api-test")
      .send()

    // then
    val check: IO[Assertion] = result.map {
      response =>
        response.code shouldBe StatusCode.Ok
    }

    check.retry()
  }
}
