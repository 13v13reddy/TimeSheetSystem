import React, { useState, useEffect, useCallback, useRef } from 'react';

// API URL constant
const API_BASE_URL = '';

// --- Helper Functions & Constants ---
const apiFetch = async (url, options = {}) => {
    const response = await fetch(url, options);
    if (!response.ok) {
        try {
            const errData = await response.json();
            throw new Error(errData.error || `Request failed with status ${response.status}`);
        } catch (e) {
            throw new Error(`Request failed with status ${response.status}`);
        }
    }
    const contentType = response.headers.get("content-type");
    if (contentType && contentType.indexOf("application/json") !== -1) {
        return response.json();
    }
    return response;
};

const downloadFile = async (url, defaultFilename, token) => {
    try {
        const response = await apiFetch(url, { headers: { 'Authorization': `Bearer ${token}` } });
        const blob = await response.blob();
        const link = document.createElement('a');
        link.href = window.URL.createObjectURL(blob);
        link.download = defaultFilename;
        document.body.appendChild(link);
        link.click();
        link.remove();
        window.URL.revokeObjectURL(link.href);
    } catch (err) {
        alert(`Export failed: ${err.message}`);
    }
};

// --- Reusable Modal Components ---
const Modal = ({ children, title }) => (
    <div className="fixed inset-0 bg-black bg-opacity-50 z-30 flex justify-center items-center p-4">
        <div className="bg-white rounded-lg shadow-xl p-6 w-full max-w-md animate-fade-in">
            <h3 className="text-xl font-semibold mb-4 text-gray-800">{title}</h3>
            {children}
        </div>
    </div>
);

const ConfirmModal = ({ message, onConfirm, onCancel }) => (
    <Modal title="Please Confirm">
        <p className="mb-6 text-gray-700">{message}</p>
        <div className="flex justify-end gap-3">
            <button onClick={onCancel} className="px-4 py-2 bg-gray-200 text-gray-800 rounded-md hover:bg-gray-300">Cancel</button>
            <button onClick={onConfirm} className="px-4 py-2 bg-red-600 text-white rounded-md hover:bg-red-700">Confirm</button>
        </div>
    </Modal>
);

const PromptModal = ({ message, onConfirm, onCancel }) => {
    const [inputValue, setInputValue] = useState('');
    const handleSubmit = (e) => {
        e.preventDefault();
        onConfirm(inputValue);
    };
    return (
        <Modal title="Input Required">
            <form onSubmit={handleSubmit}>
                <p className="mb-4 text-gray-700">{message}</p>
                <input type="password" value={inputValue} onChange={(e) => setInputValue(e.target.value)} className="w-full p-2 border rounded mb-6" autoFocus />
                <div className="flex justify-end gap-3">
                    <button type="button" onClick={onCancel} className="px-4 py-2 bg-gray-200 text-gray-800 rounded-md hover:bg-gray-300">Cancel</button>
                    <button type="submit" className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700">Submit</button>
                </div>
            </form>
        </Modal>
    );
};


// --- Child Components for the Dashboard ---

const Header = ({ onLogout, onMenuToggle }) => (
    <div className="flex justify-between items-center p-4 bg-slate-800 text-white shadow-md">
        <h1 className="text-xl font-bold">Admin Dashboard</h1>
        <div className="flex items-center gap-4">
            <button onClick={onLogout} className="px-3 py-1.5 text-sm bg-red-600 rounded-md hover:bg-red-700">Logout</button>
            <button onClick={onMenuToggle} className="md:hidden p-2">
                <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 6h16M4 12h16m-7 6h7"></path></svg>
            </button>
        </div>
    </div>
);

const Sidebar = ({ currentView, setView, isMenuOpen }) => {
    const views = ['Live Status', 'Timesheets', 'User Management', 'Audit Logs', 'Exports'];
    return (
        <aside className={`bg-slate-700 text-slate-200 w-64 space-y-2 p-4 transform ${isMenuOpen ? 'translate-x-0' : '-translate-x-full'} md:translate-x-0 transition-transform duration-300 ease-in-out absolute md:relative z-20 h-full`}>
            {views.map(view => (
                <button
                    key={view}
                    onClick={() => setView(view)}
                    className={`w-full text-left px-4 py-2 rounded-md transition-colors ${currentView === view ? 'bg-slate-900 text-white' : 'hover:bg-slate-600'}`}
                >
                    {view}
                </button>
            ))}
        </aside>
    );
};

