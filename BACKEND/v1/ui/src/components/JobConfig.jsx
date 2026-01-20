import { useState, useEffect } from 'react';
import { cronjobService } from '../services/cronjobService';
import { useAuth } from '../contexts/AuthContext';
import { getErrorMessage } from '../utils/errorUtils';
import { Play, Square, CalendarClock, Ban, Save } from 'lucide-react';
import clsx from 'clsx';

export default function JobConfig({ cronjobName }) {
    const [config, setConfig] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [success, setSuccess] = useState(null);
    const { user } = useAuth();

    // Local state for editing
    const [poolSize, setPoolSize] = useState('');
    const [expression, setExpression] = useState('');
    const [description, setDescription] = useState('');

    useEffect(() => {
        fetchConfig();
    }, [cronjobName]);

    const fetchConfig = async () => {
        try {
            setLoading(true);
            const response = await cronjobService.getAllCronjobs();
            const job = response.data.find(j => j.cronjobName === cronjobName);
            if (job) {
                setConfig(job);
                setPoolSize(job.poolSize);
                setExpression(job.expression);
            } else {
                setError('Job not found');
            }
        } catch (err) {
            setError('Failed to fetch job config');
        } finally {
            setLoading(false);
        }
    };

    const clearMessages = () => {
        setError(null);
        setSuccess(null);
    };

    const handleUpdatePoolSize = async () => {
        clearMessages();
        try {
            await cronjobService.updatePoolSize(cronjobName, parseInt(poolSize), user?.username || 'UI_USER', description);
            setSuccess('Pool size updated successfully');
            fetchConfig();
        } catch (err) {
            setError(getErrorMessage(err, 'Failed to update pool size'));
        }
    };

    const handleUpdateExpression = async () => {
        clearMessages();
        try {
            await cronjobService.updateExpression(cronjobName, expression, user?.username || 'UI_USER', description);
            setSuccess('Expression updated successfully');
            fetchConfig();
        } catch (err) {
            setError(getErrorMessage(err, 'Failed to update expression'));
        }
    };

    const handleAction = async (actionFn, successMsg) => {
        clearMessages();
        try {
            await actionFn(cronjobName, user?.username || 'UI_USER', description);
            setSuccess(successMsg);
        } catch (err) {
            setError(getErrorMessage(err, 'Action failed'));
        }
    };

    if (loading) return <div>Loading config...</div>;
    if (!config) return <div>Job not found</div>;

    return (
        <div className="space-y-6 max-w-2xl">
            {error && <div className="bg-red-100 text-red-700 p-3 rounded">{error}</div>}
            {success && <div className="bg-green-100 text-green-700 p-3 rounded">{success}</div>}

            <div className="bg-white p-6 rounded shadow-sm border border-gray-200">
                <h3 className="text-lg font-medium mb-4">Configuration</h3>

                <div className="grid gap-4">
                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1">Pool Size</label>
                        <div className="flex gap-2">
                            <input
                                type="number"
                                value={poolSize}
                                onChange={(e) => setPoolSize(e.target.value)}
                                className="flex-1 border border-gray-300 rounded px-3 py-2"
                            />
                            <button
                                onClick={handleUpdatePoolSize}
                                className="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700 flex items-center gap-2"
                            >
                                <Save size={16} /> Update
                            </button>
                        </div>
                    </div>

                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1">Cron Expression</label>
                        <div className="flex gap-2">
                            <input
                                type="text"
                                value={expression}
                                onChange={(e) => setExpression(e.target.value)}
                                className="flex-1 border border-gray-300 rounded px-3 py-2"
                            />
                            <button
                                onClick={handleUpdateExpression}
                                className="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700 flex items-center gap-2"
                            >
                                <Save size={16} /> Update
                            </button>
                        </div>
                    </div>

                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1">Reason/Description for Change</label>
                        <input
                            type="text"
                            value={description}
                            onChange={(e) => setDescription(e.target.value)}
                            className="w-full border border-gray-300 rounded px-3 py-2"
                            placeholder="Why are you making this change?"
                        />
                    </div>
                </div>
            </div>

            <div className="bg-white p-6 rounded shadow-sm border border-gray-200">
                <h3 className="text-lg font-medium mb-4">Operations</h3>
                <div className="flex flex-wrap gap-3">
                    <button
                        onClick={() => handleAction(cronjobService.schedule, 'Job scheduled')}
                        className="flex items-center gap-2 bg-indigo-600 text-white px-4 py-2 rounded hover:bg-indigo-700"
                    >
                        <CalendarClock size={16} /> Schedule
                    </button>
                    <button
                        onClick={() => handleAction(cronjobService.cancel, 'Job cancelled')}
                        className="flex items-center gap-2 bg-orange-600 text-white px-4 py-2 rounded hover:bg-orange-700"
                    >
                        <Ban size={16} /> Cancel
                    </button>
                    <button
                        onClick={() => handleAction(cronjobService.start, 'Job force started')}
                        className="flex items-center gap-2 bg-green-600 text-white px-4 py-2 rounded hover:bg-green-700"
                    >
                        <Play size={16} /> Force Start
                    </button>
                    <button
                        onClick={() => handleAction(cronjobService.stop, 'Job force stopped')}
                        className="flex items-center gap-2 bg-red-600 text-white px-4 py-2 rounded hover:bg-red-700"
                    >
                        <Square size={16} /> Force Stop
                    </button>
                </div>
            </div>
        </div>
    );
}
