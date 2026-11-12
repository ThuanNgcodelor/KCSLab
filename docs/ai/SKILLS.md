# Skills status

## Đã cài trong project

| Skill | Nguồn | Đường dẫn |
|---|---|---|
| `frontend-design` | Anthropic | `.agents/skills/frontend-design/` |
| `react-best-practices` | Vercel | `.agents/skills/react-best-practices/` |
| `web-design-guidelines` | Vercel | `.agents/skills/web-design-guidelines/` |
| `java-spring-best-practices` | Cosbort | `.agents/skills/java-spring-best-practices/` |
| `backend-api` | Nội bộ dự án | `.agents/skills/backend-api/` |

Tất cả `SKILL.md` còn lại đã vượt qua kiểm tra format. Skill Vercel có frontmatter name `vercel-react-best-practices`; thư mục project vẫn giữ tên dễ nhớ `react-best-practices`.

## Đã loại bỏ

- `spring-boot` của SivaLabs: trùng phạm vi với `java-spring-best-practices`, không cần giữ cả hai cho dự án này.
- `vercel-composition-patterns`: chưa cài vì là skill tùy chọn và chưa cần.

## Lưu ý áp dụng

- Chỉ dùng skill phù hợp với request; không tải toàn bộ references vào context.
- `java-spring-best-practices` hỗ trợ Spring Boot 3.5.x/4.x. Phải kiểm tra version thực tế trước khi code.
- `frontend-design` và `web-design-guidelines` có thể dùng cho React/Vite; bỏ qua phần Next.js-specific của `react-best-practices` nếu không có Next.js.
