package com.swu.bookkeeping.util;

import com.swu.bookkeeping.model.Bill;
import com.swu.bookkeeping.model.Category;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ExcelFileUtil {
    private static final String DATA_DIR = "data";
    private static final String BILLS_FILE = "bills.xlsx";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 获取或创建存储文件（如 data/bills.xlsx）
     * @return File对象
     */
    private static File getDataFile() {
        // 创建data目录（如果不存在）
        File dataDir = new File(DATA_DIR);
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }

        // 创建文件路径
        return new File(dataDir, BILLS_FILE);
    }

    /**
     * 从Excel文件读取所有账单数据到List<Bill>
     * @return 账单列表
     */
    public static List<Bill> readBillsFromFile() {
        return readBillsFromFile(getDataFile());
    }

    /**
     * 从指定Excel文件读取所有账单数据到List<Bill>
     * @param file 文件对象
     * @return 账单列表
     */
    public static List<Bill> readBillsFromFile(File file) {
        // 如果文件不存在，返回空列表
        if (!file.exists()) {
            System.out.println("File does not exist, returning empty list");
            return new ArrayList<>();
        }

        // 检查文件是否为空
        if (file.length() == 0) {
            System.out.println("File is empty, returning empty list");
            return new ArrayList<>();
        }

        List<Bill> bills = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);

            // 跳过标题行，从第二行开始读取数据
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                Bill bill = new Bill();

                // 读取各列数据 (增强错误处理)
                try {
                    // ID
                    Cell idCell = row.getCell(0);
                    if (idCell != null) {
                        if (idCell.getCellType() == CellType.NUMERIC) {
                            bill.setId((long) idCell.getNumericCellValue());
                        } else if (idCell.getCellType() == CellType.STRING) {
                            try {
                                bill.setId(Long.parseLong(idCell.getStringCellValue()));
                            } catch (NumberFormatException e) {
                                // ID格式不正确，跳过
                            }
                        }
                    }

                    // Amount
                    Cell amountCell = row.getCell(1);
                    if (amountCell != null) {
                        if (amountCell.getCellType() == CellType.NUMERIC) {
                            bill.setAmount(BigDecimal.valueOf(amountCell.getNumericCellValue()));
                        } else if (amountCell.getCellType() == CellType.STRING) {
                            try {
                                bill.setAmount(new BigDecimal(amountCell.getStringCellValue()));
                            } catch (NumberFormatException e) {
                                // 金额格式不正确
                            }
                        }
                    }

                    // Type
                    Cell typeCell = row.getCell(2);
                    if (typeCell != null && typeCell.getCellType() == CellType.STRING) {
                        try {
                            bill.setBilltype(Bill.BillType.valueOf(typeCell.getStringCellValue().toUpperCase()));
                        } catch (IllegalArgumentException e) {
                            System.err.println("Invalid bill type: " + typeCell.getStringCellValue());
                        }
                    }

                    // Currency
                    Cell currencyCell = row.getCell(3);
                    if (currencyCell != null && currencyCell.getCellType() == CellType.STRING) {
                        bill.setCurrency(currencyCell.getStringCellValue());
                    }

                    // Remark
                    Cell remarkCell = row.getCell(4);
                    if (remarkCell != null && remarkCell.getCellType() == CellType.STRING) {
                        bill.setRemark(remarkCell.getStringCellValue());
                    }

                    // Create Time
                    Cell createTimeCell = row.getCell(5);
                    if (createTimeCell != null) {
                        try {
                            if (createTimeCell.getCellType() == CellType.STRING) {
                                LocalDateTime createTime = LocalDateTime.parse(
                                        createTimeCell.getStringCellValue(), DATE_FORMATTER);
                                bill.setCreateTime(createTime);
                            } else if (createTimeCell.getCellType() == CellType.NUMERIC) {
                                // 处理Excel日期格式
                                bill.setCreateTime(LocalDateTime.now()); // 简化处理
                            }
                        } catch (Exception e) {
                            System.err.println("Error parsing create time: " + e.getMessage());
                        }
                    }

                    // Original Amount
                    Cell originalAmountCell = row.getCell(6);
                    if (originalAmountCell != null) {
                        if (originalAmountCell.getCellType() == CellType.NUMERIC) {
                            bill.setOriginalAmount(BigDecimal.valueOf(originalAmountCell.getNumericCellValue()));
                        } else if (originalAmountCell.getCellType() == CellType.STRING) {
                            try {
                                bill.setOriginalAmount(new BigDecimal(originalAmountCell.getStringCellValue()));
                            } catch (NumberFormatException e) {
                                // 原始金额格式不正确
                            }
                        }
                    }

                    // Original Currency
                    Cell originalCurrencyCell = row.getCell(7);
                    if (originalCurrencyCell != null && originalCurrencyCell.getCellType() == CellType.STRING) {
                        bill.setOriginalCurrency(originalCurrencyCell.getStringCellValue());
                    }


                    // Exchange Rate (第9列)
                    Cell exchangeRateCell = row.getCell(9);
                    if (exchangeRateCell != null) {
                        if (exchangeRateCell.getCellType() == CellType.NUMERIC) {
                            bill.setExchangeRate(BigDecimal.valueOf(exchangeRateCell.getNumericCellValue()));
                        } else if (exchangeRateCell.getCellType() == CellType.STRING) {
                            try {
                                bill.setExchangeRate(new BigDecimal(exchangeRateCell.getStringCellValue()));
                            } catch (NumberFormatException e) {
                                // 汇率格式不正确
                            }
                        }
                    }

                    // Exchange Time (第10列)
                    Cell exchangeTimeCell = row.getCell(10);
                    if (exchangeTimeCell != null) {
                        try {
                            if (exchangeTimeCell.getCellType() == CellType.STRING) {
                                LocalDateTime exchangeTime = LocalDateTime.parse(
                                        exchangeTimeCell.getStringCellValue(), DATE_FORMATTER);
                                bill.setExchangeTime(exchangeTime);
                            } else if (exchangeTimeCell.getCellType() == CellType.NUMERIC) {
                                // 处理Excel日期格式
                                bill.setExchangeTime(LocalDateTime.now()); // 简化处理
                            }
                        } catch (Exception e) {
                            System.err.println("Error parsing exchange time: " + e.getMessage());
                        }
                    }

                    // Tags (第11列)
                    Cell tagsCell = row.getCell(11);
                    if (tagsCell != null && tagsCell.getCellType() == CellType.STRING) {
                        bill.setTags(tagsCell.getStringCellValue());
                    }



                    // 创建默认分类（因为Excel中不包含完整的分类对象）
                    Category category = new Category();
                    category.setId(1L); // 默认分类ID
                    bill.setCategory(category);

                    bills.add(bill);
                } catch (Exception e) {
                    System.err.println("Error reading row " + i + ": " + e.getMessage());
                    // 跳过有问题的行，继续处理下一行
                }
            }

            return bills;
        } catch (Exception e) {
            System.err.println("Error reading bills from file: " + e.getMessage());
            System.err.println("File may be corrupted, returning empty list");
            e.printStackTrace();
            // 文件损坏时返回空列表
            return new ArrayList<>();
        }
    }

    /**
     * 将List<Bill>写入Excel并保存到文件
     * @param bills 账单列表
     */
    public static synchronized void writeBillsToFile(List<Bill> bills) {
        writeBillsToFile(bills, getDataFile());
    }

    /**
     * 将List<Bill>写入Excel并保存到指定文件
     * @param bills 账单列表
     * @param file 文件对象
     */
    public static synchronized void writeBillsToFile(List<Bill> bills, File file) {
        System.out.println("Writing to file: " + file.getAbsolutePath());
        System.out.println("Writing " + bills.size() + " bills to file");

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Bills");

            // 创建标题行
            Row headerRow = sheet.createRow(0);
            String[] headers = {"ID", "Amount", "Type", "Currency", "Remark", "Create Time",
                    "Original Amount", "Original Currency", "Category", "Exchange Rate", "Exchange Time", "Tags"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);

                // 设置标题样式
                CellStyle headerStyle = workbook.createCellStyle();
                Font font = workbook.createFont();
                font.setBold(true);
                headerStyle.setFont(font);
                cell.setCellStyle(headerStyle);
            }

            // 填充数据行
            int rowNum = 1;
            for (Bill bill : bills) {
                Row row = sheet.createRow(rowNum++);

                // ID
                Cell idCell = row.createCell(0);
                if (bill.getId() != null) {
                    idCell.setCellValue(bill.getId());
                }

                // Amount
                Cell amountCell = row.createCell(1);
                if (bill.getAmount() != null) {
                    amountCell.setCellValue(bill.getAmount().doubleValue());
                }

                // Type
                Cell typeCell = row.createCell(2);
                if (bill.getBilltype() != null) {
                    typeCell.setCellValue(bill.getBilltype().toString());
                }

                // Currency
                Cell currencyCell = row.createCell(3);
                if (bill.getCurrency() != null) {
                    currencyCell.setCellValue(bill.getCurrency());
                }

                // Remark
                Cell remarkCell = row.createCell(4);
                if (bill.getRemark() != null) {
                    remarkCell.setCellValue(bill.getRemark());
                }

                // Create Time
                Cell createTimeCell = row.createCell(5);
                if (bill.getCreateTime() != null) {
                    createTimeCell.setCellValue(bill.getCreateTime().format(DATE_FORMATTER));
                }

                // Original Amount
                Cell originalAmountCell = row.createCell(6);
                if (bill.getOriginalAmount() != null) {
                    originalAmountCell.setCellValue(bill.getOriginalAmount().doubleValue());
                }

                // Original Currency
                Cell originalCurrencyCell = row.createCell(7);
                if (bill.getOriginalCurrency() != null) {
                    originalCurrencyCell.setCellValue(bill.getOriginalCurrency());
                }

                // Category
                Cell categoryCell = row.createCell(8);
                if (bill.getCategory() != null && bill.getCategory().getName() != null) {
                    categoryCell.setCellValue(bill.getCategory().getName());
                }

                Cell exchangeRateCell = row.createCell(9);
                if (bill.getExchangeRate() != null) {
                    exchangeRateCell.setCellValue(bill.getExchangeRate().doubleValue());
                }

                // Exchange Time (第10列)
                Cell exchangeTimeCell = row.createCell(10);
                if (bill.getExchangeTime() != null) {
                    exchangeTimeCell.setCellValue(bill.getExchangeTime().format(DATE_FORMATTER));
                }

                // Tags (第11列)
                Cell tagsCell = row.createCell(11);
                if (bill.getTags() != null) {
                    tagsCell.setCellValue(bill.getTags());
                }



            }

            // 自动调整列宽
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // 写入文件
            try (FileOutputStream fos = new FileOutputStream(file)) {
                workbook.write(fos);
            }

            System.out.println("Successfully wrote bills to file");
        } catch (IOException e) {
            System.err.println("Error writing bills to file: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
