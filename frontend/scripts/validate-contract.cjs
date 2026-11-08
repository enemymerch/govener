const SwaggerParser = require('@apidevtools/swagger-parser');
const path = require('node:path');
SwaggerParser.validate(path.join(__dirname, '../../docs/openapi.json')).then(api => {
 const operations = Object.values(api.paths).flatMap(item => Object.values(item)).filter(item => item.operationId);
 if (new Set(operations.map(item => item.operationId)).size !== operations.length) throw Error('Duplicate operationId');
 console.log(`Validated OpenAPI ${api.openapi}: ${operations.length} operations.`);
}).catch(error => { console.error(error.message); process.exit(1); });
