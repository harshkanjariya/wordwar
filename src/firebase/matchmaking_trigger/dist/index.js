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
exports.debugLogger = exports.onJoinQueue = void 0;
const admin = __importStar(require("firebase-admin"));
const database_1 = require("firebase-functions/v2/database");
admin.initializeApp();
exports.onJoinQueue = (0, database_1.onValueWritten)({ ref: "/matchmaking_queues/{queueSize}/{userId}" }, (event) => __awaiter(void 0, void 0, void 0, function* () {
    const { before, after } = event.data;
    const { queueSize: matchSize, userId } = event.params;
    console.log("Triggered onJoinQueue", { matchSize, userId });
    if (!before.exists() && after.exists()) {
        console.log("New player joined queue", { userId });
        const queueRef = admin.database().ref(`/matchmaking_queues/${matchSize}`);
        let playersToMatch = [];
        const transactionResult = yield queueRef.transaction((currentQueue) => {
            console.log("Running transaction", { currentQueue });
            if (!currentQueue) {
                console.log("Queue empty, nothing to do");
                return null;
            }
            const ids = Object.keys(currentQueue);
            const size = parseInt(matchSize, 10);
            console.log("Current queue size", { ids, size });
            if (ids.length >= size) {
                playersToMatch = ids.slice(0, size);
                const remaining = ids.slice(size);
                const updated = {};
                for (const id of remaining)
                    updated[id] = currentQueue[id];
                console.log("Formed match", { playersToMatch, remaining });
                return updated;
            }
            console.log("Not enough players to match yet");
            return currentQueue;
        });
        console.log("Transaction result", { committed: transactionResult.committed });
        if (transactionResult.committed && playersToMatch.length) {
            console.log("Pushing live game", { playersToMatch, matchSize });
            yield admin.database().ref("live_games").push({
                players: playersToMatch,
                matchSize: parseInt(matchSize, 10),
            });
        }
    }
    else {
        console.log("Ignored event", { beforeExists: before.exists(), afterExists: after.exists() });
    }
}));
exports.debugLogger = (0, database_1.onValueWritten)({ ref: "/{path=**}" }, // catch ALL writes anywhere in RTDB
(event) => __awaiter(void 0, void 0, void 0, function* () {
    const { before, after } = event.data;
    const { path } = event.params;
    console.log("🔥 debugLogger triggered!");
    console.log("Path:", path);
    console.log("Before value:", before.val());
    console.log("After value:", after.val());
}));
