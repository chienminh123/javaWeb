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

    const profileDropdown = document.querySelector(".user-profile.dropdown");
    const dropdownMenu = document.querySelector(
        ".user-profile.dropdown .dropdown-menu"
    );
    const dropdownArrow = document.querySelector(
        ".user-profile.dropdown .dropdown-arrow"
    );

    if (profileDropdown && dropdownMenu && dropdownArrow) {
        const profileLink = document.querySelector(".profile-link");
        if (profileLink) {
            profileLink.addEventListener("click", function (event) {
                event.preventDefault();
                const isHidden =
                    dropdownMenu.style.display === "none" ||
                    dropdownMenu.style.display === "";

                if (isHidden) {
                    dropdownMenu.style.display = "block";
                    dropdownArrow.style.transform = "rotate(180deg)";
                } else {
                    dropdownMenu.style.display = "none";
                    dropdownArrow.style.transform = "rotate(0deg)";
                }
                event.stopPropagation();
            });
        }

        // Đóng dropdown khi click ra ngoài (quan trọng)
        document.addEventListener("click", function (event) {
            if (
                dropdownMenu.style.display === "block" &&
                !profileDropdown.contains(event.target)
            ) {
                dropdownMenu.style.display = "none";
                dropdownArrow.style.transform = "rotate(0deg)";
            }
        });
    }
});
