package com.alibaba.cloud.ai;

/**
 * @author HeYQ
 * @since 2024-12-29 23:09
 */

public class Main {
    public static void main(String[] args) {
        // 创建Tesseract实例
        ITesseract instance = new Tesseract();

        // 设置Tesseract数据路径（tessdata文件夹的路径）
        instance.setDatapath("path/to/tessdata");

        // 设置语言（例如中文：chi_sim，英文：eng）
        instance.setLanguage("eng");

        try {
            // 读取图像文件
            File imgFile = new File("path/to/image.png");

            // 从图像中提取文本
            String result = instance.doOCR(imgFile);

            // 输出结果
            System.out.println(result);
        } catch (TesseractException e) {
            e.printStackTrace();
        }
    }
}