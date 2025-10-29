// src/main/resources/static/assets/js/admin_scripts.js

document.addEventListener("DOMContentLoaded", function () {
    const toggleButton = document.querySelector(".toggle-btn");
    const body = document.querySelector("body");

    // 1. Logic ẩn/hiện
    if (toggleButton) {
        toggleButton.addEventListener("click", function () {
            // Thêm/Xóa class 'sidebar-toggled' trên thẻ <body>
            body.classList.toggle("sidebar-toggled");

            // Tùy chọn: Lưu trạng thái Sidebar vào Local Storage
            if (body.classList.contains("sidebar-toggled")) {
                localStorage.setItem("sidebarState", "hidden");
            } else {
                localStorage.setItem("sidebarState", "visible");
            }
        });
    }

    // 2. Tải trạng thái Sidebar từ Local Storage (Để giữ trạng thái sau khi F5)
    const savedState = localStorage.getItem("sidebarState");
    if (savedState === "hidden") {
        body.classList.add("sidebar-toggled");
    }
});
