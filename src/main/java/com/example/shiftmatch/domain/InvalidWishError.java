package com.example.shiftmatch.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 早番・遅番希望の不正値エラーを表します。
 */
@Getter
@RequiredArgsConstructor
public class InvalidWishError {

  /** エラーが発生した行番号（0 始まり）。 */
  private final int rowIndex;

  /** エラーの対象（早番希望 または 遅番希望）。 */
  private final String wishLabel;
}
