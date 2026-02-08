import pandas as pd
from sqlalchemy import create_engine
from sklearn.metrics.pairwise import cosine_similarity
from fastapi import FastAPI
import uvicorn

app = FastAPI()

DB_USER = 'root'
DB_PASSWORD = '' 
DB_HOST = 'localhost' 
DB_PORT = '3306'
DB_NAME = 'me_va_be' 

DATABASE_URL = f"mysql+pymysql://{DB_USER}:{DB_PASSWORD}@{DB_HOST}:{DB_PORT}/{DB_NAME}"
engine = create_engine(DATABASE_URL)

user_similarity_df = None
last_trained_order_count = 0  
RETRAIN_THRESHOLD = 1         

def get_current_order_count():
    """Hàm kiểm tra tổng số đơn hàng hiện tại trong DB"""
    try:
        query = "SELECT COUNT(*) FROM orders"
        count = pd.read_sql(query, engine).iloc[0, 0]
        return int(count)
    except:
        return 0

def load_data_and_train():
    global user_similarity_df, last_trained_order_count
    
    print("⏳ Đang tiến hành huấn luyện lại model...")
    
    query = """
    SELECT 
        o.user_id, 
        od.product_id, 
        SUM(od.quantity) as total_quantity
    FROM orders o
    JOIN order_detail od ON o.order_id = od.order_id
    GROUP BY o.user_id, od.product_id
    """
    
    try:
        df = pd.read_sql(query, engine)
        
        if df.empty:
            print("⚠ Chưa có dữ liệu đơn hàng.")
            return False

        # Tạo ma trận User-Item
        user_item_matrix = df.pivot_table(index='user_id', columns='product_id', values='total_quantity').fillna(0)
        print("---giai đoạn tạo ma trận User-Item ---")
        print(user_item_matrix)

        # Tính toán độ tương đồng (Cosine Similarity)
        user_similarity = cosine_similarity(user_item_matrix)
        print("---giai đoạn tính toán độ tương đồng ---")
        print(user_similarity)

        user_similarity_df = pd.DataFrame(user_similarity, index=user_item_matrix.index, columns=user_item_matrix.index)
        print("---giai đoạn tạo bảng ---")
        print(user_similarity_df)
        
        # Cập nhật số lượng đơn hàng đã train
        last_trained_order_count = get_current_order_count()
       
        print(f"✅ Huấn luyện xong! (Tổng đơn hàng đã học: {last_trained_order_count})")
        return True
    except Exception as e:
        print(f"❌ Lỗi khi train: {e}")
        return False

# Chạy lần đầu khi khởi động
load_data_and_train()

@app.get("/recommend/{user_id}")
def get_recommendations(user_id: int):
    global user_similarity_df, last_trained_order_count
    print(f"--- Tính toán gợi ý cho User ID: {user_id} ---")
    
    current_count = get_current_order_count()
    new_orders = current_count - last_trained_order_count
    # tự động học lại nếu có đủ đơn hàng mới
    if new_orders >= RETRAIN_THRESHOLD:
        print(f"⚡ Phát hiện đủ {new_orders} đơn hàng mới. Đang tự động học lại...")
        load_data_and_train()

    # Nếu chưa có model hoặc lỗi
    if user_similarity_df is None or user_id not in user_similarity_df.index:
        return {"user_id": user_id, "recommendations": []}

    try:
        # Tìm người dùng tương tự
        similar_scores = user_similarity_df[user_id].sort_values(ascending=False)
        # Loại bỏ chính user khỏi danh sách
        similar_scores = similar_scores.iloc[1:]
        # Thu thập gợi ý từ người dùng tương tự
        suggested_products = {}
        # Lấy tối đa 10 người dùng tương tự 
        valid_neighbors = similar_scores[similar_scores > 0].index.tolist()[:10] # Lấy tối đa 10 người
        
        if not valid_neighbors:
            print("Không tìm thấy hàng xóm phù hợp -> Trả về rỗng ")
            return {"user_id": user_id, "recommendations": []} 
        
        # Lấy sản phẩm từ những người dùng tương tự
        for similar_user in valid_neighbors:
            query_check = f"""
            SELECT od.product_id, SUM(od.quantity) as qty
            FROM orders o
            JOIN order_detail od ON o.order_id = od.order_id
            WHERE o.user_id = {similar_user}
            GROUP BY od.product_id
            ORDER BY qty DESC
            LIMIT 8
            """
            # Lấy sản phẩm của user tương tự
            products = pd.read_sql(query_check, engine)
            # Cộng dồn sản phẩm vào gợi ý
            for _, row in products.iterrows():
                p_id = int(row['product_id'])
                # Cộng điểm gợi ý
                suggested_products[p_id] = suggested_products.get(p_id, 0) + 1
        # Sắp xếp sản phẩm theo điểm gợi ý
        sorted_suggestions = sorted(suggested_products, key=suggested_products.get, reverse=True)
        
        return {
            "user_id": user_id, 
            "recommendations": sorted_suggestions[:8] 
        }
        
    except Exception as e:
        print(f"Lỗi gợi ý: {e}")
        return {"user_id": user_id, "recommendations": []}


@app.get("/retrain")
def force_retrain():
    success = load_data_and_train()
    return {"status": "success" if success else "failed"}

# Chạy server: uvicorn ai_service:app --reload --port 8000
# Chạy java:  .\mvnw spring-boot:run
#localhost:    http://localhost:8080/User/index




