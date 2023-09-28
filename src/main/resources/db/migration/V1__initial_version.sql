create sequence hibernate_sequence;

create table organization
(
    id              bigint not null
        primary key,
    confidentiality integer,
    created         timestamp,
    description     text,
    name            text,
    company         varchar(255),
    display_name    varchar(255),
    modified        timestamp,
    state           integer
);

create table space
(
    id                     bigint not null
        primary key,
    confidentiality        integer,
    created                timestamp,
    default_retention_time integer,
    description            text,
    identifier             text,
    name                   text,
    organization_id        bigint,
    state                  integer,
    metadata_generate      boolean,
    description_ref        varchar(255),
    display_name           varchar(255),
    gdpr_relevant          boolean,
    metadata_index_name    varchar(255),
    modified               timestamp,
    schema_ref             varchar(255),
    constraint ukev2s46ro0lijd873rmopl6qdu
        unique (organization_id, name)
);

create table appconfig
(
    id           bigint not null
        primary key,
    app_type     integer,
    display_name text,
    path         text
);

create table organization_app_configs
(
    organization_id bigint not null
        constraint fktgy4cpkvrdek50h2magg1ifiq
            references organization,
    app_configs_id  bigint not null
        constraint uk_l7ns94r5s0s9red4crk760yta
            unique
        constraint fkrjqq3pech4h1w0sj7a0atdpw1
            references appconfig
);

create table organization_owners
(
    organization_id bigint not null
        constraint fk7wr0p8clskyaae5nlchdrxyh6
            references organization,
    owners          varchar(255)
);

create table space_app_configs
(
    space_id       bigint not null
        constraint fk6py6ba02yqpir2iimhltgn7ew
            references space,
    app_configs_id bigint not null
        constraint uk_fudnugy5rfarmly1cxl85ded2
            unique
        constraint fk1k5h3bjp4k1u6sx9gdrxvcb0x
            references appconfig
);

create table space_capabilities
(
    space_id     bigint not null
        constraint fk8rgxp7fjonem7m1mgsl2xeldy
            references space,
    capabilities varchar(255)
);

create table space_owners
(
    space_id bigint not null
        constraint fkdg17umi0d65597dr1028dv9e
            references space,
    owners   varchar(255)
);

create table tag
(
    id   bigserial
        primary key,
    name text
);

create table organization_tags
(
    organization_id bigint not null
        constraint fkplf791g9ahuj15c8duwwcw50d
            references organization,
    tags_id         bigint not null
        constraint uk_h8l4evlqawpajay487cq71xwd
            unique
        constraint fkky066c51w2sicve222q26kqec
            references tag
);

create table space_tags
(
    space_id bigint not null
        constraint fktrs7a2en7rj4d6k8v8fdm3mgy
            references space,
    tags_id  bigint not null
        constraint uk_7h129i5pi8obh9ahtax824vb0
            unique
        constraint fk5pb4xutxb2w7txd5avbcwacpk
            references tag
);