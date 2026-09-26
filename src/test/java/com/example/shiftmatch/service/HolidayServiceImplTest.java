package com.example.shiftmatch.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.shiftmatch.domain.Holiday;
import com.example.shiftmatch.persistence.HolidayRepository;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

@DisplayName("HolidayServiceImpl")
class HolidayServiceImplTest {

  private HolidayServiceImpl service;
  private HolidayCsvFetcher fetcher;
  private HolidayRepository repository;

  @BeforeEach
  void setUp() {
    fetcher = mock(HolidayCsvFetcher.class);
    repository = mock(HolidayRepository.class);
    service = new HolidayServiceImpl(fetcher, repository);
  }

  @Nested
  @DisplayName("[F-10] refresh で祝日を取得・保存する")
  class RefreshHolidays {

    @Test
    @DisplayName("Given: CSV 取得に成功するとき, When: refresh を呼ぶと, Then: 解析した祝日が保存される")
    void savesHolidaysWhenFetchSucceeds() {
      String csv = "国民の祝日・休日月日,国民の祝日・休日名称\n2026/1/1,元日\n2026/10/12,スポーツの日\n";
      byte[] csvBytes = csv.getBytes(Charset.forName("Shift_JIS"));
      when(fetcher.fetch()).thenReturn(csvBytes);

      service.refresh();

      verify(repository, times(1))
          .replaceAll(
              List.of(
                  new Holiday(LocalDate.of(2026, 1, 1), "元日"),
                  new Holiday(LocalDate.of(2026, 10, 12), "スポーツの日")));
    }

    @Test
    @DisplayName("Given: CSV 取得に失敗するとき, When: refresh を呼ぶと, Then: 例外を外に出さず、保存を行わない")
    void logsErrorWhenFetchFails() {
      when(fetcher.fetch()).thenThrow(new HolidayFetchException("ネットワークエラー"));

      service.refresh();

      verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("Given: CSV が不正で解析に失敗するとき, When: refresh を呼ぶと, Then: 例外を外に出さず、保存を行わない")
    void logsErrorWhenParseFails() {
      String invalidCsv = "国民の祝日・休日月日,国民の祝日・休日名称\ninvalid-date,祝日名\n";
      byte[] csvBytes = invalidCsv.getBytes(Charset.forName("Shift_JIS"));
      when(fetcher.fetch()).thenReturn(csvBytes);

      service.refresh();

      verifyNoInteractions(repository);
    }

    @Test
    @DisplayName(
        "[F-10] Given: 保存時に DataAccessException が発生するとき, When: refresh を呼ぶと, Then:"
            + " 例外を外に出さず、保存済みデータが残る")
    void logsErrorWhenSaveFails() {
      String csv = "国民の祝日・休日月日,国民の祝日・休日名称\n2026/1/1,元日\n2026/10/12,スポーツの日\n";
      byte[] csvBytes = csv.getBytes(Charset.forName("Shift_JIS"));
      when(fetcher.fetch()).thenReturn(csvBytes);
      doThrow(new DataIntegrityViolationException("データベースエラー"))
          .when(repository)
          .replaceAll(
              List.of(
                  new Holiday(LocalDate.of(2026, 1, 1), "元日"),
                  new Holiday(LocalDate.of(2026, 10, 12), "スポーツの日")));

      service.refresh();

      verify(repository, times(1))
          .replaceAll(
              List.of(
                  new Holiday(LocalDate.of(2026, 1, 1), "元日"),
                  new Holiday(LocalDate.of(2026, 10, 12), "スポーツの日")));
    }
  }

  @Nested
  @DisplayName("[V-8] isSupported で対象年が判定できるかチェック")
  class IsSupported {

    @Test
    @DisplayName("Given: 保存済みの年のとき, When: isSupported を呼ぶと, Then: true を返し、取得を呼ばない")
    void returnsTrueWhenYearExists() {
      when(repository.existsInYear(2026)).thenReturn(true);

      boolean result = service.isSupported(java.time.YearMonth.of(2026, 1));

      assertTrue(result);
      verify(fetcher, times(0)).fetch();
    }

    @Test
    @DisplayName("Given: 保存されていない年のとき, When: isSupported を呼ぶと, Then: 取得して、あれば true、なければ false")
    void fetchesAndReturnsTrueWhenYearNotSavedButFetchSucceeds() {
      when(repository.existsInYear(2026)).thenReturn(false, true);
      String csv = "国民の祝日・休日月日,国民の祝日・休日名称\n2026/1/1,元日\n";
      byte[] csvBytes = csv.getBytes(Charset.forName("Shift_JIS"));
      when(fetcher.fetch()).thenReturn(csvBytes);

      boolean result = service.isSupported(java.time.YearMonth.of(2026, 1));

      assertTrue(result);
      verify(fetcher, times(1)).fetch();
    }

