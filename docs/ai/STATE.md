# Project State

## Overview

CFC Lab Management đã chuyển sang Giai đoạn 1 cho phạm vi authentication và user management. Các module nghiệp vụ khác chưa triển khai.

## Verified stack

- Backend: Maven + Spring Boot 4.1.2, Java 21 target, Spring Security, JPA, Validation, OAuth2 Resource Server JWT.
- Frontend: React 19 + Vite 6 + JavaScript/JSX + React Router.
- Dev database: H2 in-memory; MySQL profile đã xác minh với MySQL 8.4 container hiện tại.

## Current structure

- Frontend placeholder: `source/fe/`
- Backend placeholder: `source/be/`
- Project instructions: `AGENTS.md`, `source/fe/AGENTS.md`, `source/be/AGENTS.md`
- Project memory: `docs/ai/`
- Skill status: `docs/ai/SKILLS.md`

## Architecture direction

- Backend: modular monolith, package-by-feature; config/security và common chỉ dùng cho concerns thật sự dùng chung.
- Frontend: feature-based; API, UI, hooks và types thuộc feature, shared chỉ dành cho phần tái sử dụng rõ ràng.

## Completed

- Tạo khung thư mục frontend/backend và tài liệu AI-first.
- Tạo custom skill `.agents/skills/backend-api/SKILL.md`.
- Cập nhật kế hoạch gốc lên bản 1.1 và bổ sung PRD, RBAC, database, API conventions, architecture và roadmap sơ bộ.
- Hoàn tất deliverables tài liệu Giai đoạn 0; đang chờ LAB xác nhận, chưa chuyển sang code.
- Cài và validate 5 external skills; danh sách và phạm vi áp dụng nằm trong `docs/ai/SKILLS.md`.
- Tạo authentication bằng email, JWT cookie, refresh session server-side, logout và CSRF endpoint.
- Tạo user management Admin-only: list, create, update/lock; không có public registration.
- Tạo frontend login và quản lý user, dùng visual asset `guideline-assets/cfc-003.jpg`.

## In progress / blockers

- Backend đang chạy qua container Maven/JDK vì host không cài Java/Maven.
- Chưa có bộ integration test tự động; login và `/auth/me` đã smoke-test với MySQL thật.

## Verification

- Frontend `npm run build` đã pass.
- `npm audit --omit=dev` đã pass với 0 vulnerabilities.
- `git status` chưa chạy được vì thư mục hiện tại không được Git nhận diện là working tree hợp lệ.
- Backend `mvn test` chạy trong Docker Maven/JDK 21: pass.
- MySQL `cfc_lab` đã tạo schema `users` và `auth_sessions`; backend profile `mysql` khởi động thành công.
- Smoke test login email và `/auth/me`: HTTP 200.
- Đã tham chiếu yêu cầu hệ thống Spring Boot chính thức; Spring Boot 4.1.2 hiện yêu cầu Java tối thiểu 17 và tương thích đến Java 27, nhưng chưa chốt version cho dự án.

## Next actions

1. Kiểm thử refresh, logout, CSRF và Admin-only user endpoints trên browser.
2. Chọn migration tool (Flyway/Liquibase) trước khi đưa schema lên production.
