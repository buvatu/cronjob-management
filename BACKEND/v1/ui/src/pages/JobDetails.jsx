import { useState } from 'react';
import { useParams } from 'react-router-dom';
import clsx from 'clsx';
import JobConfig from '../components/JobConfig';
import JobOperations from '../components/JobOperations';
import RunningLog from '../components/RunningLog';

export default function JobDetails() {
    const { cronjobName } = useParams();
    const [activeTab, setActiveTab] = useState('config');
    const [selectedExecution, setSelectedExecution] = useState(null);

    const tabs = [
        { id: 'config', label: 'Job Config' },
        { id: 'operations', label: 'Job Operations' },
        { id: 'logs', label: 'Running Log' },
    ];

    const handleSelectExecution = (execution) => {
        setSelectedExecution(execution);
        setActiveTab('logs');
    };

    return (
        <div className="bg-white rounded-lg shadow-sm border border-gray-200 min-h-[500px] flex flex-col">
            <div className="border-b border-gray-200 px-6 py-4 flex justify-between items-center">
                <h1 className="text-2xl font-bold text-gray-800">{cronjobName}</h1>
                <div className="text-sm text-gray-500">
                    {selectedExecution && activeTab === 'logs' ? `Viewing Log: ${selectedExecution.instanceId}` : ''}
                </div>
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
                {activeTab === 'config' && <JobConfig cronjobName={cronjobName} />}
                {activeTab === 'operations' && <JobOperations cronjobName={cronjobName} onSelectExecution={handleSelectExecution} />}
                {activeTab === 'logs' && <RunningLog cronjobName={cronjobName} execution={selectedExecution} />}
            </div>
        </div>
    );
}
