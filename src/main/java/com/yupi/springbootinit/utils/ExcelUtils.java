package com.yupi.springbootinit.utils;

import org.springframework.util.ResourceUtils;
import org.springframework.web.multipart.MultipartFile;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.read.listener.PageReadListener;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ExcelUtils {
    public static String excelToCsvString(MultipartFile multipartFile) {
        // 创建临时文件来保存Excel内容
        File tempFile;
        try {
            tempFile = File.createTempFile("excel-", ".tmp");
            multipartFile.transferTo(tempFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
//        File tempFile = null;
//        try {
//            tempFile = ResourceUtils.getFile("classpath:test_excel.xlsx");
//        } catch (FileNotFoundException e) {
//            throw new RuntimeException(e);
//        }
        // 准备一个列表来收集所有行的数据
        List<LinkedHashMap<Integer, String>> allRows = new ArrayList<>();
        // 判断文件类型并相应地进行处理
        String fileName = multipartFile.getOriginalFilename();
        if (fileName.endsWith(".csv")) {
            // 如果是CSV文件，直接读取并返回整个文件内容
            StringBuilder stringBuilder = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(tempFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line).append("\n");
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to read CSV file", e);
            }
            return stringBuilder.toString();
        } else {
            EasyExcel.read(tempFile, LinkedHashMap.class, new PageReadListener<LinkedHashMap<Integer, String>>(dataList -> {
                allRows.addAll(dataList);
            })).sheet().doRead();
        }

        // 将读取的数据转换成CSV格式
        StringBuilder csvOutput = new StringBuilder();

        for (LinkedHashMap<Integer, String> row : allRows) {
            // 将每一行的值加入CSV
            List<String> values = row.values().stream().filter(Objects::nonNull).collect(Collectors.toList());
            String joinedRow = String.join(",", values);
            csvOutput.append(joinedRow).append("\n");
        }
        return csvOutput.toString();
    }
    public static void main(String[] args) {
        excelToCsvString(null);
    }
}
