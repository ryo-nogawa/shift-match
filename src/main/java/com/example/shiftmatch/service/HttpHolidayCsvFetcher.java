package com.example.shiftmatch.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * HTTP で祝日 CSV を取得するコンポーネント。
 *
 * <p>JDK 標準の {@link HttpClient} を使用して、指定された URL から CSV をダウンロードします。
 */
@Component
public class HttpHolidayCsvFetcher implements HolidayCsvFetcher {

  private final String url;
  private final int timeoutSeconds;

  /**
   * CSV の URL とタイムアウト時間を注入してインスタンスを生成します。
   *
   * @param url CSV の URL
   * @param timeoutSeconds タイムアウト時間（秒）
   */
  public HttpHolidayCsvFetcher(
      @Value("${holiday.csv.url}") String url,
      @Value("${holiday.csv.timeout-seconds}") int timeoutSeconds) {
    this.url = url;
    this.timeoutSeconds = timeoutSeconds;
  }

  /**
   * 祝日 CSV をダウンロードします。
   *
   * <p>HTTP 200 のとき、レスポンスボディをバイト列で返します。200 以外のステータス、
   * 接続エラー、タイムアウトの場合は {@link HolidayFetchException} を投げます。
   *
   * @return Shift_JIS エンコードの CSV バイト列
   * @throws HolidayFetchException 取得に失敗した場合
   */
  @Override
  public byte[] fetch() {
    try {
      HttpClient client = HttpClient.newHttpClient();
      HttpRequest request =
          HttpRequest.newBuilder(URI.create(url))
              .timeout(Duration.ofSeconds(timeoutSeconds))
              .GET()
              .build();

      HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

      if (response.statusCode() != 200) {
        throw new HolidayFetchException("HTTP " + response.statusCode() + " が返されました");
      }

      return response.body();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new HolidayFetchException("CSV 取得がインタラプトされました", e);
    } catch (HolidayFetchException e) {
      throw e;
    } catch (Exception e) {
      throw new HolidayFetchException("CSV 取得に失敗しました: " + e.getMessage(), e);
    }
  }
}
