package com.example.shiftmatch.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * ValidTimeRangeValidator のテスト。
 */
@DisplayName("ValidTimeRangeValidator")
class ValidTimeRangeValidatorTest {

  private Validator validator;

  @BeforeEach
  void setUp() {
    validator = Validation.buildDefaultValidatorFactory().getValidator();
  }

  @Nested
  @DisplayName("[V-3] 開始・終了の時間帯バリデーション")
  class TimeRangeValidation {

    @Test
    @DisplayName("[V-3] Given: 開始が null のとき, When: 検証すると, Then: 開始のエラーが発生する")
    void testStartIsNull() {
      // Given
      EmployeeForm form = new EmployeeForm();
      form.setName("太郎");
      form.setOff(false);
      form.setStart(null);
      form.setEnd("17:00");

      // When
      Set<ConstraintViolation<EmployeeForm>> violations = validator.validate(form);

      // Then
      assertEquals(1, violations.size(), "Should have 1 violation for null start");
      ConstraintViolation<EmployeeForm> violation = violations.iterator().next();
      assertEquals("開始が未選択です", violation.getMessage());
      assertEquals("start", violation.getPropertyPath().toString());
    }

    @Test
    @DisplayName("[V-3] Given: 開始が空文字のとき, When: 検証すると, Then: 開始のエラーが発生する")
    void testStartIsEmpty() {
      // Given
      EmployeeForm form = new EmployeeForm();
      form.setName("太郎");
      form.setOff(false);
      form.setStart("");
      form.setEnd("17:00");

      // When
      Set<ConstraintViolation<EmployeeForm>> violations = validator.validate(form);

      // Then
      assertEquals(1, violations.size(), "Should have 1 violation for empty start");
      ConstraintViolation<EmployeeForm> violation = violations.iterator().next();
      assertEquals("開始が未選択です", violation.getMessage());
      assertEquals("start", violation.getPropertyPath().toString());
    }

    @Test
    @DisplayName("[V-3] Given: 終了が null のとき, When: 検証すると, Then: 終了のエラーが発生する")
    void testEndIsNull() {
      // Given
      EmployeeForm form = new EmployeeForm();
      form.setName("太郎");
      form.setOff(false);
      form.setStart("09:00");
      form.setEnd(null);

      // When
      Set<ConstraintViolation<EmployeeForm>> violations = validator.validate(form);

      // Then
      assertEquals(1, violations.size(), "Should have 1 violation for null end");
      ConstraintViolation<EmployeeForm> violation = violations.iterator().next();
      assertEquals("終了が未選択です", violation.getMessage());
      assertEquals("end", violation.getPropertyPath().toString());
    }

    @Test
    @DisplayName("[V-3] Given: 終了が空文字のとき, When: 検証すると, Then: 終了のエラーが発生する")
    void testEndIsEmpty() {
      // Given
      EmployeeForm form = new EmployeeForm();
      form.setName("太郎");
      form.setOff(false);
      form.setStart("09:00");
      form.setEnd("");

      // When
      Set<ConstraintViolation<EmployeeForm>> violations = validator.validate(form);

      // Then
      assertEquals(1, violations.size(), "Should have 1 violation for empty end");
      ConstraintViolation<EmployeeForm> violation = violations.iterator().next();
      assertEquals("終了が未選択です", violation.getMessage());
      assertEquals("end", violation.getPropertyPath().toString());
    }

    @Test
    @DisplayName("[V-3] Given: 開始が選択肢（07:30-18:30 の 30 分刻み）にないとき, When: 検証すると, Then: 開始のエラーが発生する")
    void testStartIsNotInOptions() {
      // Given
      EmployeeForm form = new EmployeeForm();
      form.setName("太郎");
      form.setOff(false);
      form.setStart("09:15");
      form.setEnd("17:00");

      // When
      Set<ConstraintViolation<EmployeeForm>> violations = validator.validate(form);

      // Then
      assertEquals(1, violations.size(), "Should have 1 violation for invalid start");
      ConstraintViolation<EmployeeForm> violation = violations.iterator().next();
      assertEquals("開始は選択肢にありません", violation.getMessage());
      assertEquals("start", violation.getPropertyPath().toString());
    }

