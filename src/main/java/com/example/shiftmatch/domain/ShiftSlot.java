package com.example.shiftmatch.domain;

import java.time.LocalTime;

/**
 * 割り当て枠を表す列挙型。
 *
 * <p>6 種類の枠（No.1〜6）を定義し、各枠の勤務時間・人数・休憩の長さを保持します。
 */
public enum ShiftSlot {
  /**
   * 枠 1：7:30〜14:30、2 名、休憩 45 分。
   */
  SLOT_1(LocalTime.of(7, 30), LocalTime.of(14, 30), 2, 45),

  /**
   * 枠 2：8:00〜15:30、1 名、休憩 45 分。
   */
  SLOT_2(LocalTime.of(8, 0), LocalTime.of(15, 30), 1, 45),

  /**
   * 枠 3：8:30〜16:30、1 名、休憩 45 分。
   */
  SLOT_3(LocalTime.of(8, 30), LocalTime.of(16, 30), 1, 45),

  /**
   * 枠 4：9:00〜16:30、1 名、休憩 45 分。
   */
  SLOT_4(LocalTime.of(9, 0), LocalTime.of(16, 30), 1, 45),

  /**
   * 枠 5：9:00〜18:00、1 名、休憩 60 分。
   */
  SLOT_5(LocalTime.of(9, 0), LocalTime.of(18, 0), 1, 60),

  /**
   * 枠 6：9:00〜18:30、2 名、休憩 60 分。
   */
  SLOT_6(LocalTime.of(9, 0), LocalTime.of(18, 30), 2, 60);

  private final LocalTime startTime;
  private final LocalTime endTime;
  private final int numberOfEmployees;
  private final int breakDurationMinutes;

  /**
   * 枠を初期化します。
   *
   * @param startTime 開始時刻
   * @param endTime 終了時刻
   * @param numberOfEmployees 割り当て人数
   * @param breakDurationMinutes 休憩の長さ（分）
   */
  ShiftSlot(
      LocalTime startTime, LocalTime endTime, int numberOfEmployees, int breakDurationMinutes) {
    this.startTime = startTime;
    this.endTime = endTime;
    this.numberOfEmployees = numberOfEmployees;
    this.breakDurationMinutes = breakDurationMinutes;
  }

  /**
   * 開始時刻を返します。
   *
   * @return 開始時刻
   */
  public LocalTime startTime() {
    return startTime;
  }

  /**
   * 終了時刻を返します。
   *
   * @return 終了時刻
   */
  public LocalTime endTime() {
    return endTime;
  }

  /**
   * 割り当て人数を返します。
   *
   * @return 割り当て人数
   */
  public int numberOfEmployees() {
    return numberOfEmployees;
  }

  /**
   * 休憩の長さを返します。
   *
   * @return 休憩の長さ（分）
   */
  public int breakDurationMinutes() {
    return breakDurationMinutes;
  }
}
