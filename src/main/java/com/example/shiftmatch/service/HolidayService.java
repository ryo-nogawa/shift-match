package com.example.shiftmatch.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

/**
 * 祝日データを管理するサービスのインタフェース。
 *
 * <p>祝日の取得・保存と、対象月が判定可能かの確認、営業日と祝日の一覧取得を提供します。
 */
public interface HolidayService {

  /**
   * 祝日データを更新します。
   *
   * <p>外部から CSV を取得して解析し、データベースに保存します。
   * 取得または解析に失敗した場合は、例外を外に出さず、保存済みのデータを維持します。
   */
  void refresh();

  /**
   * 対象月が判定可能かどうかを確認します。
   *
   * <p>その年の祝日データが保存されている場合は true を返します。
   * 保存されていない場合は 1 回 {@link #refresh()} を呼んで判定を試みます。
   *
   * @param month 対象月
   * @return その年の祝日データが保存されている場合は true、そうでない場合は false
   */
  boolean isSupported(YearMonth month);

  /**
   * 指定月の営業日一覧を取得します。
   *
   * <p>営業日は月〜金かつ祝日でない日です。日付は昇順で返されます。
   *
   * @param month 対象月
   * @return 営業日の一覧（日付順）
   * @throws com.example.shiftmatch.domain.HolidayDataUnavailableError 祝日データが利用できない場合
   */
  List<LocalDate> businessDays(YearMonth month);

  /**
   * 指定月の祝日一覧を取得します。
   *
   * <p>祝日は日付と祝日名のマップです。日付順で返されます。
   *
   * @param month 対象月
   * @return 祝日の一覧（日付順の LinkedHashMap）
   * @throws com.example.shiftmatch.domain.HolidayDataUnavailableError 祝日データが利用できない場合
   */
  Map<LocalDate, String> holidaysOf(YearMonth month);
}
