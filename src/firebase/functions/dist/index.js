"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || (function () {
    var ownKeys = function(o) {
        ownKeys = Object.getOwnPropertyNames || function (o) {
            var ar = [];
            for (var k in o) if (Object.prototype.hasOwnProperty.call(o, k)) ar[ar.length] = k;
            return ar;
        };
        return ownKeys(o);
    };
    return function (mod) {
        if (mod && mod.__esModule) return mod;
        var result = {};
        if (mod != null) for (var k = ownKeys(mod), i = 0; i < k.length; i++) if (k[i] !== "default") __createBinding(result, mod, k[i]);
        __setModuleDefault(result, mod);
        return result;
    };
})();
var __awaiter = (this && this.__awaiter) || function (thisArg, _arguments, P, generator) {
    function adopt(value) { return value instanceof P ? value : new P(function (resolve) { resolve(value); }); }
    return new (P || (P = Promise))(function (resolve, reject) {
        function fulfilled(value) { try { step(generator.next(value)); } catch (e) { reject(e); } }
        function rejected(value) { try { step(generator["throw"](value)); } catch (e) { reject(e); } }
        function step(result) { result.done ? resolve(result.value) : adopt(result.value).then(fulfilled, rejected); }
        step((generator = generator.apply(thisArg, _arguments || [])).next());
    });
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.onJoinQueue = void 0;
const admin = __importStar(require("firebase-admin"));
const database_1 = require("firebase-functions/v2/database");
admin.initializeApp();
exports.onJoinQueue = (0, database_1.onValueCreated)({
    ref: "/matchmaking_queues/{queueSize}/{userId}",
    region: "us-central1",
}, (event) => __awaiter(void 0, void 0, void 0, function* () {
    var _a;
    const { queueSize, userId } = event.params;
    const db = admin.database();
    console.log("Triggered onJoinQueue", { queueSize, userId });
    const parentRef = event.data.ref.parent;
    if (!parentRef) {
        console.error("No parent ref found for event");
        return;
    }
    const size = parseInt(queueSize, 10);
    // 🔒 Transaction to ensure only one function instance succeeds
    const result = yield parentRef.transaction((currentQueue) => {
        console.log("Running transaction with currentQueue:", currentQueue);
        if (!currentQueue) {
            return currentQueue; // no players
        }
        const ids = Object.keys(currentQueue);
        if (ids.length >= size) {
            // Pick first `size` players
            const playersToMatch = ids.slice(0, size);
            // Keep only remaining players in queue
            const updated = {};
            for (const id of ids.slice(size)) {
                updated[id] = currentQueue[id];
            }
            console.log("Formed match", { playersToMatch, remaining: Object.keys(updated) });
            // --- Move the live_games creation logic inside the transaction ---
            // Create the new live_games object
            const playersJson = {};
            for (const playerId of playersToMatch) {
                playersJson[playerId] = {
                    actions: {},
                };
            }
            console.log(playersJson);
            db.ref("live_games").push({
                players: playersJson,
                matchSize: size,
                createdAt: Date.now(),
            });
            return updated; // This commits the change to the queue
        }
        // Not enough players yet
        return currentQueue;
    });
    console.log("Transaction result:", {
        committed: result.committed,
        snapshot: (_a = result.snapshot) === null || _a === void 0 ? void 0 : _a.val(),
    });
}));
