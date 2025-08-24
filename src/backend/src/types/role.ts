export interface Role {
    name: string;
    permissions: string[];
}

export interface Permission {
    name: string;
    value: string;
}

export const PermissionValue = {
    analytics: {
        get: "xk:analytics:view",
    },
    users: {
        update: "xk:users:update",
        delete: "xk:users:delete",
        create: "xk:users:create",
        get: "xk:users:view",
    },
    vendors: {
        update: "xk:vendors:update",
        delete: "xk:vendors:delete",
        create: "xk:vendors:create",
        get: "xk:vendors:view",
    },
    coupons: {
        update: "xk:coupons:update",
        delete: "xk:coupons:delete",
        create: "xk:coupons:create",
        get: "xk:coupons:view",
    },
    products: {
        create: "xk:products:create",
        get: "xk:products:get",
        update: "xk:products:update",
        delete: "xk:products:delete",
        categories: {
            create: "xk:product-categories:create",
            get: "xk:product-categories:get",
            update: "xk:product-categories:update",
            delete: "xk:product-categories:delete",
        },
    },
    brands: {
        create: "xk:brands:create",
        get: "xk:brands:get",
        update: "xk:brands:update",
        delete: "xk:brands:delete",
    },
    orders: {
        create: "xk:orders:create",
        get: "xk:orders:get",
        update: "xk:orders:update",
        delete: "xk:orders:delete",
    },
    payments: {
        get: "xk:payments:get",
        update: "xk:payments:update",
        delete: "xk:payments:delete",
    },
    config: {
        create: "xk:config:create",
        update: "xk:config:update",
        get: "xk:config:get",
        delete: "xk:config:delete",
    },
    roles: {
        create: "xk:roles:create",
        update: "xk:roles:update",
        delete: "xk:roles:delete",
        get: "xk:roles:get",
    },
    assets: {
        list: "xk:assets:list",
        delete: "xk:assets:delete",
    },
    cart: {
        create: "xk:cart:create",
        update: "xk:cart:update",
        delete: "xk:cart:delete",
        get: "xk:cart:get",
    },
    interactions: {
        contactList: "xk:contact:list",
    }
} as const;
