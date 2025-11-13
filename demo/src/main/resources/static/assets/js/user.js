document.addEventListener("DOMContentLoaded", function () {
  // ===============================================
  // 1. LOGIC HERO BANNER CAROUSEL (Sửa lại cấu trúc IIFE an toàn)
  // ===============================================
  (function () {
    const track = document.getElementById("carouselTrack");
    const slides = document.querySelectorAll(".mega-banner-slide");
    const dotsContainer = document.getElementById("carouselDots");
    const carousel = document.getElementById("megaBannerCarousel");

    let currentSlide = 0;
    const totalSlides = slides.length;
    const slideInterval = 3000;
    let autoSlideTimer = null;

    if (!track || !slides || totalSlides === 0) return;

    // Tạo chấm tròn
    const dots = [];
    slides.forEach((_, index) => {
      const dot = document.createElement("span");
      dot.classList.add("mega-banner__dot");
      if (index === 0) dot.classList.add("active");
      dots.push(dot);
      if (dotsContainer) {
        // Kiểm tra dotsContainer trước khi append
        dotsContainer.appendChild(dot);
      }
    });

    function goToSlide(n) {
      currentSlide = (n + totalSlides) % totalSlides;
      // Dùng công thức gốc của bạn
      const offset = -currentSlide * (100 / totalSlides);
      track.style.transform = `translateX(${offset}%)`;

      dots.forEach((dot, i) => {
        dot.classList.toggle("active", i === currentSlide);
      });
    }

    function nextSlide() {
      goToSlide(currentSlide + 1);
    }

    function startAutoSlide() {
      stopAutoSlide();
      autoSlideTimer = setInterval(nextSlide, slideInterval);
    }

    function stopAutoSlide() {
      if (autoSlideTimer) {
        clearInterval(autoSlideTimer);
        autoSlideTimer = null;
      }
    }

    startAutoSlide();

    if (carousel) {
      carousel.addEventListener("mouseenter", stopAutoSlide);
      carousel.addEventListener("mouseleave", startAutoSlide);
    }

    dots.forEach((dot, index) => {
      dot.addEventListener("click", () => {
        goToSlide(index);
        startAutoSlide();
      });
    });
  })(); // Kết thúc khối Banner

  // ===============================================
  // 2. LOGIC HOT DEAL CAROUSEL (SỬA LỖI NÚT KHÔNG BẤM ĐƯỢC)
  // ===============================================
  (function () {
    const hotTrack = document.getElementById("hotDealTrack");
    const hotPrev = document.getElementById("hotDealPrev");
    const hotNext = document.getElementById("hotDealNext");

    if (hotTrack && hotPrev && hotNext) {
      const hotCards = hotTrack.querySelectorAll(".product-card");
      const hotTotal = hotCards.length;
      const hotVisible = 4;
      let hotIndex = 0;

      function hotMoveTo(n) {
        const maxIndex = Math.ceil(hotTotal / hotVisible) - 1;

        // Vô hiệu hóa tính năng chuyển nếu chỉ có 1 trang
        if (maxIndex < 1) {
          hotPrev.style.display = "none";
          hotNext.style.display = "none";
          return;
        }

        hotIndex = Math.max(0, Math.min(n, maxIndex));

        const offset = -(hotIndex * 100);
        hotTrack.style.transform = `translateX(${offset}%)`;

        // === PHẦN SỬA LỖI QUAN TRỌNG NHẤT ===
        // GIỮ CHO NÚT CÓ THỂ BẤM ĐƯỢC MẶC DÙ CHÚNG TA ĐANG Ở TRANG CUỐI
        hotPrev.style.pointerEvents = hotIndex === 0 ? "none" : "auto";
        hotNext.style.pointerEvents = hotIndex === maxIndex ? "none" : "auto";

        // Dùng opacity để báo hiệu không thể chuyển tiếp
        hotPrev.style.opacity = hotIndex === 0 ? "0.5" : "1";
        hotNext.style.opacity = hotIndex === maxIndex ? "0.5" : "1";
      }

      // Khử trùng các sự kiện click trước khi gắn lại (an toàn hơn)
      hotNext.onclick = null;
      hotPrev.onclick = null;

      hotNext.addEventListener("click", () => hotMoveTo(hotIndex + 1));
      hotPrev.addEventListener("click", () => hotMoveTo(hotIndex - 1));

      hotMoveTo(0);
    }
  })(); // Kết thúc khối Hot Deal
});
