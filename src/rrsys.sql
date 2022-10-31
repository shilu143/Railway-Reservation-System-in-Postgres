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
	pnr varchar(10),
	passenger_no integer not NULL,
	names varchar[] not NULL,
	age integer[] not NULL,
	gender char(1)[] not NULL,
	coachno integer[] not NULL,
	coachtype char(2)[] not NULL,
	birthno integer[] not NULL,
	birthtype char(2)[] not NULL,
	primary key (pnr),
	foreign key (trainno, doj) references booking_system(trainno, doj)
);

INSERT INTO train VALUES(12345);

CREATE OR REPLACE PROCEDURE INSERT_TRAIN(trainno integer)
LANGUAGE plpgsql
as $$
	begin
		INSERT INTO train(trainno) VALUES(trainno);
		commit;
	end;
$$;

CREATE OR REPLACE PROCEDURE TABLE_CREATE(tabname varchar) 
LANGUAGE plpgsql
as $$
	begin
		EXECUTE format('CREATE TABLE %s (
			trainno INTEGER,
			doj DATE,
			coachno INTEGER,
			birthno INTEGER,
			birthtype CHAR(2),
			stat CHAR(1),
			PRIMARY KEY(coachno, birthno),
			FOREIGN KEY(trainno, doj) REFERENCES booking_system(trainno, doj)
			);', tabname
		);
	end;
$$;

CREATE OR REPLACE PROCEDURE	FILL_TABLE(ac_tabname varchar, sl_tabname varchar, trainno integer, doj date, ac integer, sl integer)
LANGUAGE plpgsql
as $$
	declare
		ac_type varchar[][] := ARRAY['LB', 'UB', 'LB', 'UB', 'SL', 'SU'];
		sl_type varchar[][] := ARRAY['LB', 'MB', 'UB', 'LB', 'MB', 'UB', 'SL', 'SU'];
		temp varchar := '';
	begin
		CALL TABLE_CREATE(ac_tabname);
		CALL TABLE_CREATE(sl_tabname);
		INSERT INTO booking_system VALUES (trainno, doj, ac, sl, 18*ac, 24*sl);

		FOR coachno in 1..ac loop
			FOR birthno in 0..17 loop
				temp := ac_type[birthno%6 + 1];
				EXECUTE format('INSERT INTO %s VALUES($1, $2, $3, $4, $5, ''E'')'
				,ac_tabname)
				USING trainno, doj, coachno, (birthno + 1), temp;
			end loop;
		end loop;
		
		FOR coachno in 1..sl loop
			FOR birthno in 0..23 loop
				temp := sl_type[birthno%8 + 1];
				EXECUTE format('INSERT INTO %s VALUES($1, $2, $3, $4, $5, ''E'')'
				,sl_tabname)
				USING trainno, doj, coachno, (birthno + 1), temp;
			end loop;
		end loop;
	end;
$$;
