-- Preserve records created with the original misspelled resource discriminator.
ALTER TABLE data_record DISABLE TRIGGER immutable_data_record;
UPDATE data_record SET record_type = 'macro-series' WHERE record_type = 'macro-seriess';
ALTER TABLE data_record ENABLE TRIGGER immutable_data_record;
