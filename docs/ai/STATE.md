# Project State

## Overview

CFC Lab Management đang ở Giai đoạn 0: phân tích và thiết kế khởi động. Repository hiện chưa chứa application implementation.

## Verified stack

- Chưa tìm thấy `pom.xml`, `build.gradle`, `package.json`, `vite.config.*` hoặc source Java/React.
- Yêu cầu dự án đề xuất Java 21 LTS/Spring Boot stable và React/Vite/JavaScript JSX; version cụ thể và hạ tầng thực tế chưa được xác minh từ source.
- MySQL là database chính cần đánh giá; chưa tạo database.

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

## In progress / blockers

- Chưa có code ứng dụng để migrate hoặc kiểm thử.
- Chưa thể tích hợp skill bên ngoài hoặc lập module map implementation trước khi có phê duyệt và source.

## Verification

- Đã kiểm tra cấu trúc repository và xác nhận thiếu manifest/source implementation.
- `git status` chưa chạy được vì thư mục hiện tại không được Git nhận diện là working tree hợp lệ.
- Chưa có build/test command khả dụng.
- Đã tham chiếu yêu cầu hệ thống Spring Boot chính thức; Spring Boot 4.1.2 hiện yêu cầu Java tối thiểu 17 và tương thích đến Java 27, nhưng chưa chốt version cho dự án.

## Next actions

1. LAB xác nhận open questions và mẫu biểu/quy trình.
2. Chốt Spring Boot version, Maven, MySQL và hạ tầng.
3. Chỉ sau phê duyệt Giai đoạn 0 mới bootstrap source và tạo module cần thiết.
