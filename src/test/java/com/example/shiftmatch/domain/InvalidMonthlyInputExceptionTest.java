package com.example.shiftmatch.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("InvalidMonthlyInputException")
class InvalidMonthlyInputExceptionTest {

  @Nested
  class 正常系 {

    @Test
    @DisplayName(
        "Given: エラーリストを与えるとき, When: InvalidMonthlyInputException を作成すると, Then: エラーリストが正しく保持される")
    void createsWithErrors() {
      List<InputError> errors = new ArrayList<>();
      errors.add(new InputError("V-2", "従業員名が重複しています: 1 行目、2 行目"));
      errors.add(new InputError("V-3", "基本シフト：月曜日が未選択です（1 行目）"));

      InvalidMonthlyInputException exception = new InvalidMonthlyInputException(errors);

      assertEquals(2, exception.errors().size());
      assertEquals("V-2", exception.errors().get(0).code());
      assertEquals("従業員名が重複しています: 1 行目、2 行目", exception.errors().get(0).message());
    }

    @Test
    @DisplayName("Given: 空のエラーリストを与えるとき, When: InvalidMonthlyInputException を作成すると, Then: 例外が作成される")
    void createsWithEmptyErrors() {
      List<InputError> errors = new ArrayList<>();
      InvalidMonthlyInputException exception = new InvalidMonthlyInputException(errors);

      assertTrue(exception.errors().isEmpty());
    }
  }
}
