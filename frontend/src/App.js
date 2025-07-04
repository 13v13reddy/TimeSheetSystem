import React, { useState, useEffect } from 'react';

// Import components from their new files
import KioskView from './components/KioskView';
import AdminLoginView from './components/AdminLoginView';
import AdminDashboard from './components/AdminDashboard';

/**
 * A custom hook for simple hash-based navigation.
 * It listens for changes to the URL hash and provides a way to navigate.
 * @returns {{path: string, navigate: function}} - The current path and a navigation function.
 */
const useHashNavigation = () => {
    const [currentPath, setCurrentPath] = useState(window.location.hash || '#/');

    useEffect(() => {
        const handleHashChange = () => {
            setCurrentPath(window.location.hash || '#/');
        };
        window.addEventListener('hashchange', handleHashChange);
        return () => window.removeEventListener('hashchange', handleHashChange);
    }, []);

    return { path: currentPath, navigate: (path) => (window.location.hash = path) };
};


/**
 * The main App component.
 * It acts as a router, determining which view to display based on the URL hash
 * and the user's authentication state.
 */
export default function App() {
    const { path, navigate } = useHashNavigation();
    
    const [token, setToken] = useState(() => {
        try {
            return sessionStorage.getItem('authToken');
        } catch (error) {
            console.warn('Could not access sessionStorage. This is expected in private browsing mode on some browsers.');
            return null;
        }
    });

    // Callback function for when an admin successfully logs in.
    const handleLoginSuccess = (jwtToken) => {
        try {
            sessionStorage.setItem('authToken', jwtToken);
        } catch (error) {
            console.warn('Could not write to sessionStorage.');
        }
        setToken(jwtToken);
        navigate('/dashboard'); // Navigate to the dashboard after login.
    };

    // Callback function for logging out.
    const handleLogout = () => {
        try {
            sessionStorage.removeItem('authToken');
        } catch (error) {
            console.warn('Could not remove item from sessionStorage.');
        }
        setToken(null);
        navigate('/'); // Navigate back to the main kiosk page.
    };
    
    // Simple routing logic
    let view;
    if (path === '#/admin' && !token) {
        view = <AdminLoginView onLoginSuccess={handleLoginSuccess} />;
    } else if (token) {
        view = <AdminDashboard token={token} onLogout={handleLogout} />;
    } else {
        view = <KioskView />;
    }

    return (
        <div className="bg-slate-900 min-h-screen flex flex-col items-center justify-center font-sans p-4">
            {view}
            <footer className="text-center mt-8 text-slate-500 text-sm">
                <p>Version 1.0 &copy; {new Date().getFullYear()} Brahma Reddy Bobba. All Rights Reserved.</p>
                <p>Admin access at <a href="#/admin" className="text-blue-500 hover:underline">/#/admin</a></p>
            </footer>
        </div>
    );
}