const LiveStatusView = ({ users, isLoading, error }) => {
    const clockedInUsers = users.filter(user => user.status === 'Clocked In');
    const formatTimestamp = (timestamp) => {
        if (!timestamp) return 'N/A';
        return new Date(timestamp + 'Z').toLocaleTimeString('en-US', { hour: 'numeric', minute: '2-digit', hour12: true });
    };

    if (isLoading) return <p className="p-8 text-center text-gray-500">Loading live status...</p>;
    if (error) return <p className="p-8 text-center text-red-500">{error}</p>;

    return (
        <div className="p-8">
            <h2 className="text-2xl font-bold text-gray-800 mb-4">Currently Clocked In ({clockedInUsers.length})</h2>
            {clockedInUsers.length > 0 ? (
                <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
                    {clockedInUsers.map(user => (
                        <div key={user.id} className="bg-green-100 border-l-4 border-green-500 text-green-800 p-4 rounded-r-lg shadow">
                            <p className="font-bold truncate">{user.email.split('@')[0]}</p>
                            <p className="text-sm">Clocked in at {formatTimestamp(user.lastActionTimestamp)}</p>
                        </div>
                    ))}
                </div>
            ) : (
                <p className="text-gray-500">No employees are currently clocked in.</p>
            )}
        </div>
    );
};

