import api from './api';

export const cronjobService = {
    getAllCronjobs: () => api.get('/cronjob/list'),

    updatePoolSize: (cronjobName, poolSize, executor, description = "") =>
        api.put(`/cronjob/${cronjobName}/poolSize`, null, { params: { poolSize, executor, description } }),

    updateExpression: (cronjobName, expression, executor, description = "") =>
        api.put(`/cronjob/${cronjobName}/expression`, null, { params: { expression, executor, description } }),

    schedule: (cronjobName, executor, description = "") =>
        api.post(`/cronjob/${cronjobName}/schedule`, null, { params: { executor, description } }),

    cancel: (cronjobName, executor, description = "") =>
        api.post(`/cronjob/${cronjobName}/cancel`, null, { params: { executor, description } }),

    start: (cronjobName, executor, description = "") =>
        api.post(`/cronjob/${cronjobName}/start`, null, { params: { executor, description } }),

    stop: (cronjobName, executor, description = "") =>
        api.post(`/cronjob/${cronjobName}/stop`, null, { params: { executor, description } }),

    getChangeHistory: (cronjobName) =>
        api.get(`/cronjob/${cronjobName}/history/logs`),

    getRunningHistory: (cronjobName) =>
        api.get(`/cronjob/${cronjobName}/history/running`),

    getTracingLogs: (cronjobName, sessionId) =>
        api.get(`/cronjob/${cronjobName}/tracing/logs`, { params: { sessionId } }),
};
