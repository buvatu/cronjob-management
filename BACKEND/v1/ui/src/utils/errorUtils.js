/**
 * Extracts a user-friendly error message from an error object (e.g., Axios error).
 * @param {any} err - The error object to extract the message from.
 * @param {string} fallback - The fallback message if no specific message is found.
 * @returns {string} The extracted error message.
 */
export const getErrorMessage = (err, fallback = 'Action failed') => {
    // If it's an axios error with a response from the backend
    if (err.response && err.response.data) {
        // If the backend returned a string directly (like from BusinessException)
        if (typeof err.response.data === 'string') {
            return err.response.data;
        }
        // If the backend returned an object with a message field
        if (err.response.data.message) {
            return err.response.data.message;
        }
    }

    // Fallback to error.message if available, otherwise the provided fallback
    return err.message || fallback;
};
