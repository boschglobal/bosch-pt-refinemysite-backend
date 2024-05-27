-- Set affiliation of product team members to 'rms-team'.  Default is
-- 'customer'.  We use this information to reduce statistic pollution on PROD.

-- RmS system user
update user_meta set affiliation = 'rms-team' where email = 'REPLACE_ME';

-- Support Account
update user_meta set affiliation = 'rms-team' where email = 'REPLACE_ME';

-- Novateccies
update user_meta set affiliation = 'rms-team' where email like '%@replace-me.de';

-- RmS Product Team
update user_meta set affiliation = 'rms-team'
    where email in
        ('REPLACE_ME_1',
         'REPLACE_ME_2'
);

-- Pen Testers
update user_meta set affiliation = 'rms-team' where email like '%@replace-me.com';
        
