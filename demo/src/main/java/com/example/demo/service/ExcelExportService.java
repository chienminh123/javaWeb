package com.example.demo.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import com.example.demo.model.Product;
import com.example.demo.model.Sizes;

@Service
public class ExcelExportService {

    // 1. XUẤT BÁO CÁO TỒN KHO (Cho trang tonkho.html)
    public ByteArrayInputStream exportInventory(List<Product> products) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Ton_Kho_Hien_Tai");

            // Tạo Header
            Row headerRow = sheet.createRow(0);
            String[] columns = {"STT", "Nhà Cung Cấp", "Tên Sản Phẩm", "Thể Loại", "Chi tiết Size & SL Tồn"};
            
            // Style cho Header in đậm
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            // Điền dữ liệu
            int rowIdx = 1;
            for (int i = 0; i < products.size(); i++) {
                Product p = products.get(i);
                Row row = sheet.createRow(rowIdx++);

                row.createCell(0).setCellValue(i + 1);
                row.createCell(1).setCellValue(p.getProvider().getProviderName());
                row.createCell(2).setCellValue(p.getProductName());
                row.createCell(3).setCellValue(p.getGenre().getGenreName());

                // Gom các size lại thành 1 chuỗi (VD: "S: 10, M: 5")
                StringBuilder sizeInfo = new StringBuilder();
                if (p.getSizes() != null) {
                    for (Sizes s : p.getSizes()) {
                        sizeInfo.append(s.getSizeName()).append(": ").append(s.getQuantity()).append(" | ");
                    }
                }
                row.createCell(4).setCellValue(sizeInfo.toString());
            }

            // Tự động chỉnh độ rộng cột
            for (int i = 0; i < columns.length; i++) sheet.autoSizeColumn(i);

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Lỗi tạo file Excel: " + e.getMessage());
        }
    }

    // 2. XUẤT PHIẾU KIỂM KÊ CHI TIẾT (Cho trang inventory.html)
    public ByteArrayInputStream exportStocktakeTemplate(List<Product> products) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Phieu_Kiem_Ke");

            Row headerRow = sheet.createRow(0);
            // Cột "Thực Tế" và "Ghi Chú" để trống cho người dùng điền
            String[] columns = {"STT", "Nhà Cung Cấp", "Tên Sản Phẩm", "Size", "Tồn Hệ Thống", "Tồn Thực Tế (Điền tay)", "Ghi chú"};
            
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            int stt = 1;
            for (Product p : products) {
                if (p.getSizes() != null) {
                    for (Sizes s : p.getSizes()) {
                        Row row = sheet.createRow(rowIdx++);
                        row.createCell(0).setCellValue(stt++);
                        row.createCell(1).setCellValue(p.getProvider().getProviderName());
                        row.createCell(2).setCellValue(p.getProductName());
                        row.createCell(3).setCellValue(s.getSizeName());
                        row.createCell(4).setCellValue(s.getQuantity()); 
                        row.createCell(5).setCellValue(""); // Trống để điền
                        row.createCell(6).setCellValue(""); // Trống ghi chú
                    }
                }
            }
            
            for (int i = 0; i < columns.length; i++) sheet.autoSizeColumn(i);

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Lỗi tạo file Excel: " + e.getMessage());
        }
    }
}