DO $$
BEGIN
  IF EXISTS (
    SELECT 1
    FROM information_schema.table_constraints
    WHERE constraint_name = 'facility_type_check'
      AND table_name = 'facility'
  ) THEN
    ALTER TABLE facility DROP CONSTRAINT facility_type_check;
  END IF;
END $$;

ALTER TABLE facility
  ADD CONSTRAINT facility_type_check
  CHECK (type IN (
    'GREENHOUSE','POULTRY','COWSHED','TURKEY','SHEEPFOLD','WORKSHOP','AUX_LAND','BORDER_LAND','FISHPOND',
    -- legacy
    'FISHFARM','STABLE','WAREHOUSE','ORCHARD','FIELD','APIARY'
  ));
