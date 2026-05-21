export async function requestMock(endpoint, options = {}) {
  return {
    code: 'OK',
    message: 'mock',
    data: {
      endpoint,
      method: options.method ?? 'GET',
    },
  }
}
