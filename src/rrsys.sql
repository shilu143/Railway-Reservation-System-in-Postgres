DROP SCHEMA public CASCADE;
CREATE SCHEMA public;
     
GRANT ALL ON SCHEMA public TO postgres;
GRANT ALL ON SCHEMA public TO public;

CREATE TABLE Trains(
	trainno INTEGER PRIMARY KEY
);

CREATE TABLE Booking_System(
	trainno INTEGER,
	doj DATE,
	AC INTEGER NOT NULL,
	SL INTEGER NOT NULL,
	AC_seat_count INTEGER NOT NULL CHECK (AC_seat_count >= 0),
	SL_seat_count INTEGER NOT NULL CHECK (SL_seat_count >= 0),
	PRIMARY KEY(trainno, doj),
	FOREIGN KEY (trainno) REFERENCES trains(trainno)
);

CREATE TABLE Ticket(
	trainno INTEGER NOT NULL,
	doj DATE NOT NULL,
	pnr VARCHAR(20),
	passenger_no INTEGER NOT NULL,
	names TEXT[] NOT NULL,
	coachno INTEGER[] NOT NULL,
	coachtype CHAR(2) NOT NULL,
	berthno INTEGER[] NOT NULL,
	berthtype CHAR(2)[] NOT NULL,
	PRIMARY KEY (pnr),
	FOREIGN KEY (trainno, doj) REFERENCES booking_system(trainno, doj)
);


CREATE OR REPLACE PROCEDURE INSERT_TRAIN(IN trainno INTEGER)
LANGUAGE plpgsql
as $$
	BEGIN
		INSERT INTO trains VALUES(trainno);
		COMMIT;
	END;
$$;

CREATE OR REPLACE PROCEDURE TABLE_CREATE(IN tabname VARCHAR) 
LANGUAGE plpgsql
as $$
	BEGIN
		EXECUTE format('CREATE TABLE %s (
			trainno INTEGER,
			doj DATE,
			coachno INTEGER,
			berthno INTEGER,
			berthtype CHAR(2),
			stat CHAR(1),
			PRIMARY KEY(coachno, berthno),
			FOREIGN KEY(trainno, doj) REFERENCES booking_system(trainno, doj)
			);', tabname
		);
		COMMIT;
	END;
$$;

CREATE OR REPLACE PROCEDURE	FILL_TABLE(IN ac_tabname VARCHAR, IN sl_tabname VARCHAR, IN trainno INTEGER,
 										IN doj DATE, IN ac INTEGER, IN sl INTEGER)
LANGUAGE plpgsql
AS $$
	DECLARE
		ac_type VARCHAR[][2] := ARRAY['LB', 'UB', 'LB', 'UB', 'SL', 'SU'];
		sl_type VARCHAR[][2] := ARRAY['LB', 'MB', 'UB', 'LB', 'MB', 'UB', 'SL', 'SU'];
		temp VARCHAR := '';
	BEGIN
		CALL TABLE_CREATE(ac_tabname);
		CALL TABLE_CREATE(sl_tabname);
		INSERT INTO booking_system VALUES (trainno, doj, ac, sl, 18*ac, 24*sl);

		FOR coachno IN 1..ac LOOP
			FOR berthno IN 0..17 LOOP
				temp := ac_type[berthno%6 + 1];
				EXECUTE format('INSERT INTO %s VALUES($1, $2, $3, $4, $5, ''E'')'
				,ac_tabname)
				USING trainno, doj, coachno, (berthno + 1), temp;
			END LOOP;
		END LOOP;
		
		FOR coachno IN 1..sl LOOP
			FOR berthno IN 0..23 LOOP
				temp := sl_type[berthno%8 + 1];
				EXECUTE format('INSERT INTO %s VALUES($1, $2, $3, $4, $5, ''E'')'
				,sl_tabname)
				USING trainno, doj, coachno, (berthno + 1), temp;
			END LOOP;
		END LOOP;
		COMMIT;
	END;
$$;


CREATE OR REPLACE PROCEDURE Book_Ticket(IN tabname VARCHAR, IN n INT, IN names VARCHAR[], IN trainno INT, IN doj DATE, IN choice VARCHAR, OUT isBooked INT)
LANGUAGE plpgsql
as $$
	DECLARE
		answer TEXT[][]; 
		pass_coach INT[];
		pass_berth INT[];
		pass_berthtype TEXT[];
		rec RECORD;
		count INT := 1;
		pnr TEXT;
		tmp1 INT := 0;
		tmp2 INT := 0;
	BEGIN
		isBooked = 0;
		EXECUTE format('SELECT ac_seat_count, sl_seat_count 
						FROM booking_system
						WHERE trainno = $1 and doj = $2')
				INTO tmp1, tmp2
				USING trainno, doj;

		IF (choice = 'AC' and n > tmp1) or (choice = 'SL' and n > tmp2) THEN
			RETURN;
		END IF;
		
		pnr := tabname;
		answer := array_fill(NULL::TEXT, array[n,3]);
		FOR rec IN EXECUTE format('SELECT * FROM %s', tabname)
    	LOOP
			IF count > n THEN
				EXIT;
			END IF;
      		IF rec.stat = 'E' THEN
				pass_coach[count] = rec.coachno;
				pass_berth[count] = rec.berthno;
				pass_berthtype[count] = rec.berthtype;

				EXECUTE format('UPDATE %s 
				set stat = ''F''
				WHERE coachno = $1 and berthno = $2'
				,tabname)
				USING rec.coachno, rec.berthno;
				
				count := count + 1;
			END IF;
    	END LOOP;

		pnr := concat(pnr, 'C', pass_coach[1], 'B', pass_berth[1]);
		EXECUTE format('INSERT INTO Ticket(trainno, doj, pnr, passenger_no, names, coachno, coachtype, berthno, berthtype) 
		VALUES($1, $2, $3, $4, $5, $6, $7, $8, $9)')
		USING trainno, doj, pnr, n, names, pass_coach, choice, pass_berth, pass_berthtype;

		IF choice = 'AC' THEN
			EXECUTE format('UPDATE booking_system
			SET ac_seat_count = ac_seat_count - $1
			WHERE trainno = $2 and doj = $3')
			USING n, trainno, doj;
		ELSE
			EXECUTE format('UPDATE booking_system
			SET sl_seat_count = sl_seat_count - $1
			WHERE trainno = $2 and doj = $3')
			USING n, trainno, doj;
		END IF;
		COMMIT;
		isBooked = 1;
	END;
$$;