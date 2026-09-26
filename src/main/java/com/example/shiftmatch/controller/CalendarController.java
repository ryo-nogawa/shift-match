package com.example.shiftmatch.controller;

import com.example.shiftmatch.service.HolidayService;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * カレンダー（営業日・祝日）の情報を提供するコントローラーです。
 */
@RestController
public class CalendarController {

  private final HolidayService holidayService;

  /**
   * コンストラクタです。
   *
   * @param holidayService 祝日サービス
   */
  @Autowired
  public CalendarController(HolidayService holidayService) {
    this.holidayService = holidayService;
  }

  /**
   * 対象月の営業日と祝日情報を返します。
   *
   * @param month 対象月（YYYY-MM 形式）
   * @return カレンダー情報、または不正な入力時は 400 エラー
   */
  @GetMapping("/calendar")
  public ResponseEntity<?> getCalendar(@RequestParam String month) {
    final String errorMessage = "対象月を判定できません。祝日データにない月です。";

    // 対象月の解析
    YearMonth yearMonth;
    try {
      yearMonth = YearMonth.parse(month);
    } catch (Exception e) {
      return ResponseEntity.badRequest().body(Map.of("message", errorMessage));
    }

    // 対象月が祝日データでサポートされているかを確認
    if (!holidayService.isSupported(yearMonth)) {
      return ResponseEntity.badRequest().body(Map.of("message", errorMessage));
    }

    // 営業日と祝日を取得
    List<LocalDate> businessDays = holidayService.businessDays(yearMonth);
    Map<LocalDate, String> holidays = holidayService.holidaysOf(yearMonth);

    // 営業日をフォーマット
    List<String> formattedBusinessDays = businessDays.stream().map(d -> d.toString()).toList();

    // 祝日を Map に変換
    List<Map<String, String>> formattedHolidays =
        holidays.entrySet().stream()
            .map(
                entry ->
                    Map.of(
                        "date", entry.getKey().toString(),
                        "name", entry.getValue()))
            .toList();

    // レスポンスを構築
    CalendarResponse response =
        new CalendarResponse(
            month, businessDays.size(), holidays.size(), formattedBusinessDays, formattedHolidays);

    return ResponseEntity.ok(response);
  }
}
