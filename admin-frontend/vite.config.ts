import { defineConfig } from 'vite';
export default defineConfig({base:process.env.ADMIN_BASE_PATH||'/',server:{host:'127.0.0.1',port:5174,strictPort:true,proxy:{'/api':{target:process.env.GOVENER_API_TARGET||'http://127.0.0.1:8080',changeOrigin:true}}}});
