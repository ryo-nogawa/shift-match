package com.example.shiftmatch.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 勤務枠希望の不正値エラーを表します。
 */
@Getter
@RequiredArgsConstructor
public class InvalidWishError {

  /** エラーが発生した行番号（0 始まり）。 */
  private final int rowIndex;

  /** エラーの対象（勤務時間）。 */
  private final String wishLabel;
}
