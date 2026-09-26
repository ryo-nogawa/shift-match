package com.example.shiftmatch.service;

import com.example.shiftmatch.domain.Holiday;
import com.example.shiftmatch.persistence.HolidayRepository;
import java.time.YearMonth;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
}
