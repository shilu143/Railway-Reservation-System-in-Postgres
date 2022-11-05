DROP SCHEMA public CASCADE;
CREATE SCHEMA public;
     
GRANT ALL ON SCHEMA public TO postgres;
GRANT ALL ON SCHEMA public TO public;

create table Train(
	trainno integer Primary key
);

create table Booking_System(
	trainno integer,
	doj date,
	AC integer not null,
	SL integer not null,
	AC_seat_count integer not null check (AC_seat_count >= 0),
	SL_seat_count integer not null check (SL_seat_count >= 0),
	primary key(trainno, doj),
	foreign key (trainno) references train(trainno)
);

create table Ticket(
	trainno integer not NULL,
	doj date not NULL,
	pnr varchar(20),
	passenger_no integer not NULL,
	names text[] not NULL,
	age integer[] ,
	gender char(1)[] ,
	coachno integer[] not NULL,
	coachtype char(2) ,
	berthno integer[] not NULL,
	berthtype char(2)[] not NULL,
	primary key (pnr),
	foreign key (trainno, doj) references booking_system(trainno, doj)
);

INSERT INTO train VALUES(12345);

CREATE OR REPLACE PROCEDURE INSERT_TRAIN(IN trainno integer)
LANGUAGE plpgsql
as $$
	begin
		INSERT INTO train(trainno) VALUES(trainno);
		commit;
	end;
$$;

CREATE OR REPLACE PROCEDURE TABLE_CREATE(IN tabname varchar) 
LANGUAGE plpgsql
as $$
	begin
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
	end;
$$;

CREATE OR REPLACE PROCEDURE	FILL_TABLE(IN ac_tabname varchar, IN sl_tabname varchar, IN trainno integer, IN doj date, IN ac integer, IN sl integer)
LANGUAGE plpgsql
as $$
	declare
		ac_type varchar[][2] := ARRAY['LB', 'UB', 'LB', 'UB', 'SL', 'SU'];
		sl_type varchar[][2] := ARRAY['LB', 'MB', 'UB', 'LB', 'MB', 'UB', 'SL', 'SU'];
		temp varchar := '';
	begin
		CALL TABLE_CREATE(ac_tabname);
		CALL TABLE_CREATE(sl_tabname);
		INSERT INTO booking_system VALUES (trainno, doj, ac, sl, 18*ac, 24*sl);

		FOR coachno in 1..ac loop
			FOR berthno in 0..17 loop
				temp := ac_type[berthno%6 + 1];
				EXECUTE format('INSERT INTO %s VALUES($1, $2, $3, $4, $5, ''E'')'
				,ac_tabname)
				USING trainno, doj, coachno, (berthno + 1), temp;
			end loop;
		end loop;
		
		FOR coachno in 1..sl loop
			FOR berthno in 0..23 loop
				temp := sl_type[berthno%8 + 1];
				EXECUTE format('INSERT INTO %s VALUES($1, $2, $3, $4, $5, ''E'')'
				,sl_tabname)
				USING trainno, doj, coachno, (berthno + 1), temp;
			end loop;
		end loop;
	end;
$$;


CREATE OR REPLACE PROCEDURE Book_Ticket(IN tabname varchar, IN n int, IN names varchar[], IN trainno int, IN doj date, IN choice varchar)
LANGUAGE plpgsql
as $$
	declare
		answer text[][]; 
		coacharr int[];
		bertharr int[];
		berthtyp text[];
		rec record;
		count int := 1;
		pnr text;
	begin
		pnr := tabname;
		answer := array_fill(null::text, array[n,3]);
		FOR rec IN EXECUTE format('SELECT * FROM %s', tabname)
    	LOOP
			IF count > n then
				EXIT;
			END IF;
      		IF rec.stat = 'E' then
				coacharr[count] = rec.coachno;
				bertharr[count] = rec.berthno;
				berthtyp[count] = rec.berthtype;

				EXECUTE format('UPDATE %s 
				set stat = ''F''
				WHERE coachno = $1 and berthno = $2'
				,tabname)
				USING rec.coachno, rec.berthno;
				
				count := count + 1;
			END IF;
    	END LOOP;

		pnr := concat(pnr, 'C', coacharr[1], 'B', bertharr[1]);
		EXECUTE format('INSERT INTO Ticket(trainno, doj, pnr, passenger_no, names, coachno, coachtype, berthno, berthtype) 
		VALUES($1, $2, $3, $4, $5, $6, $7, $8, $9)')
		USING trainno, doj, pnr, n, names, coacharr, choice, bertharr, berthtyp;
	end;
$$;