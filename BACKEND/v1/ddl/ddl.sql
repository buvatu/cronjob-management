DROP TABLE IF EXISTS public.job_config;
CREATE TABLE public.job_config
(
    id         uuid default gen_random_uuid() primary key,
    name       varchar(100) unique not null,
    expression varchar(20),
    pool_size  int  default 1,
    status     varchar(20),
    description varchar,
    created_at timestamptz,
    created_by varchar(20),
    updated_at timestamptz,
    updated_by varchar(20),
    version    bigint
);

DROP TABLE IF EXISTS public.job_execution;
CREATE TABLE public.job_execution
(
    id           uuid default gen_random_uuid() primary key,
    job_name     varchar(100),
    trigger_type varchar(10),
    executor     varchar(20),
    instance_id  varchar(100),
    exit_code    int,
    output       varchar,
    duration     bigint,
    status       varchar(20),
    description  varchar,
    created_at   timestamptz,
    created_by   varchar(20),
    updated_at   timestamptz,
    updated_by   varchar(20)
);
CREATE UNIQUE INDEX idx_unique_running_job ON job_execution (job_name) WHERE status = 'RUNNING';


DROP TABLE IF EXISTS public.job_execution_log;
CREATE TABLE public.job_execution_log
(
    id           uuid default gen_random_uuid() primary key,
    session_id     uuid,
    activity_name  varchar(100),
    progress_value int,
    description    varchar,
    created_at     timestamptz
);

DROP TABLE IF EXISTS public.job_operation;
CREATE TABLE public.job_operation
(
    id            uuid default gen_random_uuid() primary key,
    job_name      varchar(100),
    operation     varchar(50),
    executor      varchar(20),
    result        varchar(20),
    error_message varchar,
    description   varchar,
    executed_at   timestamptz
);