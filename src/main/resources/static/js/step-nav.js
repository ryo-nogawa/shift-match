/**
 * ステップナビゲーション、対象月の切り替え、ページ非スクロール機能を提供します。
 * 画面間の移動、月の前後移動、営業日情報の取得と通知を実装しています。
 */

document.addEventListener("DOMContentLoaded", function () {
  const app = document.querySelector(".app");
  const initialStep = app.getAttribute("data-initial-step") || "1";
  let currentStep = parseInt(initialStep);

  const prevBtn = document.getElementById("prev-btn");
  const nextBtn = document.getElementById("next-btn");
  const stepBtns = document.querySelectorAll(".step-btn");
  const form = document.querySelector("form");

  // 画面の表示・非表示を切り替える
  function showScreen(step) {
    const screens = document.querySelectorAll("section[data-screen]");
    screens.forEach((screen) => {
      const screenNum = parseInt(screen.getAttribute("data-screen"));
      if (screenNum === step) {
        screen.removeAttribute("hidden");
      } else {
        screen.setAttribute("hidden", "");
      }
    });

    // ボタンの表示制御
    if (step === 1) {
      prevBtn.style.display = "none";
      nextBtn.style.display = "block";
      nextBtn.textContent = "次へ";
    } else if (step === 2) {
      prevBtn.style.display = "block";
      nextBtn.style.display = "block";
      nextBtn.textContent = "1 か月分のシフトを作成";
      nextBtn.type = "submit";
    } else {
      prevBtn.style.display = "block";
      nextBtn.style.display = "none";
    }

    // ステップボタンの選択状態を更新
    stepBtns.forEach((btn) => {
      const btnStep = parseInt(btn.getAttribute("data-step"));
      if (btnStep === step) {
        btn.classList.add("active");
      } else {
        btn.classList.remove("active");
      }
    });

    currentStep = step;
  }

  // ナビゲーションボタンのイベントリスナー
  prevBtn.addEventListener("click", () => {
    if (currentStep > 1) {
      showScreen(currentStep - 1);
    }
  });

  nextBtn.addEventListener("click", () => {
    if (nextBtn.type === "submit") {
      form.submit();
    } else if (currentStep < 3) {
      showScreen(currentStep + 1);
    }
  });

  // ステップボタンのイベントリスナー
  stepBtns.forEach((btn) => {
    btn.addEventListener("click", () => {
      const step = parseInt(btn.getAttribute("data-step"));
      showScreen(step);
    });
  });

  // 対象月の切り替え機能
  const prevMonthBtn = document.getElementById("prev-month-btn");
  const nextMonthBtn = document.getElementById("next-month-btn");
  const monthLabel = document.getElementById("month-label");
  const targetMonthInput = document.querySelector("input[name='targetMonth']");
  const monthSummary = document.getElementById("month-summary");

  function parseYearMonth(yearMonthStr) {
    const [year, month] = yearMonthStr.split("-");
    return { year: parseInt(year), month: parseInt(month) };
  }

  function formatYearMonth(year, month) {
    return `${year}-${String(month).padStart(2, "0")}`;
  }

  function getJapaneseMonthLabel(year, month) {
    return `${year}年${month}月`;
  }

  function loadCalendarInfo() {
    const monthStr = targetMonthInput.value;
    if (!monthStr) return;

    fetch(`/calendar?month=${monthStr}`)
      .then((response) => {
        if (!response.ok) {
          throw new Error("Calendar data unavailable");
        }
        return response.json();
      })
      .then((data) => {
        const businessDayCount = data.businessDayCount;
        const holidayCount = data.holidayCount;
        monthSummary.textContent = `営業日 ${businessDayCount} 日・祝日 ${holidayCount} 日`;

        // customEvent を発火して画面 2 に通知
        const event = new CustomEvent("calendar-loaded", {
          detail: data,
        });
        document.dispatchEvent(event);
      })
      .catch((error) => {
        monthSummary.textContent = "対象月を判定できません。祝日データにない月です。";
        monthSummary.style.color = "red";
      });
  }

  prevMonthBtn.addEventListener("click", () => {
    const current = parseYearMonth(targetMonthInput.value);
    let { year, month } = current;
    month--;
    if (month < 1) {
      month = 12;
      year--;
    }
    targetMonthInput.value = formatYearMonth(year, month);
    monthLabel.textContent = getJapaneseMonthLabel(year, month);
    loadCalendarInfo();
  });

  nextMonthBtn.addEventListener("click", () => {
    const current = parseYearMonth(targetMonthInput.value);
    let { year, month } = current;
    month++;
    if (month > 12) {
      month = 1;
      year++;
    }
    targetMonthInput.value = formatYearMonth(year, month);
    monthLabel.textContent = getJapaneseMonthLabel(year, month);
    loadCalendarInfo();
  });

  // 初期化
  const initialMonth = targetMonthInput.value;
  if (initialMonth) {
    const { year, month } = parseYearMonth(initialMonth);
    monthLabel.textContent = getJapaneseMonthLabel(year, month);
    loadCalendarInfo();
  }

  showScreen(currentStep);
});
