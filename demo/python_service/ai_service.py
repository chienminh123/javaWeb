import pandas as pd
from sqlalchemy import create_engine
from sklearn.metrics.pairwise import cosine_similarity
from fastapi import FastAPI
import uvicorn

app = FastAPI()

# --- 1. CẤU HÌNH KẾT NỐI DATABASE (MySQL) ---
# Bạn thay đổi thông tin này cho đúng với máy của bạn
DB_USER = 'root'
DB_PASSWORD = ''
DB_HOST = 'localhost'
DB_PORT = '3306'
DB_NAME = 'me_va_be' 

# Chuỗi kết nối (Connection String)
DATABASE_URL = f"mysql+pymysql://{DB_USER}:{DB_PASSWORD}@{DB_HOST}:{DB_PORT}/{DB_NAME}"
engine = create_engine(DATABASE_URL)

# Biến toàn cục để lưu mô hình (cache lại để không phải tính toán liên tục)
user_similarity_df = None
last_trained_time = None

def load_data_and_train():
    global user_similarity_df
    
    # --- 2. TRUY VẤN DỮ LIỆU TỪ 3 BẢNG: ORDERS, ORDER_DETAIL, PRODUCT ---
    # Lưu ý: Tên bảng trong MySQL thường là snake_case (orders, order_detail) 
    # dù trong Java là CamelCase. Nếu lỗi, hãy kiểm tra lại tên bảng trong MySQL Workbench.
    query = """
    SELECT 
        o.user_id, 
        od.product_id, 
        SUM(od.quantity) as total_quantity
    FROM orders o
    JOIN order_detail od ON o.order_id = od.order_id
    GROUP BY o.user_id, od.product_id
    """
    
    print("Đang tải dữ liệu từ Database...")
    try:
        # Đọc dữ liệu vào DataFrame
        df = pd.read_sql(query, engine)
        
        if df.empty:
            print("Chưa có dữ liệu đơn hàng nào. AI sẽ không hoạt động.")
            return False

        # --- 3. TẠO MA TRẬN USER-ITEM ---
        # Dòng: User, Cột: Product, Giá trị: Số lượng đã mua
        user_item_matrix = df.pivot_table(index='user_id', columns='product_id', values='total_quantity').fillna(0)

        # --- 4. TÍNH ĐỘ TƯƠNG ĐỒNG (COLLABORATIVE FILTERING) ---
        user_similarity = cosine_similarity(user_item_matrix)
        user_similarity_df = pd.DataFrame(user_similarity, index=user_item_matrix.index, columns=user_item_matrix.index)
        
        print("Huấn luyện xong! Sẵn sàng gợi ý.")
        return True
    except Exception as e:
        print(f"Lỗi khi đọc dữ liệu: {e}")
        return False

# Gọi hàm huấn luyện lần đầu khi khởi động server
load_data_and_train()

# --- 5. API ĐỂ JAVA GỌI ---
@app.get("/recommend/{user_id}")
def get_recommendations(user_id: int):
    global user_similarity_df
    
    # Nếu chưa có mô hình hoặc user chưa từng mua gì
    if user_similarity_df is None or user_id not in user_similarity_df.index:
        # Trường hợp Cold Start: Trả về danh sách rỗng hoặc sản phẩm bán chạy nhất
        return {"user_id": user_id, "recommendations": []}

    try:
        # A. Tìm những người dùng giống user này nhất
        similar_users = user_similarity_df[user_id].sort_values(ascending=False).index[1:6] # Lấy top 5 người giống nhất
        
        suggested_products = {}
        
        # B. Xem những người đó mua gì
        # (Phần này query lại DB hoặc lưu cache matrix thì nhanh hơn, ở đây demo logic)
        for similar_user in similar_users:
            # Lấy list sản phẩm người đó đã mua (Query ngắn)
            query_check = f"""
            SELECT od.product_id, SUM(od.quantity) as qty
            FROM orders o
            JOIN order_detail od ON o.order_id = od.order_id
            WHERE o.user_id = {similar_user}
            GROUP BY od.product_id
            ORDER BY qty DESC
            LIMIT 3
            """
            products = pd.read_sql(query_check, engine)
            
            for _, row in products.iterrows():
                p_id = int(row['product_id'])
                # Cộng dồn điểm (đơn giản hóa)
                suggested_products[p_id] = suggested_products.get(p_id, 0) + 1
        
        # C. Sắp xếp theo điểm cao nhất
        sorted_suggestions = sorted(suggested_products, key=suggested_products.get, reverse=True)
        
        return {
            "user_id": user_id, 
            "recommendations": sorted_suggestions[:5] # Trả về 5 ID sản phẩm tốt nhất
        }
        
    except Exception as e:
        return {"error": str(e)}

@app.get("/retrain")
def force_retrain():
    # API này để Java gọi mỗi khi có đơn hàng mới hoặc Admin bấm nút "Cập nhật AI"
    success = load_data_and_train()
    return {"status": "success" if success else "failed"}

# Chạy server: uvicorn ai_service:app --reload --port 8000