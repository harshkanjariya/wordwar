import {plainToInstance} from "class-transformer";
import {validate} from "class-validator";
import qs from "qs";

export function ValidationPipe(dtoClass: any) {
  return async (request: any, reply: any) => {
    if (request.isMultipart()) return;

    const fields: Record<string, any> = {};
    const files: Record<string, any> = {};

    Object.assign(fields, request.query, request.body, request.params);

    const instance = plainToInstance(dtoClass, qs.parse(fields), {
      enableImplicitConversion: true,
    });

    const errors = await validate(instance as any, {
      whitelist: true,
    });

    if (errors.length > 0) {
      const errorMessage = errors
        .map((err) => Object.values(err.constraints || {}).join(", "))
        .join("; ");

      reply.code(400).send({message: `Validation failed: ${errorMessage}`});
      return; // Stop further execution
    }

    // Attach validated fields and file streams to the request object
    request.validated = instance; // Validated fields
    request.files = files; // File streams
  };
}
