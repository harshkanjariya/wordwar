import {AddProductToCartDto, UpdateCartDto} from "../dtos/cart";
import {Cart, User} from "../shared/types";
import {repositories} from "../db/repositories";
import {Filter, ObjectId} from "mongodb";
import {FullDocument} from "../shared/types/api";
import {ApiError} from "../bootstrap/errors";

export async function addProductToCart(body: AddProductToCartDto, user: FullDocument<User>) {
  const product = await repositories.products.findOne({
    filter: {
      _id: new ObjectId(body.productId),
    },
  });

  if (!product) throw new ApiError("Product not found.", 404);

  return repositories.cart.create({
    ...body,
    brandId: product.brandId,
    userId: user._id,
  });
}

export async function removeProductFromCart(id: ObjectId, user: FullDocument<User>) {
  return repositories.cart.deleteOne({
    _id: id,
    userId: user._id,
  });
}

export async function getCartDetails(user: FullDocument<User>) {
  return repositories.cart.findAll({
    filter: {
      userId: user?._id,
    },
  });
}

export async function updateCart(body: UpdateCartDto, user: FullDocument<User>) {
  const filter: Filter<Cart> = {
    productId: body.productId,
    userId: user._id,
  };
  if (body.size) filter.size = body.size;
  if (body.color) filter.color = body.color;

  return repositories.cart.update(filter, {
    quantity: body.quantity,
  });
}