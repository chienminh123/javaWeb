// Chatbot
(function () {
  "use strict";

  class ChatBot {
    constructor() {
      this.isOpen = false;
      this.init();
    }

    init() {
      this.createWidget();
      this.attachEvents();
      this.sendInitialMessage();
    }

    createWidget() {
      const widget = document.createElement("div");
      widget.className = "chatbot-widget";
      widget.innerHTML = `
        <div class="chatbot-window" id="chatbotWindow">
          <div class="chatbot-header">
            <h3>
              <span class="header-icon">🤖</span>
              Trợ lý tư vấn
            </h3>
            <button class="close-btn" id="chatbotClose">
              ✕
            </button>
          </div>
          <div class="chatbot-messages" id="chatbotMessages"></div>
          <div class="chatbot-input-area">
            <input 
              type="text" 
              id="chatbotInput" 
              placeholder="Nhập câu hỏi của bạn..."
              autocomplete="off"
            />
            <button id="chatbotSend">
              ➤
            </button>
          </div>
        </div>
        <button class="chatbot-button" id="chatbotButton" title="Chat với chúng tôi">
          <span class="chatbot-icon">💬</span>
          <span class="chatbot-tooltip">Chat với chúng tôi!</span>
        </button>
      `;
      document.body.appendChild(widget);
    }

    attachEvents() {
      const button = document.getElementById("chatbotButton");
      const closeBtn = document.getElementById("chatbotClose");
      const sendBtn = document.getElementById("chatbotSend");
      const input = document.getElementById("chatbotInput");
      const window = document.getElementById("chatbotWindow");

      button.addEventListener("click", () => this.toggle());
      closeBtn.addEventListener("click", () => this.close());

      sendBtn.addEventListener("click", () => this.sendMessage());
      input.addEventListener("keypress", (e) => {
        if (e.key === "Enter") {
          this.sendMessage();
        }
      });

      window.addEventListener("click", (e) => {
        if (e.target === window) {
          // this.close();
        }
      });
    }

    toggle() {
      this.isOpen = !this.isOpen;
      const window = document.getElementById("chatbotWindow");
      if (this.isOpen) {
        window.classList.add("active");
        document.getElementById("chatbotInput").focus();
      } else {
        window.classList.remove("active");
      }
    }

    close() {
      this.isOpen = false;
      document.getElementById("chatbotWindow").classList.remove("active");
    }

    sendInitialMessage() {}

    async sendMessage() {
      const input = document.getElementById("chatbotInput");
      const message = input.value.trim();

      if (!message) return;

      this.addMessage(message, "user");
      input.value = "";
      input.disabled = true;
      document.getElementById("chatbotSend").disabled = true;

      this.showTyping();

      try {
        const response = await fetch("/api/chatbot/message", {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify({ message: message }),
        });

        const data = await response.json();
        this.hideTyping();
        this.handleResponse(data);
      } catch (error) {
        console.error("Error:", error);
        this.hideTyping();
        this.addMessage("Xin lỗi, có lỗi xảy ra. Vui lòng thử lại sau.", "bot");
      } finally {
        input.disabled = false;
        document.getElementById("chatbotSend").disabled = false;
        input.focus();
      }
    }

    handleResponse(response) {
      // Add bot message
      this.addMessage(response.message, "bot");

      if (
        response.type === "products" &&
        response.products &&
        response.products.length > 0
      ) {
        this.addProducts(response.products);
      }
      if (
        response.type === "quick_replies" &&
        response.quickReplies &&
        response.quickReplies.length > 0
      ) {
        this.addQuickReplies(response.quickReplies);
      }
    }

    addMessage(text, type) {
      const messages = document.getElementById("chatbotMessages");
      const messageDiv = document.createElement("div");
      messageDiv.className = `chatbot-message ${type}`;

      const avatar = type === "bot" ? "🤖" : "👤";

      messageDiv.innerHTML = `
        <div class="avatar">${avatar}</div>
        <div class="content">${this.formatMessage(text)}</div>
      `;

      messages.appendChild(messageDiv);
      this.scrollToBottom();
    }

    formatMessage(text) {
      return text.replace(/\n/g, "<br>");
    }

    addProducts(products) {
      const messages = document.getElementById("chatbotMessages");
      const productsDiv = document.createElement("div");
      productsDiv.className = "chatbot-products";

      products.forEach((product) => {
        const productItem = document.createElement("a");
        productItem.href =
          product.productUrl || `/product/${product.productId}`;
        productItem.className = "chatbot-product-item";

        const imageUrl = product.imageUrl || "/img/no-image.png";
        const price = this.formatPrice(product.price);
        const discount =
          product.discount > 0
            ? `<span class="product-discount">${this.formatPrice(
                product.price / (1 - product.discount / 100)
              )}</span>`
            : "";

        productItem.innerHTML = `
          <img src="${imageUrl}" alt="${product.productName}" onerror="this.src='/img/no-image.png'">
          <div class="product-info">
            <div class="product-name">${product.productName}</div>
            <div class="product-price">
              ${price}
              ${discount}
            </div>
          </div>
        `;

        productsDiv.appendChild(productItem);
      });

      messages.appendChild(productsDiv);
      this.scrollToBottom();
    }

    addQuickReplies(quickReplies) {
      const messages = document.getElementById("chatbotMessages");
      const repliesDiv = document.createElement("div");
      repliesDiv.className = "chatbot-quick-replies";

      quickReplies.forEach((reply) => {
        const replyBtn = document.createElement("button");
        replyBtn.className = "chatbot-quick-reply";
        replyBtn.textContent = reply.text;
        replyBtn.addEventListener("click", () => {
          document.getElementById("chatbotInput").value =
            reply.payload || reply.text;
          this.sendMessage();
        });

        repliesDiv.appendChild(replyBtn);
      });

      messages.appendChild(repliesDiv);
      this.scrollToBottom();
    }

    showTyping() {
      const messages = document.getElementById("chatbotMessages");
      const typingDiv = document.createElement("div");
      typingDiv.className = "chatbot-message bot";
      typingDiv.id = "typingIndicator";
      typingDiv.innerHTML = `
        <div class="avatar">🤖</div>
        <div class="content">
          <div class="typing-indicator">
            <span></span>
            <span></span>
            <span></span>
          </div>
        </div>
      `;
      messages.appendChild(typingDiv);
      this.scrollToBottom();
    }

    hideTyping() {
      const typing = document.getElementById("typingIndicator");
      if (typing) {
        typing.remove();
      }
    }

    formatPrice(price) {
      return new Intl.NumberFormat("vi-VN", {
        style: "currency",
        currency: "VND",
      }).format(price);
    }

    scrollToBottom() {
      const messages = document.getElementById("chatbotMessages");
      messages.scrollTop = messages.scrollHeight;
    }
  }

  // Initialize chatbot when DOM is ready
  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", () => {
      console.log("🤖 Chatbot widget đang khởi tạo...");
      new ChatBot();
      console.log("✅ Chatbot widget đã sẵn sàng!");
    });
  } else {
    console.log("🤖 Chatbot widget đang khởi tạo...");
    new ChatBot();
    console.log("✅ Chatbot widget đã sẵn sàng!");
  }
})();
