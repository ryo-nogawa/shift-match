package com.example.shiftmatch.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("HttpHolidayCsvFetcher")
class HttpHolidayCsvFetcherTest {

  private HttpServer httpServer;
  private int serverPort;

  @BeforeEach
  void startServer() throws IOException {
    httpServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
    serverPort = httpServer.getAddress().getPort();
    httpServer.start();
  }

  void stopServer() {
    if (httpServer != null) {
      httpServer.stop(0);
    }
  }

  @Nested
  @DisplayName("[F-10] CSV を取得する")
  class FetchCsv {

    @Test
    @DisplayName(
        "[F-10] Given: サーバーが 200 OK で Shift_JIS の CSV を返すとき, When: fetch すると, Then: バイト列がそのまま返される")
    void fetchesSuccessfullyWithStatus200() throws IOException {
      String csv = "国民の祝日・休日月日,国民の祝日・休日名称\n2026/1/1,元日\n";
      byte[] csvBytes = csv.getBytes(Charset.forName("Shift_JIS"));

      httpServer.createContext(
          "/test.csv",
          exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "text/csv");
            exchange.sendResponseHeaders(200, csvBytes.length);
            exchange.getResponseBody().write(csvBytes);
            exchange.close();
          });

      // テスト用フェッチャーを作成
      HttpHolidayCsvFetcher testFetcher =
          new HttpHolidayCsvFetcher("http://localhost:" + serverPort + "/test.csv", 10);

      byte[] result = testFetcher.fetch();

      assertArrayEquals(csvBytes, result);
      stopServer();
    }

    @Test
    @DisplayName("[F-10] Given: サーバーが 500 を返すとき, When: fetch すると, Then: HolidayFetchException を投げる")
    void throwsWhenStatus500() {
      httpServer.createContext(
          "/error",
          exchange -> {
            exchange.sendResponseHeaders(500, 0);
            exchange.close();
          });

      HttpHolidayCsvFetcher testFetcher =
          new HttpHolidayCsvFetcher("http://localhost:" + serverPort + "/error", 10);

      assertThrows(HolidayFetchException.class, () -> testFetcher.fetch());
      stopServer();
    }

    @Test
    @DisplayName("[F-10] Given: サーバーが起動していないとき, When: fetch すると, Then: HolidayFetchException を投げる")
    void throwsWhenConnectionFails() {
      stopServer();

      HttpHolidayCsvFetcher testFetcher =
          new HttpHolidayCsvFetcher("http://localhost:" + serverPort + "/notexist", 10);

      assertThrows(HolidayFetchException.class, () -> testFetcher.fetch());
    }
  }
}
