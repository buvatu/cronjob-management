DROP TABLE IF EXISTS public.cronjob_config;
CREATE TABLE public.cronjob_config (
	id uuid DEFAULT gen_random_uuid(),
	cronjob_name varchar primary key,
	cronjob_expression varchar,
	cronjob_pool_size int default 5,
	cronjob_status varchar,
	current_session_id varchar,
	updated_timestamp timestamp default now(),
	updated_user varchar default 'SYSTEM'
);

DROP TABLE IF EXISTS public.cronjob_running_log;
CREATE TABLE public.cronjob_running_log (
	id serial,
	cronjob_name varchar,
	session_id varchar,
	activity_name varchar,
	progress_value int,
	updated_timestamp timestamp default now(),
	updated_user varchar default 'SYSTEM'
);

DROP TABLE IF EXISTS public.cronjob_change_history;
CREATE TABLE public.cronjob_change_history (
	id serial,
	cronjob_name varchar,
	session_id varchar,
	start_time timestamp,
	stop_time timestamp default now(),
	executor varchar,
	operation varchar,
	execution_result varchar,
	description varchar
);