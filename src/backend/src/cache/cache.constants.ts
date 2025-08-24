export const cacheKeys = {
  rolePermissions: (id: string) => 'xk:role_permissions:' + id,
  configuration: 'xk:configuration',
  page: 'xk:pages',
}

export const cacheExpiration = {
  one_day: 24 * 60 * 60, // 1 day
  one_hour: 60 * 60,
  ten_minutes: 10 * 60,
};