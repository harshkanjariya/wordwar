const path = require("path");
const nodeExternals = require("webpack-node-externals");

module.exports = {
    mode: "development",
    entry: "./src/index.ts", // Entry point of your Fastify app
    output: {
        path: path.resolve(__dirname, "dist"),
        filename: "main.js",
        libraryTarget: "commonjs2",
    },
    target: "node", // Ensure Webpack knows it's a Node.js app
    externals: [
        nodeExternals({allowlist: [/.*/]}),
        {
            "mongodb": "commonjs mongodb", // Prevent bundling MongoDB
            "mongodb-client-encryption": "commonjs mongodb-client-encryption", // Prevent bundling native module
        },
    ],
    module: {
        rules: [
            {
                test: /\.ts$/,
                use: "ts-loader",
                exclude: /node_modules/,
            },
        ],
    },
    resolve: {
        extensions: [".ts", ".js"],
        alias: {
            // Ensure correct module resolution
            "class-transformer": path.resolve(__dirname, "node_modules/class-transformer"),
            "class-validator": path.resolve(__dirname, "node_modules/class-validator"),
            "reflect-metadata": path.resolve(__dirname, "node_modules/reflect-metadata"),
        },
    },
    optimization: {
        minimize: false, // Disable minification for debugging
    },
};
