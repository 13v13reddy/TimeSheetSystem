import React, { useState, useEffect, useCallback } from 'react';
import companyLogo from '../logo.svg'; // Import the logo directly

// API URL constant
const API_BASE_URL = ''; 

/**
 * A single button component for the on-screen keypad.
 */
const KeypadButton = ({ value, onClick, className = '' }) => (
    <button
        type="button"
        onClick={() => onClick(value)}
        style={{ touchAction: 'manipulation' }}
        className={`w-full h-16 text-2xl font-bold text-gray-700 bg-gray-200 rounded-lg shadow-md hover:bg-gray-300 focus:outline-none focus:ring-2 focus:ring-blue-500 transition-all duration-150 ease-in-out active:scale-95 active:bg-gray-400 ${className}`}
    >
        {value}
    </button>
);

/**
 * Component for the PIN-only Employee Kiosk View.
 */
const KioskView = () => {
    const [pin, setPin] = useState('');
    const [message, setMessage] = useState({ text: 'Enter your unique PIN to clock in or out.', type: 'info' });
    const [isLoading, setIsLoading] = useState(false);
    const [currentTime, setCurrentTime] = useState(new Date());
    const [isMessageVisible, setIsMessageVisible] = useState(true);
    const [isPinAnimating, setIsPinAnimating] = useState(false);

    const handleKeyPress = useCallback((value) => {
        if (isLoading) return;
        if (pin.length < 6) {
            setPin(prevPin => prevPin + value);
        }
    }, [isLoading, pin.length]);

    const handleClear = useCallback(() => {
        if (isLoading) return;
        setPin('');
    }, [isLoading]);
    
    const handleDelete = useCallback(() => {
        if (isLoading) return;
        setPin(prevPin => prevPin.slice(0, -1));
    }, [isLoading]);

    const handleSubmit = useCallback(async () => {
        if (!pin || isLoading) return;
        setIsLoading(true);
        setMessage({ text: 'Processing...', type: 'info' });

        try {
            const response = await fetch(`${API_BASE_URL}/api/auth/kiosk/login`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ pin }),
            });
            const data = await response.json();
            if (!response.ok) {
                throw new Error(data.error || 'Clock action failed.');
            }
            
            setMessage({ text: data.message, type: 'success' });
        } catch (error) {
            setMessage({ text: error.message, type: 'error' });
        } finally {
            setIsLoading(false);
            setPin('');
        }
    }, [pin, isLoading]);

    // Effect to update the live clock every second.
    useEffect(() => {
        const timerId = setInterval(() => setCurrentTime(new Date()), 1000);
        return () => clearInterval(timerId);
    }, []);
    
    // Resets the message after a few seconds with a fade-out animation.
    useEffect(() => {
        setIsMessageVisible(true);
        if (message.type === 'success' || message.type === 'error') {
            const timer = setTimeout(() => {
                setIsMessageVisible(false);
                setTimeout(() => {
                    setMessage({ text: 'Enter your unique PIN to clock in or out.', type: 'info' });
                }, 300);
            }, 4000);
            return () => clearTimeout(timer);
        }
    }, [message]);

    // Triggers a brief animation on the PIN display when a digit is added.
    useEffect(() => {
        if(pin.length > 0) {
            setIsPinAnimating(true);
            const timer = setTimeout(() => setIsPinAnimating(false), 100);
            return () => clearTimeout(timer);
        }
    }, [pin]);

    // --- NEW: Keyboard Support ---
    // This effect adds an event listener to handle physical keyboard input.
    useEffect(() => {
        const handleKeyDown = (event) => {
            if (event.key >= '0' && event.key <= '9') {
                handleKeyPress(event.key);
            } else if (event.key === 'Enter') {
                handleSubmit();
            } else if (event.key === 'Backspace') {
                handleDelete();
            } else if (event.key === 'Escape') {
                handleClear();
            }
        };

        window.addEventListener('keydown', handleKeyDown);

        // Cleanup function to remove the event listener when the component unmounts.
        return () => {
            window.removeEventListener('keydown', handleKeyDown);
        };
    }, [handleKeyPress, handleSubmit, handleDelete, handleClear]);

    
    const getMessageColor = (type) => {
        switch (type) {
            case 'success': return 'bg-green-100 border-green-400 text-green-700';
            case 'error': return 'bg-red-100 border-red-400 text-red-700';
            default: return 'bg-blue-100 border-blue-400 text-blue-700';
        }
    };

    return (
        <div className="w-full max-w-sm bg-white rounded-xl shadow-2xl p-6 space-y-4">
            <div className="flex justify-center items-center h-16 mb-4">
                 <img src={companyLogo} alt="Company Logo" className="h-12" />
            </div>

            <div className="text-center">
                <h1 className="text-2xl font-bold text-gray-800">Employee Kiosk</h1>
                <p className="text-lg text-gray-500">{currentTime.toLocaleDateString('en-US', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' })}</p>
                <p className="text-5xl font-mono font-bold tracking-widest text-gray-800 my-2">{currentTime.toLocaleTimeString('en-US', { hour12: false, hour: '2-digit', minute: '2-digit', second: '2-digit' })}</p>
            </div>
            
            <div className={`w-full h-16 bg-gray-800 rounded-lg flex items-center justify-center text-4xl text-white font-mono tracking-widest shadow-inner transition-transform duration-100 ${isPinAnimating ? 'scale-105' : 'scale-100'}`}>
                {pin.replace(/./g, '●')}
            </div>

            <div className={`border px-4 py-3 rounded-md text-center text-sm transition-all duration-300 ease-in-out ${getMessageColor(message.type)} ${isMessageVisible ? 'opacity-100 translate-y-0' : 'opacity-0 -translate-y-2'}`} role="alert">
                <p className="font-semibold">{message.text}</p>
            </div>

            <div className="grid grid-cols-3 gap-3">
                {[...Array(9).keys()].map(i => <KeypadButton key={i + 1} value={String(i + 1)} onClick={handleKeyPress} />)}
                <KeypadButton value="Clear" onClick={handleClear} className="bg-red-200 hover:bg-red-300 col-span-1 text-xl" />
                <KeypadButton value="0" onClick={handleKeyPress} />
                <KeypadButton value="&larr;" onClick={handleDelete} className="text-3xl" />
            </div>
            <button
                type="button"
                onClick={handleSubmit}
                disabled={isLoading || !pin}
                className="w-full p-4 text-white bg-blue-600 rounded-md font-bold text-lg hover:bg-blue-700 disabled:bg-gray-400 transition-all duration-300 ease-in-out transform hover:scale-105"
            >
                Enter
            </button>
        </div>
    );
};

export default KioskView;
