import {repositories} from "../../db/repositories";
import {User, UserType} from "../../shared/types";
import {ApiError} from "../../bootstrap/errors";
import {Filter, ObjectId} from "mongodb";
import {CreateBrandDto, GetBrandsDto, UpdateBrandDto} from "../../dtos/brands";
import {FullDocument, StatusEnum} from "../../shared/types/api";
import {Brand} from "../../shared/types/brand";
import {GetDefaultDeliveryFeesDto} from "../../dtos/delivery-fees";

/**
 * Get the count of all brands
 */
export async function getBrandCount() {
  return repositories.brands.count();
}

/**
 * Create a new brand
 * @param brand CreateBrandDto
 * @param user FullDocument<User>
 */
export async function createBrand(brand: CreateBrandDto, user: FullDocument<User>) {
  if (user.type === UserType.USER) {
    throw new ApiError("Unauthorized", 401);
  }

  return await repositories.brands.create(brand as any);
}

/**
 * Update an existing brand
 * @param id string
 * @param brand UpdateBrandDto
 * @param user FullDocument<User>
 */
export async function updateBrand(id: ObjectId, brand: UpdateBrandDto, user: FullDocument<User>) {
  const isAdmin = user.type === UserType.SUPER_ADMIN || user.type === UserType.ADMIN;

  const filter: Filter<Brand> = {_id: new ObjectId(id)};
  if (!isAdmin) {
    throw new ApiError("Unauthorized", 401);
  }
  const {_id, ...updatableData} = brand as any;

  const updatedBrand = await repositories.brands.update(filter, updatableData);

  if (!updatedBrand) {
    throw new ApiError("Brand not found", 404);
  }

  return updatedBrand;
}

/**
 * Get details of a specific brand
 * @param id string
 * @param user FullDocument<User> | undefined
 */
export async function getBrandDetails(id: string, user: FullDocument<User> | undefined = undefined) {
  const filter: Filter<Brand> = {_id: ObjectId.createFromHexString(id)};
  if (user?.type === UserType.USER) {
    throw new ApiError("Unauthorized", 401);
  }

  const brand = await repositories.brands.findOne({filter});

  if (!brand) {
    throw new ApiError("Brand not found", 404);
  }

  return brand;
}

/**
 * Get a list of brands with filters
 * @param data SearchBrandDto
 */
export async function getBrandList(data: GetBrandsDto) {
  const filter: Filter<Brand> = {
    status: StatusEnum.ACTIVE,
  };
  if (data.brandIds?.length) {
    filter._id = {$in: data.brandIds.map(id => new ObjectId(id))};
  }

  return repositories.brands.findAll({filter});
}

export async function getAdminBrandList(data: GetBrandsDto) {
  const filter: Filter<Brand> = {
    status: { $ne: StatusEnum.DELETED },
  };
  if (data.brandIds?.length) {
    filter._id = { $in: data.brandIds.map(id => new ObjectId(id)) };
  }

  return repositories.brands.findAll({
    filter,
    project: {
      name: 1,
      logoUrl: 1,
      _id: 1,
    }
  });
}

export async function getBrandDetailsClient(id: ObjectId) {
  return repositories.brands.findOne({
    filter: {_id: id},
  });
}

/**
 * Delete a brand
 * @param id string
 * @param user FullDocument<User>
 */
export async function deleteBrand(id: ObjectId, user: FullDocument<User>) {
  const filter: Filter<Brand> = {_id: id};

  const impact = await getBrandImpact(id);

  if (impact?.products?.length || impact?.orders?.length) {
    throw new ApiError("Brand is associated with products or orders", 409);
  }

  const deletedBrand = await repositories.brands.deleteOne(filter);

  if (!deletedBrand) {
    throw new ApiError("Brand not found or not authorized", 404);
  }

  return deletedBrand;
}

export async function getBrandImpact(id: ObjectId) {
  return {
    products: await repositories.products.findAll({
      filter: {
        brandId: id,
        status: { $ne: StatusEnum.DELETED },
      },
      project: {
        _id: 1,
        name: 1,
      },
    }),
    orders: await repositories.order.findAll({
      filter: {
        brandId: id,
      },
      project: {
        userId: 1,
        _id: 1,
      }
    }),
  }
}

export async function getBrandDeliveryFees(id: ObjectId) {
  return repositories.deliveryFees.findAll({
    filter: {
      brandId: id,
    }
  })
}

export async function getBrandDefaultDeliveryFees(query: GetDefaultDeliveryFeesDto) {
  const feeIds = await repositories.brands.findAll({
    filter: {
      _id: {$in: query.brandIds},
    },
    project: {
      defaultDeliveryFeeId: 1,
    }
  });
  return repositories.deliveryFees.findAll({
    filter: {
      _id: {$in: feeIds.map((doc) => doc.defaultDeliveryFeeId).filter(o => !!o)},
    },
  })
}