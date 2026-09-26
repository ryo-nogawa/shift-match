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

    @Test
    @DisplayName(
        "[F-10] Given: holiday.refresh-on-startup=false のとき, When: 起動すると, Then: Bean が作られない")
    void beanNotCreatedWhenPropertyIsFalse() {
      contextRunner
          .withPropertyValues("holiday.refresh-on-startup=false")
          .withBean(HolidayService.class, () -> mock(HolidayService.class))
          .run(
              context -> {
                if (context.containsBean("holidayStartupRunner")) {
                  throw new AssertionError("Bean should not be created when property is false");
                }
              });
    }
  }
}
