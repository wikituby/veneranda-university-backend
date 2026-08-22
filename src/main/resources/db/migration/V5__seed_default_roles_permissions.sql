-- ============================================================
-- V5: Seed Default Permissions and Roles
-- ============================================================


-- ============================================================
-- PERMISSIONS
-- ============================================================


INSERT INTO permissions
(
    name,
    code,
    module,
    description,
    is_system
)

SELECT *
FROM
(
VALUES

('View customers','customer:read','customer','View customer records',TRUE),
('Create customer','customer:create','customer','Create new customer',TRUE),
('Update customer','customer:update','customer','Update customer details',TRUE),
('Delete customer','customer:delete','customer','Delete customer',TRUE),
('Export customers','customer:export','customer','Export customer data',TRUE),


('View packages','package:read','package','View packages',TRUE),
('Create package','package:create','package','Create package',TRUE),
('Update package','package:update','package','Update package',TRUE),
('Delete package','package:delete','package','Delete package',TRUE),


('View invoices','invoice:read','billing','View invoices',TRUE),
('Create invoice','invoice:create','billing','Create invoice',TRUE),
('Update invoice','invoice:update','billing','Update invoice',TRUE),
('Delete invoice','invoice:delete','billing','Delete invoice',TRUE),
('Approve invoice','invoice:approve','billing','Approve invoice',TRUE),
('Apply credit note','invoice:credit_note','billing','Apply credit note',TRUE),


('View payments','payment:read','payment','View payments',TRUE),
('Record payment','payment:create','payment','Record payments',TRUE),
('Refund payment','payment:refund','payment','Refund payments',TRUE),
('Reconcile payment','payment:reconcile','payment','Reconcile payments',TRUE),


('View vouchers','voucher:read','voucher','View vouchers',TRUE),
('Create vouchers','voucher:create','voucher','Generate vouchers',TRUE),
('Delete vouchers','voucher:delete','voucher','Delete vouchers',TRUE),
('Redeem voucher','voucher:redeem','voucher','Redeem voucher',TRUE),


('Manage hotspot users','hotspot:manage','hotspot','Manage hotspot users',TRUE),
('View hotspot sessions','hotspot:read','hotspot','View hotspot sessions',TRUE),


('Manage PPPoE secrets','pppoe:manage','pppoe','Manage PPPoE secrets',TRUE),


('View routers','router:read','router','View routers',TRUE),
('Manage routers','router:manage','router','Configure routers',TRUE),
('Backup routers','router:backup','router','Backup router configurations',TRUE),


('View monitoring','monitoring:read','monitoring','View network monitoring',TRUE),


('View inventory','inventory:read','inventory','View inventory',TRUE),
('Manage inventory','inventory:manage','inventory','Manage inventory items',TRUE),


('View finance','finance:read','finance','View finance reports',TRUE),
('Manage finance','finance:manage','finance','Manage income and expenses',TRUE),
('Manage payroll','finance:payroll','finance','Manage payroll',TRUE),


('View reports','report:read','report','View reports',TRUE),
('Export reports','report:export','report','Export reports',TRUE),


('View tickets','ticket:read','crm','View support tickets',TRUE),
('Manage tickets','ticket:manage','crm','Manage support tickets',TRUE),


('View jobs','job:read','technician','View assigned jobs',TRUE),
('Manage jobs','job:manage','technician','Manage installation jobs',TRUE),


('View commissions','commission:read','sales','View commissions',TRUE),
('Manage commissions','commission:manage','sales','Manage commissions',TRUE),


('View users','user:read','admin','View users',TRUE),
('Create user','user:create','admin','Create users',TRUE),
('Update user','user:update','admin','Update users',TRUE),
('Delete user','user:delete','admin','Delete users',TRUE),

('View roles','role:read','admin','View roles',TRUE),
('Manage roles','role:manage','admin','Manage roles and permissions',TRUE),

('View audit logs','audit:read','admin','View audit logs',TRUE),

('Manage settings','setting:manage','admin','Manage settings',TRUE),

('View branches','branch:read','admin','View branches',TRUE),
('Manage branches','branch:manage','admin','Manage branches',TRUE),


('View maps','gis:read','gis','View GIS maps',TRUE),


('View notifications','notification:read','notification','View notifications',TRUE),
('Manage notifications','notification:manage','notification','Manage notification templates',TRUE)

) AS p(
name,
code,
module,
description,
is_system
)

