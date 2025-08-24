import 'reflect-metadata';
import express, {Express} from 'express';
import cors from 'cors';
import {catchAsync, errorHandler} from './bootstrap/errors/error-handler';
import {authMiddleware} from './middlewares/auth.middleware';
import {responseMiddleware} from './middlewares/response.middleware';
import {setupServices} from './bootstrap/plugins/services';
import * as functions from "firebase-functions";


// Import all routes
import * as authRoutes from "./routes/auth";
import * as usersRoutes from "./routes/users";
import * as configurationsRoutes from "./routes/configurations";
import * as filesRoutes from "./routes/files";
import * as webhooksRoutes from "./routes/webhooks";

// Import admin routes
import * as adminAuthRoutes from "./routes/admin/auth";
import * as adminUsersRoutes from "./routes/admin/users";
import * as adminRolesRoutes from "./routes/admin/roles";
import * as adminAnalyticsRoutes from "./routes/admin/analytics";
import * as adminConfigurationsRoutes from "./routes/admin/configurations";
import * as adminImageLibraryRoutes from "./routes/admin/image-library";
import {FullDocument} from "./types/api";
import {User} from "./types";

declare global {
  namespace Express {
    export interface Request {
      user: FullDocument<User>; // Add `user` property to the request
      permissions?: string[];
      dto?: any;
    }
  }
}


let app: Express;

async function createServer() {
  app = express();

  app.use(cors({
    origin: "*",
    credentials: true,
    methods: ["GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"],
  }));

  app.use(express.json()); // Parse incoming JSON requests
  app.use(express.urlencoded({extended: true})); // Parse URL-encoded form data

  // Health check route
  app.use('/api/health-check', (_, res) => res.send('ok'));

  // Apply auth middleware
  app.use(catchAsync(authMiddleware));

  // Initialize services before other middlewares
  await setupServices();

  // Apply response middleware and error handler
  app.use(catchAsync(responseMiddleware));

  // Register routes
  const routes = [
    authRoutes, usersRoutes, configurationsRoutes,
    filesRoutes, webhooksRoutes,

    adminAuthRoutes, adminUsersRoutes, adminRolesRoutes,
    adminAnalyticsRoutes, adminConfigurationsRoutes,
    adminImageLibraryRoutes,
  ];

  for (const route of routes) {
    if ((route as any)?.default) {
      const prefix = (route.autoPrefix || "").trim();
      app.use("/api" + prefix, (route as any).default); // Register each route with its prefix
    }
  }

  app.use(errorHandler);

  return app;
}

const serverPromise = createServer(); // Initialize once at top level

// Only start the server in local environment
if (process.env.NODE_ENV === 'local') {
  serverPromise.then((server) => {
    const port = parseInt(process.env.PORT || "10000", 10);
    server.listen(port, () => {
      console.log(`Server is running on http://localhost:${port}`);
    });
  }).catch((err) => {
    console.error('Error starting server:', err);
  });
}

export const api = functions
  .https.onRequest({memory: "512MiB", region: 'asia-south1', cors: true}, async (req, res) => {
    const app = await serverPromise;
    app(req, res); // Send the request to the app (express app)
  });
