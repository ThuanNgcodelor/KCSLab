FROM maven:3.9.11-eclipse-temurin-21

USER root

# The result workflow converts uploaded KQKN DOCX files to PDF at runtime.
RUN apt-get update \
    && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
        libreoffice-writer-nogui \
        fonts-liberation \
        fonts-dejavu-core \
        fonts-crosextra-carlito \
        fonts-crosextra-caladea \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

ENV REPORT_OFFICE_COMMAND=soffice

