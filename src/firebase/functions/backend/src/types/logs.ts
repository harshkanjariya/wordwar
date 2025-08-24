export interface ApiCallLog {
  type: ApiCallType;
  body: any;
  success?: boolean;
  statusCode?: number;
  response?: any;
}

export enum ApiCallType {
  CashFreeCreateOrder = "CashFreeCreateOrder",
}

export interface WebhookLog {
  type: WebhookLogType;
  body?: any;
}

export enum WebhookLogType {
  CashFreeStatus = "CashFreeStatus",
  ShiprocketStatus = "ShiprocketStatus",
}
