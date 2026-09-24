/**
 * シフト入力フォームの行を動的に追加・削除する機能を提供します。
 */
document.addEventListener("DOMContentLoaded", function () {
  const addRowBtn = document.getElementById("add-row-btn");
  const employeeRows = document.getElementById("employee-rows");

  function updateDataValue(select) {
    select.setAttribute("data-value", select.value);
  }

  function updateDeleteButtonState() {
    const rows = employeeRows.querySelectorAll("tr");
    const deleteButtons = employeeRows.querySelectorAll(".delete-row-btn");

    deleteButtons.forEach((btn) => {
      btn.disabled = rows.length === 1;
    });
  }

  // 初期化：既存の select の data-value を現在の value に設定
  employeeRows.querySelectorAll("select").forEach((select) => {
    updateDataValue(select);
  });

  // インデックスに欠番があると Spring MVC でリストをバインドできないため、削除後に振り直す
  function renumberInputIndices() {
    const rows = employeeRows.querySelectorAll("tr");

    rows.forEach((row, index) => {
      const inputs = row.querySelectorAll("input, select");

      inputs.forEach((input) => {
        input.name = input.name.replace(
          /employees\[\d+\]/,
          "employees[" + index + "]"
        );
      });
    });
  }

  addRowBtn.addEventListener("click", function () {
    const currentRowCount = employeeRows.querySelectorAll("tr").length;
    const newRow = document.createElement("tr");

    const nameCell = document.createElement("td");
    nameCell.setAttribute("data-label", "氏名");
    const nameInput = document.createElement("input");
    nameInput.type = "text";
    nameInput.name = "employees[" + currentRowCount + "].name";
    nameInput.placeholder = "氏名を入力";
    nameCell.appendChild(nameInput);

    const earlyCell = document.createElement("td");
    earlyCell.setAttribute("data-label", "早番希望");
    const earlySelect = document.createElement("select");
    earlySelect.name = "employees[" + currentRowCount + "].earlyWish";
    earlySelect.setAttribute("data-value", "");

    const emptyOption1 = document.createElement("option");
    emptyOption1.value = "";
    emptyOption1.textContent = "-- 未選択 --";

    const desiredOption1 = document.createElement("option");
    desiredOption1.value = "DESIRED";
    desiredOption1.textContent = "◎ 希望";

    const availableOption1 = document.createElement("option");
    availableOption1.value = "AVAILABLE";
    availableOption1.textContent = "○ 可能";

    const unavailableOption1 = document.createElement("option");
    unavailableOption1.value = "UNAVAILABLE";
    unavailableOption1.textContent = "× 不可";

    earlySelect.appendChild(emptyOption1);
    earlySelect.appendChild(desiredOption1);
    earlySelect.appendChild(availableOption1);
    earlySelect.appendChild(unavailableOption1);

    earlyCell.appendChild(earlySelect);

    const lateCell = document.createElement("td");
    lateCell.setAttribute("data-label", "遅番希望");
    const lateSelect = document.createElement("select");
    lateSelect.name = "employees[" + currentRowCount + "].lateWish";
    lateSelect.setAttribute("data-value", "");

    const emptyOption2 = document.createElement("option");
    emptyOption2.value = "";
    emptyOption2.textContent = "-- 未選択 --";

    const desiredOption2 = document.createElement("option");
    desiredOption2.value = "DESIRED";
    desiredOption2.textContent = "◎ 希望";

    const availableOption2 = document.createElement("option");
    availableOption2.value = "AVAILABLE";
    availableOption2.textContent = "○ 可能";

    const unavailableOption2 = document.createElement("option");
    unavailableOption2.value = "UNAVAILABLE";
    unavailableOption2.textContent = "× 不可";

    lateSelect.appendChild(emptyOption2);
    lateSelect.appendChild(desiredOption2);
    lateSelect.appendChild(availableOption2);
    lateSelect.appendChild(unavailableOption2);

    lateCell.appendChild(lateSelect);

    const deleteCell = document.createElement("td");
    const deleteBtn = document.createElement("button");
    deleteBtn.type = "button";
    deleteBtn.className = "delete-row-btn";
    deleteBtn.textContent = "削除";
    deleteCell.appendChild(deleteBtn);

    newRow.appendChild(nameCell);
    newRow.appendChild(earlyCell);
    newRow.appendChild(lateCell);
    newRow.appendChild(deleteCell);

    employeeRows.appendChild(newRow);

    updateDeleteButtonState();
  });

  employeeRows.addEventListener("click", function (event) {
    if (event.target.classList.contains("delete-row-btn")) {
      const row = event.target.closest("tr");
      if (row) {
        row.remove();
        renumberInputIndices();
        updateDeleteButtonState();
      }
    }
  });

  // select の変更時に data-value を更新
  employeeRows.addEventListener("change", function (event) {
    if (event.target.tagName === "SELECT") {
      updateDataValue(event.target);
    }
  });

  updateDeleteButtonState();
});
