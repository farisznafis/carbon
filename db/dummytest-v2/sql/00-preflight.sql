-- Static EN-R-002 dummytest-v2 data. Run only on a dedicated test database.
DO $$ BEGIN
  IF current_database() IN ('postgres','rdsadmin') THEN RAISE EXCEPTION 'Refusing unsafe database %', current_database(); END IF;
  IF EXISTS (SELECT 1 FROM operator_company WHERE company_id='OP000000001') THEN RAISE EXCEPTION 'dummytest-v2 is already loaded; run reset.sql first'; END IF;
  IF EXISTS (SELECT 1 FROM project WHERE public_id IN ('ITPROJ001','ITPROJ002')) THEN RAISE EXCEPTION 'Reserved V2 project IDs already exist'; END IF;
  IF EXISTS (SELECT 1 FROM participating_company WHERE company_id BETWEEN 'PC000000001' AND 'PC000000006') THEN RAISE EXCEPTION 'Reserved V2 company IDs already exist'; END IF;
END $$;
