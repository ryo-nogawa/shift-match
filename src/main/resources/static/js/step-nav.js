/**
 * 3 画面のステップ操作と、対象月の切り替え（F-9、8 章冒頭・8.1 節）。
 *
 * 対象月の営業日・祝日は GET /calendar から取得し、画面 2 へ calendar-loaded イベントで渡す。
 */
(function () {
  "use strict";

  const UNAVAILABLE_MESSAGE = "対象月を判定できません。祝日データにない月です。";
  const SAVED_FETCH_ERROR_MESSAGE = "保存済みのシフトを取得できませんでした";

  /** YYYY-MM に月数を足した YYYY-MM を返す。 */
  function shiftMonth(yearMonth, delta) {
    const parts = yearMonth.split("-");
    const total = Number(parts[0]) * 12 + (Number(parts[1]) - 1) + delta;
    const year = Math.floor(total / 12);
    const month = (total % 12) + 1;
    return String(year).padStart(4, "0") + "-" + String(month).padStart(2, "0");
  }

  /** YYYY-MM を「2026 年 10 月」の形式にする。 */
  function monthLabel(yearMonth) {
    const parts = yearMonth.split("-");
    return Number(parts[0]) + " 年 " + Number(parts[1]) + " 月";
  }

  /** 画面ごとの「戻る」「次へ」の状態（8 章冒頭）。 */
  function navState(step) {
    return {
      prevDisabled: step === 1,
      nextHidden: step === 3,
      nextType: step === 2 ? "submit" : "button",
      nextLabel: step === 2 ? "1 か月分のシフトを作成" : "次へ",
    };
  }

  /**
   * 画面 3 を開くとき、保存済みシフトの取得が必要かを返す（8.3 節）。
   * 今回作成した結果（fresh）で対象月も同じときだけ、取得せずにそのまま表示する。
   */
  function needsSavedRefetch(resultSource, resultMonth, currentMonth) {
    return !(resultSource === "fresh" && resultMonth === currentMonth);
  }

  if (typeof module !== "undefined" && module.exports) {
    module.exports = { shiftMonth, monthLabel, navState, needsSavedRefetch };
  }

  if (typeof document === "undefined") {
    return;
  }

  document.addEventListener("DOMContentLoaded", function () {
    const app = document.querySelector(".app");
    const form = document.querySelector("form");
    const prevButton = document.getElementById("prev-btn");
    const nextButton = document.getElementById("next-btn");
    const stepButtons = Array.from(document.querySelectorAll(".step-btn"));
    const screens = Array.from(document.querySelectorAll("[data-screen]"));
    const monthInput = form.querySelector("input[name='targetMonth']");
    const monthLabelElement = document.getElementById("month-label");
    const monthSummary = document.getElementById("month-summary");
    const resultScreen = document.getElementById("screen-3");
    let currentStep = 1;

    function showStep(step) {
      currentStep = step;
      screens.forEach(function (screen) {
        screen.hidden = Number(screen.getAttribute("data-screen")) !== step;
      });
      stepButtons.forEach(function (button) {
        const active = Number(button.getAttribute("data-step")) === step;
        button.classList.toggle("active", active);
        if (active) {
          button.setAttribute("aria-current", "step");
        } else {
          button.removeAttribute("aria-current");
        }
      });
      const state = navState(step);
      prevButton.disabled = state.prevDisabled;
      nextButton.hidden = state.nextHidden;
      // 画面 1→2→1 と戻ったときに submit のまま残らないよう、画面ごとに毎回設定する
      nextButton.type = state.nextType;
      nextButton.textContent = state.nextLabel;
      if (step === 3) {
        refreshResult();
      }
    }

    /** 画面 3 の中身を差し替え、タブなどの初期化をやり直させる。 */
    function replaceResult(node) {
      resultScreen.replaceChildren(node);
      document.dispatchEvent(new CustomEvent("result-replaced"));
    }

    /** 取得に失敗したことを画面 3 に表示する。次に画面 3 を開いたときは再取得する。 */
    function showSavedFetchError() {
      const panel = document.createElement("div");
      panel.id = "result-panel";
      panel.setAttribute("data-result-source", "error");
      const message = document.createElement("p");
      message.className = "alert";
      message.setAttribute("role", "alert");
      message.textContent = SAVED_FETCH_ERROR_MESSAGE;
      panel.appendChild(message);
      replaceResult(panel);
    }

    /** 今回作成した結果でなければ、対象月の保存済みシフトを取得して画面 3 に表示する（8.3 節）。 */
    function refreshResult() {
      const panel = document.getElementById("result-panel");
      const month = monthInput.value;
      const source = panel ? panel.getAttribute("data-result-source") : null;
      const resultMonth = panel ? panel.getAttribute("data-result-month") : null;
      if (!needsSavedRefetch(source, resultMonth, month)) {
        return;
      }
      fetch("/shift/saved?month=" + encodeURIComponent(month))
        .then(function (response) {
          if (!response.ok) {
            throw new Error("saved " + response.status);
          }
          return response.text();
        })
        .then(function (html) {
          // 応答を待つ間に月や画面が変わったときは、古い応答で表示を上書きしない
          if (monthInput.value !== month || currentStep !== 3) {
            return;
          }
          const template = document.createElement("template");
          template.innerHTML = html;
          replaceResult(template.content);
        })
        .catch(function () {
          if (monthInput.value === month && currentStep === 3) {
            showSavedFetchError();
          }
        });
    }

    prevButton.addEventListener("click", function () {
      if (currentStep > 1) {
        showStep(currentStep - 1);
      }
    });

    nextButton.addEventListener("click", function (event) {
      // 画面 1 の処理中に type を submit へ変えると、クリックの既定動作で送信されてしまうため、
      // 既定動作は常に止めて、送信は画面 2 のときだけ明示的に行う
      event.preventDefault();
      if (currentStep === 2) {
        // form.submit() は submit イベントを発火せず、個別変更の hidden 入力の組み直しが走らないため使わない
        form.requestSubmit();
      } else if (currentStep === 1) {
        showStep(2);
      }
    });

    stepButtons.forEach(function (button) {
      button.addEventListener("click", function () {
        showStep(Number(button.getAttribute("data-step")));
      });
    });

    function loadCalendar() {
      const month = monthInput.value;
      monthLabelElement.textContent = /^\d{4}-\d{2}$/.test(month) ? monthLabel(month) : month;
      monthSummary.textContent = "読み込み中…";
      monthSummary.classList.remove("error");
      fetch("/calendar?month=" + encodeURIComponent(month))
        .then(function (response) {
          if (!response.ok) {
            throw new Error("calendar " + response.status);
          }
          return response.json();
        })
        .then(function (data) {
          // 月を素早く切り替えたとき、古い月の応答で表示を上書きしないようにする
          if (monthInput.value !== month) {
            return;
          }
          monthSummary.textContent =
            "営業日 " + data.businessDayCount + " 日・祝日 " + data.holidayCount + " 日";
          document.dispatchEvent(new CustomEvent("calendar-loaded", { detail: data }));
        })
        .catch(function () {
          if (monthInput.value !== month) {
            return;
          }
          monthSummary.textContent = UNAVAILABLE_MESSAGE;
          monthSummary.classList.add("error");
          document.dispatchEvent(
            new CustomEvent("calendar-unavailable", { detail: { month: month } })
          );
        });
    }

    function changeMonth(delta) {
      if (!/^\d{4}-\d{2}$/.test(monthInput.value)) {
        return;
      }
      monthInput.value = shiftMonth(monthInput.value, delta);
      loadCalendar();
    }

    document.getElementById("prev-month-btn").addEventListener("click", function () {
      changeMonth(-1);
    });
    document.getElementById("next-month-btn").addEventListener("click", function () {
      changeMonth(1);
    });

    const initialStep = Number(app.getAttribute("data-initial-step"));
    showStep(initialStep >= 1 && initialStep <= 3 ? initialStep : 1);
    loadCalendar();
  });
})();
