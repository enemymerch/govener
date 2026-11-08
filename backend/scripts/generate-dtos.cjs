const fs=require('node:fs');const path=require('node:path');const schema=JSON.parse(fs.readFileSync('docs/openapi.json','utf8')).components.schemas;
function expand(name,record){for(const[key,value]of Object.entries(record.properties??{})){const target=value.type==='array'?value.items:value;if(target?.type==='object'&&target.properties){const nested=name+key.charAt(0).toUpperCase()+key.slice(1)+(value.type==='array'?'Item':'');schema[nested]=target;expand(nested,target);if(value.type==='array')value.items={$ref:'#/components/schemas/'+nested};else record.properties[key]={$ref:'#/components/schemas/'+nested};}}}
for(const[name,record]of Object.entries(schema))expand(name,record);
const dir='backend/src/main/java/com/govener/adapter/in/web/dto';fs.mkdirSync(dir,{recursive:true});
function type(value){if(value.$ref)return value.$ref.split('/').pop();if(value.type==='array')return `List<${type(value.items)}>`;if(value.type==='integer')return 'Long';if(value.type==='number')return 'Double';if(value.type==='boolean')return 'Boolean';if(value.type==='object')return 'Map<String,Object>';if(value.format==='date')return 'LocalDate';if(value.format==='date-time')return 'Instant';if(value.format==='binary')return 'byte[]';return 'String';}
for(const[name,record]of Object.entries(schema)){
 const components=Object.entries(record.properties).map(([key,value])=>{
  let annotations=['@Valid'];if(record.required.includes(key)&&!value.nullable)annotations.push('@NotNull');
  if(value.minLength===1&&record.required.includes(key)&&!value.nullable)annotations.push('@NotBlank');
  if(value.minLength||value.maxLength)annotations.push(`@Size(min=${value.minLength??0},max=${value.maxLength??2147483647})`);
  if(value.type==='array'&&(value.minItems||value.maxItems))annotations.push(`@Size(min=${value.minItems??0},max=${value.maxItems??2147483647})`);
  if(value.minimum!==undefined&&value.type==='integer')annotations.push(`@Min(${value.minimum}L)`);
  if(value.maximum!==undefined&&value.type==='integer')annotations.push(`@Max(${value.maximum}L)`);
  if(value.enum)annotations.push(`@Pattern(regexp="${value.enum.join('|')}")`);
  if(value.format==='email')annotations.push('@Email');
  if(value.pattern)annotations.push(`@Pattern(regexp=${JSON.stringify(value.pattern)})`);
  return '    '+annotations.join(' ')+' '+type(value)+' '+key;
 });
 fs.writeFileSync(path.join(dir,name+'.java'),`package com.govener.adapter.in.web.dto;\n\nimport jakarta.validation.Valid;\nimport jakarta.validation.constraints.*;\nimport java.time.*;\nimport java.util.*;\n\n/** Transport contract generated from docs/openapi.json; domain models are separate. */\npublic record ${name}(\n${components.join(',\n')}\n) {${Object.keys(record.properties).some(key=>["password","token"].includes(key))?`\n  @Override public String toString(){return "${name}[REDACTED]";}\n`:""}}\n`);
}
console.log('Generated '+Object.keys(schema).length+' transport records.');