const TimesheetView = ({ token }) => {
    const [timesheetData, setTimesheetData] = useState([]);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState(null);
    const [weekStartDate, setWeekStartDate] = useState(getMonday(new Date()));

    function getMonday(d) {
        d = new Date(d);
        var day = d.getDay(),
            diff = d.getDate() - day + (day === 0 ? -6 : 1);
        return new Date(d.setDate(diff));
    }

    const loadTimesheet = useCallback(async (date) => {
        try {
            setIsLoading(true);
            const dateString = date.toISOString().split('T')[0];
            const data = await apiFetch(`${API_BASE_URL}/api/admin/timesheets?weekStartDate=${dateString}`, { headers: { 'Authorization': `Bearer ${token}` } });
            setTimesheetData(data);
            setError(null);
        } catch (err) {
            setError(err.message);
        } finally {
            setIsLoading(false);
        }
    }, [token]);

    useEffect(() => {
        loadTimesheet(weekStartDate);
    }, [weekStartDate, loadTimesheet]);

    const changeWeek = (offset) => {
        setWeekStartDate(prevDate => {
            const newDate = new Date(prevDate);
            newDate.setDate(prevDate.getDate() + offset);
            return newDate;
        });
    };
    
    const weekDays = Array.from({ length: 7 }, (_, i) => {
        const day = new Date(weekStartDate);
        day.setDate(weekStartDate.getDate() + i);
        return day;
    });

    const handleDownload = () => {
        const start = new Date(weekStartDate);
        start.setHours(0, 0, 0, 0);
        const end = new Date(start);
        end.setDate(start.getDate() + 6);
        end.setHours(23, 59, 59, 999);

        const query = new URLSearchParams({
            startDate: start.toISOString().slice(0, 19),
            endDate: end.toISOString().slice(0, 19),
        }).toString();
        
        const url = `${API_BASE_URL}/api/admin/timesheets/export?${query}`;
        const filename = `timesheet_export_${start.toISOString().split('T')[0]}_to_${end.toISOString().split('T')[0]}.csv`;
        downloadFile(url, filename, token);
    };

    return (
        <div className="p-8">
            <div className="flex items-center justify-between mb-4">
                <h2 className="text-2xl font-bold text-gray-800">Weekly Timesheet</h2>
                <button onClick={handleDownload} className="px-4 py-2 text-sm text-white bg-blue-600 rounded-md hover:bg-blue-700">Download this Week</button>
            </div>
            <div className="flex items-center justify-between mb-4 bg-gray-100 p-2 rounded-lg">
                <button onClick={() => changeWeek(-7)} className="px-4 py-2 bg-gray-300 rounded">&lt; Prev</button>
                <div className="text-center">
                    <p className="font-semibold">{weekStartDate.toLocaleDateString()}</p>
                    <p className="text-sm text-gray-600">to {new Date(weekStartDate.getTime() + 6 * 24 * 60 * 60 * 1000).toLocaleDateString()}</p>
                </div>
                <button onClick={() => changeWeek(7)} className="px-4 py-2 bg-gray-300 rounded">Next &gt;</button>
            </div>
            {isLoading ? <p>Loading timesheet...</p> : error ? <p className="text-red-500">{error}</p> : (
                <div className="overflow-x-auto">
                    <table className="min-w-full bg-white text-sm">
                        <thead className="bg-gray-200">
                            <tr>
                                <th className="py-2 px-3 text-left">Employee</th>
                                {weekDays.map(day => <th key={day} className="py-2 px-3 text-center">{day.toLocaleDateString('en-US', { weekday: 'short' })}<br/>{day.getDate()}</th>)}
                                <th className="py-2 px-3 text-right font-bold">Total</th>
                            </tr>
                        </thead>
                        <tbody>
                            {timesheetData.map(sheet => (
                                <tr key={sheet.userId} className="border-b">
                                    <td className="py-2 px-3 font-medium">{sheet.userEmail}</td>
                                    {weekDays.map(day => (
                                        <td key={day.toISOString().split('T')[0]} className="py-2 px-3 text-center">
                                            {sheet.dailyHours[day.toISOString().split('T')[0]]?.toFixed(2) || '0.00'}
                                        </td>
                                    ))}
                                    <td className="py-2 px-3 text-right font-bold">{sheet.totalHours.toFixed(2)}</td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}
        </div>
    );
};

const AuditLogView = ({ token }) => {
    const [logData, setLogData] = useState({ content: [], totalPages: 0 });
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState(null);
    const [currentPage, setCurrentPage] = useState(0);

    const loadLogs = useCallback(async (page) => {
        try {
            setIsLoading(true);
            const data = await apiFetch(`${API_BASE_URL}/api/admin/audit-logs?page=${page}&size=15`, { headers: { 'Authorization': `Bearer ${token}` } });
            setLogData(data);
            setError(null);
        } catch (err) {
            setError(err.message);
        } finally {
            setIsLoading(false);
        }
    }, [token]);

    useEffect(() => {
        loadLogs(currentPage);
    }, [currentPage, loadLogs]);

    const formatTimestamp = (timestamp) => {
        if (!timestamp) return 'N/A';
        return new Date(timestamp + 'Z').toLocaleString();
    };

    return (
        <div className="p-8">
            <h2 className="text-2xl font-bold text-gray-800 mb-4">System Audit Logs</h2>
            {isLoading ? <p>Loading logs...</p> : error ? <p className="text-red-500">{error}</p> : (
                <>
                    <div className="overflow-x-auto">
                        <table className="min-w-full bg-white text-sm">
                            <thead className="bg-gray-200">
                                <tr>
                                    <th className="py-2 px-3 text-left">Timestamp</th>
                                    <th className="py-2 px-3 text-left">User</th>
                                    <th className="py-2 px-3 text-left">Action</th>
                                    <th className="py-2 px-3 text-left">Status</th>
                                    <th className="py-2 px-3 text-left">Details</th>
                                </tr>
                            </thead>
                            <tbody>
                                {logData.content.map(log => (
                                    <tr key={log.id} className="border-b">
                                        <td className="py-2 px-3 whitespace-nowrap">{formatTimestamp(log.timestamp)}</td>
                                        <td className="py-2 px-3">{log.userEmail}</td>
                                        <td className="py-2 px-3">{log.action}</td>
                                        <td className="py-2 px-3">{log.status}</td>
                                        <td className="py-2 px-3">{log.details}</td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                    <div className="flex justify-between items-center mt-4">
                        <button onClick={() => setCurrentPage(p => p - 1)} disabled={currentPage === 0} className="px-4 py-2 bg-gray-300 rounded disabled:opacity-50">&lt; Previous</button>
                        <span>Page {currentPage + 1} of {logData.totalPages}</span>
                        <button onClick={() => setCurrentPage(p => p + 1)} disabled={currentPage >= logData.totalPages - 1} className="px-4 py-2 bg-gray-300 rounded disabled:opacity-50">Next &gt;</button>
                    </div>
                </>
            )}
        </div>
    );
};

// --- FIX APPLIED HERE: AddUserForm is now defined before it is used. ---
const AddUserForm = ({ token, onUserAdded, onCancel }) => {
    const [email, setEmail] = useState('');
    const [pin, setPin] = useState('');
    const [role, setRole] = useState('ROLE_EMPLOYEE');
    const [error, setError] = useState('');
    const [isLoading, setIsLoading] = useState(false);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setIsLoading(true);
        setError('');
        try {
            await apiFetch(`${API_BASE_URL}/api/admin/users`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` },
                body: JSON.stringify({ email, pin, role })
            });
            onUserAdded();
        } catch (err) {
            setError(err.message);
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <form onSubmit={handleSubmit} className="my-4 p-4 border rounded-lg bg-gray-50 space-y-3 animate-fade-in">
            <h3 className="text-lg font-semibold text-gray-700">Add New User</h3>
            {error && <p className="text-red-500 bg-red-100 p-2 rounded">{error}</p>}
            <input type="email" value={email} onChange={e => setEmail(e.target.value)} placeholder="User's Email" required className="w-full p-2 border rounded" />
            <input type="password" value={pin} onChange={e => setPin(e.target.value)} placeholder={role === 'ROLE_ADMIN' ? 'Set Initial Password' : 'Set 4-Digit PIN'} required className="w-full p-2 border rounded" />
            <select value={role} onChange={e => setRole(e.target.value)} className="w-full p-2 border rounded bg-white">
                <option value="ROLE_EMPLOYEE">Employee</option>
                <option value="ROLE_ADMIN">Admin</option>
            </select>
            <div className="flex gap-2">
                <button type="submit" disabled={isLoading} className="px-4 py-2 text-white bg-blue-600 rounded-md hover:bg-blue-700 disabled:bg-gray-400">
                    {isLoading ? 'Saving...' : 'Save User'}
                </button>
                <button type="button" onClick={onCancel} className="px-4 py-2 text-gray-700 bg-gray-200 rounded-md hover:bg-gray-300">
                    Cancel
                </button>
            </div>
        </form>
    );
};

const UserManagementView = ({ token, refreshData }) => {
    const [users, setUsers] = useState([]);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState(null);
    const [showAddUser, setShowAddUser] = useState(false);
    const [modalState, setModalState] = useState({ type: null, context: null });

    const loadUsers = useCallback(async () => {
        try {
            setIsLoading(true);
            const userData = await apiFetch(`${API_BASE_URL}/api/admin/users`, { headers: { 'Authorization': `Bearer ${token}` } });
            setUsers(userData);
            setError(null);
        } catch (err) {
            setError(err.message);
        } finally {
            setIsLoading(false);
        }
    }, [token]);

    useEffect(() => { loadUsers(); }, [loadUsers]);
    
    const handleResetPin = (userId, userRole) => {
        setModalState({ type: 'resetPin', context: { userId, userRole } });
    };

    const handleDeleteUser = (userId) => {
        setModalState({ type: 'delete', context: { userId } });
    };
    
    const confirmResetPin = async (newPin) => {
        if (!newPin || newPin.trim() === '') {
            setModalState({ type: null, context: null });
            return;
        }
        const { userId } = modalState.context;
        try {
            await apiFetch(`${API_BASE_URL}/api/admin/users/${userId}/reset-pin`, { method: 'POST', headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` }, body: JSON.stringify({ newPin }) });
            alert("Credentials have been reset successfully.");
        } catch (err) { alert(err.message); }
        finally {
            setModalState({ type: null, context: null });
        }
    };

    const confirmDeleteUser = async () => {
        const { userId } = modalState.context;
        try {
            await apiFetch(`${API_BASE_URL}/api/admin/users/${userId}`, { method: 'DELETE', headers: { 'Authorization': `Bearer ${token}` } });
            loadUsers();
            refreshData();
        } catch (err) { alert(err.message); }
        finally {
            setModalState({ type: null, context: null });
        }
    };

    return (
        <>
            {modalState.type === 'delete' && (
                <ConfirmModal
                    message="Are you sure you want to delete this user? This action cannot be undone."
                    onConfirm={confirmDeleteUser}
                    onCancel={() => setModalState({ type: null, context: null })}
                />
            )}
            {modalState.type === 'resetPin' && (
                <PromptModal
                    message={modalState.context.userRole === 'ROLE_ADMIN' ? "Enter the new PASSWORD for the admin:" : "Enter the new PIN for the employee:"}
                    onConfirm={confirmResetPin}
                    onCancel={() => setModalState({ type: null, context: null })}
                />
            )}
            <div className="p-8">
                <div className="flex justify-between items-center mb-4">
                    <h2 className="text-2xl font-bold text-gray-800">Manage Users</h2>
                    <button onClick={() => setShowAddUser(!showAddUser)} className="px-4 py-2 text-white bg-green-600 rounded-md hover:bg-green-700">
                        {showAddUser ? 'Cancel' : '+ Add User'}
                    </button>
                </div>
                {showAddUser && <AddUserForm token={token} onUserAdded={() => { setShowAddUser(false); loadUsers(); refreshData(); }} onCancel={() => setShowAddUser(false)} />}
                {isLoading ? <p>Loading...</p> : error ? <p className="text-red-500">{error}</p> : (
                     <table className="min-w-full bg-white mt-4">
                        <thead className="bg-gray-200">
                            <tr>
                                <th className="text-left py-2 px-4">Email</th>
                                <th className="text-left py-2 px-4">Role</th>
                                <th className="text-center py-2 px-4">Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            {users.map(user => (
                                <tr key={user.id} className="border-b">
                                    <td className="py-2 px-4">{user.email}</td>
                                    <td className="py-2 px-4">{user.role.replace('ROLE_', '')}</td>
                                    <td className="py-2 px-4 text-center space-x-2">
                                        <button onClick={() => handleResetPin(user.id, user.role)} className="text-sm text-blue-600 hover:underline">Reset Pin</button>
                                        <button onClick={() => handleDeleteUser(user.id)} className="text-sm text-red-600 hover:underline">Delete</button>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                     </table>
                )}
            </div>
        </>
    );
};

const ExportsView = ({ token }) => {
    const [auditStartDate, setAuditStartDate] = useState('');
    const [auditEndDate, setAuditEndDate] = useState('');

    const handleExport = (start, end, type) => {
        const query = new URLSearchParams({
            startDate: start.toISOString().slice(0, 19),
            endDate: end.toISOString().slice(0, 19),
        }).toString();
        
        const url = `${API_BASE_URL}/api/admin/${type}/export?${query}`;
        const filename = `${type}_export_${start.toISOString().split('T')[0]}_to_${end.toISOString().split('T')[0]}.csv`;
        downloadFile(url, filename, token);
    };

    const handleExportCustomRange = (type, startDate, endDate) => {
        if (!startDate || !endDate) {
            alert('Please select both a start and end date.');
            return;
        }
        const start = new Date(startDate);
        start.setHours(0,0,0,0);
        const end = new Date(endDate);
        end.setHours(23,59,59,999);
        handleExport(start, end, type);
    };

    return (
        <div className="p-8 space-y-8">
            <div>
                <h2 className="text-2xl font-bold text-gray-800 mb-4">Export Audit Logs</h2>
                <div className="p-4 border rounded-lg">
                    <div className="flex flex-wrap items-center gap-2">
                        <input type="date" value={auditStartDate} onChange={e => setAuditStartDate(e.target.value)} className="p-2 border rounded"/>
                        <span>to</span>
                        <input type="date" value={auditEndDate} onChange={e => setAuditEndDate(e.target.value)} className="p-2 border rounded"/>
                        <button onClick={() => handleExportCustomRange('audit-logs', auditStartDate, auditEndDate)} disabled={!auditStartDate || !auditEndDate} className="px-4 py-2 text-white bg-purple-600 rounded-md hover:bg-purple-700 disabled:bg-gray-400">Export Audit Log Range</button>
                    </div>
                </div>
            </div>
        </div>
    );
};

const AdminDashboard = ({ token, onLogout }) => {
    const [currentView, setCurrentView] = useState('Live Status');
    const [users, setUsers] = useState([]);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState(null);
    const [isMenuOpen, setIsMenuOpen] = useState(false);
    
    const loadUsers = useCallback(async () => {
        try {
            setIsLoading(true);
            const userData = await apiFetch(`${API_BASE_URL}/api/admin/users/statuses`, { headers: { 'Authorization': `Bearer ${token}` } });
            setUsers(userData);
            setError(null);
        } catch (err) {
            setError(err.message);
        } finally {
            setIsLoading(false);
        }
    }, [token]);

    useEffect(() => { 
        loadUsers();
        const interval = setInterval(loadUsers, 30000);
        return () => clearInterval(interval);
    }, [loadUsers]);

    const renderView = () => {
        switch (currentView) {
            case 'Live Status':
                return <LiveStatusView users={users} isLoading={isLoading} error={error} />;
            case 'Timesheets':
                return <TimesheetView token={token} />;
            case 'User Management':
                return <UserManagementView token={token} refreshData={loadUsers} />;
            case 'Audit Logs':
                return <AuditLogView token={token} />;
            case 'Exports':
                return <ExportsView token={token} />;
            default:
                return <LiveStatusView users={users} isLoading={isLoading} error={error} />;
        }
    };

    return (
        <div className="w-full h-screen bg-slate-100 flex flex-col">
            <Header onLogout={onLogout} onMenuToggle={() => setIsMenuOpen(!isMenuOpen)} />
            <div className="flex flex-1 overflow-hidden">
                <Sidebar currentView={currentView} setView={setCurrentView} isMenuOpen={isMenuOpen} />
                <main className="flex-1 overflow-y-auto">
                    {renderView()}
                </main>
            </div>
        </div>
    );
};

export default AdminDashboard;
