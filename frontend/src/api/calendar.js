import axios from 'axios';

// 后端接口地址（根据你的后端端口修改）
const BASE_URL = 'http://localhost:8080';

// 创建 axios 实例
const request = axios.create({
    baseURL: BASE_URL,
    timeout: 10000,
});

// 获取月份数据（日程+备忘录标记）
export const getMonthData = async (year, month) => {
    try {
        const response = await request.get('/api/calendar/month-data', {
            params: { year, month }
        });
        // 后端返回格式：{ code: 200, message: "success", data: {...} }
        if (response.data.code === 200) {
            return { data: response.data.data };
        } else {
            console.error('获取月份数据失败', response.data.message);
            return { data: { events: {}, totalEvents: 0, totalSchedules: 0, totalNotes: 0, markedDays: 0 } };
        }
    } catch (error) {
        console.error('请求失败:', error);
        // 返回空数据，避免前端报错
        return { data: { events: {}, totalEvents: 0, totalSchedules: 0, totalNotes: 0, markedDays: 0 } };
    }
};

// 获取日详情
export const getDayDetail = async (date) => {
    try {
        const response = await request.get('/api/calendar/day-detail', {
            params: { date }
        });
        if (response.data.code === 200) {
            return { data: response.data.data };
        } else {
            console.error('获取日详情失败', response.data.message);
            return { data: { schedules: [], notes: [] } };
        }
    } catch (error) {
        console.error('请求失败:', error);
        return { data: { schedules: [], notes: [] } };
    }
};