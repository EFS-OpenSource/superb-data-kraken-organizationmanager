-- ORGANIZATION

-- if organization.displayname is empty: organization.name

update organization
set display_name=name
where display_name is null;

-- if organization.state is empty: OPEN

update organization
set state=0
where state is null;

-- SPACE

-- if space.displayname is empty: space.name

update space
set display_name=name
where display_name is null;

-- if metadata_generate is empty: false

update space
set metadata_generate= false
where metadata_generate is null;

-- if gdpr_relevant is empty: false

update space
set gdpr_relevant= false
where gdpr_relevant is null;

-- if metadata_index_name is empty and not loadingzone: set default

update space
set metadata_index_name=data_table.index_name
from (select s.id,
             o.name || '_' || s.name ||
             '_measurements' as index_name
      from space s
               left outer join organization o on
          s.organization_id = o.id) as data_table
where space.id = data_table.id
  and metadata_index_name is null
  and name <> 'loadingzone';
