# Image frontend (Linux/Docker): build bằng Node rồi phục vụ thư mục dist bằng nginx ở cổng 5173.
# Context là thư mục source/ (vì bản build chép thêm trang tra cứu source/public/tracuu vào dist/tracuu).

FROM node:22-alpine AS build
WORKDIR /src/fe
COPY fe/package.json fe/package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY fe/ ./
COPY public/ /src/public/
RUN npm run build

FROM nginx:1.27-alpine
COPY fe/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /src/fe/dist /usr/share/nginx/html
EXPOSE 5173
