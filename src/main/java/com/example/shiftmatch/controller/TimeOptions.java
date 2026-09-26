package com.example.shiftmatch.controller;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * シフト作成画面の開始・終了時刻の選択肢を提供するクラスです。
 *
 * <p>07:30 から 18:30 までの 30 分刻みの時刻を選択肢として提供します。
 */
public final class TimeOptions {

  private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

  private static final LocalTime FIRST_TIME_OPTION = LocalTime.of(7, 30);

  private static final LocalTime LAST_TIME_OPTION = LocalTime.of(18, 30);

  private static final int TIME_OPTION_STEP_MINUTES = 30;

  /** 開始・終了の選択肢（07:30〜18:30 の 30 分刻み、HH:mm）。 */
  public static final List<String> VALUES = createTimeOptions();

  /**
   * コンストラクタです。
   *
   * <p>このクラスはユーティリティクラスのため、インスタンスを生成しません。
   */
  private TimeOptions() {}

  /**
   * 開始・終了の選択肢を作成します。
   *
   * @return 07:30 から 18:30 までの 30 分刻みの時刻（HH:mm）のリスト
   */
  private static List<String> createTimeOptions() {
    List<String> options = new ArrayList<>();
    for (LocalTime time = FIRST_TIME_OPTION;
        !time.isAfter(LAST_TIME_OPTION);
        time = time.plusMinutes(TIME_OPTION_STEP_MINUTES)) {
      options.add(time.format(TIME_FORMATTER));
    }
    return List.copyOf(options);
  }
}
