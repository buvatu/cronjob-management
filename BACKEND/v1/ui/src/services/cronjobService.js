import api from './api';

export const cronjobService = {
    getAllCronjobs: () => api.get('/cronjob/list'),

    updatePoolSize: (cronjobName, poolSize, executor, description = "", version) =>
        api.put(`/cronjob/${cronjobName}/poolSize`, null, { params: { poolSize, executor, description, version } }),

    updateExpression: (cronjobName, expression, executor, description = "", version) =>
        api.put(`/cronjob/${cronjobName}/expression`, null, { params: { expression, executor, description, version } }),

    schedule: (cronjobName, executor, description = "") =>
        api.post(`/cronjob/${cronjobName}/schedule`, null, { params: { executor, description } }),

    cancel: (cronjobName, executor, description = "") =>
        api.post(`/cronjob/${cronjobName}/cancel`, null, { params: { executor, description } }),

    start: (cronjobName, executor, description = "") =>
        api.post(`/cronjob/${cronjobName}/start`, null, { params: { executor, description } }),

    stop: (cronjobName, executor, description = "") =>
        api.post(`/cronjob/${cronjobName}/stop`, null, { params: { executor, description } }),

    getChangeHistory: (cronjobName, page = 0, size = 10) =>
        api.get(`/cronjob/${cronjobName}/history/logs`, { params: { page, size } }),

    getRunningHistory: (cronjobName, page = 0, size = 10) =>
        api.get(`/cronjob/${cronjobName}/history/running`, { params: { page, size } }),

    getTracingLogs: (cronjobName, sessionId) =>
        api.get(`/cronjob/${cronjobName}/tracing/logs`, { params: { sessionId } }),

    getJobDetail: (cronjobName) =>
        api.get(`/cronjob/${cronjobName}/detail`),
};
