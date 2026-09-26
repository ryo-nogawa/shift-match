package com.example.shiftmatch.service;

import java.time.YearMonth;

/**
 * 祝日データを管理するサービスのインタフェース。
 *
 * <p>祝日の取得・保存と、対象月が判定可能かの確認を提供します。
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
}
