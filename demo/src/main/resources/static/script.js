const productSuggestions = /*[[${productSuggestions}]]*/ {};
function submitForm() {
  const form = document.getElementById("multiProductForm");
  const formData = new FormData(form);

  console.clear();
  console.log("BẮT ĐẦU GỬI FORM - KIỂM TRA DỮ LIỆU");

  // Log từng dòng sản phẩm
  let rowIndex = 0;
  document
    .querySelectorAll("#productTable tbody tr:not([style*='display: none'])")
    .forEach((row) => {
      console.log(`--- DÒNG ${rowIndex + 1} ---`);

      const productName = row.querySelector('input[name="productName"]')?.value;
      const providerId = row.querySelector('select[name="providerId"]')?.value;
      const genreId = row.querySelector('select[name="genreId"]')?.value;
      const basisPrice = row.querySelector('input[name="basisPrice"]')?.value;
      const description = row.querySelector(
        'textarea[name="description"]'
      )?.value;
      const imageFile =
        row.querySelector('input[name="images"]')?.files[0]?.name ||
        "Không có ảnh";

      console.log("Tên SP:", productName);
      console.log("NCC ID:", providerId);
      console.log("Thể loại ID:", genreId);
      console.log("Giá gốc:", basisPrice);
      console.log("Mô tả:", description);
      console.log("Ảnh:", imageFile);

      // Log size
      console.log("Size & SL:");
      row.querySelectorAll(".size-row").forEach((sizeRow, idx) => {
        const sizeName = sizeRow.querySelector('input[name="sizeName"]')?.value;
        const qty = sizeRow.querySelector('input[name="quantity"]')?.value;
        console.log(`  ${idx + 1}. ${sizeName} → ${qty}`);
      });

      rowIndex++;
    });

  // Log FormData (để thấy mảng)
  console.log("\nFormData (gửi lên server):");
  for (let [key, value] of formData.entries()) {
    if (value instanceof File) {
      console.log(`${key}: [File] ${value.name} (${value.size} bytes)`);
    } else {
      console.log(`${key}: ${value}`);
    }
  }

  // Kiểm tra required
  let valid = true;
  form.querySelectorAll("select[required], input[required]").forEach((el) => {
    if (!el.value) {
      console.error("LỖI: Trường bắt buộc trống →", el);
      valid = false;
    }
  });

  if (!valid) {
    alert("Vui lòng điền đầy đủ các trường bắt buộc!");
    return;
  }

  console.log("DỮ LIỆU HỢP LỆ → GỬI FORM...");
  form.submit();
}

function addProductRow() {
  const tbody = document.querySelector("#productTable tbody");
  const template = tbody.querySelector(".product-row").cloneNode(true);
  template.style.display = "";

  // Reset tất cả input/select
  template.querySelectorAll("input, select, textarea").forEach((el) => {
    if (el.type === "file") el.value = "";
    else if (el.tagName === "SELECT") {
      el.selectedIndex = 0; // Chọn option đầu (disabled)
    } else {
      el.value = "";
    }
  });

  // Reset size
  const container = template.querySelector(".sizes-container");
  container.innerHTML = `<div class="size-row">
          <input type="text" name="sizeName" placeholder="S" style="width:40px" />
          <input type="number" name="quantity" placeholder="0" min="0" style="width:50px" />
          <button type="button" class="add-btn" onclick="addSizeRow(this)">+</button>
        </div>`;

  tbody.appendChild(template);
}

function addSizeRow(btn) {
  const container = btn.closest(".sizes-container");
  const row = document.createElement("div");
  row.className = "size-row";
  row.innerHTML = `
            <input type="text" name="sizeName" placeholder="M" style="width:40px" />
            <input type="number" name="quantity" placeholder="0" min="0" style="width:50px" />
            <button type="button" class="remove-size" onclick="this.parentElement.remove()">−</button>
        `;
  container.appendChild(row);
}

function removeRow(btn) {
  btn.closest("tr").remove();
}

function openProviderModal() {
  document.getElementById("providerModal").style.display = "flex";
}
function openGenreModal() {
  document.getElementById("genreModal").style.display = "flex";
}
function closeModal(id) {
  document.getElementById(id).style.display = "none";
}

function getCsrfToken() {
  const token = document
    .querySelector('meta[name="csrf-token"]')
    .getAttribute("content");
  const header = document
    .querySelector('meta[name="csrf-header"]')
    .getAttribute("content");
  return { token, header };
}

function saveProvider() {
  const { token, header } = getCsrfToken();
  const data = {
    providerName: document.getElementById("newProviderName").value.trim(),
    providerEmail: document.getElementById("newProviderEmail").value.trim(),
    providerPhone: document.getElementById("newProviderPhone").value.trim(),
    providerAddress: document.getElementById("newProviderAddress").value.trim(),
  };

  if (!data.providerName) {
    alert("Vui lòng nhập tên nhà cung cấp!");
    return;
  }

  fetch("/Admin/addProvider", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      [header]: token,
    },
    body: JSON.stringify(data),
  })
    .then((res) => {
      if (!res.ok) throw new Error("Lưu thất bại: " + res.status);
      return res.json();
    })
    .then((provider) => {
      document.querySelectorAll('select[name="providerId"]').forEach((sel) => {
        const opt = new Option(provider.providerName, provider.providerId);
        sel.add(opt);
        sel.value = provider.providerId;
      });
      closeModal("providerModal");
      document
        .querySelectorAll("#providerModal input")
        .forEach((input) => (input.value = ""));
    })
    .catch((err) => alert("Lỗi: " + err.message));
}

function saveGenre() {
  const { token, header } = getCsrfToken();
  const name = document.getElementById("newGenre").value.trim();

  if (!name) {
    alert("Vui lòng nhập thể loại!");
    return;
  }

  fetch("/Admin/addGenre", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      [header]: token,
    },
    body: JSON.stringify({ genre: name }),
  })
    .then((res) => {
      if (!res.ok) throw new Error("Lưu thất bại");
      return res.text();
    })
    .then(() => {
      document.querySelectorAll('select[name="genre"]').forEach((sel) => {
        const opt = new Option(name, name);
        sel.add(opt);
        sel.value = name;
      });
      closeModal("genreModal");
      document.getElementById("newGenre").value = "";
    })
    .catch((err) => alert("Lỗi: " + err.message));
}

function loadProductSuggestions(select) {
  const providerId = select.value;
  const row = select.closest("tr");
  const input = row.querySelector('input[name="productName"]');
  input.dataset.suggestions = JSON.stringify(
    productSuggestions[providerId] || []
  );
}

function showSuggestions(input) {
  const suggestions = JSON.parse(input.dataset.suggestions || "[]");
  const val = input.value.toLowerCase();
  const container = input.nextElementSibling;
  container.innerHTML = "";
  if (!val) return;

  suggestions
    .filter((p) => p.productName.toLowerCase().includes(val))
    .slice(0, 5)
    .forEach((p) => {
      const div = document.createElement("div");
      div.innerHTML = `<strong>${p.productName}</strong><br><small>Mã: ${p.productId} | Giá: ${p.basisPrice}</small>`;
      div.onclick = () => {
        input.value = p.productName;
        input.closest("tr").querySelector('select[name="genre"]').value =
          p.genre;
        input.closest("tr").querySelector('input[name="basisPrice"]').value =
          p.basisPrice;
        container.innerHTML = "";
      };
      container.appendChild(div);
    });
}

window.onload = () => addProductRow();
