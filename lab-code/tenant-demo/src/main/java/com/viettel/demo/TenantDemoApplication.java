package com.viettel.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/*
 * ==============================================================
 * Entry point của Spring Boot application
 * ==============================================================
 *
 * [Mục tiêu]
 * Khởi tạo Spring Boot application. Đây là file đầu tiên
 * bạn cần tạo khi init project.
 *
 * [Nhiệm vụ của tôi]
 * 1. Thêm annotation đánh dấu đây là Spring Boot application.
 * 2. Viết hàm main để khởi chạy.
 *
 * [Kiến thức cần tự research]
 * - @SpringBootApplication annotation
 * - SpringApplication.run()
 * - Spring Initializr: https://start.spring.io
 *   (Chọn: Java 17+, Spring Web, Spring Data JPA,
 *    PostgreSQL Driver, Flyway Migration)
 *
 * ==============================================================
 */
@SpringBootApplication
public class TenantDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(TenantDemoApplication.class, args);
    }
}
