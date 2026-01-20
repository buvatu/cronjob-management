import { useState, useEffect, useRef } from 'react';
import { cronjobService } from '../services/cronjobService';
import { getErrorMessage } from '../utils/errorUtils';
import { format } from 'date-fns';
import { RefreshCw } from 'lucide-react';

export default function RunningLog({ cronjobName, execution }) {
    const [logs, setLogs] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const pollingRef = useRef(null);

    useEffect(() => {
        if (!execution) return;

        fetchLogs();

        // Start polling if running
        if (execution.status === 'RUNNING') {
            pollingRef.current = setInterval(fetchLogs, 3000);
        }

        return () => {
            if (pollingRef.current) clearInterval(pollingRef.current);
        };
    }, [cronjobName, execution]);

    const fetchLogs = async () => {
        if (!execution?.id) return;

        // Don't show loading spinner on poll updates to avoid UI flickering
        if (!logs.length) setLoading(true);

        try {
            const res = await cronjobService.getTracingLogs(cronjobName, execution.id);
            setLogs(res.data);
        } catch (err) {
            console.error(err);
            setError(getErrorMessage(err, 'Failed to fetch logs'));
            // Stop polling on error? Maybe not.
        } finally {
            setLoading(false);
        }
    };

    if (!execution) {
        return <div className="text-gray-500 italic">Select an execution from the Job Operations tab to view logs.</div>;
    }

    return (
        <div className="space-y-4">
            <div className="flex justify-between items-center bg-gray-50 p-4 rounded border border-gray-200">
                <div>
                    <h3 className="font-medium text-gray-900">Execution Logs</h3>
                    <div className="text-sm text-gray-500">Instance: {execution.instanceId}</div>
                    <div className="text-sm text-gray-500">Session ID: {execution.id}</div>
                    <div className="text-sm text-gray-500">
                        Status: <span className={execution.status === 'RUNNING' ? 'text-green-600 font-bold' : ''}>{execution.status}</span>
                    </div>
                </div>
                {execution.status === 'RUNNING' && (
                    <div className="flex items-center text-green-600 text-sm animate-pulse">
                        <RefreshCw className="w-4 h-4 mr-1" />
                        Live Updates
                    </div>
                )}
            </div>

            {loading && logs.length === 0 && <div>Loading logs...</div>}

            <div className="bg-gray-900 text-gray-100 p-4 rounded-md font-mono text-sm max-h-[600px] overflow-y-auto">
                {logs.length === 0 && !loading ? (
                    <div className="text-gray-500 italic">No logs found.</div>
                ) : (
                    logs.map((log) => (
                        <div key={log.id} className="mb-1 border-b border-gray-800 pb-1 last:border-0">
                            <div className="flex gap-4">
                                <span className="text-gray-500 whitespace-nowrap">
                                    {log.createdAt ? format(new Date(log.createdAt), 'HH:mm:ss.SSS') : ''}
                                </span>
                                <span className="text-blue-400 font-bold w-32 shrink-0 truncate" title={log.activityName}>
                                    {log.activityName}
                                </span>
                                <span className="text-yellow-500 w-12 text-right shrink-0">
                                    {log.progressValue}%
                                </span>
                                <span className="text-gray-300 w-full text-left shrink-0">
                                    {log.description}
                                </span>
                            </div>
                        </div>
                    ))
                )}
            </div>
        </div>
    );
}
