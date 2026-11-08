const fs = require('node:fs');
const path = require('node:path');
function writeGenerated(file, content) {
  if (process.argv.includes('--check')) {
    if (!fs.existsSync(file) || fs.readFileSync(file, 'utf8') !== content) throw new Error(file + ' is out of date. Run npm run api:generate.');
  } else fs.writeFileSync(file, content);
}

const ref = name => ({ $ref: `#/components/schemas/${name}` });
const str = (extra = {}) => ({ type: 'string', ...extra });
const integer = (extra = {}) => ({ type: 'integer', format: 'int64', minimum: -9007199254740991, maximum: 9007199254740991, ...extra });
const bool = { type: 'boolean' };
const id = str({ minLength: 1, maxLength: 100 });
const date = str({ format: 'date' });
const timestamp = str({ format: 'date-time' });
const amount = integer({ minimum: 1, maximum: 999999999 });
const array = items => ({ type: 'array', items });
const enumeration = values => str({ enum: values });
const object = (properties, required = Object.keys(properties)) => ({ type: 'object', additionalProperties: false, properties, required });
const version = integer({ minimum: 0 });
const schemas = {};
const add = (name, properties, required) => schemas[name] = object(properties, required);
add('Problem', { type: str({ format: 'uri' }), title: str(), status: integer({ minimum: 400, maximum: 599 }), detail: str(), instance: str(), code: str(), traceId: str(), fieldErrors: array(object({ field: str(), code: str(), message: str() })) }, ['type','title','status','code']);
add('Profile', { id, name: str({ minLength: 1, maxLength: 100 }), email: str({ format: 'email' }), phone: str({ maxLength: 30 }), version });
add('ProfileUpdate', { name: str({ minLength: 1, maxLength: 100 }), email: str({ format: 'email' }), phone: str({ maxLength: 30 }), expectedVersion: version });
add('Preferences', { language: enumeration(['tr','en']), inApp: bool, push: bool, emailNotifications: bool, version });
add('PreferencesUpdate', { language: enumeration(['tr','en']), inApp: bool, push: bool, emailNotifications: bool, expectedVersion: version });
add('Session', { user: ref('Profile'), roles: array(enumeration(['MANAGER','RESIDENT'])), expiresAt: timestamp, csrfToken: str(), permissions: array(str()) });
add('CsrfToken', { csrfToken: str() });
add('LoginRequest', { email: str({ format: 'email' }), password: str({ minLength: 1, maxLength: 128 }) });
add('ResetRequest', { email: str({ format: 'email' }) });
add('PasswordReset', { token: str({ minLength: 1 }), password: str({ minLength: 8, maxLength: 128 }) });
add('Community', { id, name: str(), address: str(), timeZone: str({ enum: ['Europe/Istanbul'] }), currency: enumeration(['TRY']) });
add('Unit', { id, communityId: id, label: str(), blockId: id, blockLabel: str(), floor: integer(), kind: enumeration(['APARTMENT','SHOP','OFFICE']), areaSqm: { type:'number', minimum:0 }, version });
add('CommunityContext', { community: ref('Community'), roles: array(enumeration(['MANAGER','RESIDENT'])), ownUnits: array(ref('Unit')), permissions: array(str()), profile: ref('Profile') });
add('DirectoryPerson', { id, name: str() });
add('DirectoryUnit', { id, label: str(), blockId: id, blockLabel: str(), isMyUnit: bool, residents: array(ref('DirectoryPerson')) });
add('Relationship', { id, unitId: id, person: ref('DirectoryPerson'), type: enumeration(['OWNER','TENANT']), startsOn: date, endsOn: { ...date, nullable: true } });
add('FinancialAccount', { id, name: str(), kind: enumeration(['BANK','CASH']), currency: enumeration(['TRY']), openingMinor: integer(), balanceMinor: integer(), version });
add('Allocation', { chargeId: id, amountMinor: amount });
add('Charge', { id, unitId: id, description: str({ minLength:1,maxLength:300 }), periodStart: date, periodEnd: date, issuedOn: date, dueOn: date, amountMinor: amount, currency: enumeration(['TRY']), allocatedMinor: integer({minimum:0}), outstandingMinor: integer({minimum:0}), status: enumeration(['OUTSTANDING','PARTIALLY_PAID','PAID','CANCELLED']), cancelledAt: { ...timestamp, nullable:true }, cancellationReason: str(), version }, ['id','unitId','description','periodStart','periodEnd','issuedOn','dueOn','amountMinor','currency','allocatedMinor','outstandingMinor','status','version']);
add('ChargeBatchRequest', { items: array(object({ unitId:id, description:str({minLength:1,maxLength:300}), periodStart:date, periodEnd:date, dueOn:date, amountMinor:amount, currency:enumeration(['TRY']), expectedAccountVersion:version })) });
add('ChargeBatch', { id, status: enumeration(['COMPLETED']), charges: array(ref('Charge')), createdAt: timestamp });
add('ReasonCommand', { reason: str({ minLength:1,maxLength:500 }), expectedVersion: version });
add('PaymentCreate', { unitId:id, accountId:id, amountMinor:amount, currency:enumeration(['TRY']), paidOn:date, reference:str({minLength:1,maxLength:100}), allocations:array(ref('Allocation')), notificationId:id, expectedAccountVersion:version }, ['unitId','accountId','amountMinor','currency','paidOn','reference','allocations','expectedAccountVersion']);
add('Payment', { id, unitId:id, accountId:id, amountMinor:amount, currency:enumeration(['TRY']), paidOn:date, reference:str(), allocations:array(ref('Allocation')), unallocatedMinor:integer({minimum:0}), notificationId:{...id,nullable:true}, status:enumeration(['RECORDED','REVERSED']), reversedAt:{...timestamp,nullable:true}, reversalReason:str(), version }, ['id','unitId','accountId','amountMinor','currency','paidOn','reference','allocations','unallocatedMinor','status','version']);
add('UnitAccount', { unitId:id, currency:enumeration(['TRY']), chargedMinor:integer(), paidMinor:integer(), outstandingMinor:integer(), creditMinor:integer(), version });
add('AccountEntry', { id, unitId:id, type:enumeration(['CHARGE','PAYMENT','CHARGE_CANCELLATION','PAYMENT_REVERSAL']), amountMinor:integer(), occurredAt:timestamp, description:str(), sourceId:id, balanceAfterMinor:integer() });
add('PaymentNotificationCreate', { unitId:id, amountMinor:amount, currency:enumeration(['TRY']), paymentDate:date, reference:str({minLength:1,maxLength:100}), note:str({maxLength:500}), receiptId:id }, ['unitId','amountMinor','currency','paymentDate','reference','note']);
add('PaymentNotification', { id, unitId:id, amountMinor:amount, currency:enumeration(['TRY']), paymentDate:date, reference:str(), note:str(), receipt:{...ref('Attachment'),nullable:true}, status:enumeration(['PENDING','REVIEWED','REJECTED','WITHDRAWN','RECORDED']), reviewNote:str(), paymentId:{...id,nullable:true}, version });
add('NotificationReview', { decision:enumeration(['REVIEWED','REJECTED']), note:str({maxLength:500}), expectedVersion:version });
add('PaymentLink', { paymentId:id, expectedVersion:version, expectedPaymentVersion:version });
add('VersionCommand', { expectedVersion:version });
add('ExpenseCreate', { description:str({minLength:1,maxLength:300}), category:enumeration(['MAINTENANCE','UTILITIES','CLEANING','OTHER']), vendor:str({maxLength:150}), incurredOn:date, amountMinor:amount, currency:enumeration(['TRY']), documentId:id, immediatePayment:object({accountId:id, paidOn:date}) }, ['description','category','vendor','incurredOn','amountMinor','currency']);
add('Expense', { id, description:str(), category:enumeration(['MAINTENANCE','UTILITIES','CLEANING','OTHER']), vendor:str(), incurredOn:date, amountMinor:amount, currency:enumeration(['TRY']), paidMinor:integer(), document:{...ref('Attachment'),nullable:true}, version });
add('ExpensePaymentCreate', { accountId:id, paidOn:date, amountMinor:amount, expectedVersion:version });
add('ExpensePayment', { id, expenseId:id, accountId:id, paidOn:date, amountMinor:amount, status:enumeration(['RECORDED','REVERSED']), reversalReason:str(), version });
add('Attachment', { id, fileName:str(), mediaType:enumeration(['application/pdf','image/png','image/jpeg','image/webp']), sizeBytes:integer({minimum:1,maximum:5242880}), purpose:enumeration(['REQUEST_PHOTO','PAYMENT_RECEIPT','EXPENSE_DOCUMENT']), contentUrl:str(), createdAt:timestamp });
add('Upload', { purpose:enumeration(['REQUEST_PHOTO','PAYMENT_RECEIPT','EXPENSE_DOCUMENT']), file:str({format:'binary'}) });
add('RequestCreate', { unitId:id, title:str({minLength:1,maxLength:120}), location:str({minLength:1,maxLength:100}), description:str({maxLength:2000}), photoIds:array(id) }, ['title','location','description','photoIds']);
add('CommentCreate', { body:str({minLength:1,maxLength:2000}) });
add('Comment', { id, author:ref('DirectoryPerson'), body:str(), createdAt:timestamp });
add('RequestEvent', { id, kind:enumeration(['CREATED','STATUS_CHANGED','ASSIGNEE_CHANGED']), actor:ref('DirectoryPerson'), createdAt:timestamp, from:str(), to:str() }, ['id','kind','actor','createdAt']);
add('Request', { id, displayNumber:integer({minimum:1}), communityId:id, unitId:{...id,nullable:true}, title:str(), location:str(), description:str(), status:enumeration(['OPEN','IN_PROGRESS','RESOLVED']), creator:ref('DirectoryPerson'), assignee:{...ref('DirectoryPerson'),nullable:true}, photos:array(ref('Attachment')), comments:array(ref('Comment')), history:array(ref('RequestEvent')), createdAt:timestamp, version });
add('RequestStatusChange', { status:enumeration(['OPEN','IN_PROGRESS','RESOLVED']), expectedVersion:version });
add('RequestAssignment', { assigneeId:{...id,nullable:true}, expectedVersion:version });
add('Audience', { type:enumeration(['ALL','BLOCKS','UNITS']), ids:array(id) });
add('AnnouncementWrite', { title:str({minLength:1,maxLength:160}), body:str({minLength:1,maxLength:10000}), category:enumeration(['MAINTENANCE','COMMUNITY']), audience:ref('Audience'), status:enumeration(['DRAFT','PUBLISHED']) });
add('AnnouncementUpdate', { title:str({minLength:1,maxLength:160}), body:str({minLength:1,maxLength:10000}), category:enumeration(['MAINTENANCE','COMMUNITY']), audience:ref('Audience'), status:enumeration(['DRAFT','PUBLISHED']), expectedVersion:version });
add('Announcement', { id, title:str(), body:str(), category:enumeration(['MAINTENANCE','COMMUNITY']), audience:ref('Audience'), status:enumeration(['DRAFT','PUBLISHED']), author:ref('DirectoryPerson'), createdAt:timestamp, updatedAt:timestamp, publishedAt:{...timestamp,nullable:true}, version });
add('Notification', { id, type:enumeration(['ANNOUNCEMENT','CHARGE','PAYMENT','REQUEST','SYSTEM']), title:str(), body:str(), target:object({ kind:enumeration(['OVERVIEW','ANNOUNCEMENT','ACCOUNT','REQUEST','PAYMENT_NOTIFICATION']), id:{...id,nullable:true} }), createdAt:timestamp, readAt:{...timestamp,nullable:true} });
add('ReadAll', { before:timestamp });
add('Dashboard', { communityId:id, currency:enumeration(['TRY']), period:date, nextDueOn:{...date,nullable:true}, collectedMinor:integer(), outstandingMinor:integer(), targetMinor:integer(), paidUnitCount:integer(), unitCount:integer(), openRequestCount:integer(), ownAccount:{...ref('UnitAccount'),nullable:true} });
for(const name of ['Community','Unit','DirectoryUnit','Relationship','FinancialAccount','Charge','Payment','UnitAccount','AccountEntry','PaymentNotification','Expense','ExpensePayment','Request','Comment','RequestEvent','Announcement','Notification','DirectoryPerson']) add(`${name}Page`, { items:array(ref(name)), nextCursor:{...str(),nullable:true}, hasMore:bool });
const paths={};
const common = '/communities/{communityId}';
function operation(method,url,operationId,response,body,options={}) {
 const parameters=[];
 for(const match of url.matchAll(/\{(\w+)\}/g))parameters.push({name:match[1],in:'path',required:true,schema:id});
 if(response?.endsWith('Page'))parameters.push({name:'cursor',in:'query',schema:str()},{name:'limit',in:'query',schema:integer({minimum:1,maximum:100,default:20})});
 for(const [name,schema]of Object.entries(options.query??{}))parameters.push({name,in:'query',schema});
 if(options.financial)parameters.push({name:'Idempotency-Key',in:'header',required:true,schema:str({minLength:1,maxLength:100})});
 if(method!=='get'&&(!options.public||options.csrf))parameters.push({name:'X-CSRF-Token',in:'header',required:true,schema:str({minLength:1})});
 const success=options.status??(method==='post'&&response?'201':response?'200':'204');
 const op={operationId,tags:[options.tag??operationId.split(/[A-Z]/)[0]],summary:options.summary??operationId,description:options.description??'Community membership and operation permissions are enforced server-side. All object references must belong to the path community.',security:options.public?[]:[{SessionCookie:[]}],parameters,responses:{[success]:{description:'Success',...(response?{content:{[options.binary?'application/octet-stream':'application/json']:{schema:options.binary?str({format:'binary'}):ref(response)}}}:{})}}};
 if(body)op.requestBody={required:true,content:{[options.multipart?'multipart/form-data':'application/json']:{schema:ref(body)}}};
 for(const code of ['400','401','403','404','409',...(options.multipart?['413']:[]),'422','429','500'])op.responses[code]={description:{400:'Malformed request',401:'Session absent or expired',403:'Permission or CSRF validation failed',404:'Missing or outside scope',409:'Version/state/idempotency conflict',413:'Attachment exceeds size limit',422:'Business or field validation failed',429:'Rate limited',500:'Unexpected server error'}[code],content:{'application/problem+json':{schema:ref('Problem')}}};
 paths[url]??={};paths[url][method]=op;
}
operation('get','/auth/csrf','getCsrf','CsrfToken',null,{public:true});
operation('post','/auth/login','login','Session','LoginRequest',{public:true,csrf:true,status:'200',description:'Creates an HttpOnly Secure SameSite session cookie. Rate limited; invalid credentials use a generic response. Obtain CSRF material before cookie-authenticated writes.'});
operation('get','/auth/session','getSession','Session');operation('post','/auth/logout','logout');
operation('post','/auth/password-reset-requests','requestPasswordReset',null,'ResetRequest',{public:true,status:'202',description:'Always responds generically, regardless of whether an email exists. Rate limited. Reset delivery is a backend responsibility.'});
operation('post','/auth/password-resets','resetPassword',null,'PasswordReset',{public:true,description:'Consumes a single-use, expiring token and invalidates sessions according to the agreed policy.'});
operation('get','/me','getProfile','Profile');operation('patch','/me','updateProfile','Profile','ProfileUpdate');operation('get','/me/preferences','getPreferences','Preferences');operation('patch','/me/preferences','updatePreferences','Preferences','PreferencesUpdate');
operation('get','/communities','listCommunities','CommunityPage');operation('get',common+'/context','getContext','CommunityContext');operation('get',common+'/dashboard','getDashboard','Dashboard',null,{query:{unitId:id,period:str({pattern:'^\\d{4}-\\d{2}$'})}});
operation('get',common+'/units','listUnits','UnitPage',null,{query:{q:str(),blockId:id}});operation('get',common+'/units/{unitId}','getUnit','Unit');operation('get',common+'/units/{unitId}/relationships','listRelationships','RelationshipPage',null,{description:'Manager-only dated ownership/tenancy relationships. Resident directory access never grants this permission.'});
operation('get',common+'/directory','listDirectory','DirectoryUnitPage',null,{query:{q:str(),blockId:id,mine:bool},description:'Returns only current explicit residence names and unit associations within this community. Excludes finances, contact details, ownership/tenancy history and residence history.'});operation('get',common+'/directory/units/{unitId}','getDirectoryUnit','DirectoryUnit');
operation('get',common+'/financial-accounts','listFinancialAccounts','FinancialAccountPage');
operation('get',common+'/unit-accounts','listUnitAccounts','UnitAccountPage');operation('get',common+'/unit-accounts/{unitId}','getUnitAccount','UnitAccount');operation('get',common+'/unit-accounts/{unitId}/entries','listAccountEntries','AccountEntryPage',null,{query:{from:date,to:date},description:'Server-authorized account projection. Resident visibility is limited to owner/tenant relationship periods; retrospective charges use the billing period.'});
operation('get',common+'/charges','listCharges','ChargePage',null,{query:{unitId:id,status:enumeration(['OUTSTANDING','PARTIALLY_PAID','PAID','CANCELLED'])}});
operation('post',common+'/charge-batches','createChargeBatch','ChargeBatch','ChargeBatchRequest',{financial:true,description:'Manager capability required. Explicit per-unit amounts; no implicit area/share allocation. Entire batch commits atomically or fails. Same idempotency key + same payload returns the original result; changed payload returns 409.'});operation('get',common+'/charge-batches/{batchId}','getChargeBatch','ChargeBatch');
operation('post',common+'/charges/{chargeId}/cancellations','cancelCharge','Charge','ReasonCommand',{financial:true,description:'Reason and expected version required. No destructive deletion. Allocated charges cannot be cancelled until the applicable correction workflow releases their allocations.'});
operation('get',common+'/payments','listPayments','PaymentPage',null,{query:{unitId:id}});operation('post',common+'/payments','recordPayment','Payment','PaymentCreate',{financial:true,description:'Atomically records incoming cash, explicit same-unit charge allocations and optional notification link. Sum of allocations cannot exceed payment; each allocation cannot exceed outstanding charge. Unallocated amount is credit. No automatic allocation order.'});operation('post',common+'/payments/{paymentId}/reversals','reversePayment','Payment','ReasonCommand',{financial:true,description:'Retains the original record and audit trail, reverses account effects and releases allocations atomically. Notification link correction policy must be consistent with the returned state.'});
operation('get',common+'/payment-notifications','listPaymentNotifications','PaymentNotificationPage',null,{query:{unitId:id,status:enumeration(['PENDING','REVIEWED','REJECTED','WITHDRAWN','RECORDED'])}});operation('post',common+'/payment-notifications','createPaymentNotification','PaymentNotification','PaymentNotificationCreate');operation('post',common+'/payment-notifications/{notificationId}/reviews','reviewPaymentNotification','PaymentNotification','NotificationReview',{description:'REVIEWED acknowledges inspection only and does not settle debt. Rejection requires a nonblank note.'});operation('post',common+'/payment-notifications/{notificationId}/withdrawals','withdrawPaymentNotification','PaymentNotification','VersionCommand');operation('post',common+'/payment-notifications/{notificationId}/payment-links','linkPaymentNotification','PaymentNotification','PaymentLink',{financial:true,description:'Links to an active same-unit payment of matching amount; may not duplicate a current link. Does not create a second cash movement.'});
operation('get',common+'/expense-payments','listExpensePayments','ExpensePaymentPage',null,{query:{expenseId:id}});
operation('get',common+'/expenses','listExpenses','ExpensePage');operation('post',common+'/expenses','createExpense','Expense','ExpenseCreate',{financial:true,description:'Records incurred expense independently from payment; optional immediate payment is atomic with expense creation.'});operation('get',common+'/expenses/{expenseId}','getExpense','Expense');operation('post',common+'/expenses/{expenseId}/payments','payExpense','ExpensePayment','ExpensePaymentCreate',{financial:true});operation('post',common+'/expense-payments/{expensePaymentId}/reversals','reverseExpensePayment','ExpensePayment','ReasonCommand',{financial:true});
operation('get',common+'/staff','listAssignableStaff','DirectoryPersonPage');operation('get',common+'/requests','listRequests','RequestPage',null,{query:{unitId:id,status:enumeration(['OPEN','IN_PROGRESS','RESOLVED']),q:str()}});operation('post',common+'/requests','createRequest','Request','RequestCreate');operation('get',common+'/requests/{requestId}','getRequest','Request',null,{description:'Managers with capability can access community requests. Residents can access only their own authorized requests. An opaque 404 is used outside the caller scope.'});operation('get',common+'/requests/{requestId}/comments','listRequestComments','CommentPage');operation('post',common+'/requests/{requestId}/comments','addRequestComment','Comment','CommentCreate');operation('post',common+'/requests/{requestId}/assignments','assignRequest','Request','RequestAssignment');operation('post',common+'/requests/{requestId}/status-changes','changeRequestStatus','Request','RequestStatusChange');operation('get',common+'/requests/{requestId}/events','listRequestEvents','RequestEventPage');
operation('get',common+'/announcements','listAnnouncements','AnnouncementPage',null,{query:{status:enumeration(['DRAFT','PUBLISHED']),q:str()},description:'Residents receive published notices matching any authorized current unit membership; drafts and out-of-audience notices are omitted. Managers can filter drafts.'});operation('post',common+'/announcements','createAnnouncement','Announcement','AnnouncementWrite');operation('get',common+'/announcements/{announcementId}','getAnnouncement','Announcement',null,{description:'Applies the same audience rules as collection reads; outside-scope IDs return opaque 404.'});operation('put',common+'/announcements/{announcementId}','updateAnnouncement','Announcement','AnnouncementUpdate',{description:'Manager capability required. Expected version protects concurrent edits. Saving status PUBLISHED publishes to a validated audience; ALL has empty ids, BLOCKS/UNITS require nonempty same-community IDs.'});
operation('get',common+'/notifications','listNotifications','NotificationPage',null,{query:{unread:bool}});operation('put',common+'/notifications/{notificationId}/read','markNotificationRead');operation('post',common+'/notifications/read-all','markAllNotificationsRead',null,'ReadAll');
operation('post',common+'/attachments','uploadAttachment','Attachment','Upload',{multipart:true,description:'Multipart file upload. Maximum 5 MB. REQUEST_PHOTO permits PNG/JPEG/WebP; receipts/documents permit PNG/JPEG/PDF. Validate content signatures, ownership and purpose server-side. Request creation permits at most 3 photo IDs.'});operation('get',common+'/attachments/{attachmentId}','getAttachment','Attachment');operation('get',common+'/attachments/{attachmentId}/content','downloadAttachment','Attachment',null,{binary:true,description:'Authenticated, authorized binary download. Use safe Content-Disposition filename and nosniff; do not expose public storage keys.'});
// Explicit access metadata is intended for backend implementation and contract review.
for (const [url, verbs] of Object.entries(paths)) for (const [method, op] of Object.entries(verbs)) {
  const write = method !== 'get';
  let access = { roles: ['MANAGER', 'RESIDENT'], scope: 'COMMUNITY_MEMBERSHIP', permissions: ['COMMUNITY_READ'] };
  if (url.startsWith('/auth')) access = { roles: [], scope: op.security.length ? 'AUTHENTICATED_SESSION' : 'PUBLIC_RATE_LIMITED', permissions: [] };
  else if (url.startsWith('/me')) access = { roles: ['MANAGER','RESIDENT'], scope: 'CURRENT_USER', permissions: [] };
  else if (url === '/communities') access = { roles: ['MANAGER','RESIDENT'], scope: 'CURRENT_USER_MEMBERSHIPS', permissions: [] };
  else if (/staff/.test(url)) access = { roles:['MANAGER'], scope:'COMMUNITY_MEMBERSHIP', permissions:['REQUEST_MANAGE'] };
  else if (/relationships|financial-accounts|expenses|expense-payments|charge-batches/.test(url)) access = { roles:['MANAGER'], scope:'COMMUNITY_MEMBERSHIP', permissions:[write ? /charge-batches/.test(url) ? 'CHARGE_CREATE' : 'EXPENSE_WRITE' : 'FINANCE_READ'] };
  else if (/payment-notifications/.test(url)) access = { roles: /reviews|payment-links/.test(url) ? ['MANAGER'] : write ? ['RESIDENT'] : ['MANAGER','RESIDENT'], scope: 'MANAGER_COMMUNITY_OR_RESIDENT_OWN_UNIT', permissions: /reviews|payment-links/.test(url) ? ['PAYMENT_RECORD'] : [] };
  else if (/charges|payments|unit-accounts/.test(url)) access = { roles:write?['MANAGER']:['MANAGER','RESIDENT'], scope:write?'COMMUNITY_MEMBERSHIP':'MANAGER_COMMUNITY_OR_RESIDENT_AUTHORIZED_ACCOUNT_PROJECTION', permissions:write?[/charges/.test(url)?'CHARGE_CREATE':'PAYMENT_RECORD']:['FINANCE_READ'] };
  else if (/requests/.test(url)) access = { roles:/assignments|status-changes|events/.test(url)&&write?['MANAGER']:['MANAGER','RESIDENT'],scope:'MANAGER_COMMUNITY_OR_RESIDENT_CREATED_REQUEST',permissions: /assignments|status-changes/.test(url)?['REQUEST_MANAGE']:[] };
  else if (/announcements/.test(url)) access = {roles:write?['MANAGER']:['MANAGER','RESIDENT'],scope:write?'COMMUNITY_MEMBERSHIP':'MANAGER_COMMUNITY_OR_PUBLISHED_MATCHING_AUDIENCE',permissions:write?['ANNOUNCEMENT_WRITE']:[]};
  else if (/directory/.test(url)) access = {roles:['MANAGER','RESIDENT'],scope:'CURRENT_RESIDENTS_IN_OWN_COMMUNITY_ONLY',permissions:['DIRECTORY_READ']};
  else if (/notifications/.test(url)) access = {roles:['MANAGER','RESIDENT'],scope:'CURRENT_USER_IN_COMMUNITY',permissions:[]};
  else if (/attachments/.test(url)) access = {roles:['MANAGER','RESIDENT'],scope:'OWNER_OR_AUTHORIZED_PARENT_RESOURCE_AND_PURPOSE',permissions:[]};
  if (op.operationId === 'getDashboard') access.permissions = ['COMMUNITY_READ','FINANCE_READ'];
  op['x-access'] = access;
}
// Optional object relationships are omitted when absent; primitive nullable fields remain explicit.
for (const schema of Object.values(schemas)) for (const [key, value] of Object.entries(schema.properties ?? {})) {
  if (value.$ref && value.nullable) { schema.properties[key] = { $ref: value.$ref }; schema.required = schema.required.filter(field => field !== key); }
}
const doc={openapi:'3.0.3',info:{title:'Govener Resident and Manager API',version:'0.1.0',description:'Frontend-driven contract implemented by the Spring Boot backend in backend/. See BACKEND-HANDOFF.md for open product decisions and exact integration boundaries. Amounts are integer TRY minor units (kuruş); dates are ISO calendar dates, timestamps UTC instants, and community time zone is Europe/Istanbul. Localized UI labels never act as IDs or enum values.'},servers:[{url:'/api/v1'}],security:[{SessionCookie:[]}],paths,components:{securitySchemes:{SessionCookie:{type:'apiKey',in:'cookie',name:'govener_session',description:'HttpOnly Secure SameSite cookie; never persist session secrets in web storage.'}},schemas}};
writeGenerated(path.join(__dirname,'../../docs/openapi.json'),JSON.stringify(doc,null,2)+'\n');
console.log(`Generated ${Object.values(paths).reduce((sum,item)=>sum+Object.keys(item).length,0)} operations and ${Object.keys(schemas).length} schemas.`);
