package com.example.shiftmatch.config;

import com.example.shiftmatch.service.HolidayService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * アプリケーション起動時に祝日データを更新するコンポーネント。
 *
 * <p>{@code holiday.refresh-on-startup} が {@code true} のときに有効です。
 */
@Component
@ConditionalOnProperty(
    name = "holiday.refresh-on-startup",
    havingValue = "true",
    matchIfMissing = false)
public class HolidayStartupRunner implements ApplicationRunner {

  private final HolidayService holidayService;

  /**
   * 祝日サービスを注入してインスタンスを生成します。
   *
   * @param holidayService 祝日サービス
   */
  public HolidayStartupRunner(HolidayService holidayService) {
    this.holidayService = holidayService;
  }

  /**
   * アプリケーション起動時に祝日データを更新します。
   *
   * <p>取得に失敗してもアプリの起動を止めません。
   *
   * @param args アプリケーション引数（使用しません）
   */
  @Override
  public void run(ApplicationArguments args) {
    holidayService.refresh();
  }
}
