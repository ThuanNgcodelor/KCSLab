FROM maven:3.9.11-eclipse-temurin-21

USER root

# The result workflow converts uploaded KQKN DOCX files to PDF at runtime.
# ttf-mscorefonts-installer tải font chuẩn Microsoft (Times New Roman, Arial…) mà mẫu KQKN dùng,
# để PDF chuyển từ Word giống bản gốc. Chấp nhận điều khoản (EULA) của Microsoft core fonts.
RUN echo "ttf-mscorefonts-installer msttcorefonts/accepted-mscorefonts-eula select true" | debconf-set-selections \
    && apt-get update \
    && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
        ttf-mscorefonts-installer fontconfig \
        libreoffice-writer-nogui \
        fonts-liberation \
        fonts-dejavu-core \
        fonts-crosextra-carlito \
        fonts-crosextra-caladea \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

ENV REPORT_OFFICE_COMMAND=soffice

