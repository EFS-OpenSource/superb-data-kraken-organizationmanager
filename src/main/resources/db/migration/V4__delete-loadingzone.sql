delete
from space_capabilities
where space_id in (select id
                   from space
                   where name = 'loadingzone'
                     and description like 'loadingzone for %');

delete
from space
where name = 'loadingzone'
  and description like 'loadingzone for %';

commit;