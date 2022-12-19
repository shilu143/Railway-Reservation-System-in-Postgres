DROP SCHEMA public CASCADE;
CREATE SCHEMA public;
     
GRANT ALL ON SCHEMA public TO postgres;
GRANT ALL ON SCHEMA public TO public;

create table Trains (
	trainno integer Primary key
);

CREATE OR REPLACE PROCEDURE create_station(IN tabname VARCHAR) 
LANGUAGE plpgsql
as $$
	BEGIN
		EXECUTE format('CREATE TABLE IF NOT EXISTS %s (
			trainno int,
			arrivalTime varchar,
			departureTime varchar,
			day integer
			);', tabname
		);
	END;
$$;

CREATE OR REPLACE PROCEDURE trainSchedule(IN tabname VARCHAR) 
LANGUAGE plpgsql
as $$
	BEGIN
		EXECUTE format('CREATE TABLE %s (
			scode varchar,
			station_name varchar,
			arrivalTime varchar,
			departureTime varchar,
			day integer
			);', tabname
		);
	END;
$$;

CREATE OR REPLACE PROCEDURE INSERT_TRAIN(IN trainno integer) 
LANGUAGE plpgsql
as $$
	DECLARE
		tabname varchar := '';
	BEGIN
		insert into trains values(trainno);
		tabname = concat('T', trainno::varchar);	
		call trainSchedule(tabname);
	END;
$$;

CREATE OR REPLACE FUNCTION SEARCH(source varchar, destination varchar)
RETURNS TEXT[] as $$
	DECLARE
		isOK integer :=0;
		rec1 RECORD;
		rec2 RECORD;
		rec3 RECORD;
		rec4 RECORD;
		src varchar := '';
		dest varchar := '';
		train1 varchar := '';
		train2 varchar := '';
		flag boolean;
		temp1 time;
		temp2 time;
		temp3 text := '';
		arr_final text[];
	BEGIN
		src = concat('S_', source);
		dest = concat('S_', destination);
		FOR rec1 in EXECUTE format('SELECT * FROM %s', src)
		LOOP
			FOR rec2 in EXECUTE format('SELECT * FROM %s', dest)
			LOOP
				IF ((rec1.trainno = rec2.trainno) and (rec1.departureTime <> '--' and rec2.arrivalTime<>'--'))  THEN
					temp1 = (select rec1.departureTime::time);
					temp2 = (select rec2.arrivalTime::time);
					IF (rec1.day < rec2.day) or (rec1.day = rec2.day and temp1 <= temp2) THEN
						RAISE NOTICE '%', rec1.trainno;
						temp3 = concat(rec1.departureTime,' ',rec2.arrivalTime, ' ',rec1.trainno);
						arr_final = array_append(arr_final,temp3);
						isOK = 1;
					END IF;
				ELSE
					train1 = concat('T', rec1.trainno);
					train2 = concat('T', rec2.trainno);
					FOR rec3 in EXECUTE format('SELECT * FROM %s', train1)
					LOOP
						FOR rec4 in EXECUTE format('SELECT * FROM %s', train2)
						LOOP
							IF (rec3.scode = rec4.scode) and (rec3.scode <> source and rec3.scode <> destination and 
																rec3.arrivalTime<> '--'
																 and rec3.departureTime<> '--'
																  and rec4.arrivalTime<> '--'
																   and rec4.departureTime<> '--'
																   and rec1.departureTime <> '--' 
																   and rec2.arrivalTime<>'--') THEN 
								temp1 = (select rec3.arrivalTime::time);
								temp2 = (select rec4.departureTime::time);

								IF (rec3.day < rec4.day) or (rec3.day = rec4.day and temp1 <= temp2) THEN
									RAISE NOTICE '%	% % % % % % % % % % % % % %', rec1.trainno,rec1.arrivalTime, rec1.departureTime, rec1.day,
									rec3.arrivalTime, rec3.departureTime, rec3.day,
									 rec3.scode,
									 rec4.arrivalTime, rec4.departureTime, rec4.day,
									  rec2.trainno,rec2.arrivalTime, rec2.departureTime, rec2.day;

									temp3 = concat(rec1.trainno,' ',rec1.arrivalTime,' ', rec1.departureTime,' ', rec1.day,' ',
									rec3.arrivalTime,' ', rec3.departureTime,' ', rec3.day,' ',
									 rec3.scode,' ',
									 rec4.arrivalTime,' ', rec4.departureTime,' ', rec4.day,' ',
									  rec2.trainno,' ',rec2.arrivalTime,' ', rec2.departureTime,' ', rec2.day);
									  arr_final = array_append(arr_final,temp3);
									  isOK =1;
								END IF;
							END IF;	
						END LOOP;
					END LOOP;
				END IF;
			END LOOP;
		END LOOP;
		if(isOK = 0) THEN
			arr_final = array_append(arr_final,'yup');
		END IF;
		return arr_final;
	END;
$$
LANGUAGE plpgsql;