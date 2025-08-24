import { repositories } from "../../db/repositories";
import { ApiError } from "../../bootstrap/errors";
import { Filter, ObjectId } from "mongodb";
import { CreateDeliveryFeeDto, UpdateDeliveryFeeDto } from "../../dtos/delivery-fees";
import { DeliveryFee } from "../../shared/types/brand";
import { FullDocument } from "../../shared/types/api";
import { User, UserType } from "../../shared/types";

/**
 * Get all delivery fees for a specific brand
 * @param brandId ObjectId
 */
export async function getDeliveryFeesByBrand(brandId: ObjectId) {
  const filter: Filter<DeliveryFee> = { brandId };
  return repositories.deliveryFees.findAll({ filter });
}

/**
 * Create a new delivery fee for a brand
 * @param brandId ObjectId
 * @param fee CreateDeliveryFeeDto
 * @param user FullDocument<User>
 */
export async function createDeliveryFee(brandId: ObjectId, fee: CreateDeliveryFeeDto, user: FullDocument<User>) {
  // Authorization check
  if (user.type !== UserType.SUPER_ADMIN && user.type !== UserType.ADMIN) {
    throw new ApiError("Unauthorized", 401);
  }

  const newDeliveryFee: DeliveryFee = {
    ...fee,
    brandId,
  };

  return repositories.deliveryFees.create(newDeliveryFee);
}

/**
 * Update an existing delivery fee
 * @param brandId ObjectId
 * @param feeId ObjectId
 * @param fee UpdateDeliveryFeeDto
 * @param user FullDocument<User>
 */
export async function updateDeliveryFee(
  brandId: ObjectId,
  feeId: ObjectId,
  fee: UpdateDeliveryFeeDto,
  user: FullDocument<User>
) {
  // Authorization check
  if (user.type !== UserType.SUPER_ADMIN && user.type !== UserType.ADMIN) {
    throw new ApiError("Unauthorized", 401);
  }

  const filter: Filter<DeliveryFee> = { _id: feeId, brandId };
  const { _id, ...updateData } = fee as any;

  const updatedFee = await repositories.deliveryFees.update(filter, {
    ...updateData,
    updatedAt: new Date(),
  });

  if (!updatedFee) {
    throw new ApiError("DeliveryFee not found", 404);
  }

  return updatedFee;
}

/**
 * Delete a delivery fee
 * @param brandId ObjectId
 * @param feeId ObjectId
 * @param user FullDocument<User>
 */
export async function deleteDeliveryFee(brandId: ObjectId, feeId: ObjectId, user: FullDocument<User>) {
  // Authorization check
  if (user.type !== UserType.SUPER_ADMIN && user.type !== UserType.ADMIN) {
    throw new ApiError("Unauthorized", 401);
  }

  const filter: Filter<DeliveryFee> = { _id: feeId, brandId };
  const deletedFee = await repositories.deliveryFees.deleteOne(filter);

  if (!deletedFee) {
    throw new ApiError("DeliveryFee not found or not authorized", 404);
  }

  return deletedFee;
}
