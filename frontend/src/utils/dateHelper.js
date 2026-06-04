import dayjs from 'dayjs';

// 获取某月的天数
export const getDaysInMonth = (year, month) => {
    return dayjs(`${year}-${month}-01`).daysInMonth();
};

// 获取某月第一天是星期几（0=周日，1=周一...）
export const getFirstDayOfMonth = (year, month) => {
    return dayjs(`${year}-${month}-01`).day();
};

// 格式化日期
export const formatDate = (year, month, day) => {
    return dayjs(`${year}-${month}-${day}`).format('YYYY-MM-DD');
};