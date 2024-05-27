-- Set affiliation of product team members to 'rms-team'.  Default is
-- 'customer'.  We use this information to reduce statistic pollution on PROD.

-- RmS system user
update user_meta set affiliation = 'rms-team' where email = 'system@smartsite.de';

-- Support Account
update user_meta set affiliation = 'rms-team' where email = 'support.refinemysite@de.bosch.com';

-- Novateccies
update user_meta set affiliation = 'rms-team' where email like '%@novatec-gmbh.de';

-- RmS Product Team
update user_meta set affiliation = 'rms-team'
    where email in
        ('andrew.ortman@us.bosch.com',
         'axel.gauweiler@gmail.com',
         'axel.gauweiler@de.bosch.com',
         'benjamin.hoehensteiger@de.bosch.com',
         'daniel.oliveira2@pt.bosch.com',
         'daniel.rodrigues3@pt.bosch.com',
         'edgar.pires@pt.bosch.com',
         'filipe.taborda@pt.bosch.com',
         'external.francisca.pimenta@pt.bosch.com',
         'francisca.pimenta@pt.bosch.com',
         'horst.walter@de.bosch.com',
         'hugo.fonseca2@pt.bosch.com',
         'external.joana.ramos@pt.bosch.com',
         'joana.ramos@pt.bosch.com',
         'joao.cortez@pt.bosch.com',
         'joao.gouveia2@pt.bosch.com',
         'karin.pennemann@de.bosch.com',
         'karina.kuyek@us.bosch.com',
         'karina.kuyek@gmail.com',
         'katharina.kleiner@de.bosch.com',
         'lucia.camara@pt.bosch.com',
         'external.matheus.pereira2@pt.bosch.com',
         'moritz.schiek@de.bosch.com',
         'external.nathalia.rodrigues@bosch.com',
         'external.nathalia.rodrigues@br.bosch.com',
         'nathalie.oesterle2@de.bosch.com',
         'nicolas.steimmig@bosch.com',
         'nicolas.steimmig@gmx.de',
         'pedro.letra@pt.bosch.com',
         'pedro.letra@bosch.com',
         'external.pedro.motta@pt.bosch.com',
         'pedro.motta2@pt.bosch.com',
         'rafael.franca@de.bosch.com',
         'rafael.franca@us.bosch.com',
         'rafarefine@gmail.com',
         'renato.cruz@pt.bosch.com',
         'ricardo.nascimento@pt.bosch.com',
         'rita.amorim@pt.bosch.com',
         'sebastian.jackisch@de.bosch.com',
         'sgaa@gmx.de',
         'stefan.gaa@de.bosch.com',
         'simon.hartmann2@de.bosch.com',
         'thomas.flad2@de.bosch.com',
         'ute.binger@de.bosch.com',
         'vibhuti.choudhary@de.bosch.com');

-- Pen Testers
update user_meta set affiliation = 'rms-team' where email like '%@seavus.com';
        