WHERE NOT EXISTS
(
    SELECT 1
    FROM permissions existing
    WHERE existing.code=p.code
);





-- ============================================================
-- DEFAULT ROLES
-- ============================================================


INSERT INTO roles
(
tenant_id,
name,
code,
description,
is_system,
status
)

SELECT
id,
'Super Administrator',
'SUPER_ADMIN',
'Full system access',
TRUE,
'ACTIVE'

FROM tenants

WHERE code='DEFAULT'

AND NOT EXISTS
(
SELECT 1 FROM roles WHERE code='SUPER_ADMIN'
);




INSERT INTO roles
(
tenant_id,
name,
code,
description,
is_system,
status
)

SELECT
id,
'Administrator',
'ADMIN',
'Manage all operational modules',
TRUE,
'ACTIVE'

FROM tenants

WHERE code='DEFAULT'

AND NOT EXISTS
(
SELECT 1 FROM roles WHERE code='ADMIN'
);




INSERT INTO roles
(
tenant_id,
name,
code,
description,
is_system,
status
)

SELECT
id,
'Finance Officer',
'FINANCE',
'Billing and payments management',
TRUE,
'ACTIVE'

FROM tenants

WHERE code='DEFAULT'

AND NOT EXISTS
(
SELECT 1 FROM roles WHERE code='FINANCE'
);




INSERT INTO roles
(
tenant_id,
name,
code,
description,
is_system,
status
)

SELECT
id,
'Sales Agent',
'AGENT',
'Customer registration and voucher sales',
TRUE,
'ACTIVE'

FROM tenants

WHERE code='DEFAULT'

AND NOT EXISTS
(
SELECT 1 FROM roles WHERE code='AGENT'
);




INSERT INTO roles
(
tenant_id,
name,
code,
description,
is_system,
status
)

SELECT
id,
'Technician',
'TECHNICIAN',
'Installations and maintenance',
TRUE,
'ACTIVE'

FROM tenants

WHERE code='DEFAULT'

AND NOT EXISTS
(
SELECT 1 FROM roles WHERE code='TECHNICIAN'
);




INSERT INTO roles
(
tenant_id,
name,
code,
description,
is_system,
status
)

SELECT
id,
'Support Agent',
'SUPPORT',
'CRM tickets and customer support',
TRUE,
'ACTIVE'

FROM tenants

WHERE code='DEFAULT'

AND NOT EXISTS
(
SELECT 1 FROM roles WHERE code='SUPPORT'
);





-- ============================================================
-- SUPER ADMIN FULL ACCESS
-- ============================================================


INSERT INTO role_permissions
(
role_id,
permission_id
)

SELECT
r.id,
p.id

FROM roles r

CROSS JOIN permissions p

WHERE r.code='SUPER_ADMIN'

AND NOT EXISTS
(
SELECT 1

FROM role_permissions rp

WHERE rp.role_id=r.id

AND rp.permission_id=p.id
);





-- ============================================================
-- ADMIN ACCESS
-- ============================================================


INSERT INTO role_permissions
(
role_id,
permission_id
)

SELECT
r.id,
p.id

FROM roles r

JOIN permissions p

ON p.code NOT IN
(
'user:create',
'user:update',
'user:delete',
'role:manage',
'audit:read',
'setting:manage'
)

WHERE r.code='ADMIN'

AND NOT EXISTS
(
SELECT 1

FROM role_permissions rp

WHERE rp.role_id=r.id

AND rp.permission_id=p.id
);