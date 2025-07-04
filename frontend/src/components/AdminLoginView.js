import React, { useState } from 'react';

// API URL constant
const API_BASE_URL = '';

/**
 * Component for the hidden Admin Login page.
 * This form is used for administrators to log in with an email and password, not a PIN.
 * @param {object} props - Component props.
 * @param {function(string): void} props.onLoginSuccess - Callback function to execute on successful login, passing the JWT.
 */
const AdminLoginView = ({ onLoginSuccess }) => {
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');
    const [isLoading, setIsLoading] = useState(false);

    // Handles the form submission for the admin login action.
    const handleLogin = async (e) => {
        e.preventDefault();
        setIsLoading(true);
        setError('');
        try {
            const response = await fetch(`${API_BASE_URL}/api/auth/admin/login`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email, password }),
            });
            const data = await response.json();
            if (!response.ok) {
                throw new Error(data.error || 'Login failed.');
            }
            // On success, call the parent component's callback with the received token.
            onLoginSuccess(data.token);
        } catch (err) {
            setError(err.message);
            setIsLoading(false);
        }
    };

    return (
        <div className="w-full max-w-sm bg-white rounded-xl shadow-2xl p-8 space-y-6 animate-fade-in">
            <h1 className="text-2xl font-bold text-center text-gray-800">Administrator Login</h1>
            <form onSubmit={handleLogin} className="space-y-4">
                <div>
                    <label htmlFor="admin-email" className="text-sm font-bold text-gray-600 block sr-only">Admin Email</label>
                    <input id="admin-email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} className="w-full p-3 mt-1 text-gray-800 bg-gray-50 rounded-md border border-gray-300 focus:outline-none focus:ring-2 focus:ring-gray-500 transition" placeholder="Admin Email" required />
                </div>
                <div>
                    <label htmlFor="admin-password" className="text-sm font-bold text-gray-600 block sr-only">Password</label>
                    <input id="admin-password" type="password" value={password} onChange={(e) => setPassword(e.target.value)} className="w-full p-3 mt-1 text-gray-800 bg-gray-50 rounded-md border border-gray-300 focus:outline-none focus:ring-2 focus:ring-gray-500 transition" placeholder="Password" required />
                </div>
                <button type="submit" disabled={isLoading} className="w-full p-3 text-white bg-gray-700 rounded-md font-bold hover:bg-gray-800 disabled:bg-gray-400 transition-colors">
                    {isLoading ? 'Signing In...' : 'Sign In'}
                </button>
            </form>
            {error && <p className="text-red-500 text-center font-semibold">{error}</p>}
        </div>
    );
};

export default AdminLoginView;
