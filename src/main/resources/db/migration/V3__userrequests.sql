create table if not exists user_request
(
    id       bigint not null
        primary key,
    created  timestamp,
    modified timestamp,
    user_id  text,
    state    integer
);

create table if not exists organization_user_request
(
    id       bigint not null
        primary key
    constraint uk_3fn9i1qn5w7s5msi38x2atmxr
            unique,
    created  timestamp,
    modified timestamp,
    user_id  text,
    orga_id  bigint,
    role     integer,
    state    integer
);

create table if not exists space_user_request
(
    id       bigint not null
        primary key
    constraint uk_pwb5yekjkk8c8139fqudg5110
            unique,
    created  timestamp,
    modified timestamp,
    state    integer,
    user_id  text,
    orga_id  bigint,
    role     integer,
    space_id bigint
);