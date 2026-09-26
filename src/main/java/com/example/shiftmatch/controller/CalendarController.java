package com.example.shiftmatch.controller;

import com.example.shiftmatch.service.HolidayService;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

  private static final Logger LOGGER = LoggerFactory.getLogger(CalendarController.class);

  /** 内閣府の祝日 CSV が収録している最初の年です（4.1 節）。 */
  private static final int FIRST_HOLIDAY_YEAR = 1955;

  private static final String UNSUPPORTED_MESSAGE = "対象月を判定できません。祝日データにない月です。";

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
    YearMonth yearMonth;
    try {
      yearMonth = YearMonth.parse(month);
    } catch (DateTimeParseException e) {
      LOGGER.debug("対象月を解析できません: {}", month, e);
      return unsupported();
    }

    // CSV の収録範囲外の年は isSupported に渡すと毎回外部 CSV の取得が走るため、
    // 取得を連打されないよう範囲外はここで拒否する（V-8 と同じ扱い）
    if (!isWithinHolidayDataRange(yearMonth) || !holidayService.isSupported(yearMonth)) {
      return unsupported();
    }

    List<LocalDate> businessDays = holidayService.businessDays(yearMonth);
    Map<LocalDate, String> holidays = holidayService.holidaysOf(yearMonth);

    List<String> formattedBusinessDays = businessDays.stream().map(d -> d.toString()).toList();

    List<Map<String, String>> formattedHolidays =
        holidays.entrySet().stream()
            .map(
                entry ->
                    Map.of(
                        "date", entry.getKey().toString(),
                        "name", entry.getValue()))
            .toList();

    CalendarResponse response =
        new CalendarResponse(
            month, businessDays.size(), holidays.size(), formattedBusinessDays, formattedHolidays);

    return ResponseEntity.ok(response);
  }

  /**
   * 対象月の年が、祝日 CSV の収録範囲（1955 年〜翌年）に入っているかを返します。
   *
   * @param month 対象月
   * @return 範囲内なら true
   */
  private static boolean isWithinHolidayDataRange(YearMonth month) {
    int year = month.getYear();
    return year >= FIRST_HOLIDAY_YEAR && year <= Year.now().getValue() + 1;
  }

  /**
   * 対象月を判定できないときの 400 応答を作ります。
   *
   * @return メッセージを含む 400 応答
   */
  private static ResponseEntity<Map<String, String>> unsupported() {
    return ResponseEntity.badRequest().body(Map.of("message", UNSUPPORTED_MESSAGE));
  }
}
