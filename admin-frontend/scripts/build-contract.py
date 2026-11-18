"""Generate the separate administration OpenAPI contract. Run from any directory."""
import json
from pathlib import Path
import sys

root = Path(__file__).resolve().parents[2]
base = json.loads((root / 'docs/openapi.json').read_text(encoding='utf-8'))
schemas = dict(base['components']['schemas'])
def ref(name): return {'$ref': '#/components/schemas/' + name}
def text(**extra): return dict(type='string', **extra)
def array(item): return {'type': 'array', 'items': item}
def obj(properties, required=None): return {'type':'object','additionalProperties':False,'properties':properties,'required':list(properties) if required is None else required}
integer = {'type':'integer','format':'int64','minimum':0}
community = {'id':text(), 'name':text(minLength=1,maxLength=200), 'address':text(minLength=1,maxLength=1000),'version':integer}
schemas['AdminCommunity']=obj(community)
schemas['AdminCommunityCreate']=obj({k:v for k,v in community.items() if k not in ['id','version']})
schemas['AdminCommunityUpdate']=obj(dict(schemas['AdminCommunityCreate']['properties'],expectedVersion=integer))
unit = {'label':text(minLength=1,maxLength=100),'blockId':text(pattern='^[A-Za-z0-9][A-Za-z0-9_-]{0,99}$'),'blockLabel':text(minLength=1,maxLength=200),'floor':{'type':'integer','format':'int32'},'kind':text(enum=['APARTMENT','SHOP','OFFICE']),'areaSqm':{'type':'number','minimum':0}}
schemas['AdminUnitCreate']=obj(unit)
schemas['AdminUnitUpdate']=obj(dict(unit,expectedVersion=integer))
schemas['AdminUnit']=obj(dict(unit,id=text(),communityId=text(),version=integer))
permissions=['COMMUNITY_READ','UNIT_READ','DIRECTORY_READ','FINANCE_READ','CHARGE_CREATE','PAYMENT_RECORD','EXPENSE_WRITE','REQUEST_MANAGE','ANNOUNCEMENT_WRITE','POLL_MANAGE']
permission_set=dict(array(text(enum=permissions)),uniqueItems=True)
scopes=dict(array(text()),uniqueItems=True,maxItems=1000)
credentials={'name':text(minLength=1,maxLength=200),'loginEmail':text(format='email'),'phone':text(maxLength=50),'password':text(minLength=12,maxLength=128,writeOnly=True)}
schemas['AdminUserCreate']=obj(dict(credentials,role=text(enum=['MANAGER','RESIDENT']),permissions=permission_set,unitId=text(),relationshipType=text(enum=['OWNER','TENANT']),startsOn=text(format='date'),endsOn=text(format='date',nullable=True),livesHere={'type':'boolean'}),list(credentials)+['role','permissions','livesHere'])
schemas['AdminUserCreate']['description']='RESIDENT requires unitId, relationshipType and startsOn, with exactly the four read permissions. MANAGER requires COMMUNITY_READ and rejects relationship fields. Email must identify a new account; no existing-account linking.'
schemas['AdminUser']=obj({'id':text(),'name':text(),'loginEmail':text(format='email'),'email':text(format='email'),'phone':text(),'roles':array(text(enum=['MANAGER','RESIDENT'])),'permissions':permission_set,'unitAccess':array(ref('UnitAccess')),'version':integer,'membershipVersion':integer})
schemas['AdministratorCreate']=obj(dict(credentials,communityIds=scopes))
schemas['Administrator']=obj({'id':text(),'name':text(),'loginEmail':text(format='email'),'phone':text(),'communityIds':scopes,'version':integer})
schemas['AdministratorAssignment']=obj({'communityIds':scopes,'expectedVersion':integer})
schemas['AdminSession']=obj({'user':ref('Profile'),'role':text(enum=['ROOT','ADMIN']),'communityIds':scopes,'authorityVersion':integer})
for name in ['AdminCommunity','AdminUnit','AdminUser','Administrator']:
 schemas[name+'Page']=obj({'items':array(ref(name)),'nextCursor':text(nullable=True),'hasMore':{'type':'boolean'}})
paths={}
def operation(path,method,id,response,body=None,root_only=False,page=False):
 parameters=[{'name':piece[1:-1],'in':'path','required':True,'schema':text()} for piece in path.split('/') if piece.startswith('{')]
 if page: parameters += [{'name':'cursor','in':'query','schema':text()},{'name':'limit','in':'query','schema':{'type':'integer','minimum':1,'maximum':100,'default':20}}]
 if path.endswith('/units') or path.endswith('/users'):
  if method=='get': parameters.append({'name':'q','in':'query','schema':text()})
 status='201' if method=='post' else '200'
 responses={status:{'description':'Success','content':{'application/json':{'schema':ref(response)}}}}
 for code in ['400','401','403','404','409','422','429','500']:
  responses[code]={'description':'Request rejected','content':{'application/problem+json':{'schema':ref('Problem')}}}
 op={'operationId':id,'summary':id,'x-access':'ROOT' if root_only else 'ROOT or assigned ADMIN','security':[{'sessionCookie':[]}],'parameters':parameters,'responses':responses}
 if method!='get':parameters.append({'name':'X-CSRF-Token','in':'header','required':True,'schema':text()})
 if body:op['requestBody']={'required':True,'content':{'application/json':{'schema':ref(body)}}}
 paths.setdefault(path,{})[method]=op
operation('/admin/session','get','getAdminSession','AdminSession')
operation('/admin/communities','get','listAdminCommunities','AdminCommunityPage',page=True)
operation('/admin/communities','post','createCommunity','AdminCommunity','AdminCommunityCreate',True)
operation('/admin/communities/{cid}','put','updateCommunity','AdminCommunity','AdminCommunityUpdate',True)
operation('/admin/communities/{cid}/units','get','listAdminUnits','AdminUnitPage',page=True)
operation('/admin/communities/{cid}/units','post','createUnit','AdminUnit','AdminUnitCreate')
operation('/admin/communities/{cid}/units/{id}','put','updateUnit','AdminUnit','AdminUnitUpdate')
operation('/admin/communities/{cid}/users','get','listAdminUsers','AdminUserPage',page=True)
operation('/admin/communities/{cid}/users','post','createUser','AdminUser','AdminUserCreate')
operation('/admin/administrators','get','listAdministrators','AdministratorPage',root_only=True,page=True)
operation('/admin/administrators','post','createAdministrator','Administrator','AdministratorCreate',True)
operation('/admin/administrators/{id}/communities','put','assignAdministratorCommunities','Administrator','AdministratorAssignment',True)
for path in ['/auth/csrf','/auth/login','/auth/session','/auth/logout']:paths[path]=base['paths'][path]
doc={'openapi':'3.0.3','info':{'title':'Govener Administration API','version':'0.1.0','description':'Separate administrative authority. ROOT accesses every community; ADMIN only assigned communities. Manager/resident memberships grant no administrative access. Root is bootstrapped through deployment configuration, never HTTP.'},'servers':[{'url':'/api/v1'}],'paths':paths,'components':{'schemas':schemas,'securitySchemes':base['components']['securitySchemes']}}
target=root/'docs/admin-openapi.json'
content=json.dumps(doc,indent=2,ensure_ascii=False)+'\n'
if '--check' in sys.argv:
 if not target.exists() or target.read_text(encoding='utf-8')!=content:raise SystemExit('Admin contract is out of date')
else:target.write_text(content,encoding='utf-8')
