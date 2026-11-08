const fs=require('node:fs');
const path=require('node:path');
function writeGenerated(file, content) {
  if (process.argv.includes('--check')) {
    if (!fs.existsSync(file) || fs.readFileSync(file, 'utf8') !== content) throw new Error(file + ' is out of date. Run npm run api:generate.');
  } else fs.writeFileSync(file, content);
}

const contract=JSON.parse(fs.readFileSync(path.join(__dirname,'../../docs/openapi.json'),'utf8'));
function type(schema) {
 if(!schema)return 'undefined';
 let result;
 if(schema.$ref)result=schema.$ref.split('/').pop();
 else if(schema.enum)result=schema.enum.map(value=>JSON.stringify(value)).join(' | ');
 else if(schema.type==='array')result=`Array<${type(schema.items)}>`;
 else if(schema.type==='object')result=`{ ${Object.entries(schema.properties??{}).map(([key,value])=>`${JSON.stringify(key)}${schema.required?.includes(key)?'':'?'}: ${type(value)}`).join('; ')} }`;
 else if(schema.type==='integer'||schema.type==='number')result='number';
 else if(schema.type==='boolean')result='boolean';
 else if(schema.format==='binary')result='Blob';
 else result='string';
 return schema.nullable?`${result} | null`:result;
}
let output='// Generated from docs/openapi.json. Run npm run api:generate; do not edit by hand.\n';
for(const[name,schema]of Object.entries(contract.components.schemas))output+=`export type ${name} = ${type(schema)};\n`;
const meta={}; const definitions=[];
for(const[url,verbs]of Object.entries(contract.paths))for(const[method,op]of Object.entries(verbs)){
 const pathParams=op.parameters.filter(parameter=>parameter.in==='path'); const query=op.parameters.filter(parameter=>parameter.in==='query');
 const content=op.requestBody?.content;const multipart=!!content?.['multipart/form-data'];const body=content?.['application/json']?.schema;
 const response=Object.entries(op.responses).find(([code])=>Number(code)>=200&&Number(code)<300)?.[1];
 const binary=!!response.content?.['application/octet-stream'];const responseSchema=response.content?.['application/json']?.schema;
 const financial=op.parameters.some(parameter=>parameter.name==='Idempotency-Key'); const csrf=op.parameters.some(parameter=>parameter.name==='X-CSRF-Token');
 definitions.push(`${op.operationId}: { ${pathParams.length?`params: { ${pathParams.map(parameter=>`${parameter.name}: ${type(parameter.schema)}`).join('; ')} }; `:''}${query.length?`query?: { ${query.map(parameter=>`${parameter.name}?: ${type(parameter.schema)}`).join('; ')} }; `:''}${multipart?'body: FormData; ':body?`body: ${type(body)}; `:''}${financial?'idempotencyKey: string; ':''}response: ${binary?'Blob':type(responseSchema)} }`);
 meta[op.operationId]={method:method.toUpperCase(),path:url,multipart,binary,financial,csrf};
}
output+=`\nexport interface Operations {\n${definitions.map(line=>'  '+line).join('\n')}\n}\nexport const operationMetadata = ${JSON.stringify(meta,null,2)} as const;\n`;
writeGenerated(path.join(__dirname,'../src/api/generated.ts'),output);
console.log('Generated typed DTOs and operation metadata.');

let inventory = '# API Endpoint Inventory\n\nGenerated from [openapi.json](openapi.json). Edit the contract builder and run `npm run api:generate`; do not edit this table by hand.\n\nBase URL: `/api/v1`. Access is enforced server-side. See [Backend Handoff](BACKEND-HANDOFF.md) for conventions and open decisions.\n\n| Operation | Method and path | Request | Success response | Roles / scope | Permissions |\n|---|---|---|---|---|---|\n';
for (const [url, verbs] of Object.entries(contract.paths)) for (const [method, operation] of Object.entries(verbs)) {
  const content = operation.requestBody?.content;
  const input = content?.['application/json']?.schema ?? content?.['multipart/form-data']?.schema;
  const [code, response] = Object.entries(operation.responses).find(([code]) => Number(code) >= 200 && Number(code) < 300);
  const schema = response.content?.['application/json']?.schema;
  const access = operation['x-access'];
  inventory += '| `' + operation.operationId + '` | `' + method.toUpperCase() + ' ' + url + '` | ' + (input?.$ref?.split('/').pop() ?? '—') + ' | ' + code + ' ' + (schema?.$ref?.split('/').pop() ?? (response.content ? 'binary' : 'no body')) + ' | ' + (access.roles.join(', ') || 'session/public') + ' · ' + access.scope + ' | ' + (access.permissions.join(', ') || '—') + ' |\n';
}
writeGenerated(path.join(__dirname, '../../docs/API-ENDPOINTS.md'), inventory);
