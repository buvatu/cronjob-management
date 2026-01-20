import React, { useEffect, useState } from 'react';
import { NavLink } from 'react-router-dom';
import { cronjobService } from '../services/cronjobService';
import { getErrorMessage } from '../utils/errorUtils';
import { Clock } from 'lucide-react';
import clsx from 'clsx';

export default function Sidebar() {
    const [cronjobs, setCronjobs] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        fetchCronjobs();
    }, []);

    const fetchCronjobs = async () => {
        try {
            const response = await cronjobService.getAllCronjobs();
            setCronjobs(response.data);
        } catch (err) {
            console.error("Failed to fetch cronjobs:", err);
            setError(getErrorMessage(err, "Failed to load cronjobs"));
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="w-64 bg-gray-900 text-white min-h-screen flex flex-col">
            <div className="p-4 bg-gray-800 flex items-center gap-2">
                <Clock className="w-6 h-6 text-blue-400" />
                <h1 className="text-xl font-bold tracking-wider">CronManager</h1>
            </div>

            <div className="flex-1 overflow-y-auto py-4">
                {loading && <div className="text-center text-gray-400">Loading...</div>}
                {error && <div className="text-center text-red-400 px-2">{error}</div>}

                <nav className="space-y-1 px-2">
                    {cronjobs.map((jobName) => (
                        <NavLink
                            key={jobName}
                            to={`/job/${jobName}`}
                            className={({ isActive }) =>
                                clsx(
                                    'block px-4 py-3 rounded-md transition-colors duration-200 text-sm font-medium',
                                    isActive
                                        ? 'bg-blue-600 text-white'
                                        : 'text-gray-300 hover:bg-gray-800 hover:text-white'
                                )
                            }
                        >
                            {jobName}
                        </NavLink>
                    ))}
                </nav>
            </div>

            <div className="p-4 bg-gray-800 border-t border-gray-700">
                <div className="text-xs text-gray-500 text-center">
                    &copy; 2026 Admin Panel
                </div>
            </div>
        </div>
    );
}
