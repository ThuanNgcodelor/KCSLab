-- KQKN report delivered as a file (DOCX converted to PDF by LibreOffice, or a PDF exported from Word).
-- document_number = the "Số/No." printed on that file (read automatically, not unique: a new version may reuse it).
-- New result_release_files.kind values (column is VARCHAR(20), no schema change needed):
--   REPORT_SOURCE = uploaded DOCX (internal), REPORT_PDF = PDF to publish (internal until stamped),
--   GENERATED_REPORT = public PDF with QR, produced at publication.
ALTER TABLE result_releases ADD COLUMN document_number VARCHAR(60) NULL;
