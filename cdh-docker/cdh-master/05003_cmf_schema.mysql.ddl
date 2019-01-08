-- Copyright (c) 2013 Cloudera, Inc. All rights reserved.
--

-- Drop all foreign keys whose columns will be eliminated.
alter table SERVICES
    drop foreign key FK_SERVICE_CONFIG_REVISION;

alter table CONFIG_CONTAINERS
    drop foreign key FK_CONFIG_CONTAINER_CONFIG_REVISION;

alter table CONFIGS
    drop foreign key FK_CONFIG_CONFIG_REVISION;

alter table ROLE_CONFIG_GROUPS
    drop index IDX_UNIQUE_RCG_DISP,
    drop foreign key FK_ROLE_CONFIG_GROUP_CONFIG_REVISION;

alter table ROLE_CONF_GRP_TO_ROLE
    drop foreign key FK_ROLE_CONF_GRP_TO_ROLE_ROLE;

alter table ROLE_CONF_GRP_TO_ROLE
    drop foreign key FK_ROLE_CONF_GRP_TO_ROLE_RCG;

alter table CONF_REV_TO_ROLE_CONF_GRP
    drop foreign key FK_REV_TO_ROLE_CONF_GRP_RCG;

alter table CONF_REV_TO_ROLE_CONF_GRP
    drop foreign key FK_REV_TO_ROLE_CONF_GRP_REVISION;

-- Convert RCG->CR->SERVICE to RCG->SERVICE.
alter table ROLE_CONFIG_GROUPS
    add column SERVICE_ID bigint;

update ROLE_CONFIG_GROUPS rcg
    set rcg.SERVICE_ID=(
        select distinct cr.SERVICE_ID
        from CONFIG_REVISIONS cr
        where cr.REVISION_ID=rcg.REVISION_ID);

-- Delete historical configs, RCGs, and CR-related audits.
delete from CONFIGS
    where SERVICE_ID is not null
    and REVISION_ID not in (
        select CONFIG_REVISION_ID
        from SERVICES);

delete from CONFIGS
    where CONFIG_CONTAINER_ID is not null
    and REVISION_ID not in (
        select CONFIG_REVISION_ID
        from CONFIG_CONTAINERS);

delete from ROLE_CONFIG_GROUPS
    where ROLE_CONFIG_GROUP_ID not in (
        select distinct cr2rcg.ROLE_CONFIG_GROUP_ID
        from CONF_REV_TO_ROLE_CONF_GRP cr2rcg
        join SERVICES s on s.CONFIG_REVISION_ID=cr2rcg.REVISION_ID);

delete from AUDITS
    where CONFIG_REVISION_ID is not null;

-- Add RCG->SERVICE constraints.
alter table ROLE_CONFIG_GROUPS
    modify column SERVICE_ID bigint not null,
    add index FK_ROLE_CONFIG_GROUP_SERVICE (SERVICE_ID),
    add constraint FK_ROLE_CONFIG_GROUP_SERVICE
    foreign key (SERVICE_ID)
    references SERVICES (SERVICE_ID);

create unique index IDX_UNIQUE_RCG_DISP on ROLE_CONFIG_GROUPS
    (DISPLAY_NAME, SERVICE_ID);

-- Drop CR-related columns.
alter table SERVICES
    drop column CONFIG_REVISION_ID;

alter table CONFIG_CONTAINERS
    drop column CONFIG_REVISION_ID;

alter table CONFIGS
    drop column REVISION_ID;
	
ALTER TABLE ROLE_CONFIG_GROUPS DROP INDEX IDX_UNIQUE_ROLE_CONFIG_GROUP;
ALTER TABLE ROLE_CONFIG_GROUPS DROP INDEX IDX_ROLE_CONFIG_GROUP_CONFIG_REVISION;

alter table ROLE_CONFIG_GROUPS
    drop column REVISION_ID;

alter table AUDITS
    drop column CONFIG_REVISION_ID;

-- Drop CR-related tables.
drop table ROLE_CONF_GRP_TO_ROLE;

drop table CONF_REV_TO_ROLE_CONF_GRP;

drop table CONFIG_REVISIONS;
