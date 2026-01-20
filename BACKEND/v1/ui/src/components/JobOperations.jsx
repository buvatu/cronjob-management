import { useState, useEffect } from 'react';
import { cronjobService } from '../services/cronjobService';
import { format } from 'date-fns';

export default function JobOperations({ cronjobName, onSelectExecution }) {
    const [history, setHistory] = useState([]);
    const [running, setRunning] = useState([]);
    const [activeTab, setActiveTab] = useState('history'); // history | executions
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        fetchData();
    }, [cronjobName, activeTab]);

    const fetchData = async () => {
        setLoading(true);
        try {
            if (activeTab === 'history') {
                const res = await cronjobService.getChangeHistory(cronjobName);
                setHistory(res.data);
            } else {
                const res = await cronjobService.getRunningHistory(cronjobName);
                setRunning(res.data);
            }
        } catch (err) {
            console.error(err);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div>
            <div className="flex space-x-4 mb-4 border-b border-gray-200">
                <button
                    className={`py-2 px-4 ${activeTab === 'history' ? 'border-b-2 border-blue-500 font-bold' : ''}`}
                    onClick={() => setActiveTab('history')}
                >
                    Change History
                </button>
                <button
                    className={`py-2 px-4 ${activeTab === 'executions' ? 'border-b-2 border-blue-500 font-bold' : ''}`}
                    onClick={() => setActiveTab('executions')}
                >
                    Execution History
                </button>
            </div>

            {loading && <div>Loading...</div>}

            {!loading && activeTab === 'history' && (
                <div className="overflow-x-auto">
                    <table className="min-w-full divide-y divide-gray-200">
                        <thead className="bg-gray-50">
                            <tr>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Time</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Operation</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Status</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Executor</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Description / Error</th>
                            </tr>
                        </thead>
                        <tbody className="bg-white divide-y divide-gray-200">
                            {history.map((op) => (
                                <tr key={op.id} className={op.result === 'FAILURE' ? 'bg-red-50' : ''}>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                        {op.executedAt ? format(new Date(op.executedAt), 'yyyy-MM-dd HH:mm:ss') : '-'}
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">{op.operation}</td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm">
                                        <span className={`px-2 py-1 rounded-full text-xs font-bold ${op.result === 'SUCCESS' ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
                                            }`}>
                                            {op.result}
                                        </span>
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{op.executor}</td>
                                    <td className="px-6 py-4 text-sm text-gray-500">
                                        {op.result === 'FAILURE' ? (
                                            <span className="text-red-600 font-medium">{op.errorMessage}</span>
                                        ) : (
                                            op.description
                                        )}
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}

            {!loading && activeTab === 'executions' && (
                <div className="overflow-x-auto">
                    <table className="min-w-full divide-y divide-gray-200">
                        <thead className="bg-gray-50">
                            <tr>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Start Time</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Status</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Duration</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Exit Code</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Action</th>
                            </tr>
                        </thead>
                        <tbody className="bg-white divide-y divide-gray-200">
                            {running.map((exec) => (
                                <tr key={exec.id} className="hover:bg-gray-50">
                                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                        {exec.createdAt ? format(new Date(exec.createdAt), 'yyyy-MM-dd HH:mm:ss') : '-'}
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">{exec.status}</td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{exec.duration}</td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{exec.exitCode}</td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium">
                                        <button
                                            onClick={() => onSelectExecution(exec)}
                                            className="text-indigo-600 hover:text-indigo-900"
                                        >
                                            View Logs
                                        </button>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}
        </div>
    );
}
