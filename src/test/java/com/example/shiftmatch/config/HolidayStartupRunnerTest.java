package com.example.shiftmatch.config;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.example.shiftmatch.service.HolidayService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

@DisplayName("HolidayStartupRunner")
class HolidayStartupRunnerTest {

  private ApplicationContextRunner contextRunner;

  @BeforeEach
  void setUp() {
    contextRunner = new ApplicationContextRunner();
  }

  @Nested
  @DisplayName("[F-10] 起動時に祝日データを更新する")
  class StartupRefresh {

    @Test
    @DisplayName(
        "[F-10] Given: holiday.refresh-on-startup=true のとき, When: 起動すると, Then: refresh() がちょうど 1"
            + " 回呼ばれる")
    void callsRefreshWhenPropertyIsTrue() {
      HolidayService holidayService = mock(HolidayService.class);
      ApplicationArguments args = mock(ApplicationArguments.class);

      HolidayStartupRunner runner = new HolidayStartupRunner(holidayService);
      runner.run(args);

      verify(holidayService, times(1)).refresh();
    }
  }

  @Nested
  @DisplayName("[F-10] @ConditionalOnProperty の条件判定")
  class ConditionalOnPropertyBehavior {

    @Test
    @DisplayName(
        "[F-10] Given: holiday.refresh-on-startup=false のとき, When: コンテキストを起動すると, Then:"
            + " HolidayStartupRunner Bean が作られない")
    void beanNotCreatedWhenPropertyIsFalse() {
      contextRunner
          .withUserConfiguration(HolidayStartupRunner.class)
          .withBean(HolidayService.class, () -> mock(HolidayService.class))
          .withPropertyValues("holiday.refresh-on-startup=false")
          .run(
              context -> {
                if (context.containsBean("holidayStartupRunner")) {
                  throw new AssertionError("Bean should not be created when property is false");
                }
              });
    }

    @Test
    @DisplayName(
        "[F-10] Given: holiday.refresh-on-startup=true のとき, When: コンテキストを起動すると, Then:"
            + " HolidayStartupRunner Bean が作られる")
    void beanCreatedWhenPropertyIsTrue() {
      contextRunner
          .withUserConfiguration(HolidayStartupRunner.class)
          .withBean(HolidayService.class, () -> mock(HolidayService.class))
          .withPropertyValues("holiday.refresh-on-startup=true")
          .run(
              context -> {
                if (!context.containsBean("holidayStartupRunner")) {
                  throw new AssertionError("Bean should be created when property is true");
                }
              });
    }
  }
}
