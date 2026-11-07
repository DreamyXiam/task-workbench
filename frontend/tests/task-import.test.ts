import { expect,it } from 'vitest'
import { parseTaskRows } from '../src/task-import'
it('imports each populated row and preserves multiline detail',()=>{
 const rows=parseTaskRows('序号\t工作内容\t推进情况\t存在问题\t解决建议\t需要领导协调解决工作\t时间要求\t备注\n1\t测试事项\t"第一行\n第二行"\t问题\t建议\t协调\t月底\t备注\n2\t\t\t\t\t\t\t')
 expect(rows).toHaveLength(1)
 expect(rows[0].title).toBe('测试事项')
 expect(rows[0].content).toContain('第一行\n第二行')
 expect(rows[0].content).toContain('时间要求：月底')
})
