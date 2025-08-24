import {CreateConfigurationDto, UpdateConfigurationDto} from "../dtos/configurations";
import {repositories} from "../db/repositories";
import redisManager from "../cache/redis";
import {cacheExpiration, cacheKeys} from "../cache/cache.constants";
import {ApiError} from "../bootstrap/errors";
import {config} from "../config";

export async function createConfiguration(data: CreateConfigurationDto) {
  // Fetch the configurations JSON from Redis or MongoDB
  const configurations = await getConfigurationValue();

  if (configurations[data.key]) {
    throw new ApiError(`Configuration with key "${data.key}" already exists`, 400);
  }

  // Add configuration to MongoDB
  await repositories.configurations.create(data);

  // Update the configuration in Redis
  configurations[data.key] = data.value;
  await updateConfigurationsInRedis(configurations);

  return {message: `Configuration with key "${data.key}" created successfully.`};
}

export async function upsertConfiguration(data: CreateConfigurationDto) {
  // Fetch the configurations JSON from Redis or MongoDB
  const configurations = await getConfigurationValue();

  if (configurations[data.key]) {
    await repositories.configurations.update({key: data.key}, {value: data.value});
  } else {
    await repositories.configurations.create(data);
  }

  // Update the configuration in Redis
  configurations[data.key] = data.value;
  await updateConfigurationsInRedis(configurations);

  return {message: 'successfully.'};
}

export async function updateConfiguration(key: string, data: UpdateConfigurationDto) {
  // Fetch the configurations JSON from Redis or MongoDB
  const configurations = await getConfigurationValue();

  if (configurations[key] === undefined || configurations[key] === null) {
    throw new ApiError(`Configuration with key "${key}" not found`, 404);
  }

  // Update configuration in MongoDB
  await repositories.configurations.update({key}, data);

  // Update the configuration in Redis
  configurations[key] = data.value;
  await updateConfigurationsInRedis(configurations);

  return {message: `Configuration with key "${key}" updated successfully.`};
}

export async function deleteConfiguration(key: string) {
  // Fetch the configurations JSON from Redis or MongoDB
  const configurations = await getConfigurationValue();

  if (!configurations[key]) {
    throw new ApiError(`Configuration with key "${key}" not found`, 404);
  }

  // Delete configuration from MongoDB
  await repositories.configurations.deleteOne({key});

  // Remove the configuration from Redis
  delete configurations[key];
  await updateConfigurationsInRedis(configurations);

  return {message: `Configuration with key "${key}" deleted successfully.`};
}

export async function getConfigurationValue(key?: string) {
  const result = config.environment === "local" ? await _fetchConfigInJson() : await redisManager.get<Record<string, any>>(
    cacheKeys.configuration,
    cacheExpiration.one_day,
    (cachedValue) => !cachedValue,
    _fetchConfigInJson,
  );
  return key ? result[key] : result;
}

async function _fetchConfigInJson() {
  const allConfigs = await repositories.configurations.findAll({
    limit: 1000,
  });
  return allConfigs.reduce((acc, config) => {
    acc[config.key] = config.value || '';
    return acc;
  }, {} as Record<string, any>);
}

// Utility function to update the configurations JSON in Redis
async function updateConfigurationsInRedis(configurations: Record<string, any>) {
  // Directly cache the JSON object (Redis will serialize it)
  await redisManager.set(cacheKeys.configuration, configurations, cacheExpiration.one_day);
}
