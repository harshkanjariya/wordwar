import {repositories} from "../../db/repositories";
import {CreateIntegrationDto, UpdateIntegrationDto} from "../../dtos/integration";
import {ObjectId} from "mongodb";
import {FullDocument, StatusEnum} from "../../shared/types/api";
import {encrypt} from "../../utils/encryption";
import {config} from "../../config";
import {fetchPickupLocations, fetchShiprocketChannels} from "../../external-apis/shiprocket";
import {ApiError} from "../../bootstrap/errors";
import {Integration} from "../../shared/types/integrations";

export async function getIntegrationsByBrand(brandId: string) {
  return await repositories.integrations.findAll({filter: {brandId: ObjectId.createFromHexString(brandId)}});
}

export async function getIntegrationsDetails(id: string) {
  const integration = await repositories.integrations.findOne({
    filter: {
      _id: ObjectId.createFromHexString(id),
    }
  });

  if (!integration) {
    throw new ApiError("Integration not found.", 404);
  }

  return integration;
}

export async function getShipRocketsDetails(id: string) {
  const integration = await getIntegrationsDetails(id);

  return {
    token: _getToken(integration),
    pickupLocations: await fetchPickupLocations(integration),
    channels: await fetchShiprocketChannels(integration),
  }
}

function _getToken(integration: FullDocument<Integration>) {
  return encrypt({
    integrationId: integration._id,
    brandId: integration.brandId,
  }, config.encryption.webhookKey);
}

export async function createIntegration(data: CreateIntegrationDto) {
  const integration = await repositories.integrations.create({...data, status: StatusEnum.ACTIVE});
  return {
    ...integration,
    token: _getToken(integration),
  }
}

export async function updateIntegration(id: string, data: UpdateIntegrationDto) {
  return await repositories.integrations.update({_id: ObjectId.createFromHexString(id)}, data);
}

export async function deleteIntegration(id: string) {
  return await repositories.integrations.deleteOne({_id: ObjectId.createFromHexString(id)});
}
