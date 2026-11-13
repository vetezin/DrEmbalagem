// Shared by the native email inputs; backend remains responsible for validation.
export const EMAIL_PATTERN = String.raw`[^\s@]+@(?:[a-zA-Z0-9](?:[a-zA-Z0-9\x2D]{0,61}[a-zA-Z0-9])?\.)+(?:[a-zA-Z]{2,63}|[xX][nN]\x2D\x2D[a-zA-Z0-9](?:[a-zA-Z0-9\x2D]{0,57}[a-zA-Z0-9])?)`;
