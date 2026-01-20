import { useState, useEffect, useCallback } from 'react';
import { useParams } from 'react-router-dom';
import clsx from 'clsx';
import { cronjobService } from '../services/cronjobService';
import JobConfig from '../components/JobConfig';
import JobOperations from '../components/JobOperations';
import JobExecutionHistory from '../components/JobExecutionHistory';

export default function JobDetails() {
    const { cronjobName } = useParams();
    const [activeTab, setActiveTab] = useState('config');
    const [jobInfo, setJobInfo] = useState(null);

    const tabs = [
        { id: 'config', label: 'Job Config' },
        { id: 'operations', label: 'Job Operations' },
        { id: 'executions', label: 'Execution History' },
    ];


    const fetchJobInfo = useCallback(async () => {
        try {
            const response = await cronjobService.getJobDetail(cronjobName);
            if (response.data) {
                setJobInfo(response.data);
            }
        } catch (err) {
            console.error("Failed to fetch job info:", err);
        }
    }, [cronjobName]);

    useEffect(() => {
        fetchJobInfo();
    }, [cronjobName]);

    const formatLastExecution = (time) => {
        if (!time) return 'Never executed';
        return new Date(time).toLocaleString();
    };

    return (
        <div className="bg-white rounded-lg shadow-sm border border-gray-200 min-h-[500px] flex flex-col">
            <div className="border-b border-gray-200 px-6 py-4">
                <h1 className="text-2xl font-bold text-gray-800">{cronjobName}</h1>
                {jobInfo && (
                    <p className="text-sm text-gray-500 mt-1">
                        Last Execution: <span className="font-medium text-gray-700">{formatLastExecution(jobInfo.lastExecutionTime)}</span>
                    </p>
                )}
            </div>

            <div className="border-b border-gray-200">
                <nav className="flex px-6 space-x-8" aria-label="Tabs">
                    {tabs.map((tab) => (
                        <button
                            key={tab.id}
                            onClick={() => setActiveTab(tab.id)}
                            className={clsx(
                                'whitespace-nowrap py-4 px-1 border-b-2 font-medium text-sm transition-colors duration-200',
                                activeTab === tab.id
                                    ? 'border-blue-500 text-blue-600'
                                    : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                            )}
                        >
                            {tab.label}
                        </button>
                    ))}
                </nav>
            </div>

            <div className="p-6 flex-1">
                {activeTab === 'config' && <JobConfig cronjobName={cronjobName} onActionSuccess={fetchJobInfo} />}
                {activeTab === 'operations' && <JobOperations cronjobName={cronjobName} />}
                {activeTab === 'executions' && <JobExecutionHistory cronjobName={cronjobName} />}
            </div>
        </div>
    );
}
