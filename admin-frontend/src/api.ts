export type Role = 'ROOT' | 'ADMIN';
export interface Session {user:{id:string;name:string};role:Role;communityIds:string[];authorityVersion:number}
export interface Community {id:string;name:string;address:string;version:number}
export interface Unit {id:string;communityId:string;label:string;blockId:string;blockLabel:string;floor:number;kind:string;areaSqm:number;version:number}
export interface User {id:string;name:string;loginEmail:string;phone:string;roles:string[]}
export interface Administrator {id:string;name:string;loginEmail:string;phone:string;communityIds:string[];version:number}
export interface Page<T> {items:T[];nextCursor:string|null;hasMore:boolean}
let csrf = '';
export class ApiError extends Error {constructor(public status:number, message:string){super(message)}}
export async function request<T>(path:string,method='GET',body?:unknown,signal?:AbortSignal):Promise<T>{
 const response=await fetch('/api/v1'+path,{method,credentials:'include',signal,headers:{...(body===undefined?{}:{'Content-Type':'application/json'}),...(method==='GET'?{}:{'X-CSRF-Token':csrf})},body:body===undefined?undefined:JSON.stringify(body)});
 const data=response.status===204?undefined:await response.json().catch(()=>undefined);
 if(!response.ok)throw new ApiError(response.status,data?.detail||data?.message||`HTTP ${response.status}`);
 return data as T;
}
export async function restore(){const auth=await request<{csrfToken:string}>('/auth/session');csrf=auth.csrfToken;return request<Session>('/admin/session')}
export async function login(email:string,password:string){csrf=(await request<{csrfToken:string}>('/auth/csrf')).csrfToken;const auth=await request<{csrfToken:string}>('/auth/login','POST',{email,password});csrf=auth.csrfToken;return request<Session>('/admin/session')}
export async function logout(){await request('/auth/logout','POST');csrf=''}
export async function all<T>(path:string,signal?:AbortSignal):Promise<T[]>{let cursor:string|null=null;const result:T[]=[];do{const page:Page<T>=await request(path+(path.includes('?')?'&':'?')+'limit=100'+(cursor?'&cursor='+encodeURIComponent(cursor):''),'GET',undefined,signal);result.push(...page.items);cursor=page.hasMore?page.nextCursor:null;if(page.hasMore&&!cursor)throw new Error('Missing page cursor');}while(cursor);return result;}