    @Test
    @DisplayName("[V-3] Given: 終了が選択肢にないとき, When: 検証すると, Then: 終了のエラーが発生する")
    void testEndIsNotInOptions() {
      // Given
      EmployeeForm form = new EmployeeForm();
      form.setName("太郎");
      form.setOff(false);
      form.setStart("09:00");
      form.setEnd("17:15");

      // When
      Set<ConstraintViolation<EmployeeForm>> violations = validator.validate(form);

      // Then
      assertEquals(1, violations.size(), "Should have 1 violation for invalid end");
      ConstraintViolation<EmployeeForm> violation = violations.iterator().next();
      assertEquals("終了は選択肢にありません", violation.getMessage());
      assertEquals("end", violation.getPropertyPath().toString());
    }

    @Test
    @DisplayName("[V-3] Given: 開始と終了が同じとき, When: 検証すると, Then: 開始は終了より前にのエラーが発生する")
    void testStartEqualsEnd() {
      // Given
      EmployeeForm form = new EmployeeForm();
      form.setName("太郎");
      form.setOff(false);
      form.setStart("09:00");
      form.setEnd("09:00");

      // When
      Set<ConstraintViolation<EmployeeForm>> violations = validator.validate(form);

      // Then
      assertEquals(1, violations.size(), "Should have 1 violation for start equals end");
      ConstraintViolation<EmployeeForm> violation = violations.iterator().next();
      assertEquals("開始は終了より前にしてください", violation.getMessage());
      assertEquals("end", violation.getPropertyPath().toString());
    }

    @Test
    @DisplayName("[V-3] Given: 開始が終了より後のとき, When: 検証すると, Then: 開始は終了より前にのエラーが発生する")
    void testStartAfterEnd() {
      // Given
      EmployeeForm form = new EmployeeForm();
      form.setName("太郎");
      form.setOff(false);
      form.setStart("17:00");
      form.setEnd("09:00");

      // When
      Set<ConstraintViolation<EmployeeForm>> violations = validator.validate(form);

      // Then
      assertEquals(1, violations.size(), "Should have 1 violation for start after end");
      ConstraintViolation<EmployeeForm> violation = violations.iterator().next();
      assertEquals("開始は終了より前にしてください", violation.getMessage());
      assertEquals("end", violation.getPropertyPath().toString());
    }

    @Test
    @DisplayName("[V-1] Given: 氏名が空のとき, When: 検証すると, Then: エラーが発生しない")
    void testEmptyNameNoViolation() {
      // Given
      EmployeeForm form = new EmployeeForm();
      form.setName("");
      form.setOff(false);
      form.setStart(null);
      form.setEnd(null);

      // When
      Set<ConstraintViolation<EmployeeForm>> violations = validator.validate(form);

      // Then
      assertTrue(
          violations.isEmpty(), "Should have no violations when name is empty (V-1 exclusion)");
    }

    @Test
    @DisplayName("[V-1] Given: 氏名が空白のみのとき, When: 検証すると, Then: エラーが発生しない")
    void testBlankNameNoViolation() {
      // Given
      EmployeeForm form = new EmployeeForm();
      form.setName("  ");
      form.setOff(false);
      form.setStart(null);
      form.setEnd(null);

      // When
      Set<ConstraintViolation<EmployeeForm>> violations = validator.validate(form);

      // Then
      assertTrue(
          violations.isEmpty(), "Should have no violations when name is blank (V-1 exclusion)");
    }

    @Test
    @DisplayName("[V-3] Given: 休みのとき, When: 検証すると, Then: エラーが発生しない")
    void testOffNoViolation() {
      // Given
      EmployeeForm form = new EmployeeForm();
      form.setName("太郎");
      form.setOff(true);
      form.setStart(null);
      form.setEnd(null);

      // When
      Set<ConstraintViolation<EmployeeForm>> violations = validator.validate(form);

      // Then
      assertTrue(violations.isEmpty(), "Should have no violations when off is true");
    }

    @Test
    @DisplayName("[V-3] Given: 正常な開始・終了のとき, When: 検証すると, Then: エラーが発生しない")
    void testValidTimeRange() {
      // Given
      EmployeeForm form = new EmployeeForm();
      form.setName("太郎");
      form.setOff(false);
      form.setStart("09:00");
      form.setEnd("17:00");

      // When
      Set<ConstraintViolation<EmployeeForm>> violations = validator.validate(form);

      // Then
      assertTrue(violations.isEmpty(), "Should have no violations for valid time range");
    }
  }
}
