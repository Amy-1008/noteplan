import axios from 'axios';

const request = axios.create({
    baseURL: '/api',
    timeout: 5000
});

// 获取月份完整数据
export const getMonthData = async (year, month) => {
    return new Promise((resolve) => {
        setTimeout(() => {
            const events = {};
            let totalSchedules = 0;
            let totalNotes = 0;
            const markedDates = new Set();

            // 生成模拟数据 - 更丰富的内容
            for (let i = 1; i <= 31; i++) {
                const date = `${year}-${String(month).padStart(2, '0')}-${String(i).padStart(2, '0')}`;
                const dayEvents = [];

                // 日程（第1,5,10,15,20,25,30天）
                if ([1, 5, 10, 15, 20, 25, 30].includes(i)) {
                    dayEvents.push({
                        id: `s-${i}`,
                        type: 'schedule',
                        title: i === 1 ? '月度计划会' :
                            i === 5 ? '项目评审' :
                                i === 10 ? '团队周会' :
                                    i === 15 ? '需求讨论' :
                                        i === 20 ? '代码审查' :
                                            i === 25 ? '进度汇报' : '复盘总结',
                        time: '14:00',
                        status: 'pending'
                    });
                    totalSchedules++;
                    markedDates.add(date);
                }

                // 备忘录（第3,8,12,18,22,28天）
                if ([3, 8, 12, 18, 22, 28].includes(i)) {
                    dayEvents.push({
                        id: `n-${i}`,
                        type: 'note',
                        title: i === 3 ? '产品方案思路' :
                            i === 8 ? '用户反馈整理' :
                                i === 12 ? '技术难点记录' :
                                    i === 18 ? '下周工作计划' :
                                        i === 22 ? '设计稿修改建议' : '学习笔记',
                        content: '详细内容请点击查看...'
                    });
                    totalNotes++;
                    markedDates.add(date);
                }

                // 同一天既有日程又有备忘录
                if ([15, 25].includes(i) && dayEvents.length === 1) {
                    // 已经在上面添加了，这里确保有两个事件
                    if (i === 15 && dayEvents.length === 1) {
                        dayEvents.push({
                            id: `n-${i}-2`,
                            type: 'note',
                            title: '会议纪要',
                            content: '记录会议要点...'
                        });
                        totalNotes++;
                    }
                    if (i === 25 && dayEvents.length === 1) {
                        dayEvents.push({
                            id: `n-${i}-2`,
                            type: 'note',
                            title: '工作总结',
                            content: '本月工作总结...'
                        });
                        totalNotes++;
                    }
                }

                if (dayEvents.length > 0) {
                    events[date] = dayEvents;
                }
            }

            resolve({
                data: {
                    events,
                    totalEvents: totalSchedules + totalNotes,
                    totalSchedules,
                    totalNotes,
                    markedDays: markedDates.size
                }
            });
        }, 200);
    });
};

// 获取单日详情
export const getDayDetail = async (date) => {
    return new Promise((resolve) => {
        setTimeout(() => {
            const day = parseInt(date.split('-')[2]);

            // 根据日期返回不同的详情数据
            const schedules = [];
            const notes = [];

            // 日程详情
            if ([1, 5, 10, 15, 20, 25, 30].includes(day)) {
                schedules.push({
                    id: 1,
                    title: day === 1 ? '月度计划会' :
                        day === 5 ? '项目评审' :
                            day === 10 ? '团队周会' :
                                day === 15 ? '需求讨论' :
                                    day === 20 ? '代码审查' :
                                        day === 25 ? '进度汇报' : '复盘总结',
                    time: '14:00 - 15:30',
                    location: day === 1 ? '会议室A' : '线上会议',
                    remark: day === 1 ? '讨论本月工作重点和目标' :
                        day === 5 ? '评审项目代码质量和进度' :
                            day === 10 ? '同步各小组工作进展' :
                                day === 15 ? '讨论新功能需求细节' :
                                    day === 20 ? '审查代码规范和性能' :
                                        day === 25 ? '向领导汇报项目进度' : '总结本月工作得失',
                    status: 'pending'
                });
            }

            // 备忘录详情
            if ([3, 8, 12, 18, 22, 28].includes(day)) {
                notes.push({
                    id: 1,
                    title: day === 3 ? '产品方案思路' :
                        day === 8 ? '用户反馈整理' :
                            day === 12 ? '技术难点记录' :
                                day === 18 ? '下周工作计划' :
                                    day === 22 ? '设计稿修改建议' : '学习笔记',
                    content: day === 3 ? '1. 优化首页布局\n2. 增加搜索功能\n3. 提升加载速度' :
                        day === 8 ? '收到12条用户反馈，主要集中在界面美观度和操作流畅性' :
                            day === 12 ? '日历组件性能优化方案：\n- 使用虚拟滚动\n- 懒加载数据\n- 缓存月份视图' :
                                day === 18 ? '下周重点：\n1. 完成日历模块开发\n2. 对接后端接口\n3. 编写测试用例' :
                                    day === 22 ? '建议把颜色改成更明亮的淡蓝色，增加阴影效果' : '学习了Vue 3 Composition API的最佳实践',
                    createTime: '09:30',
                    tags: day === 3 ? ['产品', '规划'] :
                        day === 8 ? ['用户', '反馈'] :
                            day === 12 ? ['技术', '优化'] :
                                day === 18 ? ['计划', '工作'] :
                                    day === 22 ? ['设计', 'UI'] : ['学习', '笔记']
                });
            }

            // 如果同一天既有日程又有备忘录，确保都有数据
            if (day === 15) {
                if (schedules.length === 0) {
                    schedules.push({
                        id: 1,
                        title: '需求讨论会',
                        time: '10:00 - 11:30',
                        location: '会议室B',
                        remark: '讨论日历功能的具体需求',
                        status: 'pending'
                    });
                }
                if (notes.length === 0) {
                    notes.push({
                        id: 1,
                        title: '会议纪要',
                        content: '确定使用Vue 3 + Element Plus开发\n后端接口需要提供月份标记和详情',
                        createTime: '11:30',
                        tags: ['会议', '记录']
                    });
                }
            }

            if (day === 25) {
                if (schedules.length === 0) {
                    schedules.push({
                        id: 1,
                        title: '项目进度汇报',
                        time: '15:00 - 16:00',
                        location: '线上会议',
                        remark: '向项目经理汇报日历模块进度',
                        status: 'completed'
                    });
                }
                if (notes.length === 0) {
                    notes.push({
                        id: 1,
                        title: '工作总结',
                        content: '本月完成：\n✓ 日历基础视图\n✓ 月份切换功能\n✓ 事件展示\n进行中：\n○ 事件详情弹窗\n○ 数据持久化',
                        createTime: '17:00',
                        tags: ['总结', '工作']
                    });
                }
            }

            resolve({
                data: { schedules, notes }
            });
        }, 200);
    });
};