export function menuGroup(path:string,role:string){
 return role==='ADMIN'&&path.startsWith('/admin/')?'administration':'workspace'
}
export function navigationMotion(from:string,to:string,role:string):string{
 if(from===to)return 'page-none'
 return menuGroup(from,role)===menuGroup(to,role)?'page-slide':'page-fade'
}
