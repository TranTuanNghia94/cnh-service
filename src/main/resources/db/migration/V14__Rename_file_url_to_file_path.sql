-- Store only the S3 key/path instead of the full URL.
-- The full URL is constructed dynamically by the application (bucket + region + key),
-- so that it stays correct even when the bucket, region, or CDN origin changes.
ALTER TABLE file_infos RENAME COLUMN file_url TO file_path;
