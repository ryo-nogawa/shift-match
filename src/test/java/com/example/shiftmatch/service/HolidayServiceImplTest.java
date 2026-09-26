package com.example.shiftmatch.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
}
