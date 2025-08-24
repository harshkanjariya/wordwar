import axios from "axios";
import {FullDocument} from "../shared/types/api";
import {Order, PaymentMethod} from "../shared/types/order";
import {repositories} from "../db/repositories";
import {Integration, IntegrationType} from "../shared/types/integrations";
import {ApiError} from "../bootstrap/errors";
import {User} from "../shared/types";
import {DefaultDimensions} from "../shared/utils/constants";

const SHIPROCKET_API_BASE_URL = "https://apiv2.shiprocket.in/v1/external";

// Fetch new Shiprocket token
export async function fetchNewShiprocketToken(username: string, password: string): Promise<string> {
  try {
    const response = await axios.post(`${SHIPROCKET_API_BASE_URL}/auth/login`, {
      email: username,
      password,
    });
    return response.data.token;
  } catch (error) {
    console.log("shiprocket.ts > 20", error)
    throw new ApiError("Failed to fetch new Shiprocket token.", 502);
  }
}

export async function fetchPickupLocations(
  integration: FullDocument<Integration>,
  retry: boolean = false // Prevent infinite loop by retrying only once
): Promise<any[]> {
  try {
    const response = await axios.get(
      `${SHIPROCKET_API_BASE_URL}/settings/company/pickup`,
      {
        headers: { Authorization: `Bearer ${integration.token}` },
      }
    );
    const address = response?.data?.data?.shipping_address || [];
    return address.map((addr: any) => ({
      name: addr?.pickup_location,
      city: addr?.city,
      state: addr?.state,
      country: addr?.country,
      pin_code: addr?.pin_code,
    }));
  } catch (error: any) {
    if (error.response?.status === 401 && !retry) {
      console.warn("Shiprocket token expired. Attempting to fetch a new token...");

      // Ensure username and password are available
      if (!integration.username || !integration.password) {
        throw new ApiError("Token expired and no credentials available to refresh.", 400);
      }

      // Generate a new token
      const newToken = await fetchNewShiprocketToken(integration.username, integration.password);

      // Update the token in the database
      await repositories.integrations.update({ _id: integration._id }, { token: newToken });

      console.log("Token refreshed successfully. Retrying to fetch pickup locations...");

      // Retry fetching pickup locations with the new token
      return fetchPickupLocations({ ...integration, token: newToken }, true);
    }

    console.error("Error fetching pickup locations:", error.message || error.response?.data || error);
    throw new ApiError(
      retry ? "Failed to fetch pickup locations after refreshing the token." : "Failed to fetch pickup locations.",
      error.response?.status || 502
    );
  }
}

// Create Shiprocket order
export async function createShiprocketOrder(order: FullDocument<Order>, integration: FullDocument<Integration>, user: FullDocument<User>, paymentMethod: PaymentMethod) {
  try {
    // Fetch Shiprocket integration details

    if (!integration) {
      throw new ApiError("Shiprocket integration not found for the brand.", 404);
    }

    const { username, password, token } = integration;

    if (!token || (!username || !password)) {
      throw new ApiError("No valid token or credentials available for Shiprocket integration.", 400);
    }

    if (!integration.metadata?.channelId || !integration.metadata?.pickupLocation) {
      throw new ApiError("Channel ID or Pickup Location not set in Shiprocket integration.", 400);
    }

    try {
      // Try creating the order with the existing token
      return await createShiprocketOrderRequest(order, token, user, paymentMethod, integration.metadata);
    } catch (error: any) {
      if (error.response?.status === 401) {
        // Token expired, fetch a new token
        if (!username || !password) {
          throw new ApiError("Shiprocket token expired and no credentials available to refresh.", 400);
        }
        let authToken = await fetchNewShiprocketToken(username, password);

        // Update the token in the database
        await repositories.integrations.update(integration._id, { token: authToken });

        // Retry creating the order with the new token
        return await createShiprocketOrderRequest(order, authToken, user, paymentMethod, integration.metadata);
      } else {
        throw error;
      }
    }
  } catch (error: any) {
    console.error("Error in createShiprocketOrder:", error);
    throw new ApiError("Failed to create Shiprocket order.", 500);
  }
}

async function createShiprocketOrderRequest(order: FullDocument<Order>, token: string, user: FullDocument<User>, paymentMethod: PaymentMethod, metadata: Record<string, any>) {
  try {
    const response = await axios.post(
      `${SHIPROCKET_API_BASE_URL}/orders/create/adhoc`,
      {
        order_id: order._id,
        order_date: new Date(order.createdAt).toISOString(),
        pickup_location: metadata.pickupLocation,
        channel_id: metadata.channelId,
        billing_customer_name: user.name,
        billing_last_name: "",
        billing_address: order.shippingAddress.addressLine1 + " " + order.shippingAddress.addressLine2,
        billing_city: order.shippingAddress.city,
        billing_pincode: order.shippingAddress.postalCode,
        billing_state: order.shippingAddress.state,
        billing_country: order.shippingAddress.country || "India",
        billing_email: user.email,
        billing_phone: user.phoneNumber,
        shipping_is_billing: true,
        order_items: order.items.map((item) => ({
          name: item.name,
          sku: item.productId,
          units: item.quantity,
          selling_price: item.price,
        })),
        payment_method: paymentMethod === PaymentMethod.COD ? "COD" : "Prepaid",
        sub_total: order.totalAmount,
        length: order.dimensions?.length || DefaultDimensions.length,
        breadth: order.dimensions?.breadth || DefaultDimensions.breadth,
        height: order.dimensions?.height || DefaultDimensions.height,
        weight: order.dimensions?.weight || DefaultDimensions.weight,
      },
      {
        headers: { Authorization: `Bearer ${token}` },
      }
    );

    return response.data;
  } catch (error: any) {
    console.error("Error in createShiprocketOrderRequest:", error.message || error.response?.data || error);
    throw new ApiError("Failed to create Shiprocket order.", error.response?.status || 500);
  }
}

// Fetch channels from Shiprocket
export async function fetchShiprocketChannels(
  integration: FullDocument<Integration>,
  retry: boolean = false // Prevent infinite retry loops
): Promise<any[]> {
  try {
    const response = await axios.get(
      `${SHIPROCKET_API_BASE_URL}/channels`,
      {
        headers: { Authorization: `Bearer ${integration.token}` },
      }
    );
    const channels = response?.data?.data || [];
    return channels.map((channel: any) => ({
      id: channel?.id,
      name: channel?.name,
      brand_name: channel?.brand_name,
      status: channel?.status,
      brand_logo: channel?.brand_logo,
    })).filter((channel: any) => channel.id);
  } catch (error: any) {
    if (error.response?.status === 401 && !retry) {
      console.warn("Shiprocket token expired. Attempting to fetch a new token...");

      // Ensure username and password are available
      if (!integration.username || !integration.password) {
        throw new ApiError("Token expired and no credentials available to refresh.", 400);
      }

      // Generate a new token
      const newToken = await fetchNewShiprocketToken(integration.username, integration.password);

      // Update the token in the database
      await repositories.integrations.update({ _id: integration._id }, { token: newToken });

      console.log("Token refreshed successfully. Retrying to fetch channels...");

      // Retry fetching channels with the new token
      return fetchShiprocketChannels({ ...integration, token: newToken }, true);
    }

    console.error("Error fetching channels:", error.message || error.response?.data || error);
    throw new ApiError(
      retry ? "Failed to fetch channels after refreshing the token." : "Failed to fetch channels.",
      error.response?.status || 502
    );
  }
}
