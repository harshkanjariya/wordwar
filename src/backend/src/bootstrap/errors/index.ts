export class ApiError extends Error {
  public statusCode = 404;
  public name = 'ApiError';

  constructor(message: string, statusCode = 404) {
    super(message);
    this.statusCode = statusCode;
  }
}