    @Test
    @DisplayName("Given: 保存されておらず、取得にも失敗するとき, When: isSupported を呼ぶと, Then: false を返す")
    void returnsFalseWhenYearNotSavedAndFetchFails() {
      when(repository.existsInYear(2026)).thenReturn(false);
      when(fetcher.fetch()).thenThrow(new HolidayFetchException("ネットワークエラー"));

      boolean result = service.isSupported(java.time.YearMonth.of(2026, 1));

      assertFalse(result);
    }
  }

  @Nested
  @DisplayName("[F-10] businessDays と holidaysOf で営業日と祝日を取得")
  class BusinessDaysAndHolidays {

    @Test
    @DisplayName(
        "[F-10] Given: 2026年10月の祝日データがあるとき, When: businessDays を呼ぶと, Then: 営業日（月〜金かつ祝日でない日）が返される")
    void businessDaysReturnsWeekdaysExceptHolidays() {
      when(repository.existsInYear(2026)).thenReturn(true);
      when(repository.findByYear(2026))
          .thenReturn(List.of(new Holiday(LocalDate.of(2026, 10, 12), "スポーツの日")));

      java.time.YearMonth month = java.time.YearMonth.of(2026, 10);
      java.util.List<LocalDate> result = service.businessDays(month);

      // 2026年10月の営業日は21日（月〜金の22日から10/12（月）を除く）
      assertEquals(21, result.size());
      assertTrue(result.contains(LocalDate.of(2026, 10, 1))); // 木
      assertTrue(result.contains(LocalDate.of(2026, 10, 2))); // 金
      assertFalse(result.contains(LocalDate.of(2026, 10, 3))); // 土
      assertFalse(result.contains(LocalDate.of(2026, 10, 4))); // 日
      assertFalse(result.contains(LocalDate.of(2026, 10, 12))); // 祝日（月）
    }

    @Test
    @DisplayName(
        "[V-8] Given: 判定できない年月のとき, When: businessDays を呼ぶと, Then: HolidayDataUnavailableError")
    void businessDaysThrowsWhenDataUnavailable() {
      when(repository.existsInYear(2026)).thenReturn(false);
      when(fetcher.fetch()).thenThrow(new HolidayFetchException("ネットワークエラー"));

      java.time.YearMonth month = java.time.YearMonth.of(2026, 10);

      assertThrows(
          com.example.shiftmatch.domain.HolidayDataUnavailableError.class,
          () -> service.businessDays(month));
    }

    @Test
    @DisplayName("[F-10] Given: 祝日データがあるとき, When: holidaysOf を呼ぶと, Then: 祝日名付きのマップが返される")
    void holidaysOfReturnsHolidaysWithNames() {
      when(repository.existsInYear(2026)).thenReturn(true);
      when(repository.findByYear(2026))
          .thenReturn(
              List.of(
                  new Holiday(LocalDate.of(2026, 10, 12), "スポーツの日"),
                  new Holiday(LocalDate.of(2026, 10, 10), "土曜祝日")));

      java.time.YearMonth month = java.time.YearMonth.of(2026, 10);
      java.util.Map<LocalDate, String> result = service.holidaysOf(month);

      assertEquals(2, result.size());
      assertEquals("スポーツの日", result.get(LocalDate.of(2026, 10, 12)));
      // 土曜の祝日も含まれるが、営業日でない（businessDays に含まれない）
      assertEquals("土曜祝日", result.get(LocalDate.of(2026, 10, 10)));
    }

    @Test
    @DisplayName(
        "[V-8] Given: 判定できない年月のとき, When: holidaysOf を呼ぶと, Then: HolidayDataUnavailableError")
    void holidaysOfThrowsWhenDataUnavailable() {
      when(repository.existsInYear(2026)).thenReturn(false);
      when(fetcher.fetch()).thenThrow(new HolidayFetchException("ネットワークエラー"));

      java.time.YearMonth month = java.time.YearMonth.of(2026, 10);

      assertThrows(
          com.example.shiftmatch.domain.HolidayDataUnavailableError.class,
          () -> service.holidaysOf(month));
    }
  }
}
