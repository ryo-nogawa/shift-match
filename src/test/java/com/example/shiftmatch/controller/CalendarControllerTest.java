package com.example.shiftmatch.controller;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.shiftmatch.service.HolidayService;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CalendarController.class)
class CalendarControllerTest {

  private static final String UNSUPPORTED_MESSAGE = "対象月を判定できません。祝日データにない月です。";

  @Autowired private MockMvc mockMvc;

  @MockitoBean private HolidayService holidayService;

  @Nested
  class 正常系 {

    @Test
    @DisplayName(
        "[F-9] Given: 祝日データで判定できる月のとき, When: GET /calendar を呼ぶと,"
            + " Then: 営業日の一覧・営業日数・祝日（日付と名前）・祝日数が JSON で返る")
    void returnsBusinessDaysAndHolidaysWhenMonthIsSupported() throws Exception {
      YearMonth month = YearMonth.of(2026, 10);
      Map<LocalDate, String> holidays = new LinkedHashMap<>();
      holidays.put(LocalDate.of(2026, 10, 12), "スポーツの日");
      when(holidayService.isSupported(month)).thenReturn(true);
      when(holidayService.businessDays(month))
          .thenReturn(
              List.of(
                  LocalDate.of(2026, 10, 1),
                  LocalDate.of(2026, 10, 2),
                  LocalDate.of(2026, 10, 13)));
      when(holidayService.holidaysOf(month)).thenReturn(holidays);

      mockMvc
          .perform(get("/calendar").param("month", "2026-10"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.month", equalTo("2026-10")))
          .andExpect(jsonPath("$.businessDayCount", equalTo(3)))
          .andExpect(jsonPath("$.holidayCount", equalTo(1)))
          .andExpect(jsonPath("$.businessDays[0]", equalTo("2026-10-01")))
          .andExpect(jsonPath("$.businessDays[1]", equalTo("2026-10-02")))
          .andExpect(jsonPath("$.businessDays[2]", equalTo("2026-10-13")))
          .andExpect(jsonPath("$.holidays[0].date", equalTo("2026-10-12")))
          .andExpect(jsonPath("$.holidays[0].name", equalTo("スポーツの日")));
    }

    @Test
    @DisplayName(
        "[F-9][V-8] Given: 祝日データの範囲の最初の月（1955-01）のとき, When: GET /calendar を呼ぶと,"
            + " Then: 祝日データで判定する")
    void checksHolidayDataWhenMonthIsFirstSupportedMonth() throws Exception {
      YearMonth month = YearMonth.of(1955, 1);
      when(holidayService.isSupported(month)).thenReturn(true);
      when(holidayService.businessDays(month)).thenReturn(List.of());
      when(holidayService.holidaysOf(month)).thenReturn(Map.of());

      mockMvc.perform(get("/calendar").param("month", "1955-01")).andExpect(status().isOk());

      verify(holidayService).isSupported(month);
    }

    @Test
    @DisplayName(
        "[F-9][V-8] Given: 祝日データの範囲の最後の月（翌年 12 月）のとき, When: GET /calendar を呼ぶと,"
            + " Then: 祝日データで判定する")
    void checksHolidayDataWhenMonthIsLastSupportedMonth() throws Exception {
      YearMonth month = YearMonth.of(Year.now().getValue() + 1, 12);
      when(holidayService.isSupported(month)).thenReturn(true);
      when(holidayService.businessDays(month)).thenReturn(List.of());
      when(holidayService.holidaysOf(month)).thenReturn(Map.of());

      mockMvc.perform(get("/calendar").param("month", month.toString())).andExpect(status().isOk());

      verify(holidayService).isSupported(month);
    }
  }

  @Nested
  class 異常系 {

    @Test
    @DisplayName(
        "[F-9][V-8] Given: month が YYYY-MM 形式でないとき, When: GET /calendar を呼ぶと,"
            + " Then: 400 とメッセージが返る")
    void returnsBadRequestWhenMonthFormatIsInvalid() throws Exception {
      mockMvc
          .perform(get("/calendar").param("month", "invalid"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message", equalTo(UNSUPPORTED_MESSAGE)));

      verify(holidayService, never()).isSupported(any());
    }

    @Test
    @DisplayName(
        "[F-9][V-8] Given: 祝日データで判定できない月のとき, When: GET /calendar を呼ぶと," + " Then: 400 とメッセージが返る")
    void returnsBadRequestWhenMonthIsNotSupported() throws Exception {
      YearMonth month = YearMonth.of(2025, 1);
      when(holidayService.isSupported(month)).thenReturn(false);

      mockMvc
          .perform(get("/calendar").param("month", "2025-01"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message", equalTo(UNSUPPORTED_MESSAGE)));
    }

    @Test
    @DisplayName(
        "[F-9][V-8] Given: 祝日データの範囲より前の月（1954-12）のとき, When: GET /calendar を呼ぶと,"
            + " Then: 祝日データを確認せずに 400 とメッセージが返る")
    void returnsBadRequestWithoutCheckingWhenMonthIsBeforeRange() throws Exception {
      mockMvc
          .perform(get("/calendar").param("month", "1954-12"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message", equalTo(UNSUPPORTED_MESSAGE)));

      verify(holidayService, never()).isSupported(any());
    }

    @Test
    @DisplayName(
        "[F-9][V-8] Given: 祝日データの範囲より後の月（翌々年 1 月）のとき, When: GET /calendar を呼ぶと,"
            + " Then: 祝日データを確認せずに 400 とメッセージが返る")
    void returnsBadRequestWithoutCheckingWhenMonthIsAfterRange() throws Exception {
      YearMonth month = YearMonth.of(Year.now().getValue() + 2, 1);

      mockMvc
          .perform(get("/calendar").param("month", month.toString()))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message", equalTo(UNSUPPORTED_MESSAGE)));

      verify(holidayService, never()).isSupported(any());
    }
  }
}
