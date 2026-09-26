package com.example.shiftmatch.service;

import com.example.shiftmatch.domain.Holiday;
import com.example.shiftmatch.domain.HolidayDataUnavailableError;
import com.example.shiftmatch.persistence.HolidayRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

/**
 * 祝日データを管理するサービス実装。
 *
 * <p>外部から CSV を取得・解析して H2 に保存します。
 * 同時実行性を考慮して {@link #refresh()} は synchronized です。
 */
@Service
public class HolidayServiceImpl implements HolidayService {

  private static final Logger LOGGER = LoggerFactory.getLogger(HolidayServiceImpl.class);

  private final HolidayCsvFetcher fetcher;
  private final HolidayRepository repository;
  private final HolidayCsvParser parser = new HolidayCsvParser();

  /**
   * CSV フェッチャーとリポジトリを注入してインスタンスを生成します。
   *
   * @param fetcher CSV フェッチャー
   * @param repository 祝日リポジトリ
   */
  public HolidayServiceImpl(HolidayCsvFetcher fetcher, HolidayRepository repository) {
    this.fetcher = fetcher;
    this.repository = repository;
  }

  /**
   * 祝日データを更新します。
   *
   * <p>外部から CSV を取得して解析し、データベースに保存します。
   * 同時実行を避けるため synchronized にしています。
   * 取得または解析に失敗した場合は、例外を外に出さず、保存済みのデータを維持します。
   */
  @Override
  public synchronized void refresh() {
    try {
      byte[] csv = fetcher.fetch();
      List<Holiday> holidays = parser.parse(csv);
      repository.replaceAll(holidays);
    } catch (HolidayFetchException e) {
      LOGGER.error("祝日 CSV の取得に失敗しました", e);
    } catch (IllegalArgumentException e) {
      LOGGER.error("祝日 CSV の解析に失敗しました", e);
    } catch (DataAccessException e) {
      LOGGER.error("祝日データの保存に失敗しました", e);
    }
  }

  /**
   * 対象月が判定可能かどうかを確認します。
   *
   * <p>その年の祝日データが保存されている場合は true を返します。
   * 保存されていない場合は 1 回 {@link #refresh()} を呼んで判定を試みます。
   *
   * @param month 対象月
   * @return その年の祝日データが保存されている場合は true、そうでない場合は false
   */
  @Override
  public boolean isSupported(YearMonth month) {
    int year = month.getYear();

    // 既に保存されている場合
    if (repository.existsInYear(year)) {
      return true;
    }

    // 保存されていない場合は取得を試みる
    refresh();

    // 取得後、もう一度確認
    return repository.existsInYear(year);
  }

  /**
   * 指定月の営業日一覧を取得します。
   *
   * <p>営業日は月〜金かつ祝日でない日です。日付は昇順で返されます。
   *
   * @param month 対象月
   * @return 営業日の一覧（日付順）
   * @throws HolidayDataUnavailableError 祝日データが利用できない場合
   */
  @Override
  public List<LocalDate> businessDays(YearMonth month) {
    if (!isSupported(month)) {
      throw new HolidayDataUnavailableError(month);
    }

    List<Holiday> holidays = repository.findByYear(month.getYear());
    var holidayDates =
        holidays.stream()
            .filter(holiday -> holiday.date().getMonthValue() == month.getMonthValue())
            .map(holiday -> holiday.date())
            .collect(Collectors.toSet());

    var result =
        month
            .atDay(1)
            .datesUntil(month.atEndOfMonth().plusDays(1))
            .filter(date -> isWeekday(date) && !holidayDates.contains(date))
            .collect(Collectors.toList());

    return result;
  }

  /**
   * 指定月の祝日一覧を取得します。
   *
   * <p>祝日は日付と祝日名のマップです。日付順で返されます。
   *
   * @param month 対象月
   * @return 祝日の一覧（日付順の LinkedHashMap）
   * @throws HolidayDataUnavailableError 祝日データが利用できない場合
   */
  @Override
  public Map<LocalDate, String> holidaysOf(YearMonth month) {
    if (!isSupported(month)) {
      throw new HolidayDataUnavailableError(month);
    }

    List<Holiday> holidays = repository.findByYear(month.getYear());
    var result = new LinkedHashMap<LocalDate, String>();

    for (Holiday holiday : holidays) {
      if (holiday.date().getMonthValue() == month.getMonthValue()) {
        result.put(holiday.date(), holiday.name());
      }
    }

    return result;
  }

  /**
   * 指定日が平日（月〜金）かどうかを判定します。
   *
   * @param date 判定する日付
   * @return 平日の場合は true、そうでない場合は false
   */
  private boolean isWeekday(LocalDate date) {
    DayOfWeek dayOfWeek = date.getDayOfWeek();
    return dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY;
  }
}